import { describe, it, expect } from 'vitest'
import type { ScheduleStop } from '~/types'

// Replicate the JSON-LD construction logic from pages/stop/[stopId].vue
function buildBusStopJsonLd(stop: ScheduleStop | null | undefined): string {
  if (!stop) return '{}'
  const data: Record<string, unknown> = {
    '@context': 'https://schema.org',
    '@type': 'BusStop',
    name: stop.name,
    identifier: stop.id,
  }
  if (stop.lat != null && stop.lng != null) {
    data.geo = {
      '@type': 'GeoCoordinates',
      latitude: stop.lat,
      longitude: stop.lng,
    }
  }
  return JSON.stringify(data)
}

describe('JSON-LD BusStop', () => {
  const mockStop: ScheduleStop = {
    id: 'stop-42',
    name: 'Piazza San Marco',
    lat: 45.4342,
    lng: 12.3383,
    lines: ['1', '2'],
    departures: {},
  }

  it('produces @type === BusStop', () => {
    const result = JSON.parse(buildBusStopJsonLd(mockStop))
    expect(result['@type']).toBe('BusStop')
  })

  it('includes @context https://schema.org', () => {
    const result = JSON.parse(buildBusStopJsonLd(mockStop))
    expect(result['@context']).toBe('https://schema.org')
  })

  it('name matches stop.name', () => {
    const result = JSON.parse(buildBusStopJsonLd(mockStop))
    expect(result.name).toBe(mockStop.name)
  })

  it('identifier matches stop.id', () => {
    const result = JSON.parse(buildBusStopJsonLd(mockStop))
    expect(result.identifier).toBe(mockStop.id)
  })

  it('geo.latitude matches stop.lat', () => {
    const result = JSON.parse(buildBusStopJsonLd(mockStop))
    expect(result.geo.latitude).toBe(mockStop.lat)
  })

  it('geo.longitude matches stop.lng', () => {
    const result = JSON.parse(buildBusStopJsonLd(mockStop))
    expect(result.geo.longitude).toBe(mockStop.lng)
  })

  it('geo has @type GeoCoordinates', () => {
    const result = JSON.parse(buildBusStopJsonLd(mockStop))
    expect(result.geo['@type']).toBe('GeoCoordinates')
  })

  it('omits geo when lat is null', () => {
    const stopNoCoords: ScheduleStop = { ...mockStop, lat: null as unknown as number, lng: null as unknown as number }
    const result = JSON.parse(buildBusStopJsonLd(stopNoCoords))
    expect(result.geo).toBeUndefined()
  })

  it('returns {} string when stop is null', () => {
    const result = buildBusStopJsonLd(null)
    expect(result).toBe('{}')
  })

  it('returns {} string when stop is undefined', () => {
    const result = buildBusStopJsonLd(undefined)
    expect(result).toBe('{}')
  })

  it('parses to empty object when stop is null', () => {
    const result = JSON.parse(buildBusStopJsonLd(null))
    expect(result).toEqual({})
  })
})
