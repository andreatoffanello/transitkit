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
      <div class="flex items-start justify-between gap-2">
        <h1 class="text-[22px] font-bold leading-tight" style="color: var(--text-primary)">
          {{ stop.name }}
        </h1>
        <div class="flex items-center gap-0.5 mt-0.5 shrink-0">
          <button
            v-if="config?.features?.enableFavorites"
            type="button"
            :aria-label="isFavorite(stop.id) ? s.removeFromFavorites : s.addToFavorites"
            data-testid="btn_favorite"
            class="p-2 rounded-lg transition-opacity active:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
            :style="{ color: isFavorite(stop.id) ? 'var(--color-primary)' : 'var(--text-tertiary)' }"
            @click="$emit('toggle-favorite')"
          >
            <Bookmark :size="20" :stroke-width="1.75" :fill="isFavorite(stop.id) ? 'currentColor' : 'none'" />
          </button>
          <button
            v-if="canShare"
            type="button"
            :aria-label="s.shareStop"
            class="p-2 rounded-lg transition-opacity active:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
            style="color: var(--text-tertiary)"
            @click="$emit('share')"
          >
            <Share2 :size="20" :stroke-width="1.75" />
          </button>
          <button
            v-else
            type="button"
            :aria-label="s.copyLink"
            class="p-2 rounded-lg transition-opacity active:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
            style="color: var(--text-tertiary)"
            @click="$emit('copy-link')"
          >
            <component :is="copied ? Check : Copy" :size="20" :stroke-width="1.75" />
          </button>
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
import { Bookmark, Share2, Copy, Check, ChevronLeft } from 'lucide-vue-next'
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
