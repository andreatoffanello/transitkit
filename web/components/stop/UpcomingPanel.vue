<template>
  <section class="px-4 mb-6">

    <!-- Line filter chips (badges act as filters, parity con iOS) -->
    <div
      v-if="servingRoutes.length > 1"
      role="group"
      :aria-label="s.ariaLinesAtStop"
      class="flex gap-2 mb-4 overflow-x-auto scrollbar-none pb-0.5"
    >
      <button
        class="shrink-0 h-7 px-2.5 rounded-lg text-[13px] font-bold transition-all duration-150 active:opacity-70"
        :aria-pressed="filterLine === null"
        :style="filterLine === null
          ? { backgroundColor: 'var(--color-primary)', color: 'var(--color-text-on-primary)' }
          : { backgroundColor: 'var(--bg-secondary)', color: 'var(--text-secondary)' }"
        @click="$emit('update:filterLine', null)"
      >
        {{ s.all }}
      </button>
      <button
        v-for="r in servingRoutes"
        :key="r.id"
        class="shrink-0 transition-opacity duration-150 active:opacity-70"
        :style="{ opacity: filterLine !== null && filterLine !== r.name ? 0.3 : 1 }"
        :aria-pressed="filterLine === r.name"
        @click="$emit('update:filterLine', filterLine === r.name ? null : r.name)"
      >
        <LineBadge :name="r.name" :color="r.color" :text-color="r.textColor" :locale="config?.locale[0]" />
      </button>
    </div>

    <!-- Realtime degraded notice -->
    <div
      v-if="hasRealtimeConfigured && !isLive"
      class="flex items-center gap-2 text-xs py-2 px-3 rounded-lg mb-3"
      style="background: rgba(245,158,11,0.10); color: var(--text-secondary)"
      role="status"
    >
      <WifiOff :size="13" :stroke-width="1.75" class="shrink-0 text-amber-600 dark:text-amber-400" />
      <span class="flex-1">{{ s.realtimeUnavailable }}</span>
    </div>

    <!-- Departures present -->
    <template v-if="filteredUpcomingDepartures.length">
      <!-- Multi-direction: group by headsign ("→ destinazione") -->
      <template v-if="directionGroups.length >= 2">
        <div v-for="(group, gi) in directionGroups" :key="group.headsign" :class="gi > 0 ? 'mt-4' : ''">
          <div class="flex items-center gap-1.5 mb-1.5 px-1">
            <ArrowRight :size="11" :stroke-width="2.25" style="color: var(--text-secondary)" />
            <span class="text-[13px] font-semibold" style="color: var(--text-secondary)">{{ group.headsign }}</span>
          </div>
          <div class="overflow-hidden divide-y rounded-2xl" style="background-color: var(--bg-elevated); border: 1px solid var(--border)">
            <DepartureRow
              v-for="dep in group.deps.slice(0, 8)"
              :key="dep.id"
              :departure="dep"
              :now="now"
              :locale="config?.locale[0]"
              :from-stop-id="stopId"
              :hide-badge="filterLine !== null"
              :show-countdown="true"
            />
          </div>
        </div>
      </template>

      <!-- Single direction: "Oggi" + card -->
      <template v-else>
        <div class="flex items-center justify-between mb-1.5 px-1">
          <span class="text-[13px] font-semibold uppercase tracking-wide" style="color: var(--text-tertiary)">{{ s.today }}</span>
          <button
            v-if="isLive"
            type="button"
            :aria-label="s.refresh"
            class="flex items-center gap-1 text-xs font-semibold transition-opacity active:opacity-60"
            style="color: var(--color-primary)"
            @click="$emit('refresh')"
          >
            <RefreshCw :size="12" :stroke-width="2" :class="{ 'animate-spin': realtimeLoading }" />
            {{ realtimeLastUpdated || s.refresh }}
          </button>
        </div>
        <div class="overflow-hidden divide-y rounded-2xl" style="background-color: var(--bg-elevated); border: 1px solid var(--border)">
          <DepartureRow
            v-for="dep in filteredUpcomingDepartures.slice(0, showAllUpcoming ? undefined : 5)"
            :key="dep.id"
            :departure="dep"
            :now="now"
            :locale="config?.locale[0]"
            :from-stop-id="stopId"
            :hide-badge="filterLine !== null"
            :show-countdown="true"
          />
        </div>
        <button
          v-if="!showAllUpcoming && filteredUpcomingDepartures.length > 5"
          type="button"
          class="w-full mt-2 py-2.5 rounded-xl text-sm font-semibold flex items-center justify-center gap-1.5 transition-opacity active:opacity-60"
          style="color: var(--color-primary); background-color: color-mix(in srgb, var(--color-primary) 6%, transparent)"
          @click="$emit('update:showAllUpcoming', true)"
        >
          <ChevronDown :size="14" :stroke-width="2" />
          {{ s.showMore }} {{ filteredUpcomingDepartures.length - 5 }}
        </button>
      </template>
    </template>

    <!-- Empty state -->
    <div
      v-else
      class="rounded-2xl px-5 py-8 text-center"
      style="background-color: var(--bg-elevated); border: 1px solid var(--border)"
    >
      <div
        class="w-12 h-12 rounded-2xl flex items-center justify-center mx-auto mb-3"
        style="background-color: color-mix(in srgb, var(--color-primary) 8%, var(--bg-secondary))"
      >
        <Clock :size="24" :stroke-width="1.5" style="color: var(--color-primary); opacity: 0.7" />
      </div>
      <p class="text-[15px] font-semibold mb-1" style="color: var(--text-primary)">{{ s.noDepartures }}</p>
      <div v-if="nextDepartureTodayData" class="flex items-center justify-center gap-2 mt-2">
        <span class="text-sm" style="color: var(--text-secondary)">{{ s.nextDepartureToday }}:</span>
        <LineBadge
          :name="nextDepartureTodayData.lineName"
          :color="nextDepartureTodayData.lineColor"
          :text-color="nextDepartureTodayData.lineTextColor"
          :locale="config?.locale[0]"
        />
        <span class="text-sm font-semibold tabular-nums" style="color: var(--text-primary)">{{ formatClockTime(nextDepartureTodayData.time, config?.locale[0]) }}</span>
      </div>
    </div>

    <!-- Full schedule (iOS "Orario completo" → pushed screen, not a switch) -->
    <NuxtLink
      v-if="hasSchedule"
      :to="`/stop/${stopId}/schedule`"
      class="mt-3 w-full flex items-center justify-center gap-2 py-3 rounded-xl text-sm font-semibold transition-opacity active:opacity-70"
      style="color: var(--color-primary); background-color: color-mix(in srgb, var(--color-primary) 8%, transparent)"
    >
      <Clock :size="15" :stroke-width="2" />
      {{ s.viewFullSchedule }}
    </NuxtLink>
  </section>
