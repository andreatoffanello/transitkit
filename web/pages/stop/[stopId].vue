<template>
  <div class="max-w-lg mx-auto px-4 pb-8">
    <!-- Header operatore -->
    <PageHeader
      :back-to="fromLine ? `/lines/${fromLine.id}?stop=${stopId}` : '/'"
      :back-text="fromLine ? `${s.lineLabel} ${fromLine.name}` : s.backToHome"
      :back-label="fromLine ? `${s.lineLabel} ${fromLine.name}` : s.backToHome"
      :title="config?.name ?? '…'"
    />

    <!-- Skeleton loading -->
    <div v-if="pending" class="space-y-6 animate-pulse" aria-busy="true" :aria-label="s.ariaLoading">
      <!-- Stop name + line badges -->
      <div class="space-y-2 mb-5">
        <div class="h-7 bg-gray-200 dark:bg-white/10 rounded-lg w-3/4" />
        <div class="flex gap-2">
          <div class="h-5 w-8 bg-gray-200 dark:bg-white/10 rounded-md" />
          <div class="h-5 w-8 bg-gray-200 dark:bg-white/10 rounded-md" />
          <div class="h-5 w-8 bg-gray-200 dark:bg-white/10 rounded-md" />
        </div>
      </div>

      <!-- Sezione "Adesso" -->
      <div class="space-y-2">
        <div class="h-3 bg-gray-200 dark:bg-white/10 rounded w-16" />
        <div class="h-36 bg-gray-200 dark:bg-white/10 rounded-2xl" />
      </div>

      <!-- Sezione "Orari" -->
      <div class="space-y-2">
        <div class="h-3 bg-gray-200 dark:bg-white/10 rounded w-12" />
        <div class="flex gap-2">
          <div class="h-7 w-20 bg-gray-200 dark:bg-white/10 rounded-full" />
          <div class="h-7 w-20 bg-gray-200 dark:bg-white/10 rounded-full" />
          <div class="h-7 w-20 bg-gray-200 dark:bg-white/10 rounded-full" />
        </div>
        <div class="h-48 bg-gray-200 dark:bg-white/10 rounded-2xl" />
      </div>

      <!-- Sezione "Questa fermata nella rete" -->
      <div class="space-y-2">
        <div class="h-3 w-40 bg-gray-200 dark:bg-white/10 rounded" />
        <div class="bg-white dark:bg-white/5 rounded-2xl px-4 divide-y divide-gray-100 dark:divide-white/5">
          <div v-for="i in 3" :key="i" class="flex items-center gap-3 py-3">
            <div class="w-8 h-5 bg-gray-200 dark:bg-white/10 rounded shrink-0" />
            <div class="flex-1 h-3 bg-gray-200 dark:bg-white/10 rounded" />
            <div class="w-16 h-3 bg-gray-200 dark:bg-white/10 rounded shrink-0" />
          </div>
        </div>
      </div>
    </div>

    <template v-else-if="stop">
      <!-- Nome fermata + badge linee -->
      <div>
        <div class="flex items-start justify-between gap-2 mb-5">
          <div class="flex items-center gap-2">
            <h1 class="text-2xl font-bold leading-tight">{{ stop.name }}</h1>
            <span v-if="firstStopPosition" class="text-xs text-gray-400 dark:text-gray-500 whitespace-nowrap">
              • {{ s.stopPosition }} {{ firstStopPosition.position }} {{ s.stopPositionOf }} {{ firstStopPosition.total }}
            </span>
          </div>
          <button
            type="button"
            :aria-label="isFavorite(stopId) ? s.removeFromFavorites : s.addToFavorites"
            class="text-2xl shrink-0 mt-1 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
            @click="toggleFavorite({ stopId: stopId, name: stop.name })"
          >
            {{ isFavorite(stopId) ? '★' : '☆' }}
          </button>
        </div>
        <div
          v-if="servingRoutes.length"
          class="flex flex-wrap gap-1.5 mt-2"
          role="list"
          :aria-label="s.ariaLinesAtStop"
        >
          <NuxtLink
            v-for="r in servingRoutes"
            :key="r.id"
            :to="`/lines/${r.id}`"
            role="listitem"
            class="inline-flex active:scale-95 transition-transform duration-100"
            :aria-label="`${s.lineLabel} ${r.name}`"
          >
            <LineBadge
              :name="r.name"
              :color="r.color"
              :text-color="r.textColor"
              :locale="config?.locale[0]"
            />
          </NuxtLink>
        </div>
      </div>

      <!-- Sezione "Adesso" -->
      <section class="mb-6" aria-labelledby="section-adesso">
        <h2
          id="section-adesso"
          class="text-xs font-semibold uppercase tracking-wider text-gray-400 mb-2 flex items-center gap-2"
        >
          {{ s.upcomingDepartures }}
          <span
            v-if="isLive"
            class="w-2 h-2 rounded-full bg-green-500 animate-pulse"
            aria-hidden="true"
          />
          <button
            v-if="isLive"
            type="button"
            aria-label="Aggiorna"
            class="ml-auto text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 active:scale-95 transition-transform duration-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
            :class="{ 'animate-spin': realtimeLoading }"
            @click="refreshRealtime"
          >
            ↺
          </button>
        </h2>

        <span v-if="isLive && realtimeLastUpdated" role="status" aria-live="polite" class="text-xs text-gray-400 tabular-nums">
          {{ s.updatedAt }} {{ realtimeLastUpdated }}
        </span>

        <ul
          v-if="upcomingDepartures.length"
          role="list"
          class="bg-white dark:bg-white/5 rounded-2xl px-4 divide-y divide-gray-100 dark:divide-white/5 list-none p-0 m-0"
        >
          <li
            v-for="dep in upcomingDepartures"
            :key="dep.id"
            class="list-none"
          >
            <DepartureRow
              :departure="dep"
              :now="now"
              :locale="config?.locale[0]"
            />
          </li>
        </ul>
        <div v-else class="text-sm text-gray-400 py-4">
          <p>{{ s.noDepartures }}</p>
          <p v-if="nextDepartureTodayHint" class="text-xs text-gray-400 mt-1">
            {{ s.nextDepartureToday }}: {{ nextDepartureTodayHint }}
          </p>
        </div>

        <!-- Annuncio realtime per screen reader -->
        <div aria-live="polite" class="sr-only">
          {{ isLive ? s.updatedRealtime : '' }}
        </div>

        <!-- Indicatore realtime — visibile solo quando il feed GTFS-RT è attivo -->
        <p
          v-if="isLive"
          class="text-xs text-green-500 mt-2 flex items-center gap-1.5"
          role="status"
        >
          <span class="w-1.5 h-1.5 rounded-full bg-green-500 shrink-0" aria-hidden="true" />
          {{ s.updatedRealtime }}
        </p>

        <a
          href="#section-orari"
          class="block text-center text-sm font-medium mt-3 py-2"
          :style="{ color: config?.theme.primaryColor }"
        >
          {{ s.viewFullSchedule }} <span aria-hidden="true">→</span>
        </a>
      </section>

      <!-- Sezione "Orari" con tab day group -->
      <section aria-labelledby="section-orari">
        <h2
          id="section-orari"
          class="text-xs font-semibold uppercase tracking-wider text-gray-400 mt-6 mb-2"
        >
          {{ s.todaySchedule }}
        </h2>

        <div
          v-if="config?.gtfsRt && !isLive"
          class="text-xs text-center text-amber-600 dark:text-amber-400 py-1 bg-amber-50 dark:bg-amber-900/20"
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
            <ul
              v-if="departures.length"
              role="list"
              class="bg-white dark:bg-white/5 rounded-2xl px-4 list-none p-0 m-0"
            >
              <li
                v-for="(dep, index) in departures"
                :key="dep.id"
                class="list-none"
                :data-departure-future="dep.minutesFromMidnight >= nowMin && departures.findIndex((d: Departure) => d.minutesFromMidnight >= nowMin) === index ? 'true' : undefined"
              >
                <DepartureRow
                  :departure="dep"
                  :now="now"
                  :locale="config?.locale[0]"
                />
              </li>
            </ul>
            <div
              v-else
              class="text-center py-12 text-gray-400"
            >
              <p class="font-semibold text-gray-600 dark:text-gray-300 mb-1">{{ s.noDeparturesToday }}</p>
              <p class="text-sm">{{ s.noDeparturesHint }}</p>
              <p v-if="nextServiceLabel" class="text-sm text-gray-500 dark:text-gray-400 mt-1">
                {{ s.nextServiceDay }}: {{ nextServiceLabel }}
              </p>
            </div>
          </template>
        </DayGroupTabs>
      </section>

      <!-- Posizione nella rete -->
      <section v-if="stopPositions.length" class="mt-6" aria-labelledby="section-network">
        <h2
          id="section-network"
          class="text-xs font-semibold uppercase tracking-wider text-gray-400 mb-2"
        >
          {{ s.stopInNetwork }}
        </h2>
        <div class="bg-white dark:bg-white/5 rounded-2xl px-4 divide-y divide-gray-100 dark:divide-white/5">
          <div
            v-for="(pos, idx) in stopPositions"
            :key="idx"
            class="flex items-center gap-3 py-3"
          >
            <LineBadge
              :name="pos.route.name"
              :color="pos.route.color"
              :text-color="pos.route.textColor"
              :locale="config?.locale[0]"
            />
            <span class="flex-1 text-sm text-gray-600 dark:text-gray-400 truncate">
              {{ pos.directionName }}
            </span>
            <span class="text-sm font-semibold text-gray-900 dark:text-gray-100 shrink-0 tabular-nums">
              {{ s.stopPosition }} {{ pos.position }} {{ s.stopPositionOf }} {{ pos.total }}
            </span>
          </div>
        </div>
      </section>

      <!-- Footer -->
      <footer class="mt-8 flex flex-col gap-3 text-sm">
        <a
          :href="`https://maps.google.com/?q=${stop.lat},${stop.lng}&query=${encodeURIComponent(stop.name)}`"
          target="_blank"
          rel="noopener noreferrer"
          class="flex items-center justify-center gap-2 py-3 rounded-xl bg-gray-100 dark:bg-white/10 text-gray-700 dark:text-gray-200 font-medium hover:bg-gray-200 dark:hover:bg-white/20 transition-colors"
        >
          📍 {{ s.openInGoogleMaps }}
        </a>
        <button
          v-if="canShare"
          type="button"
          class="flex items-center justify-center gap-2 py-3 rounded-xl bg-gray-100 dark:bg-white/10 text-gray-700 dark:text-gray-200 font-medium hover:bg-gray-200 dark:hover:bg-white/20 transition-colors active:scale-95 transition-transform duration-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
          @click="shareStop"
        >
          📤 {{ s.shareStop }}
        </button>
        <button
          v-else
          @click="copyLink"
          class="flex items-center gap-1.5 text-sm text-gray-500 dark:text-gray-400 active:scale-95 transition-transform duration-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
          :aria-label="s.copyLink"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
          </svg>
          <span>{{ copied ? s.copiedFeedback : s.copyLink }}</span>
        </button>
      </footer>
    </template>

    <!-- Fermata non trovata -->
    <div v-else role="alert" class="text-center py-16 text-gray-400">
      <p class="text-lg font-medium">{{ s.stopNotFound }}</p>
      <p class="text-sm mt-1">ID: {{ stopId }}</p>
      <NuxtLink to="/" class="mt-4 inline-block text-accent text-sm underline">
        {{ s.backToHome }}
      </NuxtLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, nextTick, ref } from 'vue'
