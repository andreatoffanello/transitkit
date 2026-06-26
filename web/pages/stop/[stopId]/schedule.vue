<template>
  <AppLayout>
    <div class="max-w-lg mx-auto md:max-w-xl lg:max-w-2xl">

      <!-- Back + title -->
      <div class="px-2 pt-2">
        <NuxtLink
          :to="`/stop/${stopId}`"
          class="inline-flex items-center gap-1.5 text-sm font-medium px-2 py-1.5 rounded-lg transition-opacity active:opacity-60"
          style="color: var(--color-primary)"
        >
          <ChevronLeft :size="16" :stroke-width="2" />
          {{ stop?.name ?? s.backToHome }}
        </NuxtLink>
      </div>

      <template v-if="stop">
        <h1 class="text-[22px] font-bold leading-tight px-5 pt-3 pb-3" style="color: var(--text-primary)">
          {{ s.viewFullSchedule }}
        </h1>

        <!-- Day selector (capsule chips) -->
        <div v-if="dayGroups.length > 1" class="flex gap-2 overflow-x-auto scrollbar-none px-5 pb-3">
          <button
            v-for="dg in dayGroups"
            :key="dg.id"
            class="shrink-0 h-10 px-4 rounded-full text-[15px] transition-all duration-150 active:opacity-70"
            :style="selectedKey === dg.id
              ? { backgroundColor: 'var(--color-primary)', color: 'var(--color-text-on-primary)', fontWeight: 600 }
              : { backgroundColor: 'var(--bg-elevated)', color: 'var(--text-primary)', border: '1px solid var(--border)', fontWeight: 500 }"
            :aria-pressed="selectedKey === dg.id"
            @click="selectDay(dg.id)"
          >
            {{ dayLabel(dg) }}
          </button>
        </div>

        <!-- Line filter chips -->
        <div v-if="availableLines.length > 1" class="flex gap-2 overflow-x-auto scrollbar-none px-5 pb-3">
          <button
            class="shrink-0 h-7 px-2.5 rounded-lg text-[13px] font-bold transition-all duration-150 active:opacity-70"
            :aria-pressed="filterLine === null"
            :style="filterLine === null
              ? { backgroundColor: 'var(--color-primary)', color: 'var(--color-text-on-primary)' }
              : { backgroundColor: 'var(--bg-secondary)', color: 'var(--text-secondary)' }"
            @click="filterLine = null"
          >{{ s.all }}</button>
          <button
            v-for="r in availableLineRoutes"
            :key="r.id"
            class="shrink-0 transition-opacity duration-150 active:opacity-70"
            :style="{ opacity: filterLine !== null && filterLine !== r.name ? 0.3 : 1 }"
            :aria-pressed="filterLine === r.name"
            @click="filterLine = filterLine === r.name ? null : r.name"
          >
            <LineBadge :name="r.name" :color="r.color" :text-color="r.textColor" :locale="config?.locale[0]" />
          </button>
        </div>

        <!-- Hour-grouped board -->
        <div class="px-1 pb-24">
          <template v-if="hourGroups.length">
            <div v-for="hg in hourGroups" :key="hg.hour">
              <div class="flex items-center gap-2 px-4 pt-3 pb-1">
                <span class="text-[13px] font-semibold tabular-nums w-6 text-right" style="color: var(--text-tertiary); font-family: ui-monospace, monospace">{{ hg.hour }}</span>
                <span class="flex-1 h-px" style="background-color: var(--border)" />
              </div>
              <div
                v-for="dep in hg.deps"
                :key="dep.id"
                class="flex items-center gap-2.5 px-4 py-1.5"
              >
                <span class="w-[68px] shrink-0 whitespace-nowrap text-[15px] font-medium tabular-nums" style="color: var(--text-primary); font-family: ui-monospace, monospace">{{ formatClockTime(dep.time, config?.locale[0]) }}</span>
                <LineBadge :name="dep.lineName" :color="dep.color" :text-color="dep.textColor" :locale="config?.locale[0]" />
                <span class="flex-1 min-w-0 truncate text-sm" style="color: var(--text-secondary)">{{ dep.headsign }}</span>
                <span
                  v-if="dep.dock"
                  class="shrink-0 text-[10px] font-extrabold px-1.5 py-0.5 rounded"
                  style="color: var(--text-primary); background-color: var(--bg-secondary); border: 1px solid var(--border)"
                >{{ dep.dock }}</span>
              </div>
            </div>
          </template>
          <div v-else class="text-center py-16">
            <Clock :size="28" :stroke-width="1.5" class="mx-auto mb-2" style="color: var(--text-tertiary)" />
            <p class="text-sm" style="color: var(--text-secondary)">{{ s.noDeparturesToday }}</p>
          </div>
        </div>
      </template>

      <div v-else class="text-center py-16 px-4">
        <p class="text-lg font-semibold" style="color: var(--text-primary)">{{ s.stopNotFound }}</p>
        <NuxtLink to="/" class="mt-4 inline-block text-sm underline" style="color: var(--color-primary)">{{ s.backToHome }}</NuxtLink>
      </div>

    </div>
  </AppLayout>
