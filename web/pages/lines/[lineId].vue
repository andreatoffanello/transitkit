<template>
  <AppLayout>
    <PageHeader
      :title="route ? `${route.name}${route.longName ? ' — ' + route.longName : ''}` : ''"
      back-to="/lines"
      back-label="Linee"
    >
      <template #action>
        <button
          type="button"
          :aria-label="s.shareStop"
          class="w-9 h-9 rounded-xl flex items-center justify-center transition-opacity active:opacity-70"
          style="color: var(--text-secondary)"
          @click="canShare ? shareLine() : copyLink()"
        >
          <Share2 v-if="canShare" :size="20" :stroke-width="1.75" />
          <Copy v-else :size="20" :stroke-width="1.75" />
        </button>
      </template>
    </PageHeader>

    <div class="max-w-lg mx-auto px-4 pt-4 pb-8">
      <h1 class="sr-only">{{ route?.longName ?? route?.name ?? '' }}</h1>

      <div v-if="pending" aria-busy="true" :aria-label="s.ariaLoading">
        <!-- LineBadge placeholder -->
        <div class="h-12 w-24 rounded-xl mb-6 skeleton-shimmer" />
        <!-- Direction switcher skeleton -->
        <div class="flex gap-1 p-1 rounded-xl mb-4" style="background-color: var(--bg-elevated)">
          <div class="h-9 flex-1 rounded-lg skeleton-shimmer" />
          <div class="h-9 flex-1 rounded-lg skeleton-shimmer" />
        </div>
        <!-- Stop list skeleton — 6 rows -->
        <div class="space-y-1 px-2">
          <div
            v-for="i in 6"
            :key="i"
            class="flex items-center gap-3 py-3"
          >
            <div class="w-4 h-4 rounded-full shrink-0 skeleton-shimmer" />
            <div class="h-3 rounded skeleton-shimmer" :style="{ width: `${48 + (i * 13) % 40}%` }" />
          </div>
        </div>
      </div>

      <template v-else-if="route">
        <!-- LineBadge in evidenza -->
        <div class="mb-6">
          <LineBadge
            :name="route.name"
            :color="route.color"
            :text-color="route.textColor"
            :locale="config?.locale[0]"
            class="text-xl px-4 py-2"
          />
        </div>

        <!-- Direction switcher — pill toggle -->
        <div
          v-if="route.directions.length > 1"
          class="flex p-1 rounded-xl gap-1 mb-4"
          style="background-color: var(--bg-elevated); border: 1px solid var(--border)"
          role="tablist"
          :aria-label="s.ariaDirections"
        >
          <button
            v-for="(dir, dirIdx) in route.directions"
            :key="dir.id"
            role="tab"
            :aria-selected="selectedDirectionId === dir.id"
            aria-controls="direction-panel"
            :aria-label="dir.headsign ?? `${s.ariaDirections} ${dirIdx + 1}`"
            class="flex-1 py-2 px-3 rounded-lg text-sm font-medium transition-all truncate"
            :style="selectedDirectionId === dir.id
              ? { backgroundColor: 'var(--color-primary)', color: 'var(--color-text-on-primary)' }
              : { color: 'var(--text-secondary)' }"
            @click="selectedDirectionId = dir.id"
          >
            {{ dir.headsign ?? `${s.ariaDirections} ${dirIdx + 1}` }}
          </button>
        </div>

        <!-- Stop count -->
        <p v-if="currentStops.length" class="text-xs mb-3" style="color: var(--text-tertiary)">
          {{ currentStops.length }} {{ s.stops }}
        </p>

        <!-- Timeline stop sequence -->
        <div
          v-if="currentStops.length"
          id="direction-panel"
          role="tabpanel"
          class="relative px-2"
        >
          <!-- Linea verticale -->
          <div
            class="absolute left-[19px] top-5 bottom-5 w-[1.5px]"
            style="background-color: var(--border)"
            aria-hidden="true"
          />
          <div class="space-y-0">
            <NuxtLink
              v-for="(stop, index) in currentStops"
              :key="stop.id"
              :to="{ path: `/stop/${stop.id}`, query: { from: nuxtRoute.params.lineId } }"
              :prefetch="false"
              :aria-current="nuxtRoute.query.stop === stop.id ? 'location' : undefined"
              class="flex items-center gap-3 py-3 px-2 rounded-xl relative transition-opacity duration-150 active:opacity-70"
            >
              <div
                class="w-4 h-4 rounded-full border-2 shrink-0 z-10"
                :style="index === 0 || index === currentStops.length - 1
                  ? 'border-color: var(--color-primary); background-color: var(--bg-primary)'
                  : 'border-color: var(--border); background-color: var(--bg-secondary)'"
                aria-hidden="true"
              />
              <span
                class="flex-1 text-[15px] truncate"
                :class="index === 0 || index === currentStops.length - 1 ? 'font-semibold' : 'font-medium'"
                style="color: var(--text-primary)"
              >
                {{ stop.name }}
              </span>
              <ChevronRight :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
            </NuxtLink>
          </div>
        </div>

        <!-- Empty direction state -->
        <div
          v-else
          id="direction-panel"
          role="tabpanel"
          class="text-center py-12"
          style="color: var(--text-tertiary)"
        >
          <p class="font-semibold mb-1" style="color: var(--text-primary)">{{ s.noStopsFound }}</p>
          <p class="text-sm">{{ s.noStopsFoundHint }}</p>
        </div>

        <!-- Copy link feedback (fallback share) -->
        <p
          v-if="copied"
          class="text-center text-sm mt-6"
          style="color: var(--color-primary)"
          aria-live="polite"
        >
          {{ s.copiedFeedback }}
        </p>
      </template>

      <!-- Line not found -->
      <div v-else role="alert" class="text-center py-16" style="color: var(--text-tertiary)">
        <p class="text-lg font-medium" style="color: var(--text-primary)">{{ s.lineNotFound }}</p>
        <NuxtLink to="/lines" class="mt-4 inline-block text-sm underline" style="color: var(--color-primary)">
          {{ s.backToLines }}
        </NuxtLink>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
definePageMeta({ pageTransition: { name: 'page-slide-up', mode: 'out-in' } })
import { onMounted } from 'vue'
import { normalizeHex } from '~/utils/color'
import type { Route, RouteDirection, ScheduleStop } from '~/types'
import { Share2, Copy, ChevronRight } from 'lucide-vue-next'

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
  script: [
    {
      type: 'application/ld+json',
      innerHTML: computed(() => {
        if (!route.value) return '{}'
        const data: Record<string, string> = {
          '@context': 'https://schema.org',
          '@type': 'BusRoute',
          name: route.value.longName ?? route.value.name,
          identifier: route.value.id,
        }
        return JSON.stringify(data)
      }),
    },
  ],
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
