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
          >
            <LineBadge
              :name="route.name"
              :color="route.color"
              :text-color="route.textColor"
              :locale="config?.locale[0]"
              class="text-base px-3 py-1"
            />
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

const { config, schedules, pending } = await useOperator()
const s = useStrings(config)

const selectedType = ref<string | null>(null)

function transitTypeLabel(type: string): string {
  return s.value.transitTypes[type as TransitType] ?? type
}

const allRoutes = computed(() => schedules.value?.routes ?? [])

const availableTypes = computed(() => {
  const types = new Set(allRoutes.value.map(r => r.transitType).filter(Boolean))
  return [...types] as string[]
})

const filteredRoutes = computed(() =>
  selectedType.value
    ? allRoutes.value.filter(r => r.transitType === selectedType.value)
    : allRoutes.value,
)

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
