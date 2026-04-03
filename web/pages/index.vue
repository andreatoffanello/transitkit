<template>
  <AppLayout>
    <div class="max-w-lg mx-auto lg:max-w-2xl">

      <!-- Hero -->
      <section
        class="px-5 pt-8 pb-6 relative overflow-hidden"
        style="background: linear-gradient(
          160deg,
          color-mix(in srgb, var(--color-primary) 12%, transparent) 0%,
          color-mix(in srgb, var(--color-primary) 4%, transparent) 45%,
          transparent 70%
        )"
      >
        <!-- Cerchio decorativo sfondo -->
        <div
          class="absolute -top-12 -right-12 w-40 h-40 rounded-full pointer-events-none"
          style="background: radial-gradient(circle, color-mix(in srgb, var(--color-primary) 15%, transparent) 0%, transparent 70%)"
          aria-hidden="true"
        />
        <div
          class="w-12 h-12 rounded-2xl flex items-center justify-center mb-4"
          style="background-color: var(--color-primary)"
        >
          <Bus :size="24" :stroke-width="1.75" style="color: var(--color-text-on-primary)" />
        </div>
        <h1 class="text-2xl font-bold leading-tight mb-1" style="color: var(--text-primary)">
          {{ config?.fullName ?? config?.name }}
        </h1>
        <p class="text-sm" style="color: var(--text-secondary)">
          {{ config?.store?.subtitle ?? 'Orari e partenze in tempo reale' }}
        </p>
      </section>

      <!-- Search bar -->
      <section v-if="schedules" class="px-5 mb-6">
        <div
          class="flex items-center gap-2 rounded-2xl px-4 py-3"
          style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm)"
        >
          <Search :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
          <input
            v-model="searchStopQuery"
            type="search"
            :placeholder="s.searchStops"
            class="flex-1 bg-transparent text-sm outline-none"
            style="color: var(--text-primary)"
          />
          <button
            v-if="searchStopQuery"
            type="button"
            class="shrink-0"
            @click="searchStopQuery = ''"
            aria-label="Cancella ricerca"
          >
            <X :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" />
          </button>
        </div>

        <!-- Search results -->
        <div
          v-if="stopResults.length"
          class="mt-2 rounded-2xl overflow-hidden divide-app"
          style="background-color: var(--bg-elevated); box-shadow: var(--shadow-md); border-color: var(--border)"
        >
          <NuxtLink
            v-for="stop in stopResults"
            :key="stop.id"
            :to="`/stop/${stop.id}`"
            class="flex items-center gap-3 px-4 py-3.5 transition-opacity duration-150 active:opacity-70"
          >
            <MapPin :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
            <div class="flex-1 min-w-0 text-left">
              <span
                class="text-[15px] font-medium truncate block"
                style="color: var(--text-primary)"
                v-html="highlightMatch(stop.name, searchStopQuery)"
              />
              <!-- LineBadge per le linee che servono questa fermata -->
              <div v-if="stopRoutesMap[stop.id]?.length" class="flex gap-1 mt-1 flex-wrap">
                <LineBadge
                  v-for="r in (stopRoutesMap[stop.id] ?? []).slice(0, 3)"
                  :key="r.id"
                  :name="r.name"
                  :color="r.color"
                  :text-color="r.textColor"
                />
                <span
                  v-if="(stopRoutesMap[stop.id]?.length ?? 0) > 3"
                  class="text-[11px] px-1.5 py-0.5 rounded font-medium"
                  style="color: var(--text-tertiary)"
                >
                  +{{ (stopRoutesMap[stop.id]?.length ?? 0) - 3 }}
                </span>
              </div>
            </div>
            <ChevronRight :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
          </NuxtLink>
        </div>
      </section>

      <!-- Main content area -->
      <div class="px-5 pb-10 space-y-6">

        <!-- Fermate nelle vicinanze -->
        <ClientOnly>
          <section v-if="nearbyState !== 'denied' && (nearbyState === 'locating' || nearbyStops.length)">
            <h2 class="text-xs font-semibold uppercase tracking-wider mb-3" style="color: var(--text-tertiary)">
              {{ s.nearbyStops }}
            </h2>

            <!-- Skeleton / locating -->
            <div
              v-if="nearbyState === 'locating'"
              class="rounded-2xl overflow-hidden divide-app"
              style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm); border-color: var(--border)"
            >
              <div v-for="n in 3" :key="n" class="flex items-center gap-3 px-4 py-3.5">
                <div class="w-4 h-4 rounded-full shrink-0 skeleton-shimmer" />
                <div class="flex-1 h-3.5 rounded skeleton-shimmer" />
                <div class="w-10 h-3 rounded skeleton-shimmer" />
              </div>
            </div>

            <!-- Nearby stops list -->
            <div
              v-else-if="nearbyStops.length"
              class="rounded-2xl overflow-hidden divide-app"
              style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm); border-color: var(--border)"
            >
              <NuxtLink
                v-for="item in nearbyStops"
                :key="item.stop.id"
                :to="`/stop/${item.stop.id}`"
                :prefetch="false"
                class="flex items-center gap-3 px-4 py-3.5 transition-opacity duration-150 active:opacity-70"
              >
                <Navigation :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
                <span class="flex-1 text-[15px] font-medium truncate" style="color: var(--text-primary)">
                  {{ item.stop.name }}
                </span>
                <span class="text-xs tabular-nums mr-1" style="color: var(--text-tertiary)">
                  {{ formatDistance(item.distance) }}
                </span>
                <ChevronRight :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
              </NuxtLink>
            </div>
          </section>
        </ClientOnly>

        <!-- Preferiti / Recenti / Empty state — depend on localStorage -->
        <ClientOnly>
          <!-- Preferiti -->
          <section v-if="favoriteStops.length">
            <h2 class="text-xs font-semibold uppercase tracking-wider mb-3" style="color: var(--text-tertiary)">
              {{ s.favoriteStops }}
            </h2>
            <div
              class="rounded-2xl overflow-hidden divide-app"
              style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm); border-color: var(--border)"
            >
              <NuxtLink
                v-for="stop in sortedFavoriteStops"
                :key="stop.stopId"
                :to="`/stop/${stop.stopId}`"
                :prefetch="false"
                class="flex items-center gap-3 px-4 py-3.5 transition-opacity duration-150 active:opacity-70"
              >
                <Star :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
                <span class="flex-1 min-w-0">
                  <span class="flex items-center gap-1.5">
                    <span class="text-[15px] font-medium truncate" style="color: var(--text-primary)">{{ stop.name }}</span>
                    <span
                      v-if="hasRealtime"
                      class="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse flex-shrink-0"
                      aria-hidden="true"
                    />
                  </span>
                  <span
                    v-if="favoriteNextDepartures[stop.stopId]"
                    class="block text-xs tabular-nums truncate"
                    style="color: var(--text-tertiary)"
                  >
                    {{ favoriteNextDepartures[stop.stopId] }}
                  </span>
                </span>
                <ChevronRight :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
              </NuxtLink>
            </div>
          </section>

          <!-- Recenti -->
          <section v-if="recentStops.length">
            <h2 class="text-xs font-semibold uppercase tracking-wider mb-3" style="color: var(--text-tertiary)">
              {{ s.recentStops }}
            </h2>
            <div
              class="rounded-2xl overflow-hidden divide-app"
              style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm); border-color: var(--border)"
            >
              <NuxtLink
                v-for="stop in sortedRecentStops"
                :key="stop.stopId"
                :to="`/stop/${stop.stopId}`"
                :prefetch="false"
                class="flex items-center gap-3 px-4 py-3.5 transition-opacity duration-150 active:opacity-70"
              >
                <Clock :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
                <span class="flex-1 min-w-0">
                  <span class="flex items-center gap-1.5">
                    <span class="text-[15px] font-medium truncate" style="color: var(--text-primary)">{{ stop.name }}</span>
                    <span
                      v-if="hasRealtime"
                      class="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse flex-shrink-0"
                      aria-hidden="true"
                    />
                  </span>
                  <span
                    v-if="recentNextDepartures[stop.stopId]"
                    class="block text-xs tabular-nums truncate"
                    style="color: var(--text-tertiary)"
                  >
                    {{ recentNextDepartures[stop.stopId] }}
                  </span>
                </span>
                <ChevronRight :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
              </NuxtLink>
            </div>
          </section>

          <!-- Empty state onboarding -->
          <section
            v-if="!favoriteStops.length && !recentStops.length"
            class="flex flex-col items-center gap-3 py-10 text-center"
          >
            <div
              class="w-12 h-12 rounded-2xl flex items-center justify-center"
              style="background-color: var(--bg-elevated)"
            >
              <MapPin :size="22" :stroke-width="1.5" style="color: var(--text-tertiary)" />
            </div>
            <p class="text-sm max-w-[240px]" style="color: var(--text-secondary)">{{ s.onboardingHint }}</p>
            <NuxtLink
              to="/lines"
              prefetch
              class="text-sm font-semibold"
              style="color: var(--color-primary)"
            >
              {{ s.linesAndSchedules }}
            </NuxtLink>
          </section>
        </ClientOnly>

        <!-- Contatti -->
        <section v-if="config?.contact?.phone || config?.contact?.email">
          <h2 class="text-xs font-semibold uppercase tracking-wider mb-3" style="color: var(--text-tertiary)">
            {{ s.contacts }}
          </h2>
          <div
            class="rounded-2xl overflow-hidden divide-app"
            style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm); border-color: var(--border)"
          >
            <a
              v-if="config.contact.phone"
              :href="`tel:${config.contact.phone}`"
              class="flex items-center gap-3 px-4 py-3.5 transition-opacity duration-150 active:opacity-70"
            >
              <Phone :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
              <span class="flex-1 text-[15px] font-medium" style="color: var(--text-primary)">{{ config.contact.phone }}</span>
              <ChevronRight :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
            </a>
            <a
              v-if="config.contact.email"
              :href="`mailto:${config.contact.email}`"
              class="flex items-center gap-3 px-4 py-3.5 transition-opacity duration-150 active:opacity-70"
            >
              <Mail :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
              <span class="flex-1 text-[15px] font-medium" style="color: var(--text-primary)">{{ config.contact.email }}</span>
              <ChevronRight :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
            </a>
          </div>
        </section>

        <!-- Sito ufficiale -->
        <section v-if="config?.url || config?.store">
          <div
            class="rounded-2xl overflow-hidden divide-app"
            style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm); border-color: var(--border)"
          >
            <a
              v-if="config?.url"
              :href="config.url"
              target="_blank"
              rel="noopener noreferrer"
              class="flex items-center gap-3 px-4 py-3.5 transition-opacity duration-150 active:opacity-70"
            >
              <Globe :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
              <span class="flex-1 text-[15px] font-medium" style="color: var(--text-primary)">{{ s.officialWebsite }}</span>
              <ChevronRight :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
            </a>
            <NuxtLink
              v-if="config?.store"
              to="/lines"
              prefetch
              class="flex items-center gap-3 px-4 py-3.5 transition-opacity duration-150 active:opacity-70"
            >
              <Smartphone :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
              <span class="flex-1 min-w-0">
                <span class="block text-[15px] font-medium truncate" style="color: var(--text-primary)">{{ config.store.title }}</span>
                <span v-if="config.store.subtitle" class="block text-xs truncate" style="color: var(--text-tertiary)">{{ config.store.subtitle }}</span>
              </span>
              <ChevronRight :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
            </NuxtLink>
          </div>
        </section>

        <!-- Schedule freshness -->
        <div v-if="schedules?.lastUpdated" class="text-center text-xs space-y-0.5" style="color: var(--text-tertiary)">
          <p>{{ s.schedulesUpdated }}: {{ formatDate(schedules.lastUpdated) }}</p>
          <p v-if="schedules.validUntil">{{ s.schedulesValidUntil }}: {{ formatDate(schedules.validUntil) }}</p>
        </div>

        <!-- Privacy link -->
        <a
          v-if="config?.privacyUrl"
          :href="config.privacyUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="block text-center text-xs underline"
          style="color: var(--text-tertiary)"
        >
          {{ s.privacy }}
        </a>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { computeNowMin, getNextDeparture, sortStopsByNextDeparture } from '~/utils/schedule'
