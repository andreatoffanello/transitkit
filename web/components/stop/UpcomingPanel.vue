<template>
  <section
    id="panel-prossime"
    role="tabpanel"
    aria-labelledby="tab-prossime"
    class="px-4 mb-6"
  >
    <div class="flex items-center justify-between mb-3">
      <h2 class="text-[17px] font-bold flex items-center gap-2" style="color: var(--text-primary)">
        {{ s.upcomingDepartures }}
        <span
          v-if="isLive"
          class="inline-block w-2 h-2 rounded-full animate-pulse"
          style="background-color: var(--color-live)"
          aria-hidden="true"
        />
      </h2>
      <button
        type="button"
        :aria-label="s.refresh"
        class="flex items-center gap-1 text-xs font-semibold px-2.5 py-1.5 rounded-lg transition-opacity active:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
        style="color: var(--color-primary); background-color: color-mix(in srgb, var(--color-primary) 10%, transparent)"
        @click="$emit('refresh')"
      >
        <RefreshCw :size="13" :stroke-width="1.75" :class="{ 'animate-spin': realtimeLoading }" />
        {{ s.refresh }}
      </button>
    </div>

    <!-- Realtime degraded notice — GTFS-RT configured but not live -->
    <div
      v-if="hasRealtimeConfigured && !isLive"
      class="flex items-center gap-2 text-xs py-2 px-3 rounded-lg mb-3"
      style="background: rgba(245,158,11,0.10); color: var(--text-secondary)"
      role="status"
    >
      <WifiOff :size="13" :stroke-width="1.75" class="shrink-0 text-amber-600 dark:text-amber-400" />
      <span class="flex-1">{{ s.realtimeUnavailable }}</span>
    </div>

    <!-- Line filter chips (only if >1 line) -->
    <div
      v-if="servingRoutes.length > 1"
      role="group"
      :aria-label="s.ariaLinesAtStop"
      class="flex gap-1.5 mb-3 overflow-x-auto scrollbar-none pb-0.5"
    >
      <button
        class="shrink-0 h-7 px-3 rounded-full text-xs font-semibold transition-all duration-150 active:opacity-70"
        :aria-pressed="filterLine === null"
        :style="filterLine === null
          ? { backgroundColor: 'var(--color-primary)', color: 'var(--color-text-on-primary)' }
          : { backgroundColor: 'var(--bg-elevated)', color: 'var(--text-secondary)', border: '1px solid var(--border)' }"
        @click="$emit('update:filterLine', null)"
      >
        {{ s.all }}
      </button>
      <button
        v-for="r in servingRoutes"
        :key="r.id"
        class="shrink-0 h-7 px-3 rounded-full text-xs font-bold transition-all duration-150 active:opacity-70"
        :aria-pressed="filterLine === r.name"
        :style="filterLine === r.name
          ? { backgroundColor: r.color || 'var(--color-primary)', color: r.textColor || 'var(--color-text-on-primary)' }
          : { backgroundColor: 'var(--bg-elevated)', color: 'var(--text-secondary)', border: '1px solid var(--border)' }"
        @click="$emit('update:filterLine', r.name)"
      >
        {{ r.name }}
      </button>
    </div>

    <!-- Realtime timestamp -->
    <span
      v-if="isLive && realtimeLastUpdated"
      role="status"
      aria-live="polite"
      class="block text-xs mb-2 tabular-nums"
      style="color: var(--text-tertiary)"
    >
      {{ s.updatedAt }} {{ realtimeLastUpdated }}
    </span>

    <!-- Departure rows -->
    <div
      v-if="filteredUpcomingDepartures.length"
      class="overflow-hidden divide-y"
      style="background-color: var(--bg-elevated); border-radius: 16px; border: 1px solid var(--border)"
    >
      <DepartureRow
        :departure="filteredUpcomingDepartures[0]!"
        :now="now"
        :locale="config?.locale[0]"
        :show-countdown="true"
        :show-next-label="true"
        :next-label="s.nextDepartureLabel"
      />
      <DepartureRow
        v-for="dep in filteredUpcomingDepartures.slice(1, showAllUpcoming ? undefined : 5)"
        :key="dep.id"
        :departure="dep"
        :now="now"
        :locale="config?.locale[0]"
        :show-countdown="true"
      />
      <button
        v-if="!showAllUpcoming && filteredUpcomingDepartures.length > 5"
        type="button"
        class="w-full py-3 text-sm font-semibold flex items-center justify-center gap-1.5 transition-opacity active:opacity-60"
        style="color: var(--color-primary)"
        @click="$emit('update:showAllUpcoming', true)"
      >
        <ChevronDown :size="15" :stroke-width="2" />
        {{ s.showMore }} {{ filteredUpcomingDepartures.length - 5 }}
      </button>
    </div>

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
      <p class="text-[15px] font-semibold mb-1" style="color: var(--text-primary)">
        {{ s.noDepartures }}
      </p>
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
      <button
        type="button"
        class="inline-flex items-center gap-1 text-sm font-semibold mt-3 transition-opacity active:opacity-60"
        style="color: var(--color-primary)"
        @click="$emit('view-schedule')"
      >
        {{ s.viewFullSchedule }}
        <ChevronDown :size="14" :stroke-width="1.75" />
      </button>
    </div>

    <!-- Realtime status -->
    <p
      v-if="isLive"
      class="text-xs mt-2 flex items-center gap-1.5"
      style="color: var(--color-live)"
      role="status"
    >
      <span class="w-1.5 h-1.5 rounded-full shrink-0" style="background-color: var(--color-live)" aria-hidden="true" />
      {{ s.updatedRealtime }}
    </p>
    <div aria-live="polite" class="sr-only">{{ isLive ? s.updatedRealtime : '' }}</div>
  </section>
</template>

<script setup lang="ts">
import { RefreshCw, Clock, ChevronDown, WifiOff } from 'lucide-vue-next'
import type { OperatorConfig, Departure, Route } from '~/types'
import type { AppStrings } from '~/utils/strings'
import { formatClockTime } from '~/utils/clockTime'

const props = defineProps<{
  s: AppStrings
  config: OperatorConfig | null | undefined
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
  (e: 'view-schedule'): void
}>()

const hasRealtimeConfigured = computed(() => Boolean(props.config?.gtfsRt?.trip_updates))
</script>
