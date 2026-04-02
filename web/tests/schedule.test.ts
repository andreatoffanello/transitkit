import { describe, it, expect, vi, afterEach } from 'vitest'
import { decodeDepartures, getTodayDayGroupKey, parseDayGroup } from '~/utils/schedule'
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
    expect(dg.displayLabel).toBe('Lun–Ven')
  })

  it('parsa sabato singolo', () => {
    const dg = parseDayGroup('sat')
    expect(dg.days).toEqual(['sat'])
    expect(dg.displayLabel).toBe('Sab')
  })

  it('parsa tutti i giorni', () => {
    const dg = parseDayGroup('mon,tue,wed,thu,fri,sat,sun')
    expect(dg.displayLabel).toBe('Ogni giorno')
  })

  it('parsa sab-dom insieme', () => {
    const dg = parseDayGroup('sat,sun')
    expect(dg.displayLabel).toBe('Sab–Dom')
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
})
