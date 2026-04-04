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
          <!-- Pill background indicator -->
          <span
            v-if="isActive(tab.path)"
            class="absolute rounded-full transition-all duration-200"
            style="
              width: 52px;
              height: 32px;
              top: 50%;
              left: 50%;
              transform: translate(-50%, -55%);
              background-color: color-mix(in srgb, var(--color-primary) 12%, transparent);
            "
            aria-hidden="true"
          />
          <component :is="tab.icon" :size="22" :stroke-width="isActive(tab.path) ? 2.25 : 1.75" class="relative z-10" />
          <span class="text-[11px] font-medium leading-none relative z-10">{{ tab.label }}</span>
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
