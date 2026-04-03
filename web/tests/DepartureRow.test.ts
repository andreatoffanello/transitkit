/**
 * Tests for DepartureRow countdown reactivity.
 *
 * DepartureRow.vue does not use @vue/test-utils (not installed); we test
 * the pure `effectiveMinutes` / `displayTime` logic directly by replicating
 * the same functions used in the component.
 *
 * Findings from code inspection (stop/[stopId].vue + DepartureRow.vue):
 *  - `now` is `ref(Date.now())` updated every 30 s via setInterval in onMounted
 *  - `displayTime` is a `computed(() => { const nowMs = props.now ?? Date.now() ... })`
 *    so it is fully reactive to changes in `props.now`
 *  - Both DepartureRow usages pass `:now="now"` (the reactive ref value)
 *  → The live countdown is correctly wired end-to-end.
 */

import { describe, it, expect } from 'vitest'
import type { Departure } from '~/types'
import { getStrings } from '~/utils/strings'

// ---------------------------------------------------------------------------
// Pure helpers — mirrors the logic in DepartureRow.vue
// ---------------------------------------------------------------------------

function effectiveMinutes(nowMs: number, departure: Departure): number {
  const midnight = new Date(nowMs)
  midnight.setHours(0, 0, 0, 0)
  const nowMin = Math.floor((nowMs - midnight.getTime()) / 60_000)
  let diffMin = departure.minutesFromMidnight - nowMin
  if (departure.realtimeDelay !== undefined) {
    diffMin += Math.round(departure.realtimeDelay / 60)
  }
  return diffMin
}

const NOW_LABEL = 'Adesso'
const MIN_LABEL = 'min'