import { highlightMatch } from '~/utils/highlight'
import type { ScheduleStop } from '~/types'
import { Bus, Search, X, MapPin, Navigation, Star, Clock, ChevronRight, Phone, Mail, Globe, Smartphone } from 'lucide-vue-next'

const { config, schedules } = await useOperator()
const s = useStrings(config)

const searchStopQuery = ref('')

const stopResults = computed(() => {
  const q = searchStopQuery.value.trim()
  if (!q || !schedules.value) return []
  const norm = q.normalize('NFD').replace(/\p{Mn}/gu, '').toLowerCase()
  return schedules.value.stops
    .filter(s => s.name.normalize('NFD').replace(/\p{Mn}/gu, '').toLowerCase().includes(norm))
    .slice(0, 5)
})

// Mappa stopId → routes che servono quella fermata
const stopRoutesMap = computed(() => {
  if (!schedules.value) return {} as Record<string, typeof schedules.value.routes>
  const map: Record<string, typeof schedules.value.routes> = {}
  for (const route of schedules.value.routes ?? []) {
    for (const dir of route.directions ?? []) {
      for (const stopId of dir.stopIds ?? []) {
        if (!map[stopId]) map[stopId] = []
        if (!map[stopId].find(r => r.id === route.id)) {
          map[stopId].push(route)
        }
      }
    }
  }
  return map
})

