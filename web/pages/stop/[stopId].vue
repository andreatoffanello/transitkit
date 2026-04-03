<template>
  <AppLayout>
    <div class="max-w-lg mx-auto lg:max-w-2xl">
      <!-- Header navigazione con azioni -->
      <PageHeader
        :back-to="fromLine ? `/lines/${fromLine.id}?stop=${stopId}` : '/'"
        :back-label="fromLine ? fromLine.name : 'Home'"
        :title="stop?.name ?? config?.name ?? '…'"
      >
        <template #action>
          <div class="flex items-center gap-1">
            <button
              v-if="config?.features?.enableFavorites"
              type="button"
              :aria-label="isFavorite(stopId) ? s.removeFromFavorites : s.addToFavorites"
              class="p-2 rounded-lg transition-opacity active:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
              :style="{ color: isFavorite(stopId) ? 'var(--color-primary)' : 'var(--text-secondary)' }"
              @click="toggleFavorite({ stopId: stopId, name: stop?.name ?? '' })"
            >
              <Star :size="20" :stroke-width="1.75" :fill="isFavorite(stopId) ? 'currentColor' : 'none'" />
            </button>
            <button
              v-if="canShare"
              type="button"
              :aria-label="s.shareStop"
              class="p-2 rounded-lg transition-opacity active:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
              style="color: var(--text-secondary)"
              @click="shareStop"
            >
              <Share2 :size="20" :stroke-width="1.75" />
            </button>
            <button
              v-else
              type="button"
              :aria-label="s.copyLink"
              class="p-2 rounded-lg transition-opacity active:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
              style="color: var(--text-secondary)"
              @click="copyLink"
            >
              <Copy :size="20" :stroke-width="1.75" />
            </button>
          </div>
        </template>
      </PageHeader>

      <!-- Skeleton loading -->
      <div v-if="pending" class="px-4 pb-8 space-y-6 animate-pulse" aria-busy="true" :aria-label="s.ariaLoading">
        <!-- Stop name -->
        <div class="space-y-2 pt-2">
          <div class="h-7 rounded-lg w-3/4" style="background-color: var(--bg-elevated)" />
          <div class="flex gap-2">
            <div class="h-5 w-8 rounded-md" style="background-color: var(--bg-elevated)" />
            <div class="h-5 w-8 rounded-md" style="background-color: var(--bg-elevated)" />
            <div class="h-5 w-8 rounded-md" style="background-color: var(--bg-elevated)" />
          </div>
        </div>

        <!-- Sezione "Prossime partenze" -->
        <div class="space-y-2">
          <div class="h-3 rounded w-28" style="background-color: var(--bg-elevated)" />
          <div class="h-36 rounded-2xl" style="background-color: var(--bg-elevated)" />
        </div>

        <!-- Sezione "Orari" -->
        <div class="space-y-2">
          <div class="h-3 rounded w-16" style="background-color: var(--bg-elevated)" />
          <div class="flex gap-2">
            <div class="h-7 w-20 rounded-full" style="background-color: var(--bg-elevated)" />
            <div class="h-7 w-20 rounded-full" style="background-color: var(--bg-elevated)" />
            <div class="h-7 w-20 rounded-full" style="background-color: var(--bg-elevated)" />
          </div>
          <div class="h-48 rounded-2xl" style="background-color: var(--bg-elevated)" />
        </div>

        <!-- Sezione "Nella rete" -->
        <div class="space-y-2">
          <div class="h-3 w-40 rounded" style="background-color: var(--bg-elevated)" />
          <div class="rounded-2xl px-4 divide-y" style="background-color: var(--bg-elevated); border-color: var(--border)">
            <div v-for="i in 3" :key="i" class="flex items-center gap-3 py-3">
              <div class="w-8 h-5 rounded shrink-0" style="background-color: var(--bg-secondary)" />
              <div class="flex-1 h-3 rounded" style="background-color: var(--bg-secondary)" />
              <div class="w-16 h-3 rounded shrink-0" style="background-color: var(--bg-secondary)" />
            </div>
          </div>
        </div>
      </div>

      <template v-else-if="stop">
        <!-- Sezione "Prossime partenze" — above the fold, massima priorità -->
        <section class="px-4 mb-6" aria-labelledby="section-adesso">
          <div class="flex items-center justify-between mb-3">
            <h2
              id="section-adesso"
              class="text-xs font-semibold uppercase tracking-wider flex items-center gap-2"
              style="color: var(--text-tertiary)"
            >
              {{ s.upcomingDepartures }}
              <span
                v-if="isLive"
                class="w-2 h-2 rounded-full bg-green-500 animate-pulse"
                aria-hidden="true"
              />
            </h2>
            <button
              type="button"
              aria-label="Aggiorna"
              class="flex items-center gap-1.5 text-xs font-medium px-2.5 py-1.5 rounded-lg transition-opacity active:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
              style="color: var(--color-primary)"
              @click="refreshRealtime"
            >
              <RefreshCw :size="14" :stroke-width="1.75" :class="{ 'animate-spin': realtimeLoading }" />
              Aggiorna
            </button>
          </div>

          <!-- Last updated -->
          <span v-if="isLive && realtimeLastUpdated" role="status" aria-live="polite" class="block text-xs tabular-nums mb-2" style="color: var(--text-tertiary)">
            {{ s.updatedAt }} {{ realtimeLastUpdated }}
          </span>

          <!-- Lista partenze -->
          <div
            v-if="upcomingDepartures.length"
            class="rounded-2xl overflow-hidden divide-y"
            style="background-color: var(--bg-elevated); box-shadow: var(--shadow-md); border-color: var(--border)"
          >
            <DepartureRow
              v-for="dep in upcomingDepartures"
              :key="dep.id"
              :departure="dep"
              :now="now"
              :locale="config?.locale[0]"
            />
          </div>

          <!-- Empty state -->
          <div
            v-else
            class="rounded-2xl px-5 py-8 text-center"
            style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm)"
          >
            <div
              class="w-12 h-12 rounded-2xl flex items-center justify-center mx-auto mb-4"
              style="background-color: var(--bg-secondary)"
            >
              <Clock :size="22" :stroke-width="1.5" style="color: var(--text-tertiary)" />
            </div>
            <p class="text-[15px] font-medium mb-1" style="color: var(--text-primary)">
              {{ s.noDepartures }}
            </p>
            <p v-if="nextDepartureTodayHint" class="text-sm" style="color: var(--text-secondary)">
              {{ s.nextDepartureToday }}: {{ nextDepartureTodayHint }}
            </p>
            <p v-else class="text-sm" style="color: var(--text-secondary)">
              {{ s.viewFullSchedule }}
            </p>
          </div>

          <!-- Indicatore realtime -->
          <p
            v-if="isLive"
            class="text-xs text-green-500 mt-2 flex items-center gap-1.5"
            role="status"
          >
            <span class="w-1.5 h-1.5 rounded-full bg-green-500 shrink-0" aria-hidden="true" />
            {{ s.updatedRealtime }}
          </p>

          <!-- Annuncio realtime per screen reader -->
          <div aria-live="polite" class="sr-only">
            {{ isLive ? s.updatedRealtime : '' }}
          </div>

          <a
            href="#section-orari"
            class="block text-center text-sm font-medium mt-3 py-2"
            :style="{ color: config?.theme.primaryColor }"
          >
            {{ s.viewFullSchedule }} <span aria-hidden="true">&#8594;</span>
          </a>
        </section>

        <!-- Sezione "Orario" con DayGroupTabs -->
        <section class="px-4 mb-6" aria-labelledby="section-orari">
          <div class="flex items-center justify-between mb-3">
            <h2
              id="section-orari"
              class="text-xs font-semibold uppercase tracking-wider"
              style="color: var(--text-tertiary)"
            >
              {{ s.todaySchedule }}
            </h2>
          </div>

          <div
            v-if="config?.gtfsRt && !isLive"
            class="text-xs text-center text-amber-600 dark:text-amber-400 py-1.5 px-3 rounded-lg bg-amber-50 dark:bg-amber-900/20 mb-3"
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
                class="rounded-2xl overflow-hidden divide-y"
                style="background-color: var(--bg-elevated); box-shadow: var(--shadow-md); border-color: var(--border)"
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
                style="background-color: var(--bg-elevated)"
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

        <!-- Posizione nella rete -->
        <section v-if="stopPositions.length" class="px-4 mb-6" aria-labelledby="section-network">
          <h2
            id="section-network"
            class="text-xs font-semibold uppercase tracking-wider mb-3"
            style="color: var(--text-tertiary)"
          >
            {{ s.stopInNetwork }}
          </h2>
          <div
            class="rounded-2xl divide-y overflow-hidden"
            style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm); border-color: var(--border)"
          >
            <div
              v-for="(pos, idx) in stopPositions"
              :key="idx"
              class="flex items-center gap-3 px-4 py-3"
            >
              <LineBadge
                :name="pos.route.name"
                :color="pos.route.color"
                :text-color="pos.route.textColor"
                :locale="config?.locale[0]"
              />
              <span class="flex-1 text-sm truncate" style="color: var(--text-secondary)">
                {{ pos.directionName }}
              </span>
              <span class="text-sm font-semibold shrink-0 tabular-nums" style="color: var(--text-primary)">
                {{ s.stopPosition }} {{ pos.position }} {{ s.stopPositionOf }} {{ pos.total }}
              </span>
            </div>
          </div>
        </section>

        <!-- Footer azioni -->
        <footer class="px-4 pb-8">
          <div class="flex gap-3 pt-2">
            <a
              v-if="stop?.lat && stop?.lng"
              :href="`https://maps.google.com/?q=${stop.lat},${stop.lng}`"
              target="_blank"
              rel="noopener noreferrer"
              class="flex-1 flex items-center justify-center gap-2 py-3 rounded-2xl text-sm font-semibold transition-opacity duration-150 active:opacity-70"
              style="background-color: var(--bg-elevated); color: var(--text-primary); box-shadow: var(--shadow-sm); border: 1px solid var(--border)"
            >
              <MapPin :size="16" :stroke-width="1.75" />
              Google Maps
            </a>
            <button
              v-if="canShare"
              type="button"
              class="flex-1 flex items-center justify-center gap-2 py-3 rounded-2xl text-sm font-semibold transition-opacity duration-150 active:opacity-70"
              style="background-color: var(--bg-elevated); color: var(--text-primary); box-shadow: var(--shadow-sm); border: 1px solid var(--border)"
              @click="shareStop"
            >
              <Share2 :size="16" :stroke-width="1.75" />
              Condividi
            </button>
            <button
              v-else
              type="button"
              class="flex-1 flex items-center justify-center gap-2 py-3 rounded-2xl text-sm font-semibold transition-opacity duration-150 active:opacity-70"
              style="background-color: var(--bg-elevated); color: var(--text-primary); box-shadow: var(--shadow-sm); border: 1px solid var(--border)"
              @click="copyLink"
            >
              <component :is="copied ? Check : Copy" :size="16" :stroke-width="1.75" />
              {{ copied ? 'Copiato' : 'Copia link' }}
            </button>
          </div>
        </footer>
      </template>

      <!-- Fermata non trovata -->
      <div v-else role="alert" class="text-center py-16 px-4" style="color: var(--text-tertiary)">
        <p class="text-lg font-medium" style="color: var(--text-primary)">{{ s.stopNotFound }}</p>
        <p class="text-sm mt-1">ID: {{ stopId }}</p>
        <NuxtLink to="/" class="mt-4 inline-block text-sm underline" style="color: var(--color-primary)">
          {{ s.backToHome }}
        </NuxtLink>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { onMounted, nextTick, ref } from 'vue'
import { Star, Share2, RefreshCw, MapPin, Copy, Check, Clock } from 'lucide-vue-next'
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
  script: [
    {
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
          data.geo = {
            '@type': 'GeoCoordinates',
            latitude: stop.value.lat,
            longitude: stop.value.lng,
          }
        }
        return JSON.stringify(data)
      }),
    },
  ],
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
