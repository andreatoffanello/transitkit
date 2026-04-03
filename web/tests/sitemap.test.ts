import { describe, it, expect } from 'vitest'
import { buildSitemapUrls } from '../server/routes/sitemap.xml'
import type { ScheduleData } from '../types'

const mockSchedules: ScheduleData = {
  operator: { id: 'test', name: 'Test Operator', url: 'https://example.com' },
  lastUpdated: '2026-01-01',
  validUntil: '2027-01-01',
  headsigns: [],
  lineNames: [],
  routeIds: ['r1'],
  tripIds: [],
  routes: [
    { id: 'r1', name: '1', longName: '', color: '#fff', textColor: '#000', transitType: 'bus', directions: [] },
  ],
  stops: [
    { id: 's1', name: 'Stop A', lat: 0, lng: 0, lines: [], departures: {} },
    { id: 's2', name: 'Stop B', lat: 0, lng: 0, lines: [], departures: {} },
  ],
}

describe('buildSitemapUrls', () => {
  it('includes home and lines pages', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    expect(urls).toContain('https://example.com/')
    expect(urls).toContain('https://example.com/lines')
  })

  it('includes all route pages', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    expect(urls).toContain('https://example.com/lines/r1')
  })

  it('includes all stop pages', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    expect(urls).toContain('https://example.com/stop/s1')
    expect(urls).toContain('https://example.com/stop/s2')
  })

  it('uses https protocol', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    expect(urls.every(u => u.startsWith('https://'))).toBe(true)
  })
})