</template>

<script setup lang="ts">
definePageMeta({ pageTransition: { name: 'page-slide-up', mode: 'out-in' } })
import { ref } from 'vue'
import { ChevronLeft, Clock } from 'lucide-vue-next'
import { decodeDepartures, parseDayGroup, getDayGroupLabel, getTodayDayGroupKey } from '~/utils/schedule'
import { formatClockTime } from '~/utils/clockTime'
import type { Departure, ScheduleStop, Route, DayGroup } from '~/types'

const route = useRoute()
const stopId = computed(() => String(route.params.stopId))
const { config, schedules } = await useOperator()
const s = useStrings(config)

const stop = computed(() =>
  schedules.value?.stops.find((st: ScheduleStop) => st.id === stopId.value) ?? null,
)
if (!stop.value && schedules.value) {
  const byGtfs = schedules.value.stops.find((st: ScheduleStop) => st.gtfsStopIds?.includes(stopId.value))
  if (byGtfs) await navigateTo({ path: `/stop/${byGtfs.id}/schedule` }, { redirectCode: 301 })
}

const departuresByGroup = computed<Record<string, Departure[]>>(() => {
  if (!stop.value || !schedules.value) return {}
  const out: Record<string, Departure[]> = {}
  for (const [key, compact] of Object.entries(stop.value.departures)) {
    out[key] = decodeDepartures(compact, schedules.value, config.value?.headsignMap)
      .sort((a, b) => a.minutesFromMidnight - b.minutesFromMidnight)
  }
  return out
})

const dayGroups = computed<DayGroup[]>(() =>
  Object.keys(departuresByGroup.value).sort().map(parseDayGroup),
)

const selectedKey = ref<string | null>(null)
const filterLine = ref<string | null>(null)

const todayKey = computed(() =>
  stop.value ? getTodayDayGroupKey(stop.value.departures, config.value?.timezone) : null,
)

const activeKey = computed(() =>
  selectedKey.value ?? todayKey.value ?? dayGroups.value[0]?.id ?? null,
)

function selectDay(key: string) {
  selectedKey.value = key
  filterLine.value = null
}

function dayLabel(dg: DayGroup): string {
  return getDayGroupLabel(dg, s.value)
}

const currentDepartures = computed<Departure[]>(() =>
  activeKey.value ? (departuresByGroup.value[activeKey.value] ?? []) : [],
)

const availableLines = computed<string[]>(() => {
  const seen = new Set<string>()
  const out: string[] = []
  for (const d of currentDepartures.value) {
    if (!seen.has(d.lineName)) { seen.add(d.lineName); out.push(d.lineName) }
  }
  return out
})
const availableLineRoutes = computed<Route[]>(() => {
  const byName = new Map((schedules.value?.routes ?? []).map(r => [r.name, r]))
  return availableLines.value.map(n => byName.get(n)).filter((r): r is Route => !!r)
})

const filteredDepartures = computed<Departure[]>(() =>
  filterLine.value ? currentDepartures.value.filter(d => d.lineName === filterLine.value) : currentDepartures.value,
)

const hourGroups = computed<{ hour: string; deps: Departure[] }[]>(() => {
  const map = new Map<string, Departure[]>()
  for (const d of filteredDepartures.value) {
    const hour = d.time.slice(0, 2)
    if (!map.has(hour)) map.set(hour, [])
    map.get(hour)!.push(d)
  }
  return [...map.keys()].sort().map(hour => ({ hour, deps: map.get(hour)! }))
})

useHead(() => ({
  title: stop.value ? `${stop.value.name} — ${s.value.viewFullSchedule}` : s.value.viewFullSchedule,
  meta: [{ name: 'robots', content: 'noindex' }],
}))
useOperatorHead(config)
</script>