const requestUrl = useRequestURL()
const currentRoute = useRoute()

const { recentStops, load } = useRecentStops()
const { favoriteStops, load: loadFavorites } = useFavoriteStops()

type NearbyState = 'idle' | 'locating' | 'ready' | 'denied'
const nearbyState = ref<NearbyState>('idle')
const nearbyStops = ref<{ stop: ScheduleStop; distance: number }[]>([])

function haversineM(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371000
  const dLat = (lat2 - lat1) * Math.PI / 180
  const dLon = (lon2 - lon1) * Math.PI / 180
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLon / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

function formatDistance(m: number): string {
  if (m < 1000) return `${Math.round(m)} ${s.value.distanceM}`
  return `${(m / 1000).toFixed(1)} ${s.value.distanceKm}`
}

const now = ref(Date.now())
onMounted(() => {
  load()
  loadFavorites()
  now.value = Date.now()
  setInterval(() => { now.value = Date.now() }, 30_000)

  if (!schedules.value || !('geolocation' in navigator)) return
  const stopsWithCoords = schedules.value.stops.filter(s => s.lat != null && s.lng != null)
  if (!stopsWithCoords.length) return
  nearbyState.value = 'locating'
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      const { latitude, longitude } = pos.coords
      nearbyStops.value = stopsWithCoords
        .map(stop => ({ stop, distance: haversineM(latitude, longitude, stop.lat!, stop.lng!) }))
        .sort((a, b) => a.distance - b.distance)
        .slice(0, 3)
      nearbyState.value = 'ready'
    },
    () => { nearbyState.value = 'denied' },
    { timeout: 10000, maximumAge: 60000 },
  )
})

