<template>
  <div class="flex items-center gap-3 py-3 border-b border-gray-100 dark:border-white/5 last:border-0">
    <LineBadge
      :name="departure.lineName"
      :color="departure.color"
      :text-color="departure.textColor"
      :locale="locale"
    />

    <span
      v-if="transitTypeIcon"
      aria-hidden="true"
      class="text-base shrink-0"
    >{{ transitTypeIcon }}</span>

    <span class="flex-1 text-sm text-gray-900 dark:text-gray-100 truncate">
      {{ departure.headsign }}
      <span v-if="departure.dock" class="text-gray-400 text-xs ml-1">
        {{ s.dockPrefix }}{{ departure.dock }}
      </span>
    </span>

    <div class="flex items-center gap-1.5 shrink-0">
      <span
        v-if="departure.isRealtime"
        role="img"
        :aria-label="s.ariaRealtimeData"
        class="w-2 h-2 rounded-full bg-green-500 animate-pulse"
      />
      <span
        class="text-sm font-semibold tabular-nums"
        :class="timeClass"
      >
        {{ displayTime }}
      </span>
      <span
        v-if="showScheduledTime"
        class="text-xs text-gray-400 tabular-nums"
        :aria-label="`${s.scheduledTime} ${departure.time}`"
      >
        {{ departure.time }}
      </span>
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
}>()

const s = computed(() => getStrings(props.locale))

const TRANSIT_ICONS: Record<string, string> = {
  bus: '🚌',
  tram: '🚃',
  rail: '🚆',
  ferry: '⛴️',
  metro: '🚇',
}

const transitTypeIcon = computed(() =>
  props.departure.transitType ? TRANSIT_ICONS[props.departure.transitType] ?? null : null
)

const showScheduledTime = computed(() =>
  props.departure.realtimeDelay !== undefined &&
  props.departure.realtimeDelay !== 0
)

function effectiveMinutes(nowMs: number): number {
  // Use pre-computed minutesFromMidnight instead of re-parsing the time string
  // This correctly handles post-midnight times (e.g., 25:30 = 1530 min)
  const midnight = new Date(nowMs)
  midnight.setHours(0, 0, 0, 0)
  const nowMin = Math.floor((nowMs - midnight.getTime()) / 60_000)
  let diffMin = props.departure.minutesFromMidnight - nowMin
  if (props.departure.realtimeDelay !== undefined) {
    diffMin += Math.round(props.departure.realtimeDelay / 60)
  }
  return diffMin
}

const displayTime = computed(() => {
  const nowMs = props.now ?? Date.now()
  const diffMin = effectiveMinutes(nowMs)

  if (diffMin < 0) return props.departure.time
  if (diffMin === 0) return s.value.now
  if (diffMin < 60) return `${diffMin} ${s.value.minutes}`
  return props.departure.time
})

const timeClass = computed(() => {
  const nowMs = props.now ?? Date.now()
  const diffMin = effectiveMinutes(nowMs)
  return diffMin >= 0 && diffMin < 2
    ? 'text-green-600 dark:text-green-400'
    : 'text-gray-900 dark:text-gray-100'
})
</script>
