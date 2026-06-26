<template>
  <div>
    <!-- Back navigation (web needs an explicit back; iOS uses the nav bar) -->
    <div class="px-2 pt-2">
      <NuxtLink
        :to="fromLine ? `/lines/${fromLine.id}` : '/'"
        class="inline-flex items-center gap-1.5 text-sm font-medium px-2 py-1.5 rounded-lg transition-opacity active:opacity-60"
        style="color: var(--color-primary)"
      >
        <ChevronLeft :size="16" :stroke-width="2" />
        {{ fromLine ? fromLine.name : s.backToHome }}
      </NuxtLink>
    </div>

    <!-- Stop identity: name + transit-type row + favorite action -->
    <div class="flex items-center gap-3 px-5 pt-5 pb-3">
      <div class="min-w-0 flex-1">
        <h1 class="text-[22px] font-bold leading-tight" style="color: var(--text-primary)">
          {{ stop.name }}
        </h1>
        <div v-if="transitTypes.length" class="flex items-center gap-3 mt-1">
          <span
            v-for="t in transitTypes"
            :key="t.key"
            class="inline-flex items-center gap-1 text-xs font-medium"
            style="color: var(--text-secondary)"
          >
            <component :is="t.icon" :size="12" :stroke-width="2" />
            {{ t.label }}
          </span>
        </div>
      </div>

      <button
        v-if="config?.features?.enableFavorites"
        type="button"
        :aria-label="isFavorite(stop.id) ? s.removeFromFavorites : s.addToFavorites"
        data-testid="btn_favorite"
        class="fav-btn shrink-0 rounded-full p-2 transition-transform focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
        :class="{ 'fav-active': isFavorite(stop.id) }"
        :style="{ color: isFavorite(stop.id) ? 'var(--color-primary)' : 'var(--text-tertiary)' }"
        @click="$emit('toggle-favorite')"
      >
        <Star :size="20" :stroke-width="1.75" :fill="isFavorite(stop.id) ? 'currentColor' : 'none'" />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Star, ChevronLeft, BusFront, TramFront, Train, Ship } from 'lucide-vue-next'
import type { Component } from 'vue'
import type { OperatorConfig, Route, ScheduleStop, TransitType } from '~/types'
import type { AppStrings } from '~/utils/strings'

const props = defineProps<{
  stop: ScheduleStop
  fromLine: Route | null
  servingRoutes: Route[]
  config: OperatorConfig | null | undefined
  s: AppStrings
  isFavorite: (id: string) => boolean
}>()

defineEmits<{
  (e: 'toggle-favorite'): void
}>()

const ICONS: Partial<Record<TransitType, Component>> = {
  tram: TramFront, metro: Train, rail: Train, monorail: Train, ferry: Ship,
}

// Distinct transit modes served by this stop (iOS shows "Bus" / "Tram" etc.
// in the identity header, not the line list — those are the filter chips).
const transitTypes = computed(() => {
  const seen = new Set<TransitType>()
  const out: { key: string; icon: Component; label: string }[] = []
  for (const r of props.servingRoutes) {
    if (seen.has(r.transitType)) continue
    seen.add(r.transitType)
    out.push({
      key: r.transitType,
      icon: ICONS[r.transitType] ?? BusFront,
      label: props.s.transitTypes[r.transitType] ?? r.transitType,
    })
  }
  return out
})
</script>

<style scoped>
.fav-btn {
  transform: scale(1);
  transition: transform 220ms cubic-bezier(0.34, 1.56, 0.64, 1), color 150ms ease;
}
.fav-active {
  transform: scale(1.12);
}
</style>
