import { describe, it, expect } from 'vitest'
import { normalizeSchedules } from '~/composables/useOperatorSchedule'
import { decodeDepartures, reconstructTrip } from '~/utils/schedule'

// iOS-format raw schedule: flat per-stop departure objects carrying tripId.
// This is the shape served by the real CDN (e.g. appalcart). The normalizer
// MUST preserve tripId — the trip-detail page is dead without it (regression
// guard for the dropped-tripId bug).
const rawIosFormat = {
  operator: { id: 'op', name: 'Op', url: '' },
  lastUpdated: '2026-01-01',
  routes: [
    {
      id: 'r8', name: 'O', longName: 'Orange', color: 'DF9245',
      textColor: '', transitType: 3, directions: [],
    },
  ],
  stops: [
    {
      id: 'a', name: 'Stop A', lat: 1, lng: 1, gtfsStopIds: ['1'],
      departures: [
        { tripId: '647', routeId: 'r8', routeName: 'O', headsign: 'Stop C', departureTime: '08:00:00', serviceDays: ['monday'] },
      ],
    },
    {
      id: 'b', name: 'Stop B', lat: 2, lng: 2, gtfsStopIds: ['2'],
      departures: [
        { tripId: '647', routeId: 'r8', routeName: 'O', headsign: 'Stop C', departureTime: '08:05:00', serviceDays: ['monday'] },
      ],
    },
    {
      id: 'c', name: 'Stop C', lat: 3, lng: 3, gtfsStopIds: ['3'],
      departures: [
        { tripId: '647', routeId: 'r8', routeName: 'O', headsign: 'Stop C', departureTime: '08:12:00', serviceDays: ['monday'] },
      ],
    },
  ],
}

describe('normalizeSchedules — tripId preservation', () => {
  it('builds a tripIds index from iOS-format departures', () => {
    const data = normalizeSchedules(structuredClone(rawIosFormat))
    expect(data.tripIds).toEqual(['647'])
  })

  it('emits compact tuples whose index 5 resolves to the tripId', () => {
    const data = normalizeSchedules(structuredClone(rawIosFormat))
    const stopA = data.stops.find(s => s.id === 'a')!
    const compact = Object.values(stopA.departures)[0]![0]!
    expect(compact.length).toBeGreaterThan(5)
    expect(data.tripIds[Number(compact[5])]).toBe('647')
  })

  it('decodeDepartures surfaces the tripId on the resolved Departure', () => {
    const data = normalizeSchedules(structuredClone(rawIosFormat))
    const stopA = data.stops.find(s => s.id === 'a')!
    const deps = decodeDepartures(Object.values(stopA.departures)[0]!, data)
    expect(deps[0]!.tripId).toBe('647')
  })
})

describe('reconstructTrip', () => {
  it('rebuilds the ordered stop sequence with per-stop times', () => {
    const data = normalizeSchedules(structuredClone(rawIosFormat))
    const trip = reconstructTrip('647', data)
    expect(trip).not.toBeNull()
    expect(trip!.rows.map(r => r.stop.id)).toEqual(['a', 'b', 'c'])
    expect(trip!.rows.map(r => r.time)).toEqual(['08:00', '08:05', '08:12'])
    expect(trip!.headsign).toBe('Stop C')
    expect(trip!.lineName).toBe('O')
  })

  it('returns null for an unknown tripId', () => {
    const data = normalizeSchedules(structuredClone(rawIosFormat))
    expect(reconstructTrip('does-not-exist', data)).toBeNull()
  })
})
