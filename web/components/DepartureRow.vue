<template>
  <component
    :is="rootIs"
    :to="linkTo || undefined"
    class="flex items-center gap-2.5 py-3 px-4 transition-colors duration-150"
    :class="{ 'opacity-40': isPast, 'active:bg-black/[0.03] dark:active:bg-white/[0.04] cursor-pointer': linkTo }"
    :aria-label="rowAriaLabel"
  >
    <!-- Line badge — hidden when a line filter is active (every row is the same line) -->
    <LineBadge
      v-if="!hideBadge"
      :name="departure.lineName"
      :color="departure.color"
      :text-color="departure.textColor"
      :locale="locale"
    />

    <!-- Destination -->
    <span
      class="flex-1 min-w-0 truncate text-[13px] font-semibold"
      :style="{ color: isPast ? 'var(--text-tertiary)' : 'var(--text-primary)' }"
    >{{ departure.headsign }}</span>

    <!-- Dock badge -->
    <span
      v-if="departure.dock"
      class="shrink-0 text-[10px] font-extrabold px-1.5 py-0.5 rounded"
      style="color: var(--text-primary); background-color: var(--bg-secondary); border: 1px solid var(--border)"
    >{{ departure.dock }}</span>

    <!-- Time stack: countdown (+ live dot) with absolute clock beneath -->
    <div class="shrink-0 flex flex-col items-end leading-none whitespace-nowrap">
      <template v-if="hasDelay">
        <div class="flex items-center gap-1">
          <span
            v-if="departure.isRealtime"
            class="w-1.5 h-1.5 rounded-full animate-pulse shrink-0"
            style="background-color: var(--color-live)"
            :aria-label="s.ariaRealtimeData"
            role="img"
          />
          <span class="text-[15px] font-semibold text-orange-500" style="font-variant-numeric: tabular-nums">{{ realtimeTime }}</span>
        </div>
        <span class="text-[11px] line-through mt-0.5" style="color: var(--text-tertiary); font-variant-numeric: tabular-nums">{{ displayTime }}</span>
      </template>
      <template v-else>
        <div class="flex items-center gap-1">
          <span
            v-if="departure.isRealtime"
            class="w-1.5 h-1.5 rounded-full animate-pulse shrink-0"
            style="background-color: var(--color-live)"
            :aria-label="s.ariaRealtimeData"
            role="img"
          />
          <span
            v-if="showCountdown && typeof effectiveMinutes === 'number' && effectiveMinutes >= 0 && effectiveMinutes <= 1"
            class="inline-flex items-center justify-center h-6 px-2.5 rounded-md text-xs font-bold"
            style="background-color: var(--color-live); color: #fff"
          >{{ s.now }}</span>
          <span
            v-else-if="showCountdown && typeof effectiveMinutes === 'number' && effectiveMinutes > 1 && effectiveMinutes <= 30"
            class="text-[17px] font-bold"
            style="color: var(--color-primary); font-variant-numeric: tabular-nums; letter-spacing: -0.03em"
          >{{ effectiveMinutes }}<span class="text-[11px] font-medium ml-0.5">min</span></span>
          <span
            v-else
            class="text-[15px] font-semibold"
            style="font-variant-numeric: tabular-nums; letter-spacing: -0.02em; color: var(--text-primary)"
          >{{ displayTime }}</span>
        </div>
      </template>
    </div>
  </component>
</template>

<script setup lang="ts">
import type { Departure } from '~/types'
import { getStrings } from '~/utils/strings'
import { formatClockTime } from '~/utils/clockTime'

const props = defineProps<{
  departure: Departure
  now?: number
  locale?: string
  showCountdown?: boolean
  isPast?: boolean
  /** Hide the line badge (when a single-line filter is active). */
  hideBadge?: boolean
  /** When set, the row links to the trip detail (svolgimento corsa). */
  fromStopId?: string
}>()

const s = computed(() => getStrings(props.locale))

// Tap target → trip detail. Only when both the trip id and the originating
// stop are known (so the trip view can highlight the right "Now" stop).
const linkTo = computed(() =>
  props.fromStopId && props.departure.tripId
    ? `/trip/${props.departure.tripId}?from=${props.fromStopId}`
    : null,
)
const NuxtLinkComp = resolveComponent('NuxtLink')
const rootIs = computed(() => (linkTo.value ? NuxtLinkComp : 'div'))

const delayMinutes = computed(() =>
  props.departure.realtimeDelay !== undefined
    ? Math.round(props.departure.realtimeDelay / 60)
    : 0
)
const hasDelay = computed(() => delayMinutes.value > 0)

const realtimeTime = computed(() => {
  if (!hasDelay.value) return formatClockTime(props.departure.time, props.locale)
  const [h = 0, m = 0] = props.departure.time.split(':').map(Number)
  const totalMin = h * 60 + m + delayMinutes.value
  const rh = Math.floor(totalMin / 60) % 24
  const rm = totalMin % 60
  const raw = `${String(rh).padStart(2, '0')}:${String(rm).padStart(2, '0')}`
  return formatClockTime(raw, props.locale)
})

const displayTime = computed(() => formatClockTime(props.departure.time, props.locale))

function computeEffectiveMinutes(nowMs: number): number {
  const midnight = new Date(nowMs)
  midnight.setHours(0, 0, 0, 0)
  const nowMin = Math.floor((nowMs - midnight.getTime()) / 60_000)
  let diffMin = props.departure.minutesFromMidnight - nowMin
  if (props.departure.realtimeDelay !== undefined) diffMin += delayMinutes.value
  return diffMin
}

const effectiveMinutes = computed(() => computeEffectiveMinutes(props.now ?? Date.now()))

const rowAriaLabel = computed(() => {
  const diffMin = effectiveMinutes.value
  let timeDescription: string
  if (diffMin === 0) timeDescription = s.value.now
  else if (diffMin > 0 && diffMin < 60) timeDescription = `${diffMin} ${s.value.minutes}`
  else timeDescription = displayTime.value
  return `${s.value.lineLabel} ${props.departure.lineName}, ${props.departure.headsign}, ${timeDescription}`
})
</script>