import { decodeDepartures, getTodayDayGroupKey, parseDayGroup, getNextServiceDayGroupKey, getDayGroupLabel, computeNowMin, getNextDeparture } from '~/utils/schedule'
import type { DayGroup, Departure, ScheduleStop, Route } from '~/types'

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

// Web Share API
const canShare = ref(false)
onMounted(() => {
  canShare.value = typeof navigator !== 'undefined' && 'share' in navigator
})

async function shareStop() {
  if (!stop.value || !canShare.value) return
  try {
    await navigator.share({
      title: stop.value.name,
      url: window.location.href,
    })
  } catch { /* user cancelled or not supported */ }
}

const copied = ref(false)
async function copyLink() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch { /* clipboard not available */ }
}

// pending: true finché almeno uno dei dati non è ancora caricato
const pending = computed(() => !config.value || !schedules.value)

// Fermata corrente
const stop = computed(() =>
  schedules.value?.stops.find((st: ScheduleStop) => st.id === stopId.value) ?? null,
)

// Departures decodificate per ogni day group (ordinate per orario)
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

// Posizione della fermata nelle direzioni di ogni linea che la serve
const stopPositions = computed(() => {
  if (!schedules.value || !stop.value) return []
  const sid = stopId.value
  return servingRoutes.value.flatMap(r =>
    r.directions
      .map((dir, dirIdx) => {
        const pos = dir.stopIds.indexOf(sid)
        if (pos === -1) return null
        return {
          route: r,
          directionName: dir.headsign ?? (dirIdx === 0 ? '→' : '←'),
          position: pos + 1,
          total: dir.stopIds.length,
        }
      })
      .filter((x): x is NonNullable<typeof x> => x !== null)
  )
})

