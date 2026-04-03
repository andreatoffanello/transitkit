import { describe, it, expect, vi, afterEach, beforeEach } from 'vitest'
import { decodeDepartures, getTodayDayGroupKey, parseDayGroup, getDayGroupLabel, getNextServiceDayGroupKey, computeNowMin, getNextDeparture } from '~/utils/schedule'
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

  it('headsignMap: headsign not in map is returned unchanged', () => {
    const compact: (string | number)[][] = [['07:35', 0, 0]]
    // headsignMap only maps 'Stazione' → 'Bahnhof', not 'Centro'
    const result = decodeDepartures(compact, mockScheduleData, { Stazione: 'Bahnhof' })
    expect(result[0]!.headsign).toBe('Centro') // 'Centro' is not in map, pass-through
  })

  it('headsignMap undefined: no crash, original headsign returned', () => {
    const compact: (string | number)[][] = [['07:35', 0, 0]]
    const result = decodeDepartures(compact, mockScheduleData, undefined)
    expect(result[0]!.headsign).toBe('Centro') // undefined map → pass through to original
  })

  it('decodes 4-field compact format [time, lineIdx, headsignIdx, dock]', () => {
    const compact: (string | number)[][] = [['07:35', 0, 0, 'B']]
    const result = decodeDepartures(compact, mockScheduleData)
    expect(result).toHaveLength(1)
    expect(result[0]!.dock).toBe('B')
    expect(result[0]!.tripId).toBeUndefined()
  })

  it('decodes 5-field compact [time, line, headsign, dock, routeIdx] — tripId undefined', () => {
    const compact: (string | number)[][] = [['09:00', 0, 0, 'C', 0]]
    const result = decodeDepartures(compact, mockScheduleData)
    expect(result[0]!.dock).toBe('C')
    expect(result[0]!.tripId).toBeUndefined()
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

describe('getTodayDayGroupKey — timezone', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('Europe/Rome — Monday morning: 09:00 UTC is 10:00 in Rome (same day)', () => {
    // 2024-01-08T09:00:00Z = Monday Jan 8 in UTC; 10:00 in Rome (UTC+1) → still Monday
    vi.setSystemTime(new Date('2024-01-08T09:00:00Z').getTime())
    const departures: Record<string, (string | number)[][]> = {
      'mon,tue,wed,thu,fri': [['08:00', 0, 0]],
      'sat,sun': [['10:00', 0, 0]],
    }
    const key = getTodayDayGroupKey(departures, 'Europe/Rome')
    expect(key).toBe('mon,tue,wed,thu,fri')
  })

  it('Europe/Rome — midnight boundary: 23:30 UTC is 00:30 Tuesday in Rome', () => {
    // 2024-01-08T23:30:00Z = Monday in UTC, but 00:30 on Tuesday Jan 9 in Rome (UTC+1)
    vi.setSystemTime(new Date('2024-01-08T23:30:00Z').getTime())
    const departures: Record<string, (string | number)[][]> = {
      'mon,tue,wed,thu,fri': [['08:00', 0, 0]],
      'sat,sun': [['10:00', 0, 0]],
    }
    // With Europe/Rome → Tuesday → matches 'mon,tue,wed,thu,fri'
    const keyRome = getTodayDayGroupKey(departures, 'Europe/Rome')
    expect(keyRome).toBe('mon,tue,wed,thu,fri')

    // Without timezone (UTC) → Monday → also matches 'mon,tue,wed,thu,fri'
    // but the day name in UTC is still 'mon', so let's verify it returns the weekday key
    const keyUtc = getTodayDayGroupKey(departures)
    expect(keyUtc).toBe('mon,tue,wed,thu,fri')
  })

  it('Europe/Rome — midnight boundary: distinguishes sun from sat at 23:30 UTC on Jan 13', () => {
    // 2024-01-13T23:30:00Z:
    //   - Europe/Rome (UTC+1): 00:30 on Sunday Jan 14 → getDay() returns 0 (sun)
    //   - UTC: still Saturday Jan 13 → but resolveTodayIndex() uses new Date().getDay()
    //     which returns the local day, so on a UTC+1 machine it also returns 0 (sun)
    // To get a clear timezone split, use America/New_York (UTC-5): still Saturday there
    vi.setSystemTime(new Date('2024-01-14T03:30:00Z').getTime())
    // UTC+1 (Rome): 04:30 Sunday Jan 14 → sun
    // America/New_York (UTC-5): 22:30 Saturday Jan 13 → sat
    const departures: Record<string, (string | number)[][]> = {
      'sat': [['10:00', 0, 0]],
      'sun': [['11:00', 0, 0]],
    }
    // Europe/Rome → Sunday → returns 'sun'
    const keyRome = getTodayDayGroupKey(departures, 'Europe/Rome')
    expect(keyRome).toBe('sun')

    // America/New_York → Saturday → returns 'sat'
    const keyNY = getTodayDayGroupKey(departures, 'America/New_York')
    expect(keyNY).toBe('sat')
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
  it('returns 480 for 8:00 AM local', () => {
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

describe('getNextDeparture', () => {
  afterEach(() => vi.restoreAllMocks())

  const stopWithDepartures = {
    ...mockScheduleData,
    stops: [{
      id: 'stop-1',
      name: 'Fermata Test',
      lat: 45.0,
      lng: 11.0,
      lines: [],
      departures: {
        'mon,tue,wed,thu,fri': [
          ['08:00', 0, 0],
          ['12:00', 0, 0],
          ['18:00', 0, 0],
        ],
      },
    }],
  }

  it('returns next departure after nowMs', () => {
    // 2026-04-03 is a Friday (getDay() = 5), matches 'mon,tue,wed,thu,fri'
    const nowMs = new Date('2026-04-03T10:00:00').getTime() // 600 min
    const result = getNextDeparture('stop-1', stopWithDepartures, nowMs)
    expect(result).not.toBeNull()
    expect(result!.time).toBe('12:00')
  })

  it('returns null when stop is not found', () => {
    const nowMs = new Date('2026-04-03T10:00:00').getTime()
    const result = getNextDeparture('nonexistent', stopWithDepartures, nowMs)
    expect(result).toBeNull()
  })

  it('returns null when no departures today (no matching day group)', () => {
    // Stop only has 'sat' departures but today is Monday (getDay() = 1)
    vi.spyOn(Date.prototype, 'getDay').mockReturnValue(1)
    const satOnlyData = {
      ...mockScheduleData,
      stops: [{
        id: 'stop-1',
        name: 'Fermata Test',
        lat: 45.0,
        lng: 11.0,
        lines: [],
        departures: {
          'sat': [
            ['08:00', 0, 0],
            ['12:00', 0, 0],
          ],
        },
      }],
    }
    const nowMs = new Date('2026-04-03T10:00:00').getTime()
    const result = getNextDeparture('stop-1', satOnlyData, nowMs)
    expect(result).toBeNull()
  })

  it('returns null when all departures have passed', () => {
    // 2026-04-03 is a Friday, nowMs = 19:00 (1140 min) → all deps (08:00, 12:00, 18:00) passed
    const nowMs = new Date('2026-04-03T19:00:00').getTime() // 1140 min
    const result = getNextDeparture('stop-1', stopWithDepartures, nowMs)
    expect(result).toBeNull()
  })

  it('returns only future departure when some are in the past', () => {
    // Stop has departures at 08:00 and 14:00; nowMs = 10:00 (600 min)
    // 08:00 (480 min) is past, 14:00 (840 min) is future
    const partialData = {
      ...mockScheduleData,
      stops: [{
        id: 'stop-2',
        name: 'Fermata Parziale',
        lat: 45.0,
        lng: 11.0,
        lines: [],
        departures: {
          'mon,tue,wed,thu,fri': [
            ['08:00', 0, 0],
            ['14:00', 0, 0],
          ],
        },
      }],
    }
    // 2026-04-03 is a Friday → matches 'mon,tue,wed,thu,fri'
    const nowMs = new Date('2026-04-03T10:00:00').getTime() // 600 min
    const result = getNextDeparture('stop-2', partialData, nowMs)
    expect(result).not.toBeNull()
    expect(result!.minutesFromMidnight).toBe(840) // 14:00 = 14 * 60
  })
})
