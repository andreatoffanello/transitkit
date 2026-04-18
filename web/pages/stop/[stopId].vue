<template>
  <AppLayout>
    <div class="max-w-lg mx-auto lg:max-w-2xl">

      <!-- Map header — only when coords are valid -->
      <template v-if="!pending && stop && stop.lat && stop.lng">
        <StopMapHeader
          :lat="stop.lat"
          :lng="stop.lng"
          :primary-color="config?.theme?.primaryColor"
        />
      </template>
      <div v-else-if="pending" class="w-full skeleton-shimmer" style="height: 220px" />

      <!-- Back navigation -->
      <div v-if="!pending && stop" class="px-2 pt-2">
        <NuxtLink
          :to="fromLine ? `/lines/${fromLine.id}` : '/'"
          class="inline-flex items-center gap-1.5 text-sm font-medium px-2 py-1.5 rounded-lg transition-opacity active:opacity-60"
          style="color: var(--color-primary)"
        >
          <ChevronLeft :size="16" :stroke-width="2" />
          {{ fromLine ? fromLine.name : s.backToHome }}
        </NuxtLink>
      </div>

      <!-- Stop identity -->
      <div class="px-4 pt-2 pb-2">
        <template v-if="pending">
          <div class="h-7 rounded-lg w-3/4 skeleton-shimmer mb-2" />
          <div class="flex gap-2">
            <div class="h-6 w-12 rounded skeleton-shimmer" />
            <div class="h-6 w-12 rounded skeleton-shimmer" />
            <div class="h-6 w-12 rounded skeleton-shimmer" />
          </div>
        </template>
        <template v-else-if="stop">
          <div class="flex items-start justify-between gap-2">
            <h1 class="text-[22px] font-bold leading-tight" style="color: var(--text-primary)">
              {{ stop.name }}
            </h1>
            <div class="flex items-center gap-0.5 mt-0.5 shrink-0">
              <button
                v-if="config?.features?.enableFavorites"
                type="button"
                :aria-label="isFavorite(stopId) ? s.removeFromFavorites : s.addToFavorites"
                data-testid="btn_favorite"
                class="p-2 rounded-lg transition-opacity active:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
                :style="{ color: isFavorite(stopId) ? 'var(--color-primary)' : 'var(--text-tertiary)' }"
                @click="toggleFavorite({ stopId: stopId, name: stop.name })"
              >
                <Bookmark :size="20" :stroke-width="1.75" :fill="isFavorite(stopId) ? 'currentColor' : 'none'" />
              </button>
              <button
                v-if="canShare"
                type="button"
                :aria-label="s.shareStop"
                class="p-2 rounded-lg transition-opacity active:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
                style="color: var(--text-tertiary)"
                @click="shareStop"
              >
                <Share2 :size="20" :stroke-width="1.75" />
              </button>
              <button
                v-else
                type="button"
                :aria-label="s.copyLink"
                class="p-2 rounded-lg transition-opacity active:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
                style="color: var(--text-tertiary)"
                @click="copyLink"
              >
                <component :is="copied ? Check : Copy" :size="20" :stroke-width="1.75" />
              </button>
            </div>
          </div>
          <!-- Line badges horizontal scroll -->
          <div
            v-if="servingRoutes.length"
            class="flex gap-1.5 mt-2.5 overflow-x-auto scrollbar-none pb-0.5"
            role="list"
          >
            <LineBadge
              v-for="r in servingRoutes"
              :key="r.id"
              :name="r.name"
              :color="r.color"
              :text-color="r.textColor"
              :locale="config?.locale[0]"
              role="listitem"
            />
          </div>
        </template>
      </div>

      <!-- Segmented control: Prossime / Orario — iOS pill style with ARIA tab pattern -->
      <div
        v-if="!pending && stop"
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
          <span v-if="isLive" class="w-1.5 h-1.5 rounded-full bg-green-500 shrink-0" aria-hidden="true" />
        </button>
      </div>

      <template v-if="!pending && stop">

        <!-- ── Prossime partenze ── -->
        <section
          id="panel-prossime"
          role="tabpanel"
          aria-labelledby="tab-prossime"
          v-show="activeTab === 'prossime'"
          class="px-4 mb-6"
        >
          <div class="flex items-center justify-between mb-3">
            <h2 class="text-[17px] font-bold flex items-center gap-2" style="color: var(--text-primary)">
              {{ s.upcomingDepartures }}
              <span
                v-if="isLive"
                class="inline-block w-2 h-2 rounded-full bg-green-500 animate-pulse"
                aria-hidden="true"
              />
            </h2>
            <button
              type="button"
              :aria-label="s.refresh"
              class="flex items-center gap-1 text-xs font-semibold px-2.5 py-1.5 rounded-lg transition-opacity active:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
              style="color: var(--color-primary); background-color: color-mix(in srgb, var(--color-primary) 10%, transparent)"
              @click="refreshRealtime"
            >
              <RefreshCw :size="13" :stroke-width="1.75" :class="{ 'animate-spin': realtimeLoading }" />
              {{ s.refresh }}
            </button>
          </div>

          <!-- Line filter chips (only if >1 line) -->
          <div
            v-if="servingRoutes.length > 1"
            role="group"
            :aria-label="s.ariaLinesAtStop"
            class="flex gap-1.5 mb-3 overflow-x-auto scrollbar-none pb-0.5"
          >
            <button
              class="shrink-0 h-7 px-3 rounded-full text-xs font-semibold transition-all duration-150 active:opacity-70"
              :aria-pressed="filterLine === null"
              :style="filterLine === null
                ? { backgroundColor: 'var(--color-primary)', color: 'var(--color-text-on-primary)' }
                : { backgroundColor: 'var(--bg-elevated)', color: 'var(--text-secondary)', border: '1px solid var(--border)' }"
              @click="filterLine = null"
            >
              {{ s.all }}
            </button>
            <button
              v-for="r in servingRoutes"
              :key="r.id"
              class="shrink-0 h-7 px-3 rounded-full text-xs font-bold transition-all duration-150 active:opacity-70"
              :aria-pressed="filterLine === r.name"
              :style="filterLine === r.name
                ? { backgroundColor: r.color || 'var(--color-primary)', color: r.textColor || 'var(--color-text-on-primary)' }
                : { backgroundColor: 'var(--bg-elevated)', color: 'var(--text-secondary)', border: '1px solid var(--border)' }"
              @click="filterLine = r.name"
            >
              {{ r.name }}
            </button>
          </div>

          <!-- Realtime timestamp -->
          <span
            v-if="isLive && realtimeLastUpdated"
            role="status"
            aria-live="polite"
            class="block text-xs mb-2 tabular-nums"
            style="color: var(--text-tertiary)"
          >
            {{ s.updatedAt }} {{ realtimeLastUpdated }}
          </span>

          <!-- Departure rows -->
          <div
            v-if="filteredUpcomingDepartures.length"
            class="overflow-hidden divide-y"
            style="background-color: var(--bg-elevated); border-radius: 16px; border: 1px solid var(--border)"
          >
            <!-- First row: "prossima" pill above -->
            <div class="relative">
              <span
                class="absolute top-2 left-14 text-[10px] font-bold px-1.5 py-0.5 rounded"
                style="background-color: #16a34a; color: #fff; line-height: 1.2; z-index: 1; pointer-events: none"
                aria-hidden="true"
              >{{ s.nextDepartureLabel }}</span>
              <DepartureRow
                :departure="filteredUpcomingDepartures[0]!"
                :now="now"
                :locale="config?.locale[0]"
                :show-countdown="true"
                class="pt-7"
              />
            </div>
            <DepartureRow
              v-for="dep in filteredUpcomingDepartures.slice(1, showAllUpcoming ? undefined : 5)"
              :key="dep.id"
              :departure="dep"
              :now="now"
              :locale="config?.locale[0]"
              :show-countdown="true"
            />
            <button
              v-if="!showAllUpcoming && filteredUpcomingDepartures.length > 5"
              type="button"
              class="w-full py-3 text-sm font-semibold flex items-center justify-center gap-1.5 transition-opacity active:opacity-60"
              style="color: var(--color-primary)"
              @click="showAllUpcoming = true"
            >
              <ChevronDown :size="15" :stroke-width="2" />
              {{ s.showMore }} {{ filteredUpcomingDepartures.length - 5 }}
            </button>
          </div>

          <!-- Empty state -->
          <div
            v-else
            class="rounded-2xl px-5 py-8 text-center"
            style="background-color: var(--bg-elevated); border: 1px solid var(--border)"
          >
            <div
              class="w-12 h-12 rounded-2xl flex items-center justify-center mx-auto mb-3"
              style="background-color: color-mix(in srgb, var(--color-primary) 8%, var(--bg-secondary))"
            >
              <Clock :size="24" :stroke-width="1.5" style="color: var(--color-primary); opacity: 0.7" />
            </div>
            <p class="text-[15px] font-semibold mb-1" style="color: var(--text-primary)">
              {{ s.noDepartures }}
            </p>
            <div v-if="nextDepartureTodayData" class="flex items-center justify-center gap-2 mt-2">
              <span class="text-sm" style="color: var(--text-secondary)">{{ s.nextDepartureToday }}:</span>
              <LineBadge
                :name="nextDepartureTodayData.lineName"
                :color="nextDepartureTodayData.lineColor"
                :text-color="nextDepartureTodayData.lineTextColor"
                :locale="config?.locale[0]"
              />
              <span class="text-sm font-semibold tabular-nums" style="color: var(--text-primary)">{{ nextDepartureTodayData.time }}</span>
            </div>
            <button
              type="button"
              class="inline-flex items-center gap-1 text-sm font-semibold mt-3 transition-opacity active:opacity-60"
              style="color: var(--color-primary)"
              @click="activeTab = 'orario'"
            >
              {{ s.viewFullSchedule }}
              <ChevronDown :size="14" :stroke-width="1.75" />
            </button>
          </div>

          <!-- Realtime status -->
          <p
            v-if="isLive"
            class="text-xs text-green-600 dark:text-green-400 mt-2 flex items-center gap-1.5"
            role="status"
          >
            <span class="w-1.5 h-1.5 rounded-full bg-green-500 shrink-0" aria-hidden="true" />
            {{ s.updatedRealtime }}
          </p>
          <div aria-live="polite" class="sr-only">{{ isLive ? s.updatedRealtime : '' }}</div>
        </section>

        <!-- ── Orario ── -->
        <section
          id="panel-orario"
          role="tabpanel"
          aria-labelledby="tab-orario"
          v-show="activeTab === 'orario'"
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

        <!-- ── Footer compact ── -->
        <footer class="px-4 pb-8 flex items-center gap-4">
          <a
            v-if="stop.lat && stop.lng"
            :href="`https://maps.google.com/?q=${stop.lat},${stop.lng}`"
            target="_blank"
            rel="noopener noreferrer"
            class="flex items-center gap-1.5 text-sm transition-opacity active:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
            style="color: var(--text-tertiary)"
          >
            <MapPin :size="14" :stroke-width="1.75" />
            Google Maps
          </a>
          <button
            v-if="canShare"
            type="button"
            class="flex items-center gap-1.5 text-sm transition-opacity active:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
            style="color: var(--text-tertiary)"
            @click="shareStop"
          >
            <Share2 :size="14" :stroke-width="1.75" />
            {{ s.share }}
          </button>
          <button
            v-else
            type="button"
            class="flex items-center gap-1.5 text-sm transition-opacity active:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
            style="color: var(--text-tertiary)"
            @click="copyLink"
          >
            <component :is="copied ? Check : Copy" :size="14" :stroke-width="1.75" />
            {{ copied ? s.copiedFeedback : s.copyLink }}
          </button>
        </footer>

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
import { Bookmark, Share2, RefreshCw, MapPin, Copy, Check, Clock, ChevronDown, ChevronLeft } from 'lucide-vue-next'
import { decodeDepartures, getTodayDayGroupKey, parseDayGroup, getNextServiceDayGroupKey, getDayGroupLabel, computeNowMin, getNextDeparture } from '~/utils/schedule'
import type { DayGroup, Departure, ScheduleStop, Route } from '~/types'

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

