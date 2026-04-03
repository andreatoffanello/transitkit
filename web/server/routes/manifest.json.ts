import { resolveOperatorId, CDN_BASE } from '~/utils/operators'
import type { OperatorConfig } from '~/types'

const BASE_ICONS = [
  { src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png', purpose: 'any' },
  { src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'any' },
  { src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
  { src: '/icons/icon.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'any' },
]

const SHORTCUTS = [
  {
    name: 'Linee',
    short_name: 'Linee',
    description: 'Vedi tutte le linee',
    url: '/lines',
    icons: [{ src: '/icons/icon-192.png', sizes: '192x192' }],
  },
  {
    name: 'Preferiti',
    short_name: 'Preferiti',
    description: 'Fermate preferite',
    url: '/',
    icons: [{ src: '/icons/icon-192.png', sizes: '192x192' }],
  },
]

const FALLBACK: Record<string, unknown> = {
  name: 'TransitKit',
  short_name: 'Transit',
  theme_color: '#003366',
  description: 'Orari e fermate in tempo reale',
}

export function buildManifest(config: OperatorConfig | null): Record<string, unknown> {
  const base: Record<string, unknown> = {
    id: '/',
    start_url: '/',
    display: 'standalone',
    background_color: '#080C18',
    icons: BASE_ICONS,
    shortcuts: SHORTCUTS,
  }

  if (!config) {
    return { ...base, ...FALLBACK }
  }

  return {
    ...base,
    name: config.fullName ?? config.name,
    short_name: config.name,
    description: `Orari e fermate — ${config.name}`,
    theme_color: config.theme.primaryColor,
    lang: config.locale[0] ?? 'it',
  }
}

export default defineEventHandler(async (event) => {
  const host = getRequestHeader(event, 'host') ?? ''
  const operatorId = resolveOperatorId(host)

  setResponseHeader(event, 'content-type', 'application/manifest+json; charset=utf-8')
  setResponseHeader(event, 'cache-control', 'public, max-age=3600, s-maxage=3600')

  if (!operatorId) {
    return buildManifest(null)
  }

  let config: OperatorConfig | null = null
  try {
    config = await $fetch<OperatorConfig>(`${CDN_BASE}/${operatorId}/config.json`)
  } catch {
    // CDN unreachable — serve generic manifest
  }

  return buildManifest(config)
})
