import { resolveOperatorId, CDN_BASE } from '~/utils/operators'
import type { ScheduleData } from '~/types'

export type SitemapUrl = { loc: string; changefreq: string; priority: number; lastmod?: string }

export function buildSitemapUrls(host: string, schedules: ScheduleData): SitemapUrl[] {
  const base = `https://${host}`
  const today = new Date().toISOString().split('T')[0]
  const urls: SitemapUrl[] = [
    { loc: base + '/', changefreq: 'weekly', priority: 0.8, lastmod: today },
    { loc: base + '/lines', changefreq: 'weekly', priority: 0.7, lastmod: today },
  ]
  for (const route of schedules.routes) {
    urls.push({ loc: `${base}/lines/${route.id}`, changefreq: 'weekly', priority: 0.6 })
  }
  for (const stop of schedules.stops) {
    urls.push({ loc: `${base}/stop/${stop.id}`, changefreq: 'daily', priority: 0.9 })
  }
  return urls
}

export default defineEventHandler(async (event) => {
  const host = getRequestHeader(event, 'host') ?? ''
  const operatorId = resolveOperatorId(host)
  if (!operatorId) {
    setResponseStatus(event, 404)
    return 'Not found'
  }

  let schedules: ScheduleData | null = null
  try {
    schedules = await $fetch<ScheduleData>(`${CDN_BASE}/${operatorId}/schedules.json`)
  } catch {
    // CDN unreachable — serve minimal sitemap
  }

  const today = new Date().toISOString().split('T')[0]
  const sitemapUrls: SitemapUrl[] = schedules
    ? buildSitemapUrls(host, schedules)
    : [
        { loc: `https://${host}/`, changefreq: 'weekly', priority: 0.8, lastmod: today },
        { loc: `https://${host}/lines`, changefreq: 'weekly', priority: 0.7, lastmod: today },
      ]

  const xml = [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">',
    ...sitemapUrls.map(({ loc, changefreq, priority, lastmod }) =>
      `  <url>\n    <loc>${loc}</loc>\n    <changefreq>${changefreq}</changefreq>\n    <priority>${priority}</priority>${lastmod ? `\n    <lastmod>${lastmod}</lastmod>` : ''}\n  </url>`,
    ),
    '</urlset>',
  ].join('\n')

  setResponseHeader(event, 'content-type', 'application/xml; charset=utf-8')
  setResponseHeader(event, 'cache-control', 'public, max-age=3600, s-maxage=3600')
  return xml
})
