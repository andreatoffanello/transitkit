import { describe, it, expect } from 'vitest'
import { mergeRealtimeDelays } from '~/composables/useRealtime'
import type { Departure } from '~/types'

const baseDepartures: Departure[] = [
  {
    id: '07:35_3_Centro_',
    time: '07:35',
    lineName: '3',
    routeId: 'route-3',
    headsign: 'Centro',
    color: '#FF0000',
    textColor: '#FFFFFF',
    transitType: 'bus',
    dock: '',
    tripId: 'trip-001',
    minutesFromMidnight: 455,
  },
  {
    id: '08:00_7_Stazione_',
    time: '08:00',
    lineName: '7',
    routeId: 'route-7',
    headsign: 'Stazione',
    color: '#0000FF',
    textColor: '#FFFFFF',
    transitType: 'bus',
    dock: '',
    tripId: 'trip-002',
    minutesFromMidnight: 480,
  },
]

describe('mergeRealtimeDelays', () => {
  it('applies delay to departure with matching tripId', () => {
    const delays: Record<string, number> = { 'trip-001': 120 }
    const result = mergeRealtimeDelays(baseDepartures, delays)
    expect(result[0]?.realtimeDelay).toBe(120)
    expect(result[0]?.isRealtime).toBe(true)
  })

  it('does not modify departure without tripId in delay map', () => {
    const delays: Record<string, number> = {}
    const result = mergeRealtimeDelays(baseDepartures, delays)
    expect(result[0]?.realtimeDelay).toBeUndefined()
    expect(result[0]?.isRealtime).toBeUndefined()
  })

  it('does not mutate the original array', () => {
    const delays: Record<string, number> = { 'trip-001': 60 }
    mergeRealtimeDelays(baseDepartures, delays)
    expect(baseDepartures[0]?.realtimeDelay).toBeUndefined()
  })

  it('only marks departure with delay as isRealtime, others unchanged', () => {
    const delays: Record<string, number> = { 'trip-001': 180 }
    const result = mergeRealtimeDelays(baseDepartures, delays)
    expect(result[1]?.isRealtime).toBeUndefined()
    expect(result[1]?.realtimeDelay).toBeUndefined()
  })

  it('sets isRealtime: true when delay is 0 (on-time, tracked)', () => {
    const delays: Record<string, number> = { 'trip-001': 0 }
    const result = mergeRealtimeDelays(baseDepartures, delays)
    expect(result[0]?.realtimeDelay).toBe(0)
    expect(result[0]?.isRealtime).toBe(true)
  })

  it('handles negative delay (early departure)', () => {
    const delays: Record<string, number> = { 'trip-001': -120 }
    const result = mergeRealtimeDelays(baseDepartures, delays)
    expect(result[0]?.realtimeDelay).toBe(-120)
    expect(result[0]?.isRealtime).toBe(true)
  })

  it('handles departure with no tripId — not marked realtime', () => {
    const noTripDep: Departure[] = [
      {
        id: '09:00_1_Airport_',
        time: '09:00',
        lineName: '1',
        routeId: 'route-1',
        headsign: 'Airport',
        color: '#000',
        textColor: '#FFF',
        transitType: 'bus',
        dock: '',
        minutesFromMidnight: 540,
        // tripId intentionally absent
      },
    ]
    const delays: Record<string, number> = { 'some-trip': 60 }
    const result = mergeRealtimeDelays(noTripDep, delays)
    expect(result[0]?.isRealtime).toBeUndefined()
  })
})
