<template>
  <aside
    class="hidden lg:flex flex-col fixed left-0 top-0 bottom-0 w-60 z-40"
    style="background-color: var(--bg-elevated); border-right: 1px solid var(--border);"
  >
    <!-- Operator branding -->
    <div class="px-5 py-6" style="border-bottom: 1px solid var(--border)">
      <img
        v-if="config?.logoUrl"
        :src="config.logoUrl"
        alt=""
        class="w-10 h-10 rounded-xl object-cover mb-3"
        style="width: 40px; height: 40px"
      />
      <div
        v-else
        class="w-10 h-10 rounded-xl flex items-center justify-center mb-3"
        style="background-color: var(--color-primary)"
      >
        <Bus :size="20" :stroke-width="1.75" style="color: var(--color-text-on-primary)" />
      </div>
      <p class="text-sm font-semibold leading-tight" style="color: var(--text-primary)">
        {{ config?.brandName ?? config?.name ?? 'TransitKit' }}
      </p>
      <p v-if="config?.region" class="text-xs mt-0.5" style="color: var(--text-tertiary)">
        {{ config.region }}
      </p>
    </div>

    <!-- Navigation -->
    <nav class="flex-1 px-3 py-4" :aria-label="s.mainNavAriaLabel">
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

  </aside>
</template>

<script setup lang="ts">
import { Home, Route, Bus } from 'lucide-vue-next'
import type { Component } from 'vue'

interface Tab {
  path: string
  label: string
  icon: Component
}

const route = useRoute()
const { config } = await useOperator()

const s = useStrings(config)

const visibleTabs = computed<Tab[]>(() => [
  { path: '/', label: s.value.tabHome, icon: Home },
  { path: '/lines', label: s.value.tabLines, icon: Route },
])

function isActive(path: string): boolean {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}
</script>
