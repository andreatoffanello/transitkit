<template>
  <AppLayout>
    <div class="max-w-lg mx-auto md:max-w-xl lg:max-w-2xl">

      <!-- Back navigation -->
      <div class="px-2 pt-2">
        <NuxtLink
          :to="backTo"
          class="inline-flex items-center gap-1.5 text-sm font-medium px-2 py-1.5 rounded-lg transition-opacity active:opacity-60"
          style="color: var(--color-primary)"
        >
          <ChevronLeft :size="16" :stroke-width="2" />
          {{ backLabel }}
        </NuxtLink>
      </div>

      <template v-if="trip">
        <!-- Trip header -->
        <div class="flex items-center gap-3 px-4 pt-2 pb-3">
          <LineBadge :name="trip.lineName" :color="trip.color" :text-color="trip.textColor" :locale="locale" />
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-1.5">
              <ChevronRight :size="12" :stroke-width="2" class="shrink-0" style="color: var(--text-secondary)" />
              <p class="text-[16px] font-semibold truncate" style="color: var(--text-primary)">{{ trip.headsign }}</p>
            </div>
            <div class="flex items-center gap-2 mt-0.5">
              <span class="text-xs" style="color: var(--text-secondary)">{{ trip.rows.length }} {{ s.stops }}</span>
              <span class="text-xs tabular-nums" style="color: var(--text-secondary)">{{ formatClockTime(trip.time, locale) }}</span>
              <span
                v-if="isLive && liveDelayMin !== null"
                class="inline-flex items-center gap-1 text-[11px] font-semibold px-1.5 py-0.5 rounded"
                :style="liveDelayMin > 0
                  ? 'background-color: rgba(245,158,11,0.14); color: #d97706'
                  : 'background-color: color-mix(in srgb, var(--color-live) 16%, transparent); color: var(--color-live)'"
              >
                <span class="w-1.5 h-1.5 rounded-full" :style="`background-color: ${liveDelayMin > 0 ? '#f59e0b' : 'var(--color-live)'}`" />
                {{ liveDelayMin > 0 ? `+${liveDelayMin} min` : s.now }}
              </span>
            </div>
          </div>
        </div>

        <!-- Timeline -->
        <div class="px-4 pb-2">
          <div class="rounded-2xl overflow-hidden" style="background-color: var(--bg-elevated); border: 1px solid var(--border)">
            <NuxtLink
              v-for="(row, index) in trip.rows"
              :key="row.stop.id + '_' + row.time"
              :to="`/stop/${row.stop.id}`"
              class="flex items-stretch gap-0 px-3 transition-colors active:opacity-70"
              :style="index === originIndex ? `background-color: color-mix(in srgb, ${lineColor} 10%, transparent)` : ''"
            >
              <!-- Timeline rail -->
              <div class="relative w-6 shrink-0 flex justify-center">
                <!-- top segment -->
                <span
                  class="absolute top-0 w-[3px]"
                  style="height: 50%"
                  :style="{ backgroundColor: index === 0 ? 'transparent' : (index <= originIndex ? railDim : lineColor) }"
                />
                <!-- bottom segment -->
                <span
                  class="absolute bottom-0 w-[3px]"
                  style="height: 50%"
                  :style="{ backgroundColor: index === trip.rows.length - 1 ? 'transparent' : (index < originIndex ? railDim : lineColor) }"
                />
                <!-- dot -->
                <span
                  class="absolute rounded-full"
                  style="top: 50%; transform: translateY(-50%)"
                  :style="dotStyle(index)"
                />
                <!-- origin ring -->
                <span
                  v-if="index === originIndex"
                  class="absolute rounded-full"
                  style="top: 50%; transform: translateY(-50%); width: 20px; height: 20px"
                  :style="`border: 2px solid ${lineColor}`"
                />
              </div>

              <!-- Stop info -->
              <div class="flex-1 min-w-0 py-3 pl-2">
                <div class="flex items-center gap-2">
                  <span
                    class="text-[15px] truncate"
                    :class="index === originIndex ? 'font-semibold' : (isTerminal(index) ? 'font-semibold' : 'font-normal')"
                    :style="{ color: index < originIndex ? 'var(--text-tertiary)' : (index === originIndex ? lineColor : 'var(--text-primary)') }"
                  >{{ row.stop.name }}</span>
                  <span
                    v-if="index === originIndex"
                    class="text-[10px] font-bold px-1.5 py-0.5 rounded-full shrink-0 uppercase tracking-wide"
                    :style="{ backgroundColor: lineColor, color: '#fff' }"
                  >{{ s.now }}</span>
                </div>

                <!-- Coincidences: other lines at this stop -->
                <div v-if="otherLines(row.stop, index).length" class="flex items-center gap-1 mt-1">
                  <RefreshCw :size="10" :stroke-width="2" style="color: var(--color-primary)" />
                  <LineBadge
                    v-for="ln in otherLines(row.stop, index).slice(0, 4)"
                    :key="ln.name"
                    :name="ln.name"
                    :color="ln.color"
                    :text-color="ln.textColor"
                    :locale="locale"
                  />
                  <span v-if="otherLines(row.stop, index).length > 4" class="text-[10px] font-bold" style="color: var(--text-tertiary)">
                    +{{ otherLines(row.stop, index).length - 4 }}
                  </span>
                </div>
              </div>

              <!-- Scheduled time -->
              <div class="flex flex-col items-end justify-center py-3 pl-2 shrink-0">
                <span
                  class="text-[13px] tabular-nums"
                  :class="index === originIndex ? 'font-semibold' : 'font-medium'"
                  :style="{ color: index < originIndex ? 'var(--text-tertiary)' : (index === originIndex ? lineColor : 'var(--text-secondary)') }"
                >{{ formatClockTime(row.time, locale) }}</span>
                <span
                  v-if="index >= originIndex && isLive && liveDelayMin && liveDelayMin > 0"
                  class="text-[10px] font-semibold tabular-nums text-amber-600 dark:text-amber-400"
                >+{{ liveDelayMin }} min</span>
              </div>

              <div class="flex items-center pl-1 shrink-0">
                <ChevronRight :size="12" :stroke-width="2" style="color: var(--text-tertiary)" />
              </div>
            </NuxtLink>
          </div>
        </div>
      </template>

      <!-- No data -->
      <div v-else class="text-center py-16 px-4">
        <AlertTriangle :size="28" :stroke-width="1.5" class="mx-auto mb-2" style="color: var(--text-tertiary)" />
        <p class="text-[15px]" style="color: var(--text-secondary)">{{ s.tripNoData }}</p>
        <NuxtLink to="/" class="mt-4 inline-block text-sm underline" style="color: var(--color-primary)">
          {{ s.backToHome }}
        </NuxtLink>
      </div>

    </div>
  </AppLayout>
