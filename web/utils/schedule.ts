import type { ScheduleData, Departure, DayGroup, Route } from '~/types'
import type { AppStrings } from '~/utils/strings'

const WEEKDAY_ABBR = ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'] as const
const WEEKDAY_LABELS: Record<string, string> = {
  mon: 'Mon', tue: 'Tue', wed: 'Wed', thu: 'Thu',
  fri: 'Fri', sat: 'Sat', sun: 'Sun',
}

export function parseDayGroup(key: string): DayGroup {
  const days = key.split(',').map(d => d.trim())
  return {
    id: key,
    days,
    displayLabel: buildDisplayLabel(days),
  }
}

function buildDisplayLabel(days: string[]): string {
  if (days.length === 7) return 'Every day'
  if (days.length === 5 && days.every(d => !['sat', 'sun'].includes(d))) return 'Mon–Fri'
  if (days.length === 2 && days.includes('sat') && days.includes('sun')) return 'Sat–Sun'
  if (days.length === 1) return WEEKDAY_LABELS[days[0] ?? ''] ?? days[0] ?? ''
  return days.map(d => WEEKDAY_LABELS[d] ?? d).join(', ')
}

const DAY_KEY_MAP: Record<string, keyof AppStrings['weekdayLabels']> = {
  mon: 'mon', tue: 'tue', wed: 'wed', thu: 'thu',
  fri: 'fri', sat: 'sat', sun: 'sun',
}

export function getDayGroupLabel(dayGroup: DayGroup, strings: AppStrings): string {
  const { days } = dayGroup
  if (days.length === 7) return strings.weekdayGroupNames.everyday
  if (days.length === 5 && !days.includes('sat') && !days.includes('sun')) {
    return strings.weekdayGroupNames.weekdays
  }
  if (days.length === 2 && days.includes('sat') && days.includes('sun')) {
    return `${strings.weekdayLabels.sat}–${strings.weekdayLabels.sun}`
  }
  if (days.length === 1) {
    const key = DAY_KEY_MAP[days[0] ?? '']
    return key ? strings.weekdayLabels[key] : (days[0] ?? '')
  }
  if (days.length === 6 && !days.includes('sun')) {
    return strings.weekdayGroupNames.weekdaysSat
  }
  // Fallback: first–last abbreviated
  const sorted = [...days].sort((a, b) =>
    (WEEKDAY_ABBR.indexOf(a as typeof WEEKDAY_ABBR[number]) ?? 0) -
    (WEEKDAY_ABBR.indexOf(b as typeof WEEKDAY_ABBR[number]) ?? 0),
  )
  const firstKey = DAY_KEY_MAP[sorted[0] ?? '']
  const lastKey = DAY_KEY_MAP[sorted[sorted.length - 1] ?? '']
  const first = firstKey ? strings.weekdayLabels[firstKey] : (sorted[0] ?? '')
  const last = lastKey ? strings.weekdayLabels[lastKey] : (sorted[sorted.length - 1] ?? '')
  return `${first}–${last}`
}

function resolveTodayIndex(timezone?: string): number {
  if (timezone) {
    try {
      const formatter = new Intl.DateTimeFormat('en-US', {
        timeZone: timezone,
        weekday: 'long',
      })
      const dayStr = formatter.format(new Date()).toLowerCase().slice(0, 3)
      const idx = WEEKDAY_ABBR.indexOf(dayStr as typeof WEEKDAY_ABBR[number])
      return idx !== -1 ? idx : new Date().getDay()
    } catch {
      return new Date().getDay()
    }
  }
  return new Date().getDay()
}

export function getTodayDayGroupKey(
  departures: Record<string, (string | number)[][]>,
  timezone?: string,
): string | null {
  const dayIndex = resolveTodayIndex(timezone)
  const todayAbbr = WEEKDAY_ABBR[dayIndex]
  for (const key of Object.keys(departures)) {
    if (key.split(',').includes(todayAbbr ?? '')) return key
  }
  return null
}

/**
 * Given the departures map and a reference date, find the next day group key
 * that has service (i.e., the nearest upcoming day after today that appears
 * in the departures map). Returns null if no future day group is found.
 */
