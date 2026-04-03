<template>
  <div>
    <div
      role="tablist"
      :aria-label="strings?.ariaDayGroupTabs"
      class="flex gap-1.5 overflow-x-auto pb-1 mb-4"
      style="scrollbar-width: none; -ms-overflow-style: none;"
    >
      <button
        v-for="dg in dayGroups"
        :key="dg.id"
        :id="`tab-${dg.id}`"
        role="tab"
        :aria-selected="selected === dg.id"
        aria-controls="daytabs-panel"
        class="px-3.5 py-1.5 text-sm font-medium whitespace-nowrap shrink-0 transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-1"
        style="border-radius: 20px;"
        :style="selected === dg.id
          ? `background-color: var(--color-primary); color: var(--color-text-on-primary);`
          : `background-color: var(--bg-primary); color: var(--text-secondary); border: 1px solid var(--border);`"
        @click="selected = dg.id"
      >
        {{ strings ? getDayGroupLabel(dg, strings) : dg.displayLabel }}<template v-if="(departuresByDayGroup[dg.id]?.length ?? 0) > 0"> <span class="tabular-nums">({{ departuresByDayGroup[dg.id]!.length }})</span></template>
      </button>
    </div>

    <div
      id="daytabs-panel"
      role="tabpanel"
      :aria-labelledby="`tab-${selected}`"
    >
      <slot :departures="currentDepartures" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { DayGroup, Departure } from '~/types'
import type { AppStrings } from '~/utils/strings'
import { getDayGroupLabel } from '~/utils/schedule'

const props = defineProps<{
  dayGroups: DayGroup[]
  departuresByDayGroup: Record<string, Departure[]>
  initialKey?: string
  strings?: AppStrings
}>()

const selected = ref(props.initialKey ?? props.dayGroups[0]?.id ?? '')

const currentDepartures = computed(
  () => props.departuresByDayGroup[selected.value] ?? [],
)
</script>