</template>

<script setup lang="ts">
definePageMeta({ pageTransition: { name: 'page-slide-up', mode: 'out-in' } })
import { computed } from 'vue'
import { ChevronLeft, ChevronRight, RefreshCw, AlertTriangle } from 'lucide-vue-next'
import { reconstructTrip } from '~/utils/schedule'
import { normalizeHex } from '~/utils/color'
import { formatClockTime } from '~/utils/clockTime'
import type { Departure, ScheduleStop, Route } from '~/types'

const route = useRoute()
const tripId = computed(() => String(route.params.tripId))
const fromStopId = computed(() => (typeof route.query.from === 'string' ? route.query.from : null))

const { config, schedules } = await useOperator()
const s = useStrings(config)
const locale = computed(() => config.value?.locale[0])

const trip = computed(() =>
  schedules.value ? reconstructTrip(tripId.value, schedules.value, config.value?.headsignMap) : null,
)

// Back target: the stop we came from, else home.
const backTo = computed(() => (fromStopId.value ? `/stop/${fromStopId.value}` : '/'))
const fromStop = computed<ScheduleStop | null>(() =>
  fromStopId.value && schedules.value
    ? schedules.value.stops.find(st => st.id === fromStopId.value || st.gtfsStopIds?.includes(fromStopId.value!)) ?? null
    : null,
)
const backLabel = computed(() => fromStop.value?.name ?? s.value.backToHome)

