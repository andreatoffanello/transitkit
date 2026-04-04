<template>
  <AppLayout>
    <PageHeader title="Linee" />

    <div class="max-w-lg mx-auto px-4 pb-8">
      <h1 class="sr-only">{{ s.linesPageTitle }}</h1>

      <div v-if="pending" aria-busy="true" :aria-label="s.ariaLoading">
        <!-- Search bar placeholder -->
        <div class="h-11 rounded-2xl mb-4 skeleton-shimmer" />
        <!-- Filter chips placeholder -->
        <div class="flex gap-2 mb-4">
          <div v-for="i in 4" :key="i" class="h-8 w-16 rounded-full skeleton-shimmer" />
        </div>
        <!-- Route rows — 8 rows -->
        <div class="rounded-2xl overflow-hidden divide-app" style="border-color: var(--border)">
          <div
            v-for="i in 8"
            :key="i"
            class="h-14 skeleton-shimmer"
            style="border-radius: 0"
          />
        </div>
      </div>

      <template v-else>
        <!-- Search bar -->
        <div class="relative mb-4" role="search" :aria-label="s.searchLines">
          <Search
            :size="16"
            :stroke-width="1.75"
            class="pointer-events-none absolute left-3 inset-y-0 my-auto"
            style="color: var(--text-tertiary)"
            aria-hidden="true"
          />
          <input
            v-model="searchQuery"
            type="search"
            :placeholder="s.searchLines"
            class="w-full pl-9 pr-9 py-2.5 rounded-2xl text-sm focus:outline-none focus:ring-2 focus:ring-offset-0"
            :style="{
              backgroundColor: 'var(--bg-elevated)',
              border: '1px solid color-mix(in srgb, var(--color-primary) 22%, var(--border))',
              boxShadow: 'var(--shadow-sm), inset 0 1px 2px rgba(0,0,0,0.04)',
              color: 'var(--text-primary)',
              '--tw-ring-color': config?.theme.primaryColor,
            }"
            :aria-label="s.searchLines"
          />
          <button
            v-if="searchQuery"
            class="absolute right-3 inset-y-0 my-auto flex items-center justify-center w-5 h-5 rounded-full"
            style="color: var(--text-tertiary)"
            :aria-label="s.clearFilters"
            @click="searchQuery = ''"
          >
            <X :size="14" :stroke-width="2" />
          </button>
        </div>

        <!-- Transit type filter chips -->
        <div v-if="availableTypes.length > 1" class="flex gap-2 flex-wrap mb-4">
          <!-- "All" chip -->
          <button
            class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-sm font-medium transition-all active:scale-95"
            :style="selectedType === null
              ? { backgroundColor: 'var(--color-primary)', color: 'var(--color-text-on-primary)' }
              : { backgroundColor: 'var(--bg-elevated)', color: 'var(--text-secondary)', border: '1px solid var(--border)' }"
            :aria-pressed="selectedType === null"
            @click="selectedType = null"
          >
            <Route :size="13" :stroke-width="1.75" aria-hidden="true" />
            {{ s.all }}
            <span
              v-if="selectedType === null"
              class="inline-flex items-center justify-center min-w-[18px] h-[18px] rounded-full text-[10px] font-bold px-1"
              style="background-color: rgba(255,255,255,0.25)"
            >{{ allRoutes.length }}</span>
          </button>
          <!-- Type chips -->
          <button
            v-for="type in availableTypes"
            :key="type"
            class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-sm font-medium transition-all active:scale-95"
            :style="selectedType === type
              ? { backgroundColor: 'var(--color-primary)', color: 'var(--color-text-on-primary)' }
              : { backgroundColor: 'var(--bg-elevated)', color: 'var(--text-secondary)', border: '1px solid var(--border)' }"
            :aria-pressed="selectedType === type"
            @click="selectedType = type"
          >
            <component :is="TRANSIT_LUCIDE_ICONS[type] ?? Bus" :size="13" :stroke-width="1.75" aria-hidden="true" />
            {{ s.transitTypes[type as keyof typeof s.transitTypes] ?? type }}
          </button>
        </div>

        <!-- Results count -->
        <p
          v-if="hasActiveFilter && !pending"
          class="text-xs text-right mb-2"
          style="color: var(--text-tertiary)"
          aria-live="polite"
          aria-atomic="true"
        >
          {{ filteredRoutes.length }} {{ s.linesFound }}
        </p>

        <!-- Lines grouped by type -->
        <div
          v-for="[type, routes] in filteredRoutesByType"
          :key="type"
          class="mb-6"
        >
          <h2 class="text-xs font-semibold uppercase tracking-wider mb-3" style="color: var(--text-tertiary)">
            {{ transitTypeLabel(type) }}
          </h2>

          <div
            class="rounded-2xl overflow-hidden divide-app"
            :style="{ backgroundColor: 'var(--bg-elevated)', boxShadow: 'var(--shadow-sm)', borderColor: 'var(--border)' }"
            role="list"
            :aria-label="`${s.ariaLinesOfTypePrefix} ${transitTypeLabel(type)}`"
          >
            <NuxtLink
              v-for="route in routes"
              :key="route.id"
              :to="`/lines/${route.id}`"
              :prefetch="false"
              role="listitem"
              class="flex items-center gap-3 px-4 py-3 transition-all active:opacity-70 hover-row"
              style="border-color: var(--border)"
            >
              <LineBadge
                :name="route.name"
                :color="route.color"
                :text-color="route.textColor"
                :locale="config?.locale[0]"
                class="shrink-0"
              />
              <span class="flex-1 text-sm font-medium truncate" style="color: var(--text-primary)">
                <span
                  v-if="searchQuery && route.longName"
                  v-html="highlightMatch(route.longName, searchQuery)"
                />
                <span v-else>{{ route.longName ?? route.name }}</span>
              </span>
              <span v-if="stopCountByRoute.get(route.id)" class="text-xs shrink-0" style="color: var(--text-tertiary)">
                {{ stopCountByRoute.get(route.id) }} {{ s.stops }}
              </span>
              <ChevronRight :size="16" :stroke-width="1.75" class="shrink-0" style="color: var(--text-tertiary)" aria-hidden="true" />
            </NuxtLink>
          </div>
        </div>

        <!-- Empty state -->
        <div v-if="filteredRoutes.length === 0 && !pending" class="text-center py-16">
          <div class="inline-flex items-center justify-center w-14 h-14 rounded-2xl mb-4" style="background-color: var(--bg-elevated)">
            <Route :size="24" :stroke-width="1.5" style="color: var(--text-tertiary)" />
          </div>
          <p class="font-semibold mb-1" style="color: var(--text-primary)">{{ s.noLinesFound }}</p>
          <button
            class="text-sm underline mt-1"
            :style="{ color: 'var(--color-primary)' }"
            @click="clearFilters()"
          >
            {{ s.clearFilters }}
          </button>
        </div>
      </template>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import type { TransitType } from '~/types'