function formatDate(dateStr: string | undefined): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  return d.toLocaleDateString('it-IT', { day: 'numeric', month: 'long', year: 'numeric' })
}

const hasRealtime = computed(() => !!config.value?.gtfsRt)

const sortedFavoriteStops = computed(() => {
  if (!schedules.value) return favoriteStops.value
  return sortStopsByNextDeparture(favoriteStops.value, schedules.value, now.value, config.value?.timezone, config.value?.headsignMap)
})

const sortedRecentStops = computed(() => {
  if (!schedules.value) return recentStops.value
  return sortStopsByNextDeparture(recentStops.value, schedules.value, now.value, config.value?.timezone, config.value?.headsignMap)
})

const favoriteNextDepartures = computed<Record<string, string>>(() => {
  const result: Record<string, string> = {}
  const nowMin = computeNowMin(now.value)
  for (const fav of favoriteStops.value) {
    if (!schedules.value) continue
    const dep = getNextDeparture(fav.stopId, schedules.value, now.value, config.value?.timezone, config.value?.headsignMap)
    if (!dep) continue
    const diff = dep.minutesFromMidnight - nowMin
    if (diff < 0) continue
    if (diff === 0) result[fav.stopId] = `${dep.lineName} · ${s.value.now}`
    else if (diff < 60) result[fav.stopId] = `${dep.lineName} · ${diff} ${s.value.minutes}`
    else result[fav.stopId] = `${dep.lineName} · ${dep.time}`
  }
  return result
})

const recentNextDepartures = computed<Record<string, string>>(() => {
  const result: Record<string, string> = {}
  const nowMin = computeNowMin(now.value)
  for (const recent of recentStops.value) {
    if (!schedules.value) continue
    const dep = getNextDeparture(recent.stopId, schedules.value, now.value, config.value?.timezone, config.value?.headsignMap)
    if (!dep) continue
    const diff = dep.minutesFromMidnight - nowMin
    if (diff < 0) continue
    if (diff === 0) result[recent.stopId] = `${dep.lineName} · ${s.value.now}`
    else if (diff < 60) result[recent.stopId] = `${dep.lineName} · ${diff} ${s.value.minutes}`
    else result[recent.stopId] = `${dep.lineName} · ${dep.time}`
  }
  return result
})

useHead({
  title: computed(() => `${config.value?.fullName ?? config.value?.name ?? ''} — Orari e linee`),
  link: [{ rel: 'canonical', href: computed(() => `${requestUrl.origin}${currentRoute.path}`) }],
  script: [
    {
      type: 'application/ld+json',
      innerHTML: computed(() => {
        const data: Record<string, string> = {
          '@context': 'https://schema.org',
          '@type': 'TransitAgency',
          name: config.value?.fullName ?? config.value?.name ?? '',
        }
        if (config.value?.url) data.url = config.value.url
        if (config.value?.region) data.areaServed = config.value.region
        if (config.value?.contact?.phone) data.telephone = config.value.contact.phone
        return JSON.stringify(data)
      }),
    },
  ],
  meta: [
    {
      name: 'description',
      content: computed(() => {
        const name = config.value?.fullName ?? config.value?.name ?? ''
        const region = config.value?.region
        const location = region ? ` — ${region}` : ''
        return `${name}${location}. Orari, linee e fermate. Bus schedule and timetables.`
      }),
    },
    {
      property: 'og:title',
      content: computed(() => config.value?.fullName ?? config.value?.name ?? ''),
    },
    {
      property: 'og:description',
      content: computed(() => {
        const name = config.value?.fullName ?? config.value?.name ?? ''
        const region = config.value?.region
        const location = region ? ` — ${region}` : ''
        return `${name}${location}. Orari, linee e fermate. Bus schedule and timetables.`
      }),
    },
  ],
})
useOperatorHead(config)
</script>