// First stop position (for badge near heading)
const firstStopPosition = computed(() => stopPositions.value[0] ?? null)

// Linee che servono effettivamente questa fermata, derivate dai dati di partenza
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

// Tick ogni 30s per aggiornare countdown
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
  if (stop.value) {
    addStop({ stopId: stop.value.id, name: stop.value.name })
  }
})
onUnmounted(() => clearInterval(interval))

// Minutes since midnight — used to mark the first upcoming departure in the schedule
const nowMin = computed(() => computeNowMin(now.value))

// Real-time — client-side con fallback silenzioso
const todayDepartures = computed<Departure[]>(() => {
  const key = todayKey.value
  if (!key) return []
  return departuresByGroup.value[key] ?? []
})

// SAFETY: config.value is guaranteed to be populated here because `await useOperator()`
// above suspends the component setup (Nuxt 3 <Suspense>) until the operator config
// resolves. The snapshot passed to useRealtime() is therefore stable and correct.
const { departures: realtimeDepartures, isLive, isLoading: realtimeLoading, lastUpdated: realtimeLastUpdated, refresh: refreshRealtime } = useRealtime(
  todayDepartures,
  config.value?.gtfsRt?.trip_updates,
)

// Prossime 6 partenze nelle prossime 2 ore (oggi)
const upcomingDepartures = computed<Departure[]>(() => {
  const deps = realtimeDepartures.value
  const nowMin = computeNowMin(now.value)
  return deps
    .filter((d: Departure) => {
      const effectiveMin = d.minutesFromMidnight + Math.round((d.realtimeDelay ?? 0) / 60)
      return effectiveMin >= nowMin && effectiveMin <= nowMin + 120
    })
    .slice(0, 6)
})