const now = ref(Date.now())
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

const nowMin = computed(() => computeNowMin(now.value))

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
  const curNowMin = computeNowMin(now.value)
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
  const result = getNextDeparture(stop.value.id, schedules.value, now.value, config.value?.timezone, config.value?.headsignMap)
  if (!result) return null
  const curNowMin = computeNowMin(now.value)
  if (result.minutesFromMidnight <= curNowMin + 120) return null
  const r = schedules.value.routes.find(rt => rt.name === result.lineName)
  return { time: result.time, lineName: result.lineName, lineColor: r?.color, lineTextColor: r?.textColor }
})

// SEO
useHead({
  link: [{ rel: 'canonical', href: computed(() => `${requestUrl.origin}${route.path}`) }],
  script: [{
    type: 'application/ld+json',
    innerHTML: computed(() => {
      if (!stop.value) return '{}'
      const data: Record<string, unknown> = {
        '@context': 'https://schema.org',
        '@type': 'BusStop',
        name: stop.value.name,
        identifier: stop.value.id,
      }
      if (stop.value.lat != null && stop.value.lng != null) {
        data.geo = { '@type': 'GeoCoordinates', latitude: stop.value.lat, longitude: stop.value.lng }
      }
      return JSON.stringify(data)
    }),
  }],
  title: computed(() => {
    const opName = config.value?.fullName ?? config.value?.name ?? ''
    if (!pending.value && !stop.value) return `${s.value.stopNotFound} — ${opName}`
    const stopName = stop.value?.name ?? ''
    const base = stopName ? `${stopName} — ${opName}` : opName
    if (!upcomingDepartures.value.length) return base
    const next = upcomingDepartures.value[0]!
    const titleNowMin = computeNowMin(now.value)
    let diffMin = next.minutesFromMidnight - titleNowMin
    if (next.realtimeDelay !== undefined) diffMin += Math.round(next.realtimeDelay / 60)
    if (diffMin < 0 || diffMin >= 60) return base
    if (diffMin === 0) return `${stopName} · ${next.lineName} ${s.value.now} — ${opName}`
    return `${stopName} · ${next.lineName} ${s.value.nextDepartureIn} ${diffMin} ${s.value.minutesShort} — ${opName}`
  }),
  meta: [
    { property: 'og:title', content: computed(() => stop.value?.name ?? config.value?.fullName ?? config.value?.name ?? '') },
    {
      name: 'description',
      content: computed(() => {
        const stopName = stop.value?.name ?? ''
        const op = config.value?.fullName ?? config.value?.name ?? ''
        const count = servingRoutes.value.length
        let lineCountPart = ''
        if (count > 0) {
          const lineWord = count === 1 ? s.value.lineSingular : s.value.linePlural
          lineCountPart = ` · ${count} ${lineWord}`
        }
        return `${stopName}${lineCountPart} · ${s.value.schedulesAndDepartures}${op ? ` — ${op}` : ''}`
      }),
    },
    { name: 'robots', content: computed(() => (!pending.value && !stop.value) ? 'noindex, nofollow' : 'index, follow') },
  ],
})
useOperatorHead(config)
</script>
