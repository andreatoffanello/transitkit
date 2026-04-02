<template>
  <div class="max-w-lg mx-auto px-4 pb-8">
    <header
      class="sticky top-0 z-10 flex items-center gap-3 px-4 py-3 -mx-4 mb-4"
      :style="{ backgroundColor: headerBg }"
    >
      <NuxtLink
        to="/lines"
        aria-label="Torna alle linee"
        class="text-sm opacity-70 mr-2"
        :style="{ color: headerText }"
      >
        ← Linee
      </NuxtLink>
      <span
        class="text-lg font-bold flex-1 text-center"
        :style="{ color: headerText }"
      >
        {{ route?.name }} <span v-if="route?.longName" class="font-normal opacity-80">— {{ route.longName }}</span>
      </span>
    </header>

    <div v-if="pending" class="space-y-3 animate-pulse" aria-busy="true" aria-label="Caricamento">
      <div v-for="i in 8" :key="i" class="h-10 bg-gray-200 dark:bg-white/10 rounded-xl" />
    </div>

    <template v-else-if="route">
      <!-- Direction switcher -->
      <div v-if="route.directions.length > 1" class="flex gap-2 mb-4" role="tablist" aria-label="Direzioni">
        <button
          v-for="dir in route.directions"
          :key="dir.id"
          role="tab"
          :aria-selected="selectedDirectionId === dir.id"
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

      <!-- Stop sequence -->
      <div class="space-y-1" role="list" aria-label="Fermate della linea">
        <NuxtLink
          v-for="stop in currentStops"
          :key="stop.id"
          :to="`/stop/${stop.id}`"
          role="listitem"
          class="flex items-center gap-3 py-3 px-4 bg-white dark:bg-white/5 rounded-xl hover:bg-gray-50 dark:hover:bg-white/10 transition-colors"
        >
          <span class="w-2 h-2 rounded-full shrink-0" :style="{ backgroundColor: normalizeHex(route.color) }" aria-hidden="true" />
          <span class="text-sm">{{ stop.name }}</span>
          <span class="ml-auto text-gray-400 text-xs" aria-hidden="true">→</span>
        </NuxtLink>
      </div>

      <p v-if="currentStops.length === 0" class="text-center py-8 text-gray-400 text-sm">
        Nessuna fermata disponibile per questa direzione.
      </p>
    </template>

    <div v-else role="alert" class="text-center py-16 text-gray-400">
      <p class="text-lg font-medium">Linea non trovata</p>
      <NuxtLink to="/lines" class="mt-4 inline-block text-sm underline">
        Torna alle linee
      </NuxtLink>
    </div>
  </div>
</template>

<script setup lang="ts">
const nuxtRoute = useRoute()
const lineId = computed(() => String(nuxtRoute.params.lineId))

const { config, schedules, pending } = await useOperator()

const route = computed(() =>
  schedules.value?.routes.find(r => r.id === lineId.value) ?? null,
)

function normalizeHex(hex: string): string {
  const clean = hex.replace(/^#/, '')
  return `#${clean}`
}

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

const currentStops = computed(() => {
  const dir = route.value?.directions.find(d => d.id === selectedDirectionId.value)
  if (!dir) return []
  return dir.stopIds
    .map(id => schedules.value?.stops.find(s => s.id === id))
    .filter((s): s is NonNullable<typeof s> => s !== undefined)
})

useHead({
  title: computed(() =>
    route.value
      ? `Linea ${route.value.name} — ${config.value?.name ?? ''}`
      : 'Linea',
  ),
  meta: [
    {
      name: 'description',
      content: computed(() =>
        route.value
          ? `Fermate della linea ${route.value.name} ${route.value.longName ?? ''} — ${config.value?.name ?? ''}.`
          : 'Linea non trovata',
      ),
    },
  ],
})
</script>
