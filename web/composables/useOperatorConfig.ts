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
  const raw = await fetchJson<Record<string, unknown>>(`${cdnBase}/${operatorId}/config.json`, signal)
  const cfg = raw as unknown as OperatorConfig
  if (cfg.theme) {
    cfg.theme.primaryColor = cfg.theme.primaryColor.toLowerCase()
    cfg.theme.accentColor = cfg.theme.accentColor.toLowerCase()
    cfg.theme.textOnPrimary = cfg.theme.textOnPrimary.toLowerCase()
  }
  // Source configs (shared/operators, iOS bundle) store realtime endpoints under
  // snake_case `gtfs_rt` with an `alerts` key; the web type uses `gtfsRt` with
  // `service_alerts`. Map it so `useRealtime` gets `gtfsRt.trip_updates`.
  const rtRaw = raw.gtfs_rt as Record<string, string> | undefined
  if (!cfg.gtfsRt && rtRaw) {
    cfg.gtfsRt = {
      vehicle_positions: rtRaw.vehicle_positions,
      trip_updates: rtRaw.trip_updates,
      service_alerts: rtRaw.service_alerts ?? rtRaw.alerts,
    }
  }
  return cfg
}