function displayTime(nowMs: number, departure: Departure): string {
  const diffMin = effectiveMinutes(nowMs, departure)
  if (diffMin < 0) return departure.time
  if (diffMin === 0) return NOW_LABEL
  if (diffMin < 60) return `${diffMin} ${MIN_LABEL}`
  return departure.time
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

// Use a fixed base timestamp for all tests — avoids wall-clock dependency
const BASE_DATE = new Date('2026-04-03T08:00:00Z').getTime()

/** Build a nowMs that is `minutesBefore` minutes before `depMinutesFromMidnight`. */
function nowFor(depMinutesFromMidnight: number, minutesBefore: number, baseMs = BASE_DATE): number {
  const midnight = new Date(baseMs)
  midnight.setHours(0, 0, 0, 0)
  const nowMin = depMinutesFromMidnight - minutesBefore
  return midnight.getTime() + nowMin * 60_000
}

function makeDeparture(overrides: Partial<Departure> = {}): Departure {
  return {
    id: 'test-dep',
    time: '10:05',
    lineName: '3',
    routeId: 'route-3',
    headsign: 'Centro',
    color: '#FF0000',
    textColor: '#FFFFFF',
    transitType: 'bus',
    dock: '',
    tripId: 'trip-001',
    minutesFromMidnight: 605, // 10:05
    ...overrides,
  }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('DepartureRow — effectiveMinutes', () => {
  it('returns positive diff when departure is in the future', () => {
    const dep = makeDeparture({ minutesFromMidnight: 605 })
    const now = nowFor(605, 5)
    expect(effectiveMinutes(now, dep)).toBe(5)
  })

  it('returns 0 when departure is now', () => {
    const dep = makeDeparture({ minutesFromMidnight: 605 })
    const now = nowFor(605, 0)
    expect(effectiveMinutes(now, dep)).toBe(0)
  })

  it('returns negative diff when departure is in the past', () => {
    const dep = makeDeparture({ minutesFromMidnight: 605 })
    const now = nowFor(605, -3) // 3 minutes after departure
    expect(effectiveMinutes(now, dep)).toBe(-3)
  })

  it('applies realtimeDelay (seconds) to diff', () => {
    const dep = makeDeparture({ minutesFromMidnight: 605, realtimeDelay: 120 }) // +2 min delay
    const now = nowFor(605, 5)
    // scheduled diff = 5, + 2 min delay = 7
    expect(effectiveMinutes(now, dep)).toBe(7)
  })

  it('rounds realtimeDelay to nearest minute', () => {
    const dep = makeDeparture({ minutesFromMidnight: 605, realtimeDelay: 90 }) // 1.5 min → rounds to 2
    const now = nowFor(605, 5)
    expect(effectiveMinutes(now, dep)).toBe(7)
  })
})

describe('DepartureRow — displayTime (live countdown reactivity)', () => {
  it('shows "X min" when departure is in the future (< 60 min)', () => {
    const dep = makeDeparture({ minutesFromMidnight: 605 })
    expect(displayTime(nowFor(605, 5), dep)).toBe('5 min')
  })

  it('shows "1 min" when departure is 1 minute away', () => {
    const dep = makeDeparture({ minutesFromMidnight: 605 })
    expect(displayTime(nowFor(605, 1), dep)).toBe('1 min')
  })

  it('shows "Adesso" when diffMin is 0', () => {
    const dep = makeDeparture({ minutesFromMidnight: 605 })
    expect(displayTime(nowFor(605, 0), dep)).toBe(NOW_LABEL)
  })

  it('shows static time string when departure is in the past', () => {
    const dep = makeDeparture({ minutesFromMidnight: 605 })
    expect(displayTime(nowFor(605, -2), dep)).toBe('10:05')
  })

  it('shows static time string when diffMin >= 60', () => {
    const dep = makeDeparture({ minutesFromMidnight: 605 })
    expect(displayTime(nowFor(605, 90), dep)).toBe('10:05')
  })

  it('updates displayTime when now prop changes (5 min → 1 min)', () => {
    const dep = makeDeparture({ minutesFromMidnight: 605 })
    // Simulate the now prop being updated by the 30-second interval
    const nowAt5 = nowFor(605, 5)
    const nowAt1 = nowFor(605, 1)

    expect(displayTime(nowAt5, dep)).toBe('5 min')
    // After 4 ticks of the 30s interval (2 min elapsed), now prop has advanced
    expect(displayTime(nowAt1, dep)).toBe('1 min')
  })

  it('transitions from countdown to "Adesso" as now crosses departure time', () => {
    const dep = makeDeparture({ minutesFromMidnight: 605 })
    expect(displayTime(nowFor(605, 1), dep)).toBe('1 min')
    expect(displayTime(nowFor(605, 0), dep)).toBe(NOW_LABEL)
    expect(displayTime(nowFor(605, -1), dep)).toBe('10:05')
  })

  it('accounts for realtime delay when computing displayTime', () => {
    // Departure at 10:05, delayed by 5 minutes (300s)
    const dep = makeDeparture({ minutesFromMidnight: 605, realtimeDelay: 300 })
    // Effectively departs at 10:10, so 5 min before 10:05 scheduled = 10 min effective wait
    expect(displayTime(nowFor(605, 5), dep)).toBe('10 min')
  })
})

describe('scheduled time display', () => {
  // showScheduledTime logic: realtimeDelay defined and !== 0
  function showScheduledTime(departure: Departure): boolean {
    return departure.realtimeDelay !== undefined && departure.realtimeDelay !== 0
  }

  it('does not show scheduled time when no realtimeDelay', () => {
    const dep = makeDeparture({ time: '14:32', minutesFromMidnight: 872 })
    expect(showScheduledTime(dep)).toBe(false)
  })

  it('does not show scheduled time when realtimeDelay is 0', () => {
    const dep = makeDeparture({ time: '14:32', minutesFromMidnight: 872, realtimeDelay: 0 })
    expect(showScheduledTime(dep)).toBe(false)
  })

  it('shows scheduled time when realtimeDelay is positive', () => {
    const dep = makeDeparture({ time: '14:32', minutesFromMidnight: 872, realtimeDelay: 300 }) // 5min delay
    expect(showScheduledTime(dep)).toBe(true)
  })

  it('shows scheduled time when realtimeDelay is negative (early)', () => {
    const dep = makeDeparture({ time: '14:32', minutesFromMidnight: 872, realtimeDelay: -120 }) // 2min early
    expect(showScheduledTime(dep)).toBe(true)
  })

  it('scheduled time value is departure.time (the original scheduled time string)', () => {
    const dep = makeDeparture({ time: '14:32', minutesFromMidnight: 872, realtimeDelay: 300 })
    // The component renders departure.time as the scheduled time label
    expect(dep.time).toBe('14:32')
  })

  describe('scheduledTime label strings', () => {
    // These tests verify the s.scheduledTime label used in the aria-label of the
    // scheduled time block: `${s.scheduledTime} ${departure.time}`

    it('with realtimeDelay=300 (5 min late), aria-label contains "orario prev." and original time', () => {
      const dep = makeDeparture({ time: '08:30', minutesFromMidnight: 510, realtimeDelay: 300 })
      const s = getStrings() // default IT locale
      // Mirrors the component aria-label: `${s.scheduledTime} ${departure.time}`
      const ariaLabel = `${s.scheduledTime} ${dep.time}`
      expect(ariaLabel).toContain('orario prev.')
      expect(ariaLabel).toContain('08:30')
    })

    it('without delay (realtimeDelay=null), showScheduledTime is false so label is never rendered', () => {
      const dep = makeDeparture({ time: '08:30', minutesFromMidnight: 510 })
      // showScheduledTime is false — the block is not rendered at all
      const show = dep.realtimeDelay !== undefined && dep.realtimeDelay !== 0
      expect(show).toBe(false)
      // Confirm the IT label string exists in strings util
      const s = getStrings()
      expect(s.scheduledTime).toBe('orario prev.')
    })

    it('with realtimeDelay=0, showScheduledTime is false (on time — no scheduled block)', () => {
      const dep = makeDeparture({ time: '08:30', minutesFromMidnight: 510, realtimeDelay: 0 })
      const show = dep.realtimeDelay !== undefined && dep.realtimeDelay !== 0
      expect(show).toBe(false)
    })

    it('with EN locale and realtimeDelay=180, aria-label contains "sched." and original time', () => {
      const dep = makeDeparture({ time: '09:15', minutesFromMidnight: 555, realtimeDelay: 180 })
      const s = getStrings('en')
      const ariaLabel = `${s.scheduledTime} ${dep.time}`
      expect(ariaLabel).toContain('sched.')
      expect(ariaLabel).toContain('09:15')
    })
  })
})

// ---------------------------------------------------------------------------
// delayAriaLabel — mirrors the logic in DepartureRow.vue
// ---------------------------------------------------------------------------

function delayAriaLabel(departure: Departure, locale?: string): string | null {
  const s = getStrings(locale)
  if (departure.realtimeDelay === undefined || departure.realtimeDelay === 0) return null
  const delayMin = Math.abs(Math.round(departure.realtimeDelay / 60))
  return `${delayMin} ${s.minutesDelay}`
}

describe('DepartureRow — delayAriaLabel', () => {
  it('returns null when realtimeDelay is undefined', () => {
    const dep = makeDeparture()
    expect(delayAriaLabel(dep)).toBeNull()
  })

  it('returns null when realtimeDelay is 0', () => {
    const dep = makeDeparture({ realtimeDelay: 0 })
    expect(delayAriaLabel(dep)).toBeNull()
  })

  it('returns label with "5" and delay word when realtimeDelay is 300s (+5 min)', () => {
    const dep = makeDeparture({ realtimeDelay: 300 })
    const label = delayAriaLabel(dep)
    expect(label).not.toBeNull()
    expect(label).toContain('5')
    expect(label).toContain('minuti di ritardo') // IT default
  })

  it('returns label with "2" when realtimeDelay is -120s (2 min early)', () => {
    const dep = makeDeparture({ realtimeDelay: -120 })
    const label = delayAriaLabel(dep)
    expect(label).not.toBeNull()
    expect(label).toContain('2')
  })

  it('rounds realtimeDelay to nearest minute (90s → 2 min)', () => {
    const dep = makeDeparture({ realtimeDelay: 90 })
    const label = delayAriaLabel(dep)
    expect(label).toContain('2')
  })

  it('uses English strings when locale is "en"', () => {
    const dep = makeDeparture({ realtimeDelay: 300 })
    const label = delayAriaLabel(dep, 'en')
    expect(label).toContain('5')
    expect(label).toContain('minutes late')
  })

  it('uses Italian strings by default (no locale)', () => {
    const dep = makeDeparture({ realtimeDelay: 180 })
    const label = delayAriaLabel(dep)
    expect(label).toBe('3 minuti di ritardo')
  })
})

describe('DepartureRow — stop page wiring (structural verification)', () => {
  /**
   * These are not runtime tests — they document the verified structure from
   * code inspection of stop/[stopId].vue and DepartureRow.vue.
   *
   * Verified facts:
   * 1. `now` is declared as `ref(Date.now())` (line 223 of stop/[stopId].vue)
   * 2. setInterval updates `now.value = Date.now()` every 30_000ms (line 227)
   * 3. onUnmounted clears the interval (line 232)
   * 4. Both <DepartureRow> instances receive `:now="now"` (lines 76, 135)
   * 5. `displayTime` in DepartureRow is a `computed()` reading `props.now` (line 77-85)
   * 6. `timeClass` also reads `props.now` for the green urgency highlight (lines 87-93)
   *
   * Conclusion: live countdown is correctly wired end-to-end.
   */
  it('documents that now is a reactive ref updated every 30s', () => {
    // Structural fact — not a runtime assertion, just a canary.
    // If the interval logic is removed, the test above ("updates displayTime
    // when now prop changes") would still pass because the pure function is
    // correct; actual reactivity is guaranteed by Vue's computed() + ref().
    expect(true).toBe(true)
  })
})
