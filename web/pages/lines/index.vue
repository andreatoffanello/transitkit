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

    <div v-if="pending" class="space-y-4 animate-pulse" aria-busy="true" aria-label="Caricamento">
      <div v-for="i in 3" :key="i" class="h-6 bg-gray-200 dark:bg-white/10 rounded" />
    </div>

    <template v-else>
      <div
        v-for="[type, routes] in routesByType"
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
              class="text-base px-3 py-1"
            />
          </NuxtLink>
        </div>
      </div>

      <div v-if="routesByType.length === 0" class="text-center py-16 text-gray-400">
        {{ s.noLines }}
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { TransitType } from '~/types'

const { config, schedules, pending } = await useOperator()
const s = useStrings(config)

function transitTypeLabel(type: string): string {
  return s.value.transitTypes[type as TransitType] ?? type
}

const routesByType = computed(() => {
  const routes = schedules.value?.routes ?? []
  const map = new Map<string, typeof routes>()
  for (const route of routes) {
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
</script>