// First departure beyond the 2h window — shown when upcoming list is empty
const nextDepartureTodayHint = computed<string | null>(() => {
  if (!stop.value || !schedules.value) return null
  const result = getNextDeparture(stop.value.id, schedules.value, now.value, config.value?.timezone, config.value?.headsignMap)
  if (!result) return null
  const curNowMin = computeNowMin(now.value)
  if (result.minutesFromMidnight <= curNowMin + 120) return null
  return `${result.time} (${result.lineName})`
})

// SEO
useHead({
  link: [{ rel: 'canonical', href: computed(() => `${requestUrl.origin}${route.path}`) }],
  title: computed(() => {
    const opName = config.value?.fullName ?? config.value?.name ?? ''

    // Not-found check: show "Stop not found — OpName" when stop is null and not loading
    if (!pending.value && !stop.value) {
      return `${s.value.stopNotFound} — ${opName}`
    }

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
    {
      property: 'og:title',
      content: computed(() => stop.value?.name ?? config.value?.fullName ?? config.value?.name ?? ''),
    },
    {
      name: 'description',
      content: computed(() => {
        const stopName = stop.value?.name ?? ''
        const op = config.value?.fullName ?? config.value?.name ?? ''
        const count = servingRoutes.value.length

        // Build line count part if there are serving routes
        let lineCountPart = ''
        if (count > 0) {
          const lineWord = count === 1 ? s.value.lineSingular : s.value.linePlural
          lineCountPart = ` · ${count} ${lineWord}`
        }

        return `${stopName}${lineCountPart} · ${s.value.schedulesAndDepartures}${op ? ` — ${op}` : ''}`
      }),
    },
    {
      name: 'robots',
      content: computed(() => (!pending.value && !stop.value) ? 'noindex, nofollow' : 'index, follow'),
    },
  ],
})
useOperatorHead(config)
</script>
