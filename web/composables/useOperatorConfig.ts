import { fetchWithRetry } from '~/utils/fetchWithRetry'
import type { OperatorConfig } from '~/types'

async function fetchJson<T>(url: string, signal: AbortSignal): Promise<T> {
  const res = await fetchWithRetry(url, 3, 1000, signal)
  if (!res.ok) {
    throw Object.assign(new Error(`HTTP ${res.status} fetching ${url}`), { status: res.status })
  }
  return res.json() as Promise<T>
}

/**
 * Fetch + normalize `config.json` for the given operator.
 *
 * Normalizes hex theme colors to lowercase so SSR style output matches client
 * hydration. Vue's server-renderer lowercases hex values in style attributes,
 * but the client uses the raw value from the Nuxt state — normalizing here
 * makes them consistent.
 */
export async function fetchOperatorConfig(
  cdnBase: string,
  operatorId: string,
  signal: AbortSignal,
): Promise<OperatorConfig> {
  const cfg = await fetchJson<OperatorConfig>(`${cdnBase}/${operatorId}/config.json`, signal)
  if (cfg.theme) {
    cfg.theme.primaryColor = cfg.theme.primaryColor.toLowerCase()
    cfg.theme.accentColor = cfg.theme.accentColor.toLowerCase()
    cfg.theme.textOnPrimary = cfg.theme.textOnPrimary.toLowerCase()
  }
  return cfg
}
