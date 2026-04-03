import { resolveOperatorId, CDN_BASE } from '~/utils/operators'
import type { OperatorConfig } from '~/types'

/**
 * Branded og:image SVG — 1200x630
 * Used as the og:image for all pages.
 * Query params (color, name) are accepted as fallback from client-side head tags,
 * but the server also resolves the operator from the host for SSR accuracy.
 */

function escapeXml(str: string): string {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
}

function buildSvg(primaryColor: string, name: string): string {
  const safeColor = /^#(?:[0-9A-Fa-f]{3,4}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$/.test(primaryColor) ? primaryColor : '#003366'
  const safeName = escapeXml(name.slice(0, 80))

  return `<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="630" viewBox="0 0 1200 630">
  <defs>
    <linearGradient id="grad" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#000000"/>
      <stop offset="100%" stop-color="#ffffff"/>
    </linearGradient>
  </defs>
  <rect width="1200" height="630" fill="${safeColor}"/>
  <rect x="0" y="0" width="1200" height="630" fill="url(#grad)" opacity="0.25"/>
  <text
    x="600"
    y="280"
    text-anchor="middle"
    dominant-baseline="middle"
    fill="white"
    font-size="80"
    font-family="system-ui, -apple-system, Helvetica Neue, Arial, sans-serif"
    font-weight="700"
    opacity="0.95"
  >${safeName}</text>
  <text
    x="600"
    y="390"
    text-anchor="middle"
    dominant-baseline="middle"
    fill="white"
    font-size="32"
    font-family="system-ui, -apple-system, Helvetica Neue, Arial, sans-serif"
    font-weight="400"
    opacity="0.65"
  >Orari e linee</text>
</svg>`
}

export default defineEventHandler(async (event) => {
  setResponseHeader(event, 'Content-Type', 'image/svg+xml')
  setResponseHeader(event, 'Cache-Control', 'public, max-age=3600, s-maxage=3600')

  // Try to resolve operator from host first (server-side accuracy)
  const host = getRequestHeader(event, 'host') ?? ''
  const operatorId = resolveOperatorId(host)

  if (operatorId) {
    try {
      const config = await $fetch<OperatorConfig>(`${CDN_BASE}/${operatorId}/config.json`)
      return buildSvg(config.theme.primaryColor, config.fullName ?? config.name)
    } catch {
      // CDN unreachable — fall through to query params
    }
  }

  // Fallback: use query params (client-side head injects ?color=&name=)
  const query = getQuery(event)
  const color = typeof query.color === 'string' ? query.color : '#003366'
  const name = typeof query.name === 'string' ? query.name : 'Transit'

  return buildSvg(color, name)
})
