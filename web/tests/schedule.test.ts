import { describe, it, expect, vi, afterEach } from 'vitest'
import { decodeDepartures, getTodayDayGroupKey, parseDayGroup, getDayGroupLabel, getNextServiceDayGroupKey, computeNowMin } from '~/utils/schedule'
import { getStrings } from '~/utils/strings'
import type { ScheduleData } from '~/types'

const mockScheduleData: ScheduleData = {
  operator: { id: 'test', name: 'Test', url: '' },
  lastUpdated: '2026-01-01',
  validUntil: '2026-12-31',
  headsigns: ['Centro', 'Stazione'],
  lineNames: ['3', '7'],
  routeIds: ['route-3', 'route-7'],
  tripIds: ['trip-001', 'trip-002'],
  routes: [
    {
      id: 'route-3',
      name: '3',
      longName: 'Linea 3',
      color: '#FF0000',
      textColor: '#FFFFFF',
      transitType: 'bus',
      directions: [],
    },
    {
      id: 'route-7',
      name: '7',
      longName: 'Linea 7',
      color: '#0000FF',
      textColor: '#FFFFFF',
      transitType: 'bus',
      directions: [],
    },
  ],
  stops: [],
}

describe('decodeDepartures', () => {
  it('decodes minimal compact departure [time, lineIdx, headsignIdx]', () => {
    const compact: (string | number)[][] = [['07:35', 0, 0]]
    const result = decodeDepartures(compact, mockScheduleData)
    expect(result).toHaveLength(1)
    expect(result[0]!.time).toBe('07:35')
    expect(result[0]!.lineName).toBe('3')
    expect(result[0]!.headsign).toBe('Centro')
    expect(result[0]!.color).toBe('#FF0000')
    expect(result[0]!.dock).toBe('')
    expect(result[0]!.tripId).toBeUndefined()
  })

  it('decodes compact departure with dock and tripId', () => {
    const compact: (string | number)[][] = [['08:00', 1, 1, 'A', 0, 1]]
    const result = decodeDepartures(compact, mockScheduleData)
    expect(result[0]!.lineName).toBe('7')
    expect(result[0]!.headsign).toBe('Stazione')
    expect(result[0]!.dock).toBe('A')
    expect(result[0]!.tripId).toBe('trip-002')
  })

  it('calcola minutesFromMidnight correttamente', () => {
    const compact: (string | number)[][] = [['01:30', 0, 0]]
    const result = decodeDepartures(compact, mockScheduleData)
    expect(result[0]!.minutesFromMidnight).toBe(90)
  })

  it('salta righe con indici out-of-bounds', () => {
    const compact: (string | number)[][] = [['07:35', 99, 0]]
    const result = decodeDepartures(compact, mockScheduleData)
    expect(result).toHaveLength(0)
  })

  it('salta righe con indici negativi', () => {
    const compact: (string | number)[][] = [['07:35', -1, 0]]
    const result = decodeDepartures(compact, mockScheduleData)
    expect(result).toHaveLength(0)
  })

  it('applica headsignMap se fornita', () => {
    const compact: (string | number)[][] = [['07:35', 0, 0]]
    const result = decodeDepartures(compact, mockScheduleData, { Centro: 'Center' })
    expect(result[0]!.headsign).toBe('Center')
  })
})

describe('parseDayGroup', () => {
  it('parsa key weekdays', () => {
    const dg = parseDayGroup('mon,tue,wed,thu,fri')
    expect(dg.days).toEqual(['mon', 'tue', 'wed', 'thu', 'fri'])
    expect(dg.displayLabel).toBe('Mon–Fri')
  })

  it('parsa sabato singolo', () => {
    const dg = parseDayGroup('sat')
    expect(dg.days).toEqual(['sat'])
    expect(dg.displayLabel).toBe('Sat')
  })

  it('parsa tutti i giorni', () => {
    const dg = parseDayGroup('mon,tue,wed,thu,fri,sat,sun')
    expect(dg.displayLabel).toBe('Every day')
  })

  it('parsa sab-dom insieme', () => {
    const dg = parseDayGroup('sat,sun')
    expect(dg.displayLabel).toBe('Sat–Sun')
  })
})

describe('getDayGroupLabel', () => {
  const it_strings = getStrings('it')
  const en_strings = getStrings('en')

  it('weekdays: IT → Lun-Ven, EN → Mon-Fri', () => {
    const dg = parseDayGroup('mon,tue,wed,thu,fri')
    expect(getDayGroupLabel(dg, it_strings)).toBe('Lun-Ven')
    expect(getDayGroupLabel(dg, en_strings)).toBe('Mon-Fri')
  })

  it('everyday: IT → Ogni giorno, EN → Every day', () => {
    const dg = parseDayGroup('mon,tue,wed,thu,fri,sat,sun')
    expect(getDayGroupLabel(dg, it_strings)).toBe('Ogni giorno')
    expect(getDayGroupLabel(dg, en_strings)).toBe('Every day')
  })

  it('sat–sun range: IT → Sab–Dom, EN → Sat–Sun', () => {
    const dg = parseDayGroup('sat,sun')
    expect(getDayGroupLabel(dg, it_strings)).toBe('Sab–Dom')
    expect(getDayGroupLabel(dg, en_strings)).toBe('Sat–Sun')
  })

  it('single day: IT → Sab, EN → Sat', () => {
    const dg = parseDayGroup('sat')
    expect(getDayGroupLabel(dg, it_strings)).toBe('Sab')
    expect(getDayGroupLabel(dg, en_strings)).toBe('Sat')
  })

  it('mon-sat (6 days): IT → Lun-Sab, EN → Mon-Sat', () => {
    const dg = parseDayGroup('mon,tue,wed,thu,fri,sat')
    expect(getDayGroupLabel(dg, it_strings)).toBe('Lun-Sab')
    expect(getDayGroupLabel(dg, en_strings)).toBe('Mon-Sat')
  })
})

