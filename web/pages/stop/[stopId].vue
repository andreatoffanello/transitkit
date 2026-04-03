<template>
  <div class="max-w-lg mx-auto px-4 pb-8">
    <!-- Header operatore -->
    <PageHeader
      :primary-color="config?.theme.primaryColor"
      :text-color="config?.theme.textOnPrimary"
      back-to="/"
      :back-label="s.backToHome"
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
    </div>

    <template v-else-if="stop">
      <!-- Nome fermata + badge linee -->
      <div class="mb-5">
        <h1 class="text-2xl font-bold leading-tight">{{ stop.name }}</h1>
        <div class="flex flex-wrap gap-1.5 mt-2" role="list" :aria-label="s.ariaLinesAtStop">
          <LineBadge
            v-for="r in stopRoutes"
            :key="r.id"
            :name="r.name"
            :color="r.color"
            :text-color="r.textColor"
            :locale="config?.locale[0]"
            role="listitem"
          />
        </div>
      </div>

      <!-- Sezione "Adesso" -->
      <section class="mb-6" aria-labelledby="section-adesso">
        <h2
          id="section-adesso"
          class="text-xs font-semibold uppercase tracking-wider text-gray-400 mb-2"
        >
          {{ s.sectionNow }}
        </h2>

        <div
          v-if="upcomingDepartures.length"
          class="bg-white dark:bg-white/5 rounded-2xl px-4 divide-y divide-gray-100 dark:divide-white/5"
        >
          <DepartureRow
            v-for="dep in upcomingDepartures"
            :key="dep.id"
            :departure="dep"
            :now="now"
            :locale="config?.locale[0]"
          />
        </div>
        <p v-else class="text-sm text-gray-400 py-4">
          {{ s.noDepartures }}
        </p>

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
      </section>

      <!-- Sezione "Orari" con tab day group -->
      <section aria-labelledby="section-orari">
        <h2
          id="section-orari"
          class="text-xs font-semibold uppercase tracking-wider text-gray-400 mb-2"
        >
          {{ s.sectionSchedule }}
        </h2>

        <div
          v-if="config?.gtfsRt && !isLive"
          class="text-xs text-center text-amber-600 dark:text-amber-400 py-1 bg-amber-50 dark:bg-amber-900/20"
          role="status"
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
              class="bg-white dark:bg-white/5 rounded-2xl px-4"
            >
              <DepartureRow
                v-for="dep in departures"
                :key="dep.id"
                :departure="dep"
                :now="now"
                :locale="config?.locale[0]"
              />
            </div>
            <div
              v-else
              class="text-center py-12 text-gray-400"
            >
              <p class="font-semibold text-gray-600 dark:text-gray-300 mb-1">{{ s.noDeparturesToday }}</p>
              <p class="text-sm">{{ s.noDeparturesHint }}</p>
            </div>
          </template>
        </DayGroupTabs>
      </section>

      <!-- Footer -->
      <footer class="mt-8 flex flex-col gap-3 text-sm">
        <a
          :href="`https://maps.google.com/?q=${stop.lat},${stop.lng}`"
          target="_blank"
          rel="noopener noreferrer"
          class="flex items-center justify-center gap-2 py-3 rounded-xl bg-gray-100 dark:bg-white/10 text-gray-700 dark:text-gray-200 font-medium hover:bg-gray-200 dark:hover:bg-white/20 transition-colors"
        >
          📍 {{ s.openInGoogleMaps }}
        </a>
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
import { decodeDepartures, getTodayDayGroupKey, parseDayGroup } from '~/utils/schedule'
import type { DayGroup, Departure, ScheduleStop, Route } from '~/types'

const route = useRoute()
const stopId = computed(() => String(route.params.stopId))

const { config, schedules } = await useOperator()
const s = useStrings(config)

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

// Badge linee risolte dalle route
const stopRoutes = computed(() => {
  if (!stop.value || !schedules.value) return []
  return stop.value.lines
    .map((lineName: string) => schedules.value!.routes.find((r: Route) => r.name === lineName))
    .filter((r: Route | undefined): r is Route => r !== undefined)
    .map((r: Route) => ({ id: r.id, name: r.name, color: r.color, textColor: r.textColor }))
})

// Tick ogni 30s per aggiornare countdown
const now = ref(Date.now())
let interval: ReturnType<typeof setInterval>
onMounted(() => {
  now.value = Date.now()
  interval = setInterval(() => { now.value = Date.now() }, 30_000)
})
onUnmounted(() => clearInterval(interval))

// Real-time — client-side con fallback silenzioso
const todayDepartures = computed<Departure[]>(() => {
  const key = todayKey.value
  if (!key) return []
  return departuresByGroup.value[key] ?? []
})

// SAFETY: config.value is guaranteed to be populated here because `await useOperator()`
// above suspends the component setup (Nuxt 3 <Suspense>) until the operator config
// resolves. The snapshot passed to useRealtime() is therefore stable and correct.
const { departures: realtimeDepartures, isLive } = useRealtime(
  todayDepartures,
  config.value?.gtfsRt?.trip_updates,
)

// Prossime 6 partenze nelle prossime 2 ore (oggi)
const upcomingDepartures = computed<Departure[]>(() => {
  const deps = realtimeDepartures.value
  const midnight = new Date(now.value)
  midnight.setHours(0, 0, 0, 0)
  const nowMin = Math.floor((now.value - midnight.getTime()) / 60_000)
  return deps
    .filter((d: Departure) => {
      const effectiveMin = d.minutesFromMidnight + Math.round((d.realtimeDelay ?? 0) / 60)
      return effectiveMin >= nowMin && effectiveMin <= nowMin + 120
    })
    .slice(0, 6)
})

// SEO
useHead({
  title: computed(() =>
    stop.value ? `${stop.value.name} — ${config.value?.name ?? ''}` : 'Fermata',
  ),
  meta: [
    {
      name: 'description',
      content: computed(() =>
        stop.value
          ? `Orari e prossime partenze dalla fermata ${stop.value.name}. Linee: ${stop.value.lines.join(', ')}.`
          : 'Fermata non trovata',
      ),
    },
    { property: 'og:title', content: computed(() => stop.value?.name ?? 'Fermata') },
  ],
})
useOperatorHead(config)
</script>
