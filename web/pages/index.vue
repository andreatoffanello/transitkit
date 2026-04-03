<template>
  <div>
    <!-- Hero -->
    <div
      class="flex flex-col items-center justify-center text-center px-6 py-16 min-h-[40vh]"
      style="background-color: var(--color-primary, #003366); color: var(--color-text-on-primary, #ffffff)"
    >
      <h1 class="text-3xl font-bold mb-2">{{ config?.fullName ?? config?.name }}</h1>
      <p v-if="config?.region" class="text-sm opacity-70 mb-8">{{ config.region }}</p>

      <NuxtLink
        to="/lines"
        prefetch
        class="px-6 py-3 rounded-2xl font-semibold text-sm"
        style="background-color: var(--color-text-on-primary, #ffffff); color: var(--color-primary, #003366)"
      >
        {{ s.linesAndSchedules }}
      </NuxtLink>
    </div>

    <!-- Body -->
    <div class="max-w-lg mx-auto px-4 py-8 space-y-4">
      <!-- Store info (app link) -->
      <div v-if="config?.store" class="bg-white dark:bg-white/5 rounded-2xl p-4">
        <p class="text-xs font-semibold uppercase tracking-wider text-gray-400 mb-2">{{ s.officialApp }}</p>
        <p class="font-semibold">{{ config.store.title }}</p>
        <p class="text-sm text-gray-400">{{ config.store.subtitle }}</p>
      </div>

      <!-- Contacts -->
      <div v-if="config?.contact?.phone || config?.contact?.email" class="bg-white dark:bg-white/5 rounded-2xl p-4 space-y-3">
        <p class="text-xs font-semibold uppercase tracking-wider text-gray-400 mb-1">{{ s.contacts }}</p>
        <a
          v-if="config.contact.phone"
          :href="`tel:${config.contact.phone}`"
          class="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-200"
        >
          📞 {{ config.contact.phone }}
        </a>
        <a
          v-if="config.contact.email"
          :href="`mailto:${config.contact.email}`"
          class="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-200"
        >
          ✉️ {{ config.contact.email }}
        </a>
      </div>

      <!-- Official website -->
      <a
        v-if="config?.url"
        :href="config.url"
        target="_blank"
        rel="noopener noreferrer"
        class="flex items-center justify-center gap-2 py-3 rounded-xl bg-gray-100 dark:bg-white/10 text-sm font-medium text-gray-700 dark:text-gray-200 hover:bg-gray-200 dark:hover:bg-white/20 transition-colors"
      >
        🌐 {{ s.officialWebsite }}
      </a>

      <!-- Preferiti / Recenti / Onboarding — client-only: depend on localStorage -->
      <ClientOnly>
        <!-- Preferiti -->
        <div v-if="favoriteStops.length" class="bg-white dark:bg-white/5 rounded-2xl p-4">
          <p class="text-xs font-semibold uppercase tracking-wider text-gray-400 mb-3">{{ s.favoriteStops }}</p>
          <div class="space-y-2">
            <NuxtLink
              v-for="stop in sortedFavoriteStops"
              :key="stop.stopId"
              :to="`/stop/${stop.stopId}`"
              :prefetch="false"
              class="flex items-center justify-between py-1.5"
            >
              <span class="flex flex-col">
                <span class="flex items-center gap-1.5">
                  <span class="text-sm text-gray-900 dark:text-gray-100">{{ stop.name }}</span>
                  <span
                    v-if="hasRealtime"
                    class="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse flex-shrink-0"
                    aria-hidden="true"
                  />
                </span>
                <span v-if="favoriteNextDepartures[stop.stopId]" class="text-xs text-gray-400 tabular-nums">
                  {{ favoriteNextDepartures[stop.stopId] }}
                </span>
              </span>
              <span class="text-gray-400 text-sm" aria-hidden="true">›</span>
            </NuxtLink>
          </div>
        </div>

        <!-- Fermate recenti -->
        <div v-if="recentStops.length" class="bg-white dark:bg-white/5 rounded-2xl p-4">
          <p class="text-xs font-semibold uppercase tracking-wider text-gray-400 mb-3">{{ s.recentStops }}</p>
          <div class="space-y-2">
            <NuxtLink
              v-for="stop in sortedRecentStops"
              :key="stop.stopId"
              :to="`/stop/${stop.stopId}`"
              :prefetch="false"
              class="flex items-center justify-between py-1.5"
            >
              <span class="flex flex-col">
                <span class="flex items-center gap-1.5">
                  <span class="text-sm text-gray-900 dark:text-gray-100">{{ stop.name }}</span>
                  <span
                    v-if="hasRealtime"
                    class="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse flex-shrink-0"
                    aria-hidden="true"
                  />
                </span>
                <span v-if="recentNextDepartures[stop.stopId]" class="text-xs text-gray-400 tabular-nums">
                  {{ recentNextDepartures[stop.stopId] }}
                </span>
              </span>
              <span class="text-gray-400 text-sm" aria-hidden="true">›</span>
            </NuxtLink>
          </div>
        </div>

        <!-- Onboarding empty state -->
        <div
          v-if="!favoriteStops.length && !recentStops.length"
          class="flex flex-col items-center gap-3 py-8 text-center text-gray-400"
        >
          <span class="text-4xl" aria-hidden="true">🚏</span>
          <p class="text-sm max-w-[240px]">{{ s.onboardingHint }}</p>
          <NuxtLink
            to="/lines"
            prefetch
            class="text-sm font-semibold underline"
            :style="{ color: config?.theme.primaryColor }"
          >
            {{ s.linesAndSchedules }}
          </NuxtLink>
        </div>
      </ClientOnly>

      <!-- Schedule freshness -->
      <div v-if="schedules?.lastUpdated" class="text-center text-xs text-gray-400 space-y-0.5">
        <p>{{ s.schedulesUpdated }}: {{ schedules.lastUpdated }}</p>
        <p v-if="schedules.validUntil">{{ s.schedulesValidUntil }}: {{ schedules.validUntil }}</p>
      </div>

      <!-- Privacy link -->
      <a
        v-if="config?.privacyUrl"
        :href="config.privacyUrl"
        target="_blank"
        rel="noopener noreferrer"
        class="block text-center text-xs text-gray-400 underline"
      >
        {{ s.privacy }}
      </a>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computeNowMin, getNextDeparture, sortStopsByNextDeparture } from '~/utils/schedule'

const { config, schedules } = await useOperator()
const s = useStrings(config)

const requestUrl = useRequestURL()
const currentRoute = useRoute()

const { recentStops, load } = useRecentStops()
const { favoriteStops, load: loadFavorites } = useFavoriteStops()

const now = ref(Date.now())
onMounted(() => {
  load()
  loadFavorites()
  now.value = Date.now()
  setInterval(() => { now.value = Date.now() }, 30_000)
})

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