// Origin = the stop the rider tapped from (highlighted "Now"). Default first.
const originIndex = computed(() => {
  if (!trip.value || !fromStopId.value) return 0
  const idx = trip.value.rows.findIndex(r =>
    r.stop.id === fromStopId.value || r.stop.gtfsStopIds?.includes(fromStopId.value!),
  )
  return idx >= 0 ? idx : 0
})

// Line color, guarded against near-white (which would vanish on the card).
const lineColor = computed(() => {
  const hex = normalizeHex(trip.value?.color, '#888888')
  return isVeryLight(hex) ? '#1d6ff2' : hex
})
const railDim = computed(() => `color-mix(in srgb, ${lineColor.value} 35%, transparent)`)

function isVeryLight(hex: string): boolean {
  const h = hex.replace('#', '')
  if (h.length < 6) return false
  const r = parseInt(h.slice(0, 2), 16)
  const g = parseInt(h.slice(2, 4), 16)
  const b = parseInt(h.slice(4, 6), 16)
  return (0.299 * r + 0.587 * g + 0.114 * b) / 255 > 0.85
}

function isTerminal(index: number): boolean {
  return index === 0 || index === (trip.value?.rows.length ?? 1) - 1
}

function dotStyle(index: number): Record<string, string> {
  const isOrigin = index === originIndex.value
  const terminal = isTerminal(index)
  const size = isOrigin ? 14 : terminal ? 12 : 8
  const past = index < originIndex.value
  const fill = past ? `color-mix(in srgb, ${lineColor.value} 45%, transparent)` : lineColor.value
  const style: Record<string, string> = {
    width: `${size}px`,
    height: `${size}px`,
    backgroundColor: fill,
  }
  if (terminal || isOrigin) style.boxShadow = 'inset 0 0 0 3px #fff'
  return style
}

// Coincidence badges: other lines serving the stop (not the current line).
const routesByName = computed(() => {
  const m = new Map<string, Route>()
  for (const r of schedules.value?.routes ?? []) m.set(r.name, r)
  return m
})
function otherLines(stop: ScheduleStop, index: number): { name: string; color?: string; textColor?: string }[] {
  if (index < originIndex.value) return []
  return (stop.lines ?? [])
    .filter(name => name !== trip.value?.lineName)
    .map(name => {
      const r = routesByName.value.get(name)
      return { name, color: r?.color, textColor: r?.textColor }
    })
}

// Trip-level live delay, reusing the same trip_updates polling as the stop page.
const probe = computed<Departure[]>(() =>
  trip.value
    ? [{
        id: 'probe',
        time: trip.value.time,
        lineName: trip.value.lineName,
        routeId: trip.value.routeId,
        headsign: trip.value.headsign,
        color: trip.value.color,
        textColor: trip.value.textColor,
        transitType: trip.value.transitType,
        dock: '',
        tripId: tripId.value,
        minutesFromMidnight: 0,
      }]
    : [],
)
const { departures: liveProbe, isLive } = useRealtime(probe, config.value?.gtfsRt?.trip_updates)
const liveDelayMin = computed<number | null>(() => {
  const delay = liveProbe.value[0]?.realtimeDelay
  return typeof delay === 'number' ? Math.round(delay / 60) : null
})

useHead(() => ({
  title: trip.value
    ? `${s.value.tripTitlePrefix}${trip.value.lineName} → ${trip.value.headsign}`
    : s.value.tripNoData,
  meta: [{ name: 'robots', content: 'noindex' }],
}))
</script>
