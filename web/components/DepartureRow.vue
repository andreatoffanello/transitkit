<template>
  <div class="flex items-center gap-3 py-3 border-b border-gray-100 dark:border-white/5 last:border-0">
    <LineBadge
      :name="departure.lineName"
      :color="departure.color"
      :text-color="departure.textColor"
    />

    <span class="flex-1 text-sm text-gray-900 dark:text-gray-100 truncate">
      {{ departure.headsign }}
      <span v-if="departure.dock" class="text-gray-400 text-xs ml-1">
        Dock {{ departure.dock }}
      </span>
    </span>

    <div class="flex items-center gap-1.5 shrink-0">
      <span
        v-if="departure.isRealtime"
        role="img"
        aria-label="Dati in tempo reale"
        class="w-2 h-2 rounded-full bg-green-500 animate-pulse"
      />
      <span
        class="text-sm font-semibold tabular-nums"
        :class="timeClass"
      >
        {{ displayTime }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Departure } from '~/types'

const props = defineProps<{
  departure: Departure
  now?: number
}>()

function effectiveMinutes(nowMs: number): number {
  const parts = props.departure.time.split(':')
  const h = parseInt(parts[0] ?? '0', 10)
  const m = parseInt(parts[1] ?? '0', 10)

  const depDate = new Date(nowMs)
  depDate.setHours(h, m, 0, 0)

  let diffMin = Math.round((depDate.getTime() - nowMs) / 60_000)

  if (props.departure.realtimeDelay !== undefined) {
    diffMin += Math.round(props.departure.realtimeDelay / 60)
  }

  return diffMin
}

const displayTime = computed(() => {
  const nowMs = props.now ?? Date.now()
  const diffMin = effectiveMinutes(nowMs)

  if (diffMin < 0) return props.departure.time
  if (diffMin === 0) return 'Ora'
  if (diffMin < 60) return `${diffMin} min`
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
