<template>
  <section
    id="panel-orario"
    role="tabpanel"
    aria-labelledby="tab-orario"
    class="px-4 mb-6"
  >
    <h2 class="text-[17px] font-bold mb-3" style="color: var(--text-primary)">
      {{ s.tabSchedule }}
    </h2>
    <div
      v-if="config?.gtfsRt && !isLive"
      class="text-xs text-center text-amber-600 dark:text-amber-400 py-1.5 px-3 rounded-lg mb-3"
      style="background: rgba(245,158,11,0.10)"
      role="alert"
    >
      {{ s.schedulesNotLive }}
    </div>
    <DayGroupTabs
      :day-groups="dayGroups"
      :departures-by-day-group="departuresByGroup"
      :initial-key="todayKey ?? undefined"
      :strings="s"
    >
      <template #default="{ departures }">
        <div
          v-if="departures.length"
          class="overflow-hidden divide-y"
          style="background-color: var(--bg-elevated); border-radius: 16px; border: 1px solid var(--border)"
        >
          <DepartureRow
            v-for="(dep, index) in departures"
            :key="dep.id"
            :departure="dep"
            :now="now"
            :locale="config?.locale[0]"
            :data-departure-future="dep.minutesFromMidnight >= nowMin && departures.findIndex((d: Departure) => d.minutesFromMidnight >= nowMin) === index ? 'true' : undefined"
          />
        </div>
        <div
          v-else
          class="rounded-2xl px-5 py-12 text-center"
          style="background-color: var(--bg-elevated); border: 1px solid var(--border)"
        >
          <p class="font-semibold mb-1" style="color: var(--text-primary)">{{ s.noDeparturesToday }}</p>
          <p class="text-sm" style="color: var(--text-secondary)">{{ s.noDeparturesHint }}</p>
          <p v-if="nextServiceLabel" class="text-sm mt-1" style="color: var(--text-tertiary)">
            {{ s.nextServiceDay }}: {{ nextServiceLabel }}
          </p>
        </div>
      </template>
    </DayGroupTabs>
  </section>
</template>

<script setup lang="ts">
import type { OperatorConfig, DayGroup, Departure } from '~/types'
import type { AppStrings } from '~/utils/strings'

defineProps<{
  s: AppStrings
  config: OperatorConfig | null
  dayGroups: DayGroup[]
  departuresByGroup: Record<string, Departure[]>
  todayKey: string | null
  isLive: boolean
  now: number
  nowMin: number
  nextServiceLabel: string | null
}>()
</script>