describe('getTodayDayGroupKey', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('restituisce la key che contiene il giorno corrente', () => {
    const departures: Record<string, (string | number)[][]> = {
      'mon,tue,wed,thu,fri': [['07:00', 0, 0]],
      'sat,sun': [['09:00', 0, 0]],
    }
    vi.spyOn(Date.prototype, 'getDay').mockReturnValue(1) // Monday
    const key = getTodayDayGroupKey(departures)
    expect(key).toBe('mon,tue,wed,thu,fri')
  })

  it('restituisce null se nessuna key matcha', () => {
    const key = getTodayDayGroupKey({})
    expect(key).toBeNull()
  })

  it('usa il timezone fornito se passato', () => {
    // Force Intl to return Wednesday via the 'long' format path
    const origIntl = global.Intl
    const mockFormat = vi.fn().mockReturnValue('Wednesday')
    global.Intl = {
      ...origIntl,
      DateTimeFormat: vi.fn().mockReturnValue({ format: mockFormat }) as any,
    }
    const departures: Record<string, (string | number)[][]> = {
      'mon,tue,wed,thu,fri': [['08:00', 0, 0]],
      'sat,sun': [['10:00', 0, 0]],
    }
    const key = getTodayDayGroupKey(departures as any, 'America/New_York')
    expect(key).toBe('mon,tue,wed,thu,fri')
    global.Intl = origIntl
  })

  it('rimane backward compatible senza timezone', () => {
    const departures: Record<string, (string | number)[][]> = {
      'mon,tue,wed,thu,fri': [['07:00', 0, 0]],
      'sat,sun': [['09:00', 0, 0]],
    }
    vi.spyOn(Date.prototype, 'getDay').mockReturnValue(5) // Friday
    const key = getTodayDayGroupKey(departures)
    expect(key).toBe('mon,tue,wed,thu,fri')
  })

  it('non lancia errori con timezone invalido', () => {
    vi.spyOn(Date.prototype, 'getDay').mockReturnValue(1) // Monday
    const departures: Record<string, (string | number)[][]> = {
      'mon,tue,wed,thu,fri': [['08:00', 0, 0]],
      'sat,sun': [['10:00', 0, 0]],
    }
    // Invalid timezone triggers the catch block; fallback uses getDay() → Monday
    const key = getTodayDayGroupKey(departures as any, 'Invalid/Timezone')
    expect(key).toBe('mon,tue,wed,thu,fri')
    vi.restoreAllMocks()
  })
})

describe('getNextServiceDayGroupKey', () => {
  afterEach(() => { vi.restoreAllMocks() })

  it('returns null when departures is empty', () => {
    expect(getNextServiceDayGroupKey({})).toBeNull()
  })

  it('finds next weekday key when today has no service', () => {
    // Mock today as Monday (index 1)
    vi.spyOn(Date.prototype, 'getDay').mockReturnValue(1)
    const deps = { 'wed': [['08:00', 0, 0]] }
    const key = getNextServiceDayGroupKey(deps)
    expect(key).toBe('wed')
  })

  it('skips today even if today is in a key', () => {
    vi.spyOn(Date.prototype, 'getDay').mockReturnValue(1) // Monday
    const deps = { 'mon': [['08:00', 0, 0]], 'tue': [['09:00', 0, 0]] }
    // today is mon, should return tue (next day)
    const key = getNextServiceDayGroupKey(deps)
    expect(key).toBe('tue')
  })

  it('wraps around week boundary', () => {
    vi.spyOn(Date.prototype, 'getDay').mockReturnValue(6) // Saturday
    const deps = { 'mon': [['08:00', 0, 0]] }
    const key = getNextServiceDayGroupKey(deps)
    expect(key).toBe('mon')
  })
})

describe('computeNowMin', () => {
  it('returns 0 at midnight', () => {
    const ms = new Date('2026-04-03T08:00:00').getTime() // 8:00 AM local
    const result = computeNowMin(ms)
    expect(result).toBe(480) // 8 * 60 = 480
  })

  it('returns 90 for 1:30 AM', () => {
    const ms = new Date('2026-04-03T01:30:00').getTime()
    expect(computeNowMin(ms)).toBe(90)
  })

  it('returns 1439 for 23:59', () => {
    const ms = new Date('2026-04-03T23:59:00').getTime()
    expect(computeNowMin(ms)).toBe(1439)
  })
})
