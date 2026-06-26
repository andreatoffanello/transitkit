<template>
  <AppLayout>
    <div class="max-w-lg mx-auto md:max-w-xl lg:max-w-2xl">

      <!-- Identity skeleton -->
      <div v-if="pending" class="px-5 pt-5 pb-2">
        <div class="flex items-center justify-between gap-3 mb-3">
          <div class="h-7 rounded-lg w-3/4 skeleton-shimmer" />
          <div class="h-9 w-9 rounded-full skeleton-shimmer shrink-0" />
        </div>
        <div class="flex gap-1.5">
          <div class="h-6 w-12 rounded skeleton-shimmer" />
          <div class="h-6 w-14 rounded skeleton-shimmer" />
          <div class="h-6 w-12 rounded skeleton-shimmer" />
        </div>
      </div>

      <template v-else-if="stop">
        <StopHeader
          :stop="stop"
          :from-line="fromLine"
          :serving-routes="servingRoutes"
          :config="config"
          :s="s"
          :is-favorite="isFavorite"
          @toggle-favorite="toggleFavorite({ stopId: stopId, name: stop.name })"
        />

        <StopUpcomingPanel
          :s="s"
          :config="config"
          :stop-id="stopId"
          :has-schedule="hasSchedule"
          :serving-routes="servingRoutes"
          :filtered-upcoming-departures="filteredUpcomingDepartures"
          :filter-line="filterLine"
          :show-all-upcoming="showAllUpcoming"
          :is-live="isLive"
          :realtime-loading="realtimeLoading"
          :realtime-last-updated="realtimeLastUpdated"
          :now="now"
          :next-departure-today-data="nextDepartureTodayData"
          @refresh="refreshRealtime"
          @update:filter-line="filterLine = $event"
          @update:show-all-upcoming="showAllUpcoming = $event"
        />
      </template>

      <!-- Stop not found -->
      <div v-else-if="!pending" role="alert" class="text-center py-16 px-4">
        <p class="text-lg font-semibold" style="color: var(--text-primary)">{{ s.stopNotFound }}</p>
        <p class="text-sm mt-1" style="color: var(--text-tertiary)">ID: {{ stopId }}</p>
        <NuxtLink to="/" class="mt-4 inline-block text-sm underline" style="color: var(--color-primary)">
          {{ s.backToHome }}
        </NuxtLink>
      </div>

    </div>
  </AppLayout>
</template>

<script setup lang="ts">
definePageMeta({ pageTransition: { name: 'page-slide-up', mode: 'out-in' } })
import { onMounted, ref, watch } from 'vue'
import { decodeDepartures, getTodayDayGroupKey, computeNowMin, getNextDeparture } from '~/utils/schedule'
import type { Departure, ScheduleStop, Route } from '~/types'
import { useStopHead } from '~/components/stop/useStopHead'

const filterLine = ref<string | null>(null)
const showAllUpcoming = ref(false)

const route = useRoute()
const stopId = computed(() => String(route.params.stopId))
const requestUrl = useRequestURL()

const { config, schedules } = await useOperator()
const s = useStrings(config)

const fromLineId = computed(() => {
  const from = route.query.from
  return typeof from === 'string' ? from : null
})

const fromLine = computed(() => {
  if (!fromLineId.value || !schedules.value) return null
  return schedules.value.routes.find(r => r.id === fromLineId.value) ?? null
})

// Reset per-stop UI state when navigating to a different stop
watch(stopId, () => {
  filterLine.value = null
  showAllUpcoming.value = false
})

const pending = computed(() => !config.value || !schedules.value)

const stop = computed(() =>
  schedules.value?.stops.find((st: ScheduleStop) => st.id === stopId.value) ?? null,
)

// I QR stampati sulle paline codificano il GTFS stop_id raw (es. /stop/11),
// contratto a lungo termine: risolvi allo slug canonico senza rompere il link.
if (!stop.value && schedules.value) {
  const byGtfsId = schedules.value.stops.find((st: ScheduleStop) => st.gtfsStopIds?.includes(stopId.value))
  if (byGtfsId) await navigateTo({ path: `/stop/${byGtfsId.id}`, query: route.query }, { redirectCode: 301 })
}

