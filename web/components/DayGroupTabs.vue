<template>
  <div>
    <div
      role="tablist"
      aria-label="Gruppi orari"
      class="flex gap-1 mb-4 overflow-x-auto pb-1"
    >
      <button
        v-for="dg in dayGroups"
        :key="dg.id"
        role="tab"
        :aria-selected="selected === dg.id"
        :aria-controls="`tabpanel-${dg.id}`"
        class="px-3 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
        :class="selected === dg.id
          ? 'bg-accent text-white'
          : 'bg-gray-100 dark:bg-white/10 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-white/20'"
        @click="selected = dg.id"
      >
        {{ dg.displayLabel }}
      </button>
    </div>

    <div
      :id="`tabpanel-${selected}`"
      role="tabpanel"
    >
      <slot :departures="currentDepartures" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { DayGroup, Departure } from '~/types'

const props = defineProps<{
  dayGroups: DayGroup[]
  departuresByDayGroup: Record<string, Departure[]>
  initialKey?: string
}>()

const selected = ref(props.initialKey ?? props.dayGroups[0]?.id ?? '')

const currentDepartures = computed(
  () => props.departuresByDayGroup[selected.value] ?? [],
)
</script>
