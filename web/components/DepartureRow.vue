<template>
  <div
    class="py-3 px-4 relative transition-colors duration-150"
    :class="{ 'opacity-40': isPast }"
    :aria-label="rowAriaLabel"
  >
    <!-- "Prossima" pill — solo per la prima riga upcoming. Posizionata
         dentro la flex column del row, non absolute, per essere robusta a
         cambi di layout. -->
    <div v-if="showNextLabel" class="mb-1.5">
      <span
        class="inline-block text-[10px] font-bold px-1.5 py-0.5 rounded uppercase tracking-wide"
        style="background-color: var(--color-live); color: #fff; line-height: 1.2"
      >{{ nextLabel ?? 'prossima' }}</span>
    </div>

    <div class="flex items-center gap-3">
    <!-- Transit type icon square (line color bg) -->
    <div
      class="w-8 h-8 rounded-lg flex items-center justify-center shrink-0"
      :style="{ backgroundColor: bgColor }"
      aria-hidden="true"
    >
      <component :is="transitIcon" :size="16" color="white" :stroke-width="2" />
    </div>

    <!-- Line badge -->
    <LineBadge
      :name="departure.lineName"
      :color="departure.color"
      :text-color="departure.textColor"
      :locale="locale"
    />

    <!-- Headsign + dock — up to 2 lines so the "going where" info stays readable on narrow screens -->
    <span
      class="flex-1 text-[15px] font-medium headsign-clamp"
      style="color: var(--text-primary); line-height: 1.25"
    >
      {{ departure.headsign }}<span
        v-if="departure.dock"
        class="text-xs ml-1"
        style="color: var(--text-tertiary)"
      >{{ s.dockPrefix }}{{ departure.dock }}</span>
    </span>

    <!-- Time / countdown -->
    <div class="flex items-center gap-1.5 shrink-0">
      <span
        v-if="departure.isRealtime"
        class="w-1.5 h-1.5 rounded-full animate-pulse shrink-0"
        style="background-color: var(--color-live)"
        :aria-label="s.ariaRealtimeData"
        role="img"
      />
      <div class="text-right">
        <!-- Delayed -->
        <template v-if="hasDelay">
          <span
            class="block text-[12px] line-through"
            style="color: var(--text-tertiary); font-variant-numeric: tabular-nums"
          >{{ displayTime }}</span>
          <span
            class="block text-[15px] font-semibold text-orange-500"
            style="font-variant-numeric: tabular-nums"
          >{{ realtimeTime }}</span>
        </template>
        <!-- Countdown or fixed time -->
        <template v-else>
          <span
            v-if="showCountdown && typeof effectiveMinutes === 'number' && effectiveMinutes >= 0 && effectiveMinutes <= 1"
            class="inline-flex items-center justify-center h-6 px-2.5 rounded-md text-xs font-bold"
            style="background-color: var(--color-live); color: #fff"
          >{{ s.now }}</span>
          <span
            v-else-if="showCountdown && typeof effectiveMinutes === 'number' && effectiveMinutes > 1 && effectiveMinutes <= 30"
            class="text-[17px] font-bold leading-none"
            style="color: var(--color-primary); font-variant-numeric: tabular-nums; letter-spacing: -0.03em"
          >{{ effectiveMinutes }}<span class="text-[11px] font-medium ml-0.5">min</span></span>
          <span
            v-else
            class="text-[15px] font-semibold"
            style="font-variant-numeric: tabular-nums; letter-spacing: -0.02em; color: var(--text-primary)"
          >{{ displayTime }}</span>
        </template>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { BusFront, TramFront, Train, Ship } from 'lucide-vue-next'
import type { Departure } from '~/types'
import { getStrings } from '~/utils/strings'
import { normalizeHex } from '~/utils/color'
import { formatClockTime } from '~/utils/clockTime'

const props = defineProps<{
  departure: Departure
  now?: number
  locale?: string
  showCountdown?: boolean
  isPast?: boolean
  showNextLabel?: boolean
  nextLabel?: string
}>()

const s = computed(() => getStrings(props.locale))

const transitIcon = computed(() => {
  switch (props.departure.transitType) {
    case 'tram': return TramFront
    case 'metro':
    case 'rail':
    case 'monorail': return Train
    case 'ferry': return Ship
    default: return BusFront
  }
})

const bgColor = computed(() =>
  props.departure.color ? normalizeHex(props.departure.color) : 'var(--color-primary)'
)

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

<style scoped>
.headsign-clamp {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  overflow-wrap: anywhere;
  word-break: break-word;
}
</style>
