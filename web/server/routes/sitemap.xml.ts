import { resolveOperatorId, CDN_BASE } from '~/utils/operators'
import type { ScheduleData } from '~/types'

export function buildSitemapUrls(host: string, schedules: ScheduleData): string[] {
  const base = `https://${host}`
  const urls: string[] = [base + '/', base + '/lines']
  for (const route of schedules.routes) {
    urls.push(`${base}/lines/${route.id}`)
  }
  for (const stop of schedules.stops) {
    urls.push(`${base}/stop/${stop.id}`)
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

  const urls = schedules
    ? buildSitemapUrls(host, schedules)
    : [`https://${host}/`, `https://${host}/lines`]

  const xml = [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">',
    ...urls.map(url => `  <url><loc>${url}</loc></url>`),
    '</urlset>',
  ].join('\n')

  setResponseHeader(event, 'content-type', 'application/xml; charset=utf-8')
  setResponseHeader(event, 'cache-control', 'public, max-age=3600, s-maxage=3600')
  return xml
})