import { filterRoutes, sortRoutes } from '~/utils/routes'
import { highlightMatch } from '~/utils/highlight'
import { Bus, TramFront, TrainFront, Ship, ChevronRight, Search, X, Route } from 'lucide-vue-next'

const { config, schedules, pending } = await useOperator()
const s = useStrings(config)

const route = useRoute()
const requestUrl = useRequestURL()
const router = useRouter()

const searchQuery = computed({
  get: () => String(route.query.q ?? ''),
  set: (val: string) => {
    router.replace({ query: { ...route.query, q: val || undefined } })
  },
})

const selectedType = computed({
  get: () => (route.query.type ? String(route.query.type) : null),
  set: (val: string | null) => {
    router.replace({ query: { ...route.query, type: val ?? undefined } })
  },
})

function transitTypeLabel(type: string): string {
  return s.value.transitTypes[type as TransitType] ?? type
}

function clearFilters() {
  router.replace({ query: {} })
}

const TRANSIT_LUCIDE_ICONS: Record<string, unknown> = {
  bus: Bus,
  tram: TramFront,
  rail: TrainFront,
  ferry: Ship,
  metro: TrainFront,
}

const allRoutes = computed(() => schedules.value?.routes ?? [])

const availableTypes = computed(() => {
  const types = new Set(allRoutes.value.map(r => r.transitType).filter(Boolean))
  return [...types] as string[]
})

const hasActiveFilter = computed(() =>
  searchQuery.value !== '' || selectedType.value !== null
)

const filteredRoutes = computed(() =>
  sortRoutes(
    filterRoutes(allRoutes.value, selectedType.value, searchQuery.value),
    config.value?.locale?.[0],
  )
)

// Count unique stop IDs per route across all directions
const stopCountByRoute = computed((): Map<string, number> => {
  const routes = schedules.value?.routes ?? []
  const map = new Map<string, number>()
  for (const route of routes) {
    const uniqueStopIds = new Set<string>()
    for (const dir of route.directions) {
      for (const stopId of dir.stopIds) {
        uniqueStopIds.add(stopId)
      }
    }
    if (uniqueStopIds.size > 0) {
      map.set(route.id, uniqueStopIds.size)
    }
  }
  return map
})

const filteredRoutesByType = computed(() => {
  const map = new Map<string, typeof allRoutes.value>()
  for (const route of filteredRoutes.value) {
    const list = map.get(route.transitType) ?? []
    list.push(route)
    map.set(route.transitType, list)
  }
  return [...map.entries()].sort(([a], [b]) => a.localeCompare(b))
})

useHead({
  title: computed(() => `${s.value.linesPageTitle} — ${config.value?.name ?? ''}`),
  link: [{ rel: 'canonical', href: computed(() => `${requestUrl.origin}${route.path}`) }],
  meta: [
    {
      name: 'description',
      content: computed(() => {
        const name = config.value?.fullName ?? config.value?.name ?? ''
        return `${s.value.linesPageDescription} ${name}.`
      }),
    },
  ],
})
useOperatorHead(config)
</script>