export function getNextServiceDayGroupKey(
  departures: Record<string, (string | number)[][]>,
  timezone?: string,
): string | null {
  // Find today's index
  const todayIndex = resolveTodayIndex(timezone)

  // Search the next 7 days (skip today)
  for (let offset = 1; offset <= 7; offset++) {
    const nextIndex = (todayIndex + offset) % 7
    const nextAbbr = WEEKDAY_ABBR[nextIndex]
    for (const key of Object.keys(departures)) {
      if (key.split(',').includes(nextAbbr ?? '')) return key
    }
  }
  return null
}

export function decodeDepartures(
  compactDeps: (string | number)[][],
  data: ScheduleData,
  headsignMap?: Record<string, string>,
): Departure[] {
  const result: Departure[] = []
  const routeMap = new Map<string, Route>(data.routes.map(r => [r.id, r]))
  let depIdx = 0

  for (const compact of compactDeps) {
    if (compact.length < 3) continue

    const time = String(compact[0])
    const lineIdx = Number(compact[1])
    const headsignIdx = Number(compact[2])

    if (
      lineIdx < 0 ||
      lineIdx >= data.lineNames.length ||
      lineIdx >= data.routeIds.length ||
      headsignIdx < 0 ||
      headsignIdx >= data.headsigns.length
    ) continue

    const lineName = data.lineNames[lineIdx]!
    const routeId = data.routeIds[lineIdx]!
    const rawHeadsign = data.headsigns[headsignIdx]!
    const headsign = headsignMap?.[rawHeadsign] ?? rawHeadsign

    const dock = compact.length > 3 && typeof compact[3] === 'string' ? String(compact[3]) : ''
    const tripIdIdx = compact.length > 5 ? Number(compact[5]) : undefined
    const tripId =
      tripIdIdx !== undefined && tripIdIdx < data.tripIds.length
        ? data.tripIds[tripIdIdx]
        : undefined

    const route = routeMap.get(routeId)

    const [hStr, mStr] = time.split(':')
    const h = parseInt(hStr ?? '0', 10)
    const m = parseInt(mStr ?? '0', 10)

    result.push({
      id: `${depIdx}_${time}_${lineName}_${headsign}`,
      time,
      lineName,
      routeId,
      headsign,
      color: route?.color ?? '#888888',
      textColor: route?.textColor ?? '#FFFFFF',
      transitType: route?.transitType ?? 'bus',
      dock,
      tripId,
      minutesFromMidnight: h * 60 + m,
    })
    depIdx++
  }

  return result
}

/**
 * Given a timestamp in milliseconds, returns the number of whole minutes
 * elapsed since the start of that calendar day (midnight = 0).
 */
export function computeNowMin(nowMs: number): number {
  const midnight = new Date(nowMs)
  midnight.setHours(0, 0, 0, 0)
  return Math.floor((nowMs - midnight.getTime()) / 60_000)
}

/**
 * Sorts an array of stops by their next departure time today (ascending).
 * Stops with no upcoming departure are placed at the end.
 */
export function sortStopsByNextDeparture<T extends { stopId: string; name: string }>(
  stops: T[],
  scheduleData: ScheduleData,
  nowMs: number,
  timezone?: string,
  headsignMap?: Record<string, string>,
): T[] {
  return [...stops].sort((a, b) => {
    const aMin = getNextDeparture(a.stopId, scheduleData, nowMs, timezone, headsignMap)?.minutesFromMidnight ?? Infinity
    const bMin = getNextDeparture(b.stopId, scheduleData, nowMs, timezone, headsignMap)?.minutesFromMidnight ?? Infinity
    return aMin - bMin
  })
}

/**
 * Returns the first upcoming departure for a given stop today, or null.
 */
export function getNextDeparture(
  stopId: string,
  scheduleData: ScheduleData,
  nowMs: number,
  timezone?: string,
  headsignMap?: Record<string, string>,
): Departure | null {
  const stop = scheduleData.stops.find(s => s.id === stopId)
  if (!stop) return null
  const todayKey = getTodayDayGroupKey(stop.departures, timezone)
  if (!todayKey) return null
  const compact = stop.departures[todayKey]
  if (!compact) return null
  const deps = decodeDepartures(compact, scheduleData, headsignMap)
  const nowMin = computeNowMin(nowMs)
  return deps
    .sort((a, b) => a.minutesFromMidnight - b.minutesFromMidnight)
    .find(d => d.minutesFromMidnight >= nowMin) ?? null
}