</template>

<script setup lang="ts">
import { RefreshCw, Clock, ChevronDown, WifiOff, ArrowRight } from 'lucide-vue-next'
import type { OperatorConfig, Departure, Route } from '~/types'
import type { AppStrings } from '~/utils/strings'
import { formatClockTime } from '~/utils/clockTime'

const props = defineProps<{
  s: AppStrings
  config: OperatorConfig | null | undefined
  stopId: string
  hasSchedule: boolean
  servingRoutes: Route[]
  filteredUpcomingDepartures: Departure[]
  filterLine: string | null
  showAllUpcoming: boolean
  isLive: boolean
  realtimeLoading: boolean
  realtimeLastUpdated: string | null
  now: number
  nextDepartureTodayData: { time: string; lineName: string; lineColor?: string; lineTextColor?: string } | null
}>()

defineEmits<{
  (e: 'refresh'): void
  (e: 'update:filterLine', v: string | null): void
  (e: 'update:showAllUpcoming', v: boolean): void
}>()

const hasRealtimeConfigured = computed(() => Boolean(props.config?.gtfsRt?.trip_updates))

// Group upcoming by destination, preserving first-occurrence order — a
// multi-direction stop must not interleave the two directions by time.
const directionGroups = computed<{ headsign: string; deps: Departure[] }[]>(() => {
  const seen = new Map<string, Departure[]>()
  const order: string[] = []
  for (const dep of props.filteredUpcomingDepartures) {
    if (!seen.has(dep.headsign)) {
      seen.set(dep.headsign, [])
      order.push(dep.headsign)
    }
    seen.get(dep.headsign)!.push(dep)
  }
  return order.map(h => ({ headsign: h, deps: seen.get(h)! }))
})
</script>
