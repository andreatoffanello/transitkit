<template>
  <div class="max-w-lg mx-auto px-4 pb-8">
    <PageHeader
      :primary-color="config?.theme.primaryColor"
      :text-color="config?.theme.textOnPrimary"
      back-to="/"
      :back-text="config?.name ?? ''"
      :back-label="s.backToHome"
      :title="s.linesPageTitle"
      margin-bottom="mb-6"
    />

    <div v-if="pending" aria-busy="true" :aria-label="s.ariaLoading">
      <!-- Search bar placeholder -->
      <div class="h-10 bg-gray-200 dark:bg-white/10 rounded-xl mb-4 animate-pulse" />
      <!-- Route rows — 8 rows -->
      <div class="space-y-2">
        <div
          v-for="i in 8"
          :key="i"
          class="h-14 bg-gray-200 dark:bg-white/10 rounded-xl animate-pulse"
        />
      </div>
    </div>

    <template v-else>
      <!-- Transit type filter chips -->
      <div v-if="availableTypes.length > 1" class="flex gap-2 flex-wrap mb-4">
        <!-- "All" chip -->
        <button
          class="px-3 py-1 rounded-full text-sm font-medium transition-colors"
          :class="selectedType === null ? 'text-white' : 'bg-gray-100 dark:bg-white/10 text-gray-600 dark:text-gray-300'"
          :style="selectedType === null ? { backgroundColor: config?.theme.primaryColor } : {}"
          @click="selectedType = null"
        >
          {{ s.all }}
        </button>
        <!-- Type chips -->
        <button
          v-for="type in availableTypes"
          :key="type"
          class="px-3 py-1 rounded-full text-sm font-medium transition-colors"
          :class="selectedType === type ? 'text-white' : 'bg-gray-100 dark:bg-white/10 text-gray-600 dark:text-gray-300'"
          :style="selectedType === type ? { backgroundColor: config?.theme.primaryColor } : {}"
          @click="selectedType = type"
        >
          {{ s.transitTypes[type as keyof typeof s.transitTypes] ?? type }}
        </button>
      </div>

      <!-- Search bar -->
      <div class="relative mb-4">
        <input
          v-model="searchQuery"
          type="search"
          :placeholder="s.searchLines"
          class="w-full px-4 py-2 pr-10 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-offset-0"
          :style="{ '--tw-ring-color': config?.theme.primaryColor }"
          aria-label="Cerca linea"
        />
        <span class="pointer-events-none absolute inset-y-0 right-3 flex items-center text-gray-400">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-4.35-4.35M17 11A6 6 0 111 11a6 6 0 0116 0z" />
          </svg>
        </span>
      </div>

      <div
        v-for="[type, routes] in filteredRoutesByType"
        :key="type"
        class="mb-6"
      >
        <h2 class="text-xs font-semibold uppercase tracking-wider text-gray-400 mb-3">
          {{ transitTypeLabel(type) }}
        </h2>
        <div class="flex flex-wrap gap-2" role="list" :aria-label="`Linee ${transitTypeLabel(type)}`">
          <NuxtLink
            v-for="route in routes"
            :key="route.id"
            :to="`/lines/${route.id}`"
            role="listitem"
            class="flex flex-col items-center gap-1"
          >
            <LineBadge
              :name="route.name"
              :color="route.color"
              :text-color="route.textColor"
              :locale="config?.locale[0]"
              class="text-base px-3 py-1"
            />
            <span v-if="stopCountByRoute.get(route.id)" class="text-xs text-gray-400">
              {{ stopCountByRoute.get(route.id) }} {{ s.stops }}
            </span>
          </NuxtLink>
        </div>
      </div>

      <div v-if="filteredRoutesByType.length === 0" class="text-center py-16 text-gray-400">
        {{ s.noLines }}
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { TransitType } from '~/types'
import { filterRoutes } from '~/utils/routes'

const { config, schedules, pending } = await useOperator()
const s = useStrings(config)

const selectedType = ref<string | null>(null)
const searchQuery = ref('')

function transitTypeLabel(type: string): string {
  return s.value.transitTypes[type as TransitType] ?? type
}

const allRoutes = computed(() => schedules.value?.routes ?? [])

const availableTypes = computed(() => {
  const types = new Set(allRoutes.value.map(r => r.transitType).filter(Boolean))
  return [...types] as string[]
})

const filteredRoutes = computed(() =>
  filterRoutes(allRoutes.value, selectedType.value, searchQuery.value),
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
  title: computed(() => `Linee — ${config.value?.name ?? ''}`),
  meta: [
    {
      name: 'description',
      content: computed(() => `Tutte le linee di ${config.value?.fullName ?? config.value?.name ?? ''}.`),
    },
  ],
})
useOperatorHead(config)
</script>