const hasSchedule = computed(() => (stop.value ? Object.keys(stop.value.departures).length > 0 : false))

const departuresByGroup = computed<Record<string, Departure[]>>(() => {
  if (!stop.value || !schedules.value) return {}
  const result: Record<string, Departure[]> = {}
  for (const [key, compact] of Object.entries(stop.value.departures)) {
    const deps = decodeDepartures(compact, schedules.value, config.value?.headsignMap)
    result[key] = deps.sort((a: Departure, b: Departure) => a.minutesFromMidnight - b.minutesFromMidnight)
  }
  return result
})

const todayKey = computed(() =>
  stop.value ? getTodayDayGroupKey(stop.value.departures, config.value?.timezone) : null,
)

const servingRoutes = computed((): Route[] => {
  if (!stop.value || !schedules.value) return []
  const routeIds = new Set<string>()
  for (const deps of Object.values(stop.value.departures)) {
    for (const dep of deps) {
      if (dep.length < 2) continue
      const lineIdx = Number(dep[1])
      const routeId = schedules.value.routeIds[lineIdx]
      if (routeId) routeIds.add(routeId)
    }
  }
  const routeMap = new Map(schedules.value.routes.map(r => [r.id, r]))
  return [...routeIds].map(id => routeMap.get(id)).filter((r): r is Route => r !== undefined)
})

// useState: il valore SSR viene serializzato nel payload e riusato al
// hydrate — client e server renderizzano lo stesso minuto (no mismatch).
const now = useState('now-ms', () => Date.now())
let interval: ReturnType<typeof setInterval>
const { addStop } = useRecentStops()
const { load: loadFavorites, toggleFavorite, isFavorite } = useFavoriteStops()

onMounted(() => {
  now.value = Date.now()
  interval = setInterval(() => { now.value = Date.now() }, 30_000)
  loadFavorites()
  if (stop.value) addStop({ stopId: stop.value.id, name: stop.value.name })
})
onUnmounted(() => clearInterval(interval))

const todayDepartures = computed<Departure[]>(() => {
  const key = todayKey.value
  if (!key) return []
  return departuresByGroup.value[key] ?? []
})

const { departures: realtimeDepartures, isLive, isLoading: realtimeLoading, lastUpdated: realtimeLastUpdated, refresh: refreshRealtime } = useRealtime(
  todayDepartures,
  config.value?.gtfsRt?.trip_updates,
)

const upcomingDepartures = computed<Departure[]>(() => {
  const deps = realtimeDepartures.value
  const curNowMin = computeNowMin(now.value, config.value?.timezone)
  return deps
    .filter((d: Departure) => {
      const effectiveMin = d.minutesFromMidnight + Math.round((d.realtimeDelay ?? 0) / 60)
      return effectiveMin >= curNowMin && effectiveMin <= curNowMin + 120
    })
    .slice(0, 15)
})

const filteredUpcomingDepartures = computed<Departure[]>(() => {
  if (!filterLine.value) return upcomingDepartures.value
  return upcomingDepartures.value.filter((d: Departure) => d.lineName === filterLine.value)
})

const nextDepartureTodayData = computed<{ time: string; lineName: string; lineColor?: string; lineTextColor?: string } | null>(() => {
  if (!stop.value || !schedules.value) return null
  const result = getNextDeparture(
    stop.value.id,
    schedules.value,
    now.value,
    config.value?.timezone,
    config.value?.headsignMap,
    filterLine.value,
  )
  if (!result) return null
  const curNowMin = computeNowMin(now.value, config.value?.timezone)
  if (result.minutesFromMidnight <= curNowMin + 120) return null
  const r = schedules.value.routes.find(rt => rt.name === result.lineName)
  return { time: result.time, lineName: result.lineName, lineColor: r?.color, lineTextColor: r?.textColor }
})

useStopHead({
  stop,
  config,
  s,
  pending,
  upcomingDepartures,
  servingRoutes,
  now,
  canonicalHref: computed(() => `${requestUrl.origin}${route.path}`),
})
useOperatorHead(config)
</script>
