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
          v-for="(dir, dirIdx) in route.directions"
          :key="dir.id"
          role="tab"
          :aria-selected="selectedDirectionId === dir.id"
          aria-controls="direction-panel"
          :aria-label="dir.headsign ?? `${s.ariaDirections} ${dirIdx + 1}`"
          class="flex-1 py-2 rounded-xl text-sm font-medium transition-colors"
          :class="selectedDirectionId === dir.id
            ? 'text-white'
            : 'bg-gray-100 dark:bg-white/10 text-gray-600 dark:text-gray-300'"
          :style="selectedDirectionId === dir.id ? { backgroundColor: normalizeHex(route.color) } : {}"
          @click="selectedDirectionId = dir.id"
        >
          → {{ dir.headsign ?? `${s.ariaDirections} ${dirIdx + 1}` }}
          <span v-if="dir.stopIds?.length" class="text-xs opacity-60 ml-1">({{ dir.stopIds.length }})</span>
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
          v-for="(stop, index) in currentStops"
          :key="stop.id"
          :to="{ path: `/stop/${stop.id}`, query: { from: nuxtRoute.params.lineId } }"
          :prefetch="false"
          class="flex items-center gap-3 py-3 px-4 bg-white dark:bg-white/5 rounded-xl hover:bg-gray-50 dark:hover:bg-white/10 transition-colors"
        >
          <span class="text-xs font-mono text-gray-400 dark:text-gray-500 w-6 shrink-0 text-right tabular-nums">
            {{ index + 1 }}
          </span>
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
      <!-- Share footer -->
      <footer class="mt-8">
        <button
          v-if="canShare"
          type="button"
          class="w-full flex items-center justify-center gap-2 py-3 rounded-xl bg-gray-100 dark:bg-white/10 text-gray-700 dark:text-gray-200 font-medium hover:bg-gray-200 dark:hover:bg-white/20 transition-colors active:scale-95 duration-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
          :aria-label="s.shareStop"
          @click="shareLine"
        >
          📤 {{ s.shareStop }}
        </button>
        <button
          v-else
          type="button"
          class="flex items-center gap-1.5 text-sm text-gray-500 dark:text-gray-400 active:scale-95 transition-transform duration-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
          :aria-label="s.copyLink"
          @click="copyLink"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
          </svg>
          <span>{{ copied ? s.copiedFeedback : s.copyLink }}</span>
        </button>
      </footer>
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
import { onMounted } from 'vue'
import { normalizeHex } from '~/utils/color'
import type { Route, RouteDirection, ScheduleStop } from '~/types'

const nuxtRoute = useRoute()
const lineId = computed(() => String(nuxtRoute.params.lineId))
const requestUrl = useRequestURL()

const { config, schedules, pending } = await useOperator()
const s = useStrings(config)

// Web Share API
const canShare = ref(false)
onMounted(() => {
  canShare.value = typeof navigator !== 'undefined' && 'share' in navigator
})

const copied = ref(false)
async function copyLink() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch { /* clipboard not available */ }
}

async function shareLine() {
  if (!route.value || !canShare.value) return
  try {
    await navigator.share({
      title: route.value.longName ?? route.value.name,
      url: window.location.href,
    })
  } catch { /* user cancelled or not supported */ }
}

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
    const currentDir = route.value?.directions.find((d: RouteDirection) => d.id === selectedDirectionId.value)
    const headsign = currentDir?.headsign

    if (!lineName) return op
    if (headsign) return `${lineName} → ${headsign} — ${op}`
    return `${lineName} — ${op}`
  }),
  link: [{ rel: 'canonical', href: computed(() => `${requestUrl.origin}${nuxtRoute.path}`) }],
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
        const currentDir = route.value?.directions.find((d: RouteDirection) => d.id === selectedDirectionId.value)
        const headsign = currentDir?.headsign
        const stopCount = currentStops.value.length

        let desc = lineName
        if (headsign) desc += ` → ${headsign}`
        if (stopCount > 0) desc += ` · ${stopCount} ${s.value.stops}`
        desc += ` · ${s.value.stopsAndSchedules}`
        if (op) desc += ` — ${op}`
        return desc
      }),
    },
    {
      name: 'robots',
      content: computed(() => (!pending.value && !route.value) ? 'noindex, nofollow' : 'index, follow'),
    },
  ],
})
useOperatorHead(config)
</script>
