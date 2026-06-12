<template>
  <AppLayout>
    <div class="max-w-lg mx-auto lg:max-w-2xl">

      <!-- Map header — only when coords are valid -->
      <template v-if="!pending && stop && stop.lat && stop.lng">
        <StopMapHeader
          :lat="stop.lat"
          :lng="stop.lng"
          :primary-color="config?.theme?.primaryColor"
          :aria-open-in-maps="s.openInMaps"
        />
      </template>
      <!-- Hero skeleton: forma prima dei dati — riproduce silhouette del pin Signpost + bottom-corners -->
      <div
        v-else-if="pending"
        class="relative w-full overflow-hidden skeleton-hero"
        aria-hidden="true"
      >
        <div class="absolute inset-0 skeleton-shimmer" />
        <!-- Pin placeholder al centro -->
        <div class="absolute inset-0 flex items-center justify-center">
          <div
            class="rounded-full border-[3px] border-white"
            style="width: 30px; height: 30px; background: var(--border); box-shadow: 0 2px 10px rgba(0,0,0,0.18)"
          />
        </div>
        <!-- Expand FAB placeholder -->
        <div
          class="absolute bottom-3 right-3 rounded-xl"
          style="width: 36px; height: 36px; background: rgba(255,255,255,0.6); backdrop-filter: blur(8px)"
        />
      </div>

      <!-- Identity skeleton -->
      <div v-if="pending" class="px-4 pt-3 pb-2">
        <div class="flex items-center justify-between gap-3 mb-3">
          <div class="h-7 rounded-lg w-3/4 skeleton-shimmer" />
          <div class="h-10 w-10 rounded-full skeleton-shimmer shrink-0" />
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
          :can-share="canShare"
          :copied="copied"
          :is-favorite="isFavorite"
          @toggle-favorite="toggleFavorite({ stopId: stopId, name: stop.name })"
          @share="shareStop"
          @copy-link="copyLink"
        />

        <!-- Segmented control: Prossime / Orario — iOS pill style with ARIA tab pattern -->
        <div
          role="tablist"
          :aria-label="s.upcomingDepartures + ' / ' + s.tabSchedule"
          class="mx-4 mt-3 mb-4 flex p-1 rounded-xl gap-1"
          style="background-color: rgba(120,120,128,0.12)"
        >
          <button
            id="tab-prossime"
            role="tab"
            :aria-selected="activeTab === 'prossime'"
            aria-controls="panel-prossime"
            class="flex-1 py-1.5 px-3 rounded-[10px] text-sm font-semibold transition-all duration-150 active:opacity-70"
            :style="activeTab === 'prossime'
              ? { backgroundColor: 'var(--bg-elevated)', color: 'var(--text-primary)', boxShadow: '0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.08)' }
              : { color: 'var(--text-secondary)' }"
            @click="activeTab = 'prossime'"
          >
            {{ s.tabUpcoming }}
          </button>
          <button
            id="tab-orario"
            role="tab"
            :aria-selected="activeTab === 'orario'"
            aria-controls="panel-orario"
            class="flex-1 py-1.5 px-3 rounded-[10px] text-sm font-semibold transition-all duration-150 active:opacity-70 flex items-center justify-center gap-1.5"
            :style="activeTab === 'orario'
              ? { backgroundColor: 'var(--bg-elevated)', color: 'var(--text-primary)', boxShadow: '0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.08)' }
              : { color: 'var(--text-secondary)' }"
            @click="activeTab = 'orario'"
          >
            {{ s.tabSchedule }}
            <span v-if="isLive" class="w-1.5 h-1.5 rounded-full shrink-0" style="background-color: var(--color-live)" aria-hidden="true" />
          </button>
        </div>

        <StopUpcomingPanel
          v-show="activeTab === 'prossime'"
          :s="s"
          :config="config"
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
          @view-schedule="activeTab = 'orario'"
        />

        <StopSchedulePanel
          v-show="activeTab === 'orario'"
          :s="s"
          :config="config"
          :day-groups="dayGroups"
          :departures-by-group="departuresByGroup"
          :today-key="todayKey"
          :is-live="isLive"
          :now="now"
          :now-min="nowMin"
          :next-service-label="nextServiceLabel"
        />

        <StopShareActions
          :stop="stop"
          :s="s"
          :can-share="canShare"
          :copied="copied"
          @share="shareStop"
          @copy-link="copyLink"
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
import { onMounted, nextTick, ref, watch } from 'vue'
import { decodeDepartures, getTodayDayGroupKey, parseDayGroup, getNextServiceDayGroupKey, getDayGroupLabel, computeNowMin, getNextDeparture } from '~/utils/schedule'
import type { DayGroup, Departure, ScheduleStop, Route } from '~/types'
import { useStopHead } from '~/components/stop/useStopHead'

const activeTab = ref<'prossime' | 'orario'>('prossime')
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
  activeTab.value = 'prossime'
})

// Web Share API
const canShare = ref(false)
onMounted(() => {
  canShare.value = typeof navigator !== 'undefined' && 'share' in navigator
})

async function shareStop() {
  if (!stop.value || !canShare.value) return
  try {
    await navigator.share({ title: stop.value.name, url: window.location.href })
  } catch { /* user cancelled */ }
}

const copied = ref(false)
async function copyLink() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch { /* clipboard not available */ }
}

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

const departuresByGroup = computed<Record<string, Departure[]>>(() => {
  if (!stop.value || !schedules.value) return {}
  const result: Record<string, Departure[]> = {}
  for (const [key, compact] of Object.entries(stop.value.departures)) {
    const deps = decodeDepartures(compact, schedules.value, config.value?.headsignMap)
    result[key] = deps.sort((a: Departure, b: Departure) => a.minutesFromMidnight - b.minutesFromMidnight)
  }
  return result
})

const dayGroups = computed<DayGroup[]>(() =>
  Object.keys(departuresByGroup.value).map(parseDayGroup),
)

const todayKey = computed(() =>
  stop.value ? getTodayDayGroupKey(stop.value.departures, config.value?.timezone) : null,
)

const nextServiceDayKey = computed(() =>
  todayKey.value !== null
    ? null
    : getNextServiceDayGroupKey(stop.value?.departures ?? {}, config.value?.timezone)
)

const nextServiceLabel = computed(() => {
  if (!nextServiceDayKey.value || !stop.value) return null
  const dg = parseDayGroup(nextServiceDayKey.value)
  return getDayGroupLabel(dg, s.value)
})

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

onMounted(async () => {
  now.value = Date.now()
  interval = setInterval(() => { now.value = Date.now() }, 30_000)
  loadFavorites()
  await nextTick()
  const el = document.querySelector<HTMLElement>('[data-departure-future="true"]')
  el?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  if (stop.value) addStop({ stopId: stop.value.id, name: stop.value.name })
})
onUnmounted(() => clearInterval(interval))

const nowMin = computed(() => computeNowMin(now.value, config.value?.timezone))

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
    .slice(0, 10)
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
