<template>
  <div>
    <!-- Back navigation -->
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

    <!-- Stop identity -->
    <div class="px-4 pt-2 pb-2">
      <div class="flex items-center justify-between gap-3">
        <div class="min-w-0 flex-1">
          <h1 class="text-[22px] font-bold leading-tight" style="color: var(--text-primary)">
            {{ stop.name }}
          </h1>
        </div>

        <div class="flex items-center gap-1.5 shrink-0">
          <!-- Favorite — Star, parity con iOS/Android -->
          <button
            v-if="config?.features?.enableFavorites"
            type="button"
            :aria-label="isFavorite(stop.id) ? s.removeFromFavorites : s.addToFavorites"
            data-testid="btn_favorite"
            class="fav-btn rounded-lg p-2 transition-transform focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
            :class="{ 'fav-active': isFavorite(stop.id) }"
            :style="{ color: isFavorite(stop.id) ? 'var(--color-primary)' : 'var(--text-tertiary)' }"
            @click="$emit('toggle-favorite')"
          >
            <Star :size="20" :stroke-width="1.75" :fill="isFavorite(stop.id) ? 'currentColor' : 'none'" />
          </button>

          <!-- Navigate primary action — apre maps esterne (parity native quick action) -->
          <a
            v-if="stop.lat && stop.lng"
            :href="`geo:${stop.lat},${stop.lng}?q=${stop.lat},${stop.lng}`"
            :aria-label="s.navigate"
            class="navigate-btn inline-flex items-center justify-center rounded-full transition-opacity active:opacity-70 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
            :style="{ color: 'var(--color-primary)', backgroundColor: 'color-mix(in srgb, var(--color-primary) 10%, transparent)' }"
          >
            <Navigation :size="18" :stroke-width="2" />
          </a>
        </div>
      </div>

      <!-- Line badges horizontal scroll -->
      <div
        v-if="servingRoutes.length"
        class="flex gap-1.5 mt-2.5 overflow-x-auto scrollbar-none pb-0.5"
        role="list"
      >
        <LineBadge
          v-for="r in servingRoutes"
          :key="r.id"
          :name="r.name"
          :color="r.color"
          :text-color="r.textColor"
          :locale="config?.locale[0]"
          role="listitem"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Star, Navigation, ChevronLeft } from 'lucide-vue-next'
import type { OperatorConfig, Route, ScheduleStop } from '~/types'
import type { AppStrings } from '~/utils/strings'

defineProps<{
  stop: ScheduleStop
  fromLine: Route | null
  servingRoutes: Route[]
  config: OperatorConfig | null
  s: AppStrings
  canShare: boolean
  copied: boolean
  isFavorite: (id: string) => boolean
}>()

defineEmits<{
  (e: 'toggle-favorite'): void
  (e: 'share'): void
  (e: 'copy-link'): void
}>()
</script>

<style scoped>
.fav-btn {
  transform: scale(1);
  transition: transform 220ms cubic-bezier(0.34, 1.56, 0.64, 1), color 150ms ease;
}
.fav-active {
  transform: scale(1.12);
}
.navigate-btn {
  width: 40px;
  height: 40px;
}
</style>
