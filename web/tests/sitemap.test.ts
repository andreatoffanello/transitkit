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
function buildXml(urls: ReturnType<typeof buildSitemapUrls>): string {
  return [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">',
    ...urls.map(({ loc, changefreq, priority, lastmod }) =>
      `  <url>\n    <loc>${loc}</loc>\n    <changefreq>${changefreq}</changefreq>\n    <priority>${priority}</priority>${lastmod ? `\n    <lastmod>${lastmod}</lastmod>` : ''}\n  </url>`,
    ),
    '</urlset>',
  ].join('\n')
}

describe('buildSitemapUrls', () => {
  it('includes home and lines pages', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const locs = urls.map(u => u.loc)
    expect(locs).toContain('https://example.com/')
    expect(locs).toContain('https://example.com/lines')
  })

  it('includes all route pages', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const locs = urls.map(u => u.loc)
    expect(locs).toContain('https://example.com/lines/r1')
  })

  it('includes all stop pages', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const locs = urls.map(u => u.loc)
    expect(locs).toContain('https://example.com/stop/s1')
    expect(locs).toContain('https://example.com/stop/s2')
  })

  it('uses https protocol', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    expect(urls.every(u => u.loc.startsWith('https://'))).toBe(true)
  })

  it('empty routes array — still includes home and /lines, no route entries', () => {
    const data: ScheduleData = { ...mockSchedules, routes: [], routeIds: [] }
    const urls = buildSitemapUrls('example.com', data)
    const locs = urls.map(u => u.loc)
    expect(locs).toContain('https://example.com/')
    expect(locs).toContain('https://example.com/lines')
    expect(locs.some(u => u.includes('/lines/'))).toBe(false)
  })

  it('empty stops array — still includes home and /lines, no stop entries', () => {
    const data: ScheduleData = { ...mockSchedules, stops: [] }
    const urls = buildSitemapUrls('example.com', data)
    const locs = urls.map(u => u.loc)
    expect(locs).toContain('https://example.com/')
    expect(locs).toContain('https://example.com/lines')
    expect(locs.some(u => u.includes('/stop/'))).toBe(false)
  })

  it('stop URLs have changefreq daily and priority 0.9', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const stopUrls = urls.filter(u => u.loc.includes('/stop/'))
    expect(stopUrls.length).toBeGreaterThan(0)
    for (const u of stopUrls) {
      expect(u.changefreq).toBe('daily')
      expect(u.priority).toBe(0.9)
    }
  })

  it('route (lines/:id) URLs have changefreq weekly and priority 0.6', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const routeUrls = urls.filter(u => u.loc.includes('/lines/'))
    expect(routeUrls.length).toBeGreaterThan(0)
    for (const u of routeUrls) {
      expect(u.changefreq).toBe('weekly')
      expect(u.priority).toBe(0.6)
    }
  })

  it('home URL has priority 0.8', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const home = urls.find(u => u.loc === 'https://example.com/')
    expect(home).toBeDefined()
    expect(home!.priority).toBe(0.8)
  })

  it('/lines URL has changefreq weekly and priority 0.7', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const lines = urls.find(u => u.loc === 'https://example.com/lines')
    expect(lines).toBeDefined()
    expect(lines!.changefreq).toBe('weekly')
    expect(lines!.priority).toBe(0.7)
  })

  it('home URL has lastmod set to today (YYYY-MM-DD)', () => {
    const today = new Date().toISOString().split('T')[0]
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const home = urls.find(u => u.loc === 'https://example.com/')
    expect(home).toBeDefined()
    expect(home!.lastmod).toBe(today)
  })

  it('/lines URL has lastmod set to today (YYYY-MM-DD)', () => {
    const today = new Date().toISOString().split('T')[0]
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const lines = urls.find(u => u.loc === 'https://example.com/lines')
    expect(lines).toBeDefined()
    expect(lines!.lastmod).toBe(today)
  })

  it('/lines/:id URLs do NOT have lastmod', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const routeUrls = urls.filter(u => u.loc.includes('/lines/'))
    expect(routeUrls.length).toBeGreaterThan(0)
    for (const u of routeUrls) {
      expect(u.lastmod).toBeUndefined()
    }
  })

  it('/stop/:id URLs do NOT have lastmod', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const stopUrls = urls.filter(u => u.loc.includes('/stop/'))
    expect(stopUrls.length).toBeGreaterThan(0)
    for (const u of stopUrls) {
      expect(u.lastmod).toBeUndefined()
    }
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

  it('wraps each URL in <url>...</url> with loc, changefreq, priority', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const xml = buildXml(urls)
    for (const { loc, changefreq, priority } of urls) {
      expect(xml).toContain(`<loc>${loc}</loc>`)
      expect(xml).toContain(`<changefreq>${changefreq}</changefreq>`)
      expect(xml).toContain(`<priority>${priority}</priority>`)
    }
  })

  it('stop URLs emit <changefreq>daily</changefreq> and <priority>0.9</priority> in XML', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const xml = buildXml(urls)
    expect(xml).toContain('<changefreq>daily</changefreq>')
    expect(xml).toContain('<priority>0.9</priority>')
  })

  it('route URLs emit <changefreq>weekly</changefreq> and <priority>0.6</priority> in XML', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const xml = buildXml(urls)
    expect(xml).toContain('<changefreq>weekly</changefreq>')
    expect(xml).toContain('<priority>0.6</priority>')
  })

  it('home URL emits <priority>0.8</priority> in XML', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const xml = buildXml(urls)
    expect(xml).toContain('<priority>0.8</priority>')
  })

  it('home URL emits <lastmod> with today\'s date in XML', () => {
    const today = new Date().toISOString().split('T')[0]
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const xml = buildXml(urls)
    const homeBlock = xml.split('</url>').find(block => block.includes('<loc>https://example.com/</loc>'))
    expect(homeBlock).toBeDefined()
    expect(homeBlock).toContain(`<lastmod>${today}</lastmod>`)
  })

  it('/lines URL emits <lastmod> with today\'s date in XML', () => {
    const today = new Date().toISOString().split('T')[0]
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const xml = buildXml(urls)
    const linesBlock = xml.split('</url>').find(block => block.includes('<loc>https://example.com/lines</loc>'))
    expect(linesBlock).toBeDefined()
    expect(linesBlock).toContain(`<lastmod>${today}</lastmod>`)
  })

  it('route (lines/:id) URLs do NOT emit <lastmod> in XML', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const xml = buildXml(urls)
    const routeBlocks = xml.split('</url>').filter(block => block.includes('/lines/r'))
    expect(routeBlocks.length).toBeGreaterThan(0)
    for (const block of routeBlocks) {
      expect(block).not.toContain('<lastmod>')
    }
  })

  it('stop URLs do NOT emit <lastmod> in XML', () => {
    const urls = buildSitemapUrls('example.com', mockSchedules)
    const xml = buildXml(urls)
    const stopBlocks = xml.split('</url>').filter(block => block.includes('/stop/'))
    expect(stopBlocks.length).toBeGreaterThan(0)
    for (const block of stopBlocks) {
      expect(block).not.toContain('<lastmod>')
    }
  })
})
