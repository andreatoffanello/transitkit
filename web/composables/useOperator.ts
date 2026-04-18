import { fetchWithRetry } from '~/utils/fetchWithRetry'
import type { OperatorConfig, ScheduleData, TransitType } from '~/types'

// iOS format uses integer transit_type codes; web uses string keys.
const TRANSIT_TYPE_INT_MAP: Record<number, TransitType> = {
  0: 'tram', 1: 'metro', 2: 'rail', 3: 'bus',
  4: 'ferry', 5: 'cable_tram', 6: 'gondola',
  7: 'funicular', 11: 'trolleybus', 12: 'monorail',
}

const FULL_DAY_TO_ABBR: Record<string, string> = {
  monday: 'mon', tuesday: 'tue', wednesday: 'wed', thursday: 'thu',
  friday: 'fri', saturday: 'sat', sunday: 'sun',
}
const DAY_ORDER = ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat']

function normalizeRoutes(routes: any[]): any[] {
  return routes.map((r: any) => ({
    ...r,
    transitType: typeof r.transitType === 'number'
      ? (TRANSIT_TYPE_INT_MAP[r.transitType as number] ?? 'bus')
      : r.transitType,
    directions: (r.directions ?? []).map((d: any) => ({
      ...d,
      id: d.directionId ?? d.id ?? 0,
    })),
  }))
}

function normalizeSchedules(raw: any): ScheduleData {
  // Already web format (has lineNames index array) — just fix routes
  if (Array.isArray(raw.lineNames)) {
    return { ...raw, routes: normalizeRoutes(raw.routes ?? []) }
  }

  // iOS format: flat departure objects per stop, no global index arrays.
  // Build global index arrays first, then convert per-stop departures.
  const lineNameToIdx = new Map<string, number>()
  const headsignToIdx = new Map<string, number>()
  const lineNames: string[] = []
  const routeIds: string[] = []
  const headsigns: string[] = []

  for (const stop of (raw.stops ?? [])) {
    if (!Array.isArray(stop.departures)) continue
    for (const dep of stop.departures) {
      const name = dep.routeName ?? ''
      const rid = dep.routeId ?? ''
      const hs = dep.headsign ?? ''
      if (!lineNameToIdx.has(name)) {
        lineNameToIdx.set(name, lineNames.length)
        lineNames.push(name)
        routeIds.push(rid)
      }
      if (!headsignToIdx.has(hs)) {
        headsignToIdx.set(hs, headsigns.length)
        headsigns.push(hs)
      }
    }
  }

  const convertedStops = (raw.stops ?? []).map((stop: any) => {
    if (!Array.isArray(stop.departures)) return stop
    const groups = new Map<string, (string | number)[][]>()
    for (const dep of stop.departures) {
      const days = ((dep.serviceDays ?? []) as string[])
        .map(d => FULL_DAY_TO_ABBR[d.toLowerCase()] ?? d)
        .sort((a, b) => DAY_ORDER.indexOf(a) - DAY_ORDER.indexOf(b))
      const key = days.join(',')
      const lineIdx = lineNameToIdx.get(dep.routeName ?? '') ?? 0
      const headsignIdx = headsignToIdx.get(dep.headsign ?? '') ?? 0
      const time = String(dep.departureTime ?? '').slice(0, 5)
      if (!groups.has(key)) groups.set(key, [])
      groups.get(key)!.push([time, lineIdx, headsignIdx])
    }
    return { ...stop, departures: Object.fromEntries(groups) }
  })

  return {
    ...raw,
    lineNames,
    routeIds,
    headsigns,
    tripIds: [],
    routes: normalizeRoutes(raw.routes ?? []),
    stops: convertedStops,
  }
}

// Module-level controller: aborts in-flight CDN fetches on rapid re-navigation.
let currentAbortController: AbortController | null = null

async function fetchJson<T>(url: string, signal: AbortSignal): Promise<T> {
  const res = await fetchWithRetry(url, 3, 1000, signal)
  if (!res.ok) {
    throw Object.assign(new Error(`HTTP ${res.status} fetching ${url}`), { status: res.status })
  }
  return res.json() as Promise<T>
}

export async function useOperator() {
  const operatorId = useState<string>('operatorId')
  const id = operatorId.value
  const { public: { cdnBase } } = useRuntimeConfig()

  // Cancel any in-flight fetch session from a previous navigation
  currentAbortController?.abort()
  currentAbortController = new AbortController()
  const { signal } = currentAbortController

  // Use stable keys that don't depend on hostname or dynamic state.
  // The operator ID is scoped per-request via the CDN URL inside the fetcher;
  // using a fixed key ensures the SSR payload key always matches the client
  // hydration key, preventing hydration style mismatches on theme :style bindings.
  const nuxtApp = useNuxtApp()
  const getCachedData = <T>(key: string) =>
    (nuxtApp.payload.data as Record<string, T>)[key] ?? (nuxtApp.static.data as Record<string, T>)[key]

  const configAsync = useAsyncData<OperatorConfig>(
    'operator-config',
    async () => {
      const cfg = await fetchJson<OperatorConfig>(`${cdnBase}/${id}/config.json`, signal)
      // Normalize hex colors to lowercase so SSR style output matches client hydration.
      // Vue's server-renderer lowercases hex values in style attributes, but the client
      // uses the raw value from the Nuxt state — normalizing here makes them consistent.
      if (cfg.theme) {
        cfg.theme.primaryColor = cfg.theme.primaryColor.toLowerCase()
        cfg.theme.accentColor = cfg.theme.accentColor.toLowerCase()
        cfg.theme.textOnPrimary = cfg.theme.textOnPrimary.toLowerCase()
      }
      return cfg
    },
    { getCachedData: key => getCachedData<OperatorConfig>(key) },
  )
  const schedulesAsync = useAsyncData<ScheduleData>(
    'operator-schedules',
    async () => normalizeSchedules(await fetchJson<ScheduleData>(`${cdnBase}/${id}/schedules.json`, signal)),
    { getCachedData: key => getCachedData<ScheduleData>(key) },
  )
  const { data: config, error: configError } = await configAsync
  const { data: schedules, error: schedulesError } = await schedulesAsync

  // If either critical fetch failed, surface a proper error page.
  // AbortError means navigation moved on — silently ignore, no error page.
  if (configError.value || schedulesError.value) {
    const err = configError.value || schedulesError.value
    if (err instanceof Error && err.name === 'AbortError') {
      currentAbortController = null
      const pending = computed(() => !config.value || !schedules.value)
      return { operatorId, config, schedules, pending }
    }
    const status = (err as { status?: number }).status
    const statusCode = status === 404 ? 404 : 502
    const statusMessage = statusCode === 404
      ? 'Operator not found'
      : 'Impossibile caricare i dati. Riprova tra qualche minuto.'
    throw createError({ statusCode, statusMessage })
  }

  currentAbortController = null

  const pending = computed(() => !config.value || !schedules.value)

  return { operatorId, config, schedules, pending }
}
