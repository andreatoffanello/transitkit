<template>
  <div
    class="flex items-center gap-3 py-3.5 px-4 relative transition-colors duration-150"
    :class="{ 'opacity-40': isPast }"
    :style="isNext ? `border-left: 2.5px solid var(--color-primary); padding-left: 14px; background-color: color-mix(in srgb, var(--color-primary) 10%, transparent);` : ''"
  >
    <span class="sr-only">{{ rowAriaLabel }}</span>

    <!-- Line badge -->
    <LineBadge
      :name="departure.lineName"
      :color="departure.color"
      :text-color="departure.textColor"
      :locale="locale"
    />

    <!-- Headsign + dock -->
    <span
      class="flex-1 text-[15px] font-medium truncate"
      style="color: var(--text-primary)"
    >
      {{ departure.headsign }}
      <span
        v-if="departure.dock"
        class="text-xs ml-1"
        style="color: var(--text-tertiary)"
      >{{ s.dockPrefix }}{{ departure.dock }}</span>
    </span>

    <!-- Time + realtime -->
    <div class="flex items-center gap-2 shrink-0">
      <span
        v-if="departure.isRealtime"
        class="w-2 h-2 rounded-full bg-green-500 animate-pulse shrink-0"
        :aria-label="s.ariaRealtimeData"
        role="img"
      />
      <div class="text-right">
        <!-- Con ritardo -->
        <template v-if="hasDelay">
          <span
            class="block text-[13px] line-through"
            style="color: var(--text-tertiary); font-variant-numeric: tabular-nums; letter-spacing: -0.02em"
          >{{ departure.time }}</span>
          <span
            class="block text-[15px] font-semibold text-orange-500"
            style="font-variant-numeric: tabular-nums; letter-spacing: -0.02em"
          >{{ realtimeTime }}</span>
        </template>
        <!-- Countdown o orario normale -->
        <template v-else>
          <!-- Imminent: green pill badge "Ora" -->
          <span
            v-if="showCountdown && typeof effectiveMinutes === 'number' && effectiveMinutes >= 0 && effectiveMinutes <= 1"
            class="inline-flex items-center justify-center h-6 px-2.5 rounded-md text-xs font-bold leading-none"
            style="background-color: #16a34a; color: #ffffff; letter-spacing: 0"
          >{{ s.now }}</span>
          <!-- Short countdown: large primary number -->
          <span
            v-else-if="showCountdown && typeof effectiveMinutes === 'number' && effectiveMinutes >= 0 && effectiveMinutes <= 30"
            class="text-[17px] font-bold leading-none"
            style="color: var(--color-primary); font-variant-numeric: tabular-nums; letter-spacing: -0.03em"
          >{{ effectiveMinutes }}<span class="text-[11px] font-medium ml-0.5">min</span></span>
          <!-- Fixed time: full-weight when in schedule context, secondary when in countdown context -->
          <span
            v-else
            class="text-[15px]"
            :class="showCountdown ? 'font-medium' : 'font-semibold'"
            :style="`font-variant-numeric: tabular-nums; letter-spacing: -0.02em; color: ${showCountdown ? 'var(--text-secondary)' : 'var(--text-primary)'}`"
          >{{ departure.time }}</span>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Departure } from '~/types'
import { getStrings } from '~/utils/strings'

const props = defineProps<{
  departure: Departure
  now?: number
  locale?: string
  showCountdown?: boolean
  isPast?: boolean
  isNext?: boolean
}>()

const s = computed(() => getStrings(props.locale))

// Delay in seconds — convert to minutes
const delayMinutes = computed(() =>
  props.departure.realtimeDelay !== undefined
    ? Math.round(props.departure.realtimeDelay / 60)
    : 0
)

const hasDelay = computed(() => delayMinutes.value > 0)

// Compute realtime display time by adding delay to scheduled time string (HH:MM)
const realtimeTime = computed(() => {
  if (!hasDelay.value) return props.departure.time
  const [h, m] = props.departure.time.split(':').map(Number)
  const totalMin = h * 60 + m + delayMinutes.value
  const rh = Math.floor(totalMin / 60) % 24
  const rm = totalMin % 60
  return `${String(rh).padStart(2, '0')}:${String(rm).padStart(2, '0')}`
})

function computeEffectiveMinutes(nowMs: number): number {
  const midnight = new Date(nowMs)
  midnight.setHours(0, 0, 0, 0)
  const nowMin = Math.floor((nowMs - midnight.getTime()) / 60_000)
  let diffMin = props.departure.minutesFromMidnight - nowMin
  if (props.departure.realtimeDelay !== undefined) {
    diffMin += delayMinutes.value
  }
  return diffMin
}

const effectiveMinutes = computed(() => computeEffectiveMinutes(props.now ?? Date.now()))

const timeColorStyle = computed(() =>
  effectiveMinutes.value >= 0 && effectiveMinutes.value < 2
    ? 'color: #16a34a'
    : 'color: var(--text-primary)'
)

const rowAriaLabel = computed(() => {
  const diffMin = effectiveMinutes.value
  let timeDescription: string
  if (diffMin === 0) {
    timeDescription = s.value.now
  } else if (diffMin > 0 && diffMin < 60) {
    timeDescription = `${diffMin} ${s.value.minutes}`
  } else {
    timeDescription = props.departure.time
  }
  return `${s.value.lineLabel} ${props.departure.lineName}, ${props.departure.headsign}, ${timeDescription}`
})
</script>
