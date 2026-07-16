<template>
  <header
    class="hidden lg:block sticky top-0 z-40"
    style="
      background-color: color-mix(in srgb, var(--bg-elevated) 80%, transparent);
      backdrop-filter: blur(20px) saturate(180%);
      -webkit-backdrop-filter: blur(20px) saturate(180%);
      border-bottom: 1px solid var(--border);
    "
  >
    <div class="max-w-2xl mx-auto px-5 h-14 flex items-center gap-4">
      <!-- Brand — doubles as Home link -->
      <NuxtLink
        to="/"
        class="flex items-center gap-2.5 min-w-0 rounded-lg transition-opacity duration-150 active:opacity-70"
        :aria-label="config?.brandName ?? config?.name ?? 'Home'"
      >
        <img
          v-if="appLogoUrl && !logoFailed"
          :src="appLogoUrl"
          alt=""
          class="shrink-0 object-contain"
          style="width: 30px; height: 30px"
          @error="logoFailed = true"
        />
        <div
          v-else
          class="shrink-0 rounded-lg flex items-center justify-center"
          style="width: 30px; height: 30px; background-color: var(--color-primary)"
        >
          <Bus :size="17" :stroke-width="1.75" style="color: var(--color-text-on-primary)" />
        </div>
        <div class="min-w-0 leading-tight">
          <p class="text-[13px] font-semibold truncate" style="color: var(--text-primary)">
            {{ config?.brandName ?? config?.name ?? 'TransitKit' }}
          </p>
          <p v-if="config?.region" class="text-[11px] truncate" style="color: var(--text-tertiary)">
            {{ config.region }}
          </p>
        </div>
      </NuxtLink>

      <!-- Navigation -->
      <nav class="ml-auto flex items-center gap-1" :aria-label="s.mainNavAriaLabel">
        <NuxtLink
          v-for="tab in visibleTabs"
          :key="tab.path"
          :to="tab.path"
          class="flex items-center gap-2 px-3 py-1.5 rounded-lg text-[13px] font-medium transition-colors duration-150"
          :class="isActive(tab.path) ? '' : 'hover-row'"
          :style="isActive(tab.path)
            ? 'background-color: color-mix(in srgb, var(--color-primary) 10%, transparent); color: var(--color-primary);'
            : 'color: var(--text-secondary);'"
          :aria-current="isActive(tab.path) ? 'page' : undefined"
        >
          <component :is="tab.icon" :size="17" :stroke-width="isActive(tab.path) ? 2.25 : 1.75" />
          {{ tab.label }}
        </NuxtLink>
      </nav>
    </div>
  </header>
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

// Il bus dell'app (foreground trasparente), non `config.logoUrl` — quello è
// l'icona launcher col fondo. Parity con l'header iOS (imageset OperatorLogo).
const operatorId = useState<string>('operatorId')
const logoFailed = ref(false)
const appLogoUrl = computed(() =>
  operatorId.value ? `/brand/${operatorId.value}/app-logo.png` : null,
)

const visibleTabs = computed<Tab[]>(() => [
  { path: '/', label: s.value.tabHome, icon: Home },
  { path: '/lines', label: s.value.tabLines, icon: Route },
])

function isActive(path: string): boolean {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}
</script>
