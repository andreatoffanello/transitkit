<template>
  <div class="max-w-lg mx-auto px-4 pb-8">
    <!-- Header operatore -->
    <header
      class="sticky top-0 z-10 flex items-center gap-3 px-4 py-3 -mx-4 mb-4"
      :style="{ backgroundColor: config?.theme.primaryColor }"
    >
      <NuxtLink
        to="/"
        aria-label="Torna alla home"
        class="text-sm opacity-70 mr-2"
        :style="{ color: config?.theme.textOnPrimary }"
      >
        ←
      </NuxtLink>
      <span class="text-lg font-bold" :style="{ color: config?.theme.textOnPrimary }">
        {{ config?.name ?? '…' }}
      </span>
    </header>

    <!-- Skeleton loading -->
    <div v-if="pending" class="space-y-4 animate-pulse" aria-busy="true" aria-label="Caricamento">
      <div class="h-8 bg-gray-200 dark:bg-white/10 rounded-lg w-2/3" />
      <div class="h-4 bg-gray-200 dark:bg-white/10 rounded w-1/2" />
      <div class="h-32 bg-gray-200 dark:bg-white/10 rounded-2xl" />
    </div>

    <template v-else-if="stop">
      <!-- Nome fermata + badge linee -->
      <div class="mb-5">
        <h1 class="text-2xl font-bold leading-tight">{{ stop.name }}</h1>
        <div class="flex flex-wrap gap-1.5 mt-2" role="list" aria-label="Linee che fermano qui">
          <LineBadge
            v-for="r in stopRoutes"
            :key="r.id"
            :name="r.name"
            :color="r.color"
            :text-color="r.textColor"
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
          Adesso
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
          />
        </div>
        <p v-else class="text-sm text-gray-400 py-4">
          Nessuna partenza nelle prossime 2 ore.
        </p>

        <!-- Indicatore realtime — placeholder, attivato in T8 -->
        <p
          v-if="isLive"
          class="text-xs text-green-500 mt-2 flex items-center gap-1.5"
          role="status"
        >
          <span class="w-1.5 h-1.5 rounded-full bg-green-500 shrink-0" aria-hidden="true" />
          Aggiornato in tempo reale
        </p>
      </section>

      <!-- Sezione "Orari" con tab day group -->
      <section aria-labelledby="section-orari">
        <h2
          id="section-orari"
          class="text-xs font-semibold uppercase tracking-wider text-gray-400 mb-2"
        >
          Orari
        </h2>

        <DayGroupTabs
          :day-groups="dayGroups"
          :departures-by-day-group="departuresByGroup"
          :initial-key="todayKey ?? undefined"
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
              />
            </div>
            <p v-else class="text-sm text-gray-400 py-4">
              Nessun orario disponibile.
            </p>
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
          📍 Apri in Google Maps
        </a>
      </footer>
    </template>

    <!-- Fermata non trovata -->
    <div v-else role="alert" class="text-center py-16 text-gray-400">
      <p class="text-lg font-medium">Fermata non trovata</p>
      <p class="text-sm mt-1">ID: {{ stopId }}</p>
      <NuxtLink to="/" class="mt-4 inline-block text-accent text-sm underline">
        Torna alla home
      </NuxtLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import { decodeDepartures, getTodayDayGroupKey, parseDayGroup } from '~/utils/schedule'
import type { DayGroup, Departure } from '~/types'

const route = useRoute()
const stopId = computed(() => String(route.params.stopId))

const { config, schedules } = await useOperator()

// pending: true finché almeno uno dei dati non è ancora caricato
const pending = computed(() => !config.value || !schedules.value)

// Fermata corrente
const stop = computed(() =>
  schedules.value?.stops.find(s => s.id === stopId.value) ?? null,
)

// Departures decodificate per ogni day group (ordinate per orario)
const departuresByGroup = computed<Record<string, Departure[]>>(() => {
  if (!stop.value || !schedules.value) return {}
  const result: Record<string, Departure[]> = {}
  for (const [key, compact] of Object.entries(stop.value.departures)) {
    const deps = decodeDepartures(compact, schedules.value, config.value?.headsignMap)
    result[key] = deps.sort((a, b) => a.minutesFromMidnight - b.minutesFromMidnight)
  }
  return result
})

const dayGroups = computed<DayGroup[]>(() =>
  Object.keys(departuresByGroup.value).map(parseDayGroup),
)

const todayKey = computed(() =>
  stop.value ? getTodayDayGroupKey(stop.value.departures) : null,
)

// Badge linee risolte dalle route
const stopRoutes = computed(() => {
  if (!stop.value || !schedules.value) return []
  return stop.value.lines
    .map(lineName => schedules.value!.routes.find(r => r.name === lineName))
    .filter((r): r is NonNullable<typeof r> => r !== undefined)
    .map(r => ({ id: r.id, name: r.name, color: r.color, textColor: r.textColor }))
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
    .filter(d => {
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
</script>
