<template>
  <nav
    aria-label="Navigazione principale"
    class="fixed bottom-0 left-0 right-0 z-40 lg:hidden"
    style="
      border-top: 1px solid var(--border);
      box-shadow: 0 -4px 16px rgba(0,0,0,0.08);
      backdrop-filter: blur(20px) saturate(180%);
      -webkit-backdrop-filter: blur(20px) saturate(180%);
      background-color: color-mix(in srgb, var(--bg-elevated) 88%, transparent);
      padding-bottom: env(safe-area-inset-bottom);
    "
  >
    <ul class="flex items-stretch h-16">
      <li v-for="tab in visibleTabs" :key="tab.path" class="flex-1">
        <NuxtLink
          :to="tab.path"
          :aria-label="tab.label"
          class="flex flex-col items-center justify-center gap-0.5 h-full w-full transition-colors duration-200 relative"
          :style="isActive(tab.path) ? 'color: var(--color-primary)' : 'color: var(--text-tertiary)'"
        >
          <!-- Active dot indicator -->
          <span
            v-if="isActive(tab.path)"
            class="absolute top-2 w-1 h-1 rounded-full"
            style="background-color: var(--color-primary)"
            aria-hidden="true"
          />
          <component :is="tab.icon" :size="24" :stroke-width="1.75" />
          <span class="text-[11px] font-medium leading-none">{{ tab.label }}</span>
        </NuxtLink>
      </li>
    </ul>
  </nav>
</template>

<script setup lang="ts">
import { Home, Route, Map, Settings } from 'lucide-vue-next'
import type { Component } from 'vue'

interface Tab {
  path: string
  label: string
  icon: Component
  feature?: string
}

const route = useRoute()
const { config } = await useOperator()

const ALL_TABS: Tab[] = [
  { path: '/', label: 'Home', icon: Home },
  { path: '/lines', label: 'Linee', icon: Route },
  { path: '/map', label: 'Mappa', icon: Map, feature: 'enableMap' },
  { path: '/settings', label: 'Impostazioni', icon: Settings },
]

const visibleTabs = computed(() =>
  ALL_TABS.filter(tab => {
    if (!tab.feature) return true
    return (config.value?.features as unknown as Record<string, boolean>)?.[tab.feature] ?? false
  })
)

function isActive(path: string): boolean {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}
</script>
