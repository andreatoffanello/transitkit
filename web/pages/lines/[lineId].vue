<template>
  <div class="max-w-lg mx-auto px-4 pb-8">
    <PageHeader
      :primary-color="headerBg"
      :text-color="headerText"
      back-to="/lines"
      :back-text="s.backToLines"
      :back-label="s.backToLines"
      :title="route ? `${route.name}${route.longName ? ' — ' + route.longName : ''}` : ''"
    />

    <div v-if="pending" aria-busy="true" :aria-label="s.ariaLoading">
      <!-- Direction tabs skeleton -->
      <div class="flex gap-2 mb-4">
        <div class="h-9 flex-1 bg-gray-200 dark:bg-white/10 rounded-xl animate-pulse" />
        <div class="h-9 flex-1 bg-gray-200 dark:bg-white/10 rounded-xl animate-pulse" />
      </div>

      <!-- Stop list skeleton — 6 rows -->
      <div class="space-y-1">
        <div
          v-for="i in 6"
          :key="i"
          class="flex items-center gap-3 py-3 px-4 bg-gray-200 dark:bg-white/10 rounded-xl animate-pulse"
        >
          <div class="w-2 h-2 rounded-full bg-gray-300 dark:bg-white/20 shrink-0" />
          <div class="h-3 rounded bg-gray-300 dark:bg-white/20" :style="{ width: `${48 + (i * 13) % 40}%` }" />
        </div>
      </div>
    </div>

    <template v-else-if="route">
      <!-- Direction switcher -->
      <div v-if="route.directions.length > 1" class="flex gap-2 mb-4" role="tablist" :aria-label="s.ariaDirections">
        <button
          v-for="dir in route.directions"
          :key="dir.id"
          role="tab"
          :aria-selected="selectedDirectionId === dir.id"
          aria-controls="direction-panel"
          class="flex-1 py-2 rounded-xl text-sm font-medium transition-colors"
          :class="selectedDirectionId === dir.id
            ? 'text-white'
            : 'bg-gray-100 dark:bg-white/10 text-gray-600 dark:text-gray-300'"
          :style="selectedDirectionId === dir.id ? { backgroundColor: normalizeHex(route.color) } : {}"
          @click="selectedDirectionId = dir.id"
        >
          → {{ dir.headsign }}
        </button>
      </div>

      <!-- Stop count -->
      <p v-if="currentStops.length" class="text-xs text-gray-400 mt-0.5 mb-3">
        {{ currentStops.length }} {{ s.stops }}
      </p>

      <!-- Stop sequence -->
      <div
        v-if="currentStops.length"
        id="direction-panel"
        role="tabpanel"
        class="space-y-1"
      >
        <NuxtLink
          v-for="stop in currentStops"
          :key="stop.id"
          :to="`/stop/${stop.id}`"
          class="flex items-center gap-3 py-3 px-4 bg-white dark:bg-white/5 rounded-xl hover:bg-gray-50 dark:hover:bg-white/10 transition-colors"
        >
          <span class="w-2 h-2 rounded-full shrink-0" :style="{ backgroundColor: normalizeHex(route.color) }" aria-hidden="true" />
          <span class="text-sm">{{ stop.name }}</span>
          <span class="ml-auto text-gray-400 text-xs" aria-hidden="true">→</span>
        </NuxtLink>
      </div>
      <div
        v-else
        id="direction-panel"
        role="tabpanel"
        class="text-center py-12 text-gray-400"
      >
        <p class="font-semibold text-gray-600 dark:text-gray-300 mb-1">{{ s.noStopsFound }}</p>
        <p class="text-sm">{{ s.noStopsFoundHint }}</p>
      </div>
    </template>

    <div v-else role="alert" class="text-center py-16 text-gray-400">
      <p class="text-lg font-medium">{{ s.lineNotFound }}</p>
      <NuxtLink to="/lines" class="mt-4 inline-block text-sm underline text-accent">
        {{ s.backToLines }}
      </NuxtLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import { normalizeHex } from '~/utils/color'
import type { Route, RouteDirection, ScheduleStop } from '~/types'

const nuxtRoute = useRoute()
const lineId = computed(() => String(nuxtRoute.params.lineId))

const { config, schedules, pending } = await useOperator()
const s = useStrings(config)

const route = computed(() =>
  schedules.value?.routes.find((r: Route) => r.id === lineId.value) ?? null,
)

const headerBg = computed(() => {
  if (route.value?.color) return normalizeHex(route.value.color)
  return config.value?.theme.primaryColor ?? undefined
})

const headerText = computed(() => {
  if (route.value?.textColor) return normalizeHex(route.value.textColor)
  return config.value?.theme.textOnPrimary ?? undefined
})

const selectedDirectionId = ref<number>(0)

// Reset direction when route changes
watch(route, (r) => {
  selectedDirectionId.value = r?.directions[0]?.id ?? 0
}, { immediate: true })

const stopMap = computed<Map<string, ScheduleStop>>(() => {
  const stops = schedules.value?.stops ?? []
  return new Map(stops.map(s => [s.id, s]))
})

const currentStops = computed(() => {
  const dir = route.value?.directions.find((d: RouteDirection) => d.id === selectedDirectionId.value)
  if (!dir) return []
  return dir.stopIds
    .map((id: string) => stopMap.value.get(id))
    .filter((s: ScheduleStop | undefined): s is ScheduleStop => s !== undefined)
})

useHead({
  title: computed(() => {
    const lineName = route.value?.longName ?? route.value?.name ?? ''
    const op = config.value?.fullName ?? config.value?.name ?? ''
    return lineName ? `${lineName} — ${op}` : op
  }),
  meta: [
    {
      property: 'og:title',
      content: computed(() => route.value?.longName ?? route.value?.name ?? config.value?.fullName ?? config.value?.name ?? ''),
    },
    {
      name: 'description',
      content: computed(() => {
        const lineName = route.value?.longName ?? route.value?.name ?? ''
        const op = config.value?.fullName ?? config.value?.name ?? ''
        return `${lineName} — ${s.value.stopsAndSchedules}${op ? ` — ${op}` : ''}`
      }),
    },
  ],
})
useOperatorHead(config)
</script>
