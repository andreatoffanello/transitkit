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

/** Replicates the XML construction logic from the Nitro event handler. */
function buildXml(urls: string[]): string {
  return [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">',
    ...urls.map(url => `  <url><loc>${url}</loc></url>`),
    '</urlset>',
  ].join('\n')
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

  it('empty routes array — still includes home and /lines, no route entries', () => {
    const data: ScheduleData = { ...mockSchedules, routes: [], routeIds: [] }
    const urls = buildSitemapUrls('example.com', data)
    expect(urls).toContain('https://example.com/')
    expect(urls).toContain('https://example.com/lines')
    expect(urls.some(u => u.includes('/lines/'))).toBe(false)
  })

  it('empty stops array — still includes home and /lines, no stop entries', () => {
    const data: ScheduleData = { ...mockSchedules, stops: [] }
    const urls = buildSitemapUrls('example.com', data)
    expect(urls).toContain('https://example.com/')
    expect(urls).toContain('https://example.com/lines')
    expect(urls.some(u => u.includes('/stop/'))).toBe(false)
  })
})

describe('XML output structure', () => {
  it('starts with XML declaration', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const xml = buildXml(urls)
    expect(xml.startsWith('<?xml version="1.0" encoding="UTF-8"?>')).toBe(true)
  })

  it('contains the sitemap urlset namespace', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const xml = buildXml(urls)
    expect(xml).toContain('<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">')
  })

  it('wraps each URL in <url><loc>...</loc></url>', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const xml = buildXml(urls)
    for (const url of urls) {
      expect(xml).toContain(`<url><loc>${url}</loc></url>`)
    }
  })
})
