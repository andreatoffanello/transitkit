<template>
  <footer class="px-4 pb-8 flex items-center gap-4">
    <a
      v-if="stop.lat && stop.lng"
      :href="`https://maps.google.com/?q=${stop.lat},${stop.lng}`"
      target="_blank"
      rel="noopener noreferrer"
      class="flex items-center gap-1.5 text-sm transition-opacity active:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
      style="color: var(--text-tertiary)"
    >
      <MapPin :size="14" :stroke-width="1.75" />
      Google Maps
    </a>
    <button
      v-if="canShare"
      type="button"
      class="flex items-center gap-1.5 text-sm transition-opacity active:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
      style="color: var(--text-tertiary)"
      @click="$emit('share')"
    >
      <Share2 :size="14" :stroke-width="1.75" />
      {{ s.share }}
    </button>
    <button
      v-else
      type="button"
      class="flex items-center gap-1.5 text-sm transition-opacity active:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-gray-400"
      style="color: var(--text-tertiary)"
      @click="$emit('copy-link')"
    >
      <component :is="copied ? Check : Copy" :size="14" :stroke-width="1.75" />
      {{ copied ? s.copiedFeedback : s.copyLink }}
    </button>
  </footer>
</template>

<script setup lang="ts">
import { MapPin, Share2, Copy, Check } from 'lucide-vue-next'
import type { ScheduleStop } from '~/types'
import type { AppStrings } from '~/utils/strings'

defineProps<{
  stop: ScheduleStop
  s: AppStrings
  canShare: boolean
  copied: boolean
}>()

defineEmits<{
  (e: 'share'): void
  (e: 'copy-link'): void
}>()
</script>
