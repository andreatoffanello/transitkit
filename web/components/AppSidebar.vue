<template>
  <aside
    class="hidden lg:flex flex-col fixed left-0 top-0 bottom-0 w-60 z-40"
    style="background-color: var(--bg-elevated); border-right: 1px solid var(--border);"
  >
    <!-- Operator branding -->
    <div class="px-5 py-6" style="border-bottom: 1px solid var(--border)">
      <div
        class="w-10 h-10 rounded-xl flex items-center justify-center mb-3"
        style="background-color: var(--color-primary)"
      >
        <Bus :size="20" :stroke-width="1.75" style="color: var(--color-text-on-primary)" />
      </div>
      <p class="text-sm font-semibold leading-tight" style="color: var(--text-primary)">
        {{ config?.name ?? 'TransitKit' }}
      </p>
      <p v-if="config?.region" class="text-xs mt-0.5" style="color: var(--text-tertiary)">
        {{ config.region }}
      </p>
    </div>

    <!-- Navigation -->
    <nav class="flex-1 px-3 py-4" aria-label="Navigazione principale">
      <ul class="space-y-0.5">
        <li v-for="tab in visibleTabs" :key="tab.path">
          <NuxtLink
            :to="tab.path"
            class="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors duration-150 hover-row"
            :style="isActive(tab.path)
              ? `background-color: color-mix(in srgb, var(--color-primary) 10%, transparent); color: var(--color-primary);`
              : `color: var(--text-secondary);`"
          >
            <component :is="tab.icon" :size="20" :stroke-width="isActive(tab.path) ? 2.25 : 1.75" />
            {{ tab.label }}
          </NuxtLink>
        </li>
      </ul>
    </nav>

    <!-- Theme toggle -->
    <div class="px-3 pb-5 pt-2" style="border-top: 1px solid var(--border)">
      <button
        @click="toggleTheme"
        class="flex items-center gap-3 px-3 py-2.5 rounded-xl w-full text-sm font-medium transition-colors duration-150"
        style="color: var(--text-secondary)"
        :aria-label="isDark ? 'Passa al tema chiaro' : 'Passa al tema scuro'"
      >
        <component :is="isDark ? Sun : Moon" :size="20" :stroke-width="1.75" />
        {{ isDark ? 'Tema chiaro' : 'Tema scuro' }}
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { Home, Route, Map, Settings, Bus, Sun, Moon } from 'lucide-vue-next'
import type { Component } from 'vue'

interface Tab {
  path: string
  label: string
  icon: Component
  feature?: string
}

const route = useRoute()
const { config } = await useOperator()
const { isDark, toggleTheme } = useTheme()

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
