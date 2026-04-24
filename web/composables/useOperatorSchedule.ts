import { fetchWithRetry } from '~/utils/fetchWithRetry'
import type { ScheduleData, TransitType } from '~/types'

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

export function normalizeSchedules(raw: any): ScheduleData {
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

async function fetchJson<T>(url: string, signal: AbortSignal): Promise<T> {
  const res = await fetchWithRetry(url, 3, 1000, signal)
  if (!res.ok) {
    throw Object.assign(new Error(`HTTP ${res.status} fetching ${url}`), { status: res.status })
  }
  return res.json() as Promise<T>
}

/**
 * Fetch + normalize `schedules.json` for the given operator.
 * Accepts either the web-native format (with `lineNames` index) or the iOS
 * flat-departure format (normalized into web format).
 */
export async function fetchOperatorSchedules(
  cdnBase: string,
  operatorId: string,
  signal: AbortSignal,
): Promise<ScheduleData> {
  const raw = await fetchJson<ScheduleData>(`${cdnBase}/${operatorId}/schedules.json`, signal)
  return normalizeSchedules(raw)
}
