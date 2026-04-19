# Web App Redesign — Refined Minimal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Trasformare la web app transit-engine in un'interfaccia top-tier "Refined Minimal" coerente con le app iOS/Android: Plus Jakarta Sans, Lucide icons (zero emoji), bottom tab bar con glassmorphism (mobile) / sidebar (desktop), dark mode toggle manuale.

**Architecture:** Foundation (design tokens + font + Lucide) → Componenti base (TabBar, Sidebar, Header) → Componenti UI (DepartureRow, LineBadge, DayGroupTabs) → Pagine (Settings, Home, Lines, Stop) → UX review con Playwright.

**Tech Stack:** Nuxt 3, Vue 3, Tailwind CSS (darkMode: 'class'), lucide-vue-next, Plus Jakarta Sans (Google Fonts), CSS custom properties

**Spec di riferimento:** `docs/superpowers/specs/2026-04-04-web-redesign-design.md`

---

## FASE 1 — Foundation

### Task 1: Installa lucide-vue-next e Plus Jakarta Sans

**Files:**
- Modify: `web/package.json`
- Modify: `web/nuxt.config.ts`
- Modify: `web/assets/css/main.css`

- [ ] **Step 1.1: Installa lucide-vue-next**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/web && npm install lucide-vue-next
```

- [ ] **Step 1.2: Aggiorna assets/css/main.css — font + CSS variables**

Sostituire il contenuto di `web/assets/css/main.css` con:

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:ital,wght@0,400;0,500;0,600;0,700;0,800&display=swap');

:root {
  /* Typography */
  --font-sans: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;

  /* Backgrounds */
  --bg-primary: #FFFFFF;
  --bg-secondary: #F5F5F7;
  --bg-elevated: #FFFFFF;

  /* Text */
  --text-primary: #1A1A1A;
  --text-secondary: #6B6B6B;
  --text-tertiary: #ABABAB;

  /* Border */
  --border: rgba(0, 0, 0, 0.08);

  /* Shadows */
  --shadow-sm: 0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.08), 0 1px 4px rgba(0,0,0,0.04);
  --shadow-lg: 0 16px 48px rgba(0,0,0,0.12), 0 4px 16px rgba(0,0,0,0.06);

  /* Operator (overridden by theme plugin) */
  --color-primary: #003366;
  --color-accent: #0066CC;
  --color-text-on-primary: #FFFFFF;
  --color-secondary: #E8F0FA;
}

html.dark {
  /* Backgrounds */
  --bg-primary: #0A0A0F;
  --bg-secondary: #131318;
  --bg-elevated: #1C1C24;

  /* Text */
  --text-primary: #F2F2F7;
  --text-secondary: #8E8E9A;
  --text-tertiary: #48484E;

  /* Border */
  --border: rgba(255, 255, 255, 0.07);

  /* Shadows (più leggeri nel dark) */
  --shadow-sm: 0 1px 3px rgba(0,0,0,0.2), 0 1px 2px rgba(0,0,0,0.12);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.3), 0 1px 4px rgba(0,0,0,0.15);
  --shadow-lg: 0 16px 48px rgba(0,0,0,0.4), 0 4px 16px rgba(0,0,0,0.2);
}

* {
  box-sizing: border-box;
}

html {
  font-family: var(--font-sans);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

body {
  background-color: var(--bg-secondary);
  color: var(--text-primary);
  font-family: var(--font-sans);
}

/* Safe area per iPhone notch/home indicator */
.safe-area-bottom {
  padding-bottom: env(safe-area-inset-bottom);
}
```

- [ ] **Step 1.3: Aggiorna tailwind.config.ts**

Sostituire il contenuto con:

```typescript
import type { Config } from 'tailwindcss'

export default {
  darkMode: 'class',
  content: [
    './components/**/*.{js,vue,ts}',
    './layouts/**/*.vue',
    './pages/**/*.vue',
    './plugins/**/*.{js,ts}',
    './app.vue',
    './error.vue',
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Plus Jakarta Sans', '-apple-system', 'BlinkMacSystemFont', 'sans-serif'],
      },
      colors: {
        primary: 'var(--color-primary)',
        accent: 'var(--color-accent)',
        'text-on-primary': 'var(--color-text-on-primary)',
      },
      backgroundColor: {
        'app-primary': 'var(--bg-primary)',
        'app-secondary': 'var(--bg-secondary)',
        'app-elevated': 'var(--bg-elevated)',
      },
      textColor: {
        'app-primary': 'var(--text-primary)',
        'app-secondary': 'var(--text-secondary)',
        'app-tertiary': 'var(--text-tertiary)',
      },
      borderColor: {
        app: 'var(--border)',
      },
      boxShadow: {
        'app-sm': 'var(--shadow-sm)',
        'app-md': 'var(--shadow-md)',
        'app-lg': 'var(--shadow-lg)',
      },
    },
  },
  plugins: [],
} satisfies Config
```

- [ ] **Step 1.4: Commit foundation**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/web && git add package.json package-lock.json assets/css/main.css tailwind.config.ts && git commit -m "feat(web): foundation - Plus Jakarta Sans, CSS vars, Lucide, darkMode class"
```

---

### Task 2: Composable useTheme + app.vue

**Files:**
- Create: `web/composables/useTheme.ts`
- Modify: `web/app.vue`

- [ ] **Step 2.1: Crea web/composables/useTheme.ts**

```typescript
// web/composables/useTheme.ts
export function useTheme() {
  const isDark = useState<boolean>('isDark', () => false)

  function initTheme() {
    if (import.meta.client) {
      const stored = localStorage.getItem('theme')
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      const dark = stored === 'dark' || (!stored && prefersDark)
      isDark.value = dark
      applyTheme(dark)
    }
  }

  function toggleTheme() {
    isDark.value = !isDark.value
    applyTheme(isDark.value)
    if (import.meta.client) {
      localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
    }
  }

  function applyTheme(dark: boolean) {
    if (import.meta.client) {
      if (dark) {
        document.documentElement.classList.add('dark')
      } else {
        document.documentElement.classList.remove('dark')
      }
    }
  }

  return { isDark, initTheme, toggleTheme }
}
```

- [ ] **Step 2.2: Aggiorna web/app.vue**

Leggere il file corrente, poi sostituire completamente con:

```vue
<template>
  <div>
    <a href="#main-content" class="sr-only focus:not-sr-only focus:absolute focus:z-50 focus:p-4 focus:bg-primary focus:text-text-on-primary">
      Vai al contenuto principale
    </a>
    <NuxtLoadingIndicator :color="primaryColor" :height="2" />
    <NuxtPage id="main-content" />
  </div>
</template>

<script setup lang="ts">
const { initTheme } = useTheme()
const { config } = useOperator()

const primaryColor = computed(() => config.value?.theme?.primaryColor ?? '#003366')

onMounted(() => {
  initTheme()
})
</script>
```

- [ ] **Step 2.3: Commit**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/web && git add composables/useTheme.ts app.vue && git commit -m "feat(web): dark mode toggle composable + clean app.vue"
```

---

## FASE 2 — Componenti di Navigazione

### Task 3: AppTabBar.vue (bottom nav mobile) + AppSidebar.vue (desktop)

**Files:**
- Create: `web/components/AppTabBar.vue`
- Create: `web/components/AppSidebar.vue`
- Create: `web/components/AppLayout.vue`

- [ ] **Step 3.1: Crea web/components/AppTabBar.vue**

```vue
<template>
  <nav
    aria-label="Navigazione principale"
    class="fixed bottom-0 left-0 right-0 z-40 lg:hidden"
    style="
      background-color: var(--bg-elevated);
      border-top: 1px solid var(--border);
      box-shadow: var(--shadow-lg);
      backdrop-filter: blur(20px) saturate(180%);
      -webkit-backdrop-filter: blur(20px) saturate(180%);
      background-color: color-mix(in srgb, var(--bg-elevated) 85%, transparent);
      padding-bottom: env(safe-area-inset-bottom);
    "
  >
    <ul class="flex items-stretch h-16">
      <li v-for="tab in visibleTabs" :key="tab.path" class="flex-1">
        <NuxtLink
          :to="tab.path"
          :aria-label="tab.label"
          class="flex flex-col items-center justify-center gap-0.5 h-full w-full transition-colors duration-200 relative"
          :class="isActive(tab.path) ? 'text-primary' : 'text-app-tertiary'"
        >
          <component :is="tab.icon" :size="24" :stroke-width="1.75" />
          <span class="text-[11px] font-medium leading-none">{{ tab.label }}</span>
          <span
            v-if="isActive(tab.path)"
            class="absolute top-1.5 w-1 h-1 rounded-full bg-primary"
            aria-hidden="true"
          />
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
const { config } = useOperator()

const ALL_TABS: Tab[] = [
  { path: '/', label: 'Home', icon: Home },
  { path: '/lines', label: 'Linee', icon: Route },
  { path: '/map', label: 'Mappa', icon: Map, feature: 'enableMap' },
  { path: '/settings', label: 'Impostazioni', icon: Settings },
]

const visibleTabs = computed(() =>
  ALL_TABS.filter(tab => {
    if (!tab.feature) return true
    return config.value?.features?.[tab.feature as keyof typeof config.value.features] ?? false
  })
)

function isActive(path: string): boolean {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}
</script>
```

- [ ] **Step 3.2: Crea web/components/AppSidebar.vue**

```vue
<template>
  <aside
    class="hidden lg:flex flex-col fixed left-0 top-0 bottom-0 w-60 z-40"
    style="
      background-color: var(--bg-elevated);
      border-right: 1px solid var(--border);
      box-shadow: var(--shadow-sm);
    "
  >
    <!-- Operator branding -->
    <div class="px-6 py-6 border-b" style="border-color: var(--border)">
      <div
        class="w-10 h-10 rounded-xl flex items-center justify-center mb-3"
        style="background-color: var(--color-primary)"
      >
        <Bus :size="20" :stroke-width="1.75" style="color: var(--color-text-on-primary)" />
      </div>
      <p class="text-sm font-semibold leading-tight" style="color: var(--text-primary)">
        {{ config?.name ?? 'TransitKit' }}
      </p>
      <p class="text-xs mt-0.5" style="color: var(--text-tertiary)">
        {{ config?.region ?? '' }}
      </p>
    </div>

    <!-- Navigation -->
    <nav class="flex-1 px-3 py-4" aria-label="Navigazione principale">
      <ul class="space-y-1">
        <li v-for="tab in visibleTabs" :key="tab.path">
          <NuxtLink
            :to="tab.path"
            class="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors duration-150"
            :class="isActive(tab.path)
              ? 'bg-primary/10 text-primary'
              : 'hover:bg-black/5 dark:hover:bg-white/5 text-app-secondary'"
            :style="isActive(tab.path) ? `color: var(--color-primary); background-color: color-mix(in srgb, var(--color-primary) 10%, transparent)` : ''"
          >
            <component :is="tab.icon" :size="20" :stroke-width="1.75" />
            {{ tab.label }}
          </NuxtLink>
        </li>
      </ul>
    </nav>

    <!-- Dark mode toggle -->
    <div class="px-3 pb-6 pt-2 border-t" style="border-color: var(--border)">
      <button
        @click="toggleTheme"
        class="flex items-center gap-3 px-3 py-2.5 rounded-xl w-full text-sm font-medium transition-colors duration-150 hover:bg-black/5 dark:hover:bg-white/5"
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
const { config } = useOperator()
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
    return config.value?.features?.[tab.feature as keyof typeof config.value.features] ?? false
  })
)

function isActive(path: string): boolean {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}
</script>
```

- [ ] **Step 3.3: Crea web/components/AppLayout.vue — wrapper comune di tutte le pagine**

```vue
<template>
  <div class="min-h-screen" style="background-color: var(--bg-secondary)">
    <!-- Desktop sidebar -->
    <AppSidebar />

    <!-- Main content: shifted right on desktop, bottom padding on mobile -->
    <main
      class="lg:ml-60 pb-20 lg:pb-0 min-h-screen"
      style="background-color: var(--bg-secondary)"
    >
      <slot />
    </main>

    <!-- Mobile bottom tab bar -->
    <AppTabBar />
  </div>
</template>
```

- [ ] **Step 3.4: Commit**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/web && git add components/AppTabBar.vue components/AppSidebar.vue components/AppLayout.vue && git commit -m "feat(web): AppTabBar (mobile) + AppSidebar (desktop) + AppLayout wrapper"
```

---

### Task 4: PageHeader.vue — redesign con due varianti

**Files:**
- Modify: `web/components/PageHeader.vue`

- [ ] **Step 4.1: Leggi il file corrente prima di modificarlo**

Leggi `web/components/PageHeader.vue` per capire le props attuali.

- [ ] **Step 4.2: Sostituisci con nuovo design**

```vue
<template>
  <!-- Variante Large (primo livello, nessun back button) -->
  <header v-if="!backTo" class="px-5 pt-6 pb-3">
    <h1
      class="text-xl font-bold leading-tight"
      style="color: var(--text-primary)"
    >
      {{ title }}
    </h1>
    <p
      v-if="subtitle"
      class="text-sm mt-0.5"
      style="color: var(--text-secondary)"
    >
      {{ subtitle }}
    </p>
  </header>

  <!-- Variante Navigation (pagine dettaglio con back) -->
  <header
    v-else
    class="sticky top-0 z-30 flex items-center h-[52px] px-2 transition-all duration-200"
    :class="scrolled ? 'border-b' : ''"
    :style="{
      backgroundColor: scrolled
        ? `color-mix(in srgb, var(--bg-primary) 90%, transparent)`
        : 'transparent',
      backdropFilter: scrolled ? 'blur(16px) saturate(180%)' : 'none',
      WebkitBackdropFilter: scrolled ? 'blur(16px) saturate(180%)' : 'none',
      borderColor: 'var(--border)',
    }"
  >
    <!-- Back -->
    <NuxtLink
      :to="backTo"
      class="flex items-center gap-1 pl-2 pr-3 py-2 rounded-lg -ml-1 transition-colors duration-150"
      style="color: var(--color-primary)"
      :aria-label="`Torna a ${backLabel}`"
    >
      <ChevronLeft :size="20" :stroke-width="1.75" />
      <span class="text-sm font-medium">{{ backLabel }}</span>
    </NuxtLink>

    <!-- Title (centrato) -->
    <h1
      class="flex-1 text-center text-[15px] font-semibold truncate px-2"
      style="color: var(--text-primary)"
    >
      {{ title }}
    </h1>

    <!-- Action slot (destra) -->
    <div class="w-[72px] flex justify-end pr-2">
      <slot name="action" />
    </div>
  </header>
</template>

<script setup lang="ts">
import { ChevronLeft } from 'lucide-vue-next'

const props = defineProps<{
  title: string
  subtitle?: string
  backTo?: string
  backLabel?: string
}>()

const scrolled = ref(false)

onMounted(() => {
  if (props.backTo) {
    const handler = () => { scrolled.value = window.scrollY > 4 }
    window.addEventListener('scroll', handler, { passive: true })
    onUnmounted(() => window.removeEventListener('scroll', handler))
  }
})
</script>
```

- [ ] **Step 4.3: Commit**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/web && git add components/PageHeader.vue && git commit -m "feat(web): PageHeader redesign - Large + Navigation variants, scroll-aware border"
```

---

## FASE 3 — Componenti UI

### Task 5: DepartureRow.vue — rimozione emoji, Lucide, stile raffinato

**Files:**
- Modify: `web/components/DepartureRow.vue`

- [ ] **Step 5.1: Leggi il file corrente**

Leggi `web/components/DepartureRow.vue` per capire la struttura attuale.

- [ ] **Step 5.2: Riscrivi DepartureRow.vue**

```vue
<template>
  <div
    class="flex items-center gap-3 py-3.5 px-4"
    :class="{ 'opacity-50': isPast }"
  >
    <!-- Line badge -->
    <LineBadge :name="departure.lineName" :color="departure.color" :text-color="departure.textColor" />

    <!-- Transit type icon -->
    <component
      :is="transitIcon"
      :size="16"
      :stroke-width="1.75"
      class="shrink-0"
      style="color: var(--text-tertiary)"
      aria-hidden="true"
    />

    <!-- Headsign -->
    <span
      class="flex-1 text-[15px] font-medium truncate"
      style="color: var(--text-primary)"
    >
      {{ departure.headsign }}
    </span>

    <!-- Time + realtime indicator -->
    <div class="flex items-center gap-2 shrink-0">
      <!-- Realtime dot -->
      <span
        v-if="departure.isRealtime"
        class="w-2 h-2 rounded-full bg-green-500 animate-pulse shrink-0"
        aria-label="Dati in tempo reale"
        role="img"
      />

      <!-- Time display -->
      <div class="text-right">
        <!-- Orario con ritardo -->
        <template v-if="departure.delayMinutes && departure.delayMinutes > 0">
          <span
            class="block text-[13px] line-through"
            style="color: var(--text-tertiary); font-variant-numeric: tabular-nums; letter-spacing: -0.02em"
          >
            {{ departure.scheduledTime }}
          </span>
          <span
            class="block text-[15px] font-semibold text-orange-500"
            style="font-variant-numeric: tabular-nums; letter-spacing: -0.02em"
          >
            {{ departure.realtimeTime }}
          </span>
        </template>

        <!-- Countdown o orario normale -->
        <template v-else>
          <span
            v-if="showCountdown && departure.minutesUntil !== undefined && departure.minutesUntil <= 30"
            class="text-[15px] font-semibold"
            :style="`color: var(--color-primary); font-variant-numeric: tabular-nums; letter-spacing: -0.02em`"
          >
            {{ departure.minutesUntil <= 1 ? 'Ora' : `${departure.minutesUntil} min` }}
          </span>
          <span
            v-else
            class="text-[15px] font-semibold"
            style="color: var(--text-primary); font-variant-numeric: tabular-nums; letter-spacing: -0.02em"
          >
            {{ departure.scheduledTime }}
          </span>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Bus, TramFront, TrainFront, Ship, Cable } from 'lucide-vue-next'
import type { Component } from 'vue'
import type { Departure } from '~/types'

const TRANSIT_ICONS: Record<string, Component> = {
  bus: Bus,
  tram: TramFront,
  rail: TrainFront,
  ferry: Ship,
  metro: Cable,
  default: Bus,
}

const props = defineProps<{
  departure: Departure
  showCountdown?: boolean
  isPast?: boolean
}>()

const transitIcon = computed(() =>
  TRANSIT_ICONS[props.departure.routeType ?? 'default'] ?? TRANSIT_ICONS.default
)
</script>
```

- [ ] **Step 5.3: Commit**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/web && git add components/DepartureRow.vue && git commit -m "feat(web): DepartureRow - Lucide icons, countdown, delay display, no emoji"
```

---

### Task 6: LineBadge.vue e DayGroupTabs.vue — aggiornamento stile

**Files:**
- Modify: `web/components/LineBadge.vue`
- Modify: `web/components/DayGroupTabs.vue`

- [ ] **Step 6.1: Leggi entrambi i file**

Leggi `web/components/LineBadge.vue` e `web/components/DayGroupTabs.vue`.

- [ ] **Step 6.2: Aggiorna LineBadge.vue**

Mantieni la logica esistente, aggiorna solo il template per il border-radius e leggero padding:

```vue
<template>
  <span
    class="inline-flex items-center justify-center px-2 py-0.5 text-xs font-bold leading-none rounded shrink-0"
    :style="{
      backgroundColor: bgColor,
      color: textColor,
      minWidth: '2rem',
    }"
  >
    {{ name }}
  </span>
</template>

<script setup lang="ts">
const props = defineProps<{
  name: string
  color?: string
  textColor?: string
}>()

const bgColor = computed(() => props.color ?? 'var(--color-primary)')
const textColor = computed(() => props.textColor ?? 'var(--color-text-on-primary)')
</script>
```

- [ ] **Step 6.3: Aggiorna DayGroupTabs.vue — pill tabs raffinati**

```vue
<template>
  <div
    class="flex gap-1.5 overflow-x-auto pb-0.5 scrollbar-hide"
    role="tablist"
    :aria-label="label ?? 'Gruppi giorno'"
  >
    <button
      v-for="group in groups"
      :key="group.id"
      role="tab"
      :aria-selected="group.id === selectedId"
      :aria-controls="`tabpanel-${group.id}`"
      class="px-3.5 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition-all duration-200 shrink-0"
      :style="group.id === selectedId
        ? `background-color: var(--color-primary); color: var(--color-text-on-primary);`
        : `background-color: var(--bg-primary); color: var(--text-secondary); border: 1px solid var(--border);`"
      @click="$emit('update:selectedId', group.id)"
    >
      {{ group.label }}
    </button>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  groups: Array<{ id: string; label: string }>
  selectedId: string
  label?: string
}>()

defineEmits<{
  'update:selectedId': [id: string]
}>()
</script>
```

- [ ] **Step 6.4: Commit**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/web && git add components/LineBadge.vue components/DayGroupTabs.vue && git commit -m "feat(web): LineBadge + DayGroupTabs - refined pill tabs, clean badges"
```

---

## FASE 4 — Pagine

### Task 7: settings.vue — nuova pagina impostazioni

**Files:**
- Create: `web/pages/settings.vue`

- [ ] **Step 7.1: Crea web/pages/settings.vue**

```vue
<template>
  <AppLayout>
    <div class="max-w-lg mx-auto lg:max-w-2xl">
      <PageHeader title="Impostazioni" />

      <div class="px-4 space-y-3 pb-8">
        <!-- Aspetto -->
        <section>
          <h2
            class="text-xs font-semibold uppercase tracking-wider px-1 mb-2"
            style="color: var(--text-tertiary)"
          >
            Aspetto
          </h2>
          <div
            class="rounded-2xl overflow-hidden"
            style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm)"
          >
            <button
              class="flex items-center gap-4 w-full px-4 py-4 transition-colors duration-150 active:opacity-70"
              @click="toggleTheme"
              :aria-pressed="isDark"
            >
              <div
                class="w-9 h-9 rounded-xl flex items-center justify-center shrink-0"
                style="background-color: var(--bg-secondary)"
              >
                <component :is="isDark ? Sun : Moon" :size="18" :stroke-width="1.75" style="color: var(--text-secondary)" />
              </div>
              <div class="flex-1 text-left">
                <p class="text-[15px] font-medium" style="color: var(--text-primary)">Tema scuro</p>
                <p class="text-sm" style="color: var(--text-secondary)">{{ isDark ? 'Attivo' : 'Non attivo' }}</p>
              </div>
              <!-- Toggle switch -->
              <div
                class="w-12 h-7 rounded-full relative transition-colors duration-300 shrink-0"
                :style="isDark ? `background-color: var(--color-primary)` : `background-color: var(--text-tertiary)`"
              >
                <div
                  class="absolute top-0.5 w-6 h-6 bg-white rounded-full shadow transition-transform duration-300"
                  :class="isDark ? 'translate-x-5' : 'translate-x-0.5'"
                />
              </div>
            </button>
          </div>
        </section>

        <!-- Info app -->
        <section>
          <h2
            class="text-xs font-semibold uppercase tracking-wider px-1 mb-2"
            style="color: var(--text-tertiary)"
          >
            Informazioni
          </h2>
          <div
            class="rounded-2xl overflow-hidden divide-y"
            style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm); divide-color: var(--border)"
          >
            <div class="flex items-center gap-4 px-4 py-4">
              <div
                class="w-9 h-9 rounded-xl flex items-center justify-center shrink-0"
                style="background-color: var(--bg-secondary)"
              >
                <Bus :size="18" :stroke-width="1.75" style="color: var(--text-secondary)" />
              </div>
              <div class="flex-1">
                <p class="text-[15px] font-medium" style="color: var(--text-primary)">{{ config?.name ?? 'TransitKit' }}</p>
                <p class="text-sm" style="color: var(--text-secondary)">{{ config?.fullName }}</p>
              </div>
            </div>
            <a
              v-if="config?.privacyUrl"
              :href="config.privacyUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="flex items-center gap-4 px-4 py-4 transition-colors duration-150 active:opacity-70"
            >
              <div
                class="w-9 h-9 rounded-xl flex items-center justify-center shrink-0"
                style="background-color: var(--bg-secondary)"
              >
                <Shield :size="18" :stroke-width="1.75" style="color: var(--text-secondary)" />
              </div>
              <span class="flex-1 text-[15px] font-medium" style="color: var(--text-primary)">Privacy</span>
              <ExternalLink :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" />
            </a>
          </div>
        </section>

        <!-- Version -->
        <p class="text-center text-xs py-4" style="color: var(--text-tertiary)">
          TransitKit · Powered by transit-engine
        </p>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { Sun, Moon, Bus, Shield, ExternalLink } from 'lucide-vue-next'

const { isDark, toggleTheme } = useTheme()
const { config } = useOperator()

useHead({ title: 'Impostazioni' })
</script>
```

- [ ] **Step 7.2: Commit**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/web && git add pages/settings.vue && git commit -m "feat(web): settings page - dark mode toggle + app info"
```

---

### Task 8: index.vue — redesign Home

**Files:**
- Modify: `web/pages/index.vue`

- [ ] **Step 8.1: Leggi il file corrente per capire la logica**

Leggi `web/pages/index.vue` (tutte le righe) — mantieni tutta la logica Vue (composables, computed, watch) e riscrivi SOLO il template.

- [ ] **Step 8.2: Sostituisci il template in index.vue**

Il template deve seguire questa struttura (adattando le variabili Vue già esistenti nel file):

```html
<template>
  <AppLayout>
    <div class="max-w-lg mx-auto lg:max-w-2xl">

      <!-- Hero section -->
      <section
        class="px-5 pt-8 pb-6"
        style="background: linear-gradient(135deg, color-mix(in srgb, var(--color-primary) 8%, transparent), transparent)"
      >
        <div
          class="w-12 h-12 rounded-2xl flex items-center justify-center mb-4"
          style="background-color: var(--color-primary)"
        >
          <Bus :size="24" :stroke-width="1.75" style="color: var(--color-text-on-primary)" />
        </div>
        <h1 class="text-2xl font-bold leading-tight mb-1" style="color: var(--text-primary)">
          {{ config?.name ?? 'Trasporto' }}
        </h1>
        <p class="text-sm" style="color: var(--text-secondary)">
          {{ config?.store?.subtitle ?? 'Orari e partenze in tempo reale' }}
        </p>
      </section>

      <!-- Search bar -->
      <section class="px-4 pb-4">
        <div
          class="flex items-center gap-3 px-4 rounded-2xl h-12"
          style="background-color: var(--bg-elevated); box-shadow: var(--shadow-md); border: 1px solid var(--border)"
        >
          <Search :size="18" :stroke-width="1.75" style="color: var(--text-tertiary)" />
          <input
            v-model="stopQuery"
            type="search"
            :placeholder="strings.searchStops ?? 'Cerca fermate...'"
            class="flex-1 bg-transparent text-[15px] outline-none"
            style="color: var(--text-primary); font-family: var(--font-sans)"
            @focus="showSearch = true"
          />
          <button
            v-if="stopQuery"
            @click="stopQuery = ''; showSearch = false"
            :aria-label="'Cancella ricerca'"
          >
            <X :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" />
          </button>
        </div>

        <!-- Search results -->
        <div
          v-if="showSearch && filteredStops.length > 0"
          class="mt-2 rounded-2xl overflow-hidden"
          style="background-color: var(--bg-elevated); box-shadow: var(--shadow-md)"
        >
          <button
            v-for="stop in filteredStops.slice(0, 8)"
            :key="stop.id"
            class="flex items-center gap-3 w-full px-4 py-3 text-left transition-colors duration-150 active:opacity-70 border-b last:border-b-0"
            style="border-color: var(--border)"
            @click="navigateToStop(stop)"
          >
            <MapPin :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
            <span
              class="flex-1 text-[15px] font-medium truncate"
              style="color: var(--text-primary)"
              v-html="highlightMatch(stop.name, stopQuery)"
            />
          </button>
        </div>
      </section>

      <div class="px-4 space-y-6 pb-8">

        <!-- Nelle vicinanze -->
        <section v-if="features?.enableGeolocation && nearbyStops?.length">
          <h2 class="text-xs font-semibold uppercase tracking-wider mb-3" style="color: var(--text-tertiary)">
            Nelle vicinanze
          </h2>
          <div
            class="rounded-2xl overflow-hidden divide-y"
            style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm); divide-color: var(--border)"
          >
            <NuxtLink
              v-for="stop in nearbyStops.slice(0, 5)"
              :key="stop.id"
              :to="`/stop/${stop.id}`"
              class="flex items-center gap-3 px-4 py-3.5 transition-colors duration-150 active:opacity-70"
            >
              <Navigation :size="16" :stroke-width="1.75" style="color: var(--color-primary)" class="shrink-0" />
              <div class="flex-1 min-w-0">
                <p class="text-[15px] font-medium truncate" style="color: var(--text-primary)">{{ stop.name }}</p>
                <p class="text-xs" style="color: var(--text-tertiary)">{{ stop.distance }}</p>
              </div>
              <ChevronRight :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
            </NuxtLink>
          </div>
        </section>

        <!-- Preferiti -->
        <section v-if="features?.enableFavorites && favoriteStops?.length">
          <h2 class="text-xs font-semibold uppercase tracking-wider mb-3" style="color: var(--text-tertiary)">
            Preferiti
          </h2>
          <div
            class="rounded-2xl overflow-hidden divide-y"
            style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm); divide-color: var(--border)"
          >
            <NuxtLink
              v-for="stop in favoriteStops"
              :key="stop.id"
              :to="`/stop/${stop.id}`"
              class="flex items-center gap-3 px-4 py-3.5 transition-colors duration-150 active:opacity-70"
            >
              <Star :size="16" :stroke-width="1.75" style="color: var(--color-primary)" class="shrink-0" />
              <span class="flex-1 text-[15px] font-medium truncate" style="color: var(--text-primary)">{{ stop.name }}</span>
              <ChevronRight :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
            </NuxtLink>
          </div>
        </section>

        <!-- Recenti -->
        <section v-if="recentStops?.length">
          <h2 class="text-xs font-semibold uppercase tracking-wider mb-3" style="color: var(--text-tertiary)">
            Recenti
          </h2>
          <div
            class="rounded-2xl overflow-hidden divide-y"
            style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm); divide-color: var(--border)"
          >
            <NuxtLink
              v-for="stop in recentStops.slice(0, 5)"
              :key="stop.id"
              :to="`/stop/${stop.id}`"
              class="flex items-center gap-3 px-4 py-3.5 transition-colors duration-150 active:opacity-70"
            >
              <Clock :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
              <span class="flex-1 text-[15px] font-medium truncate" style="color: var(--text-primary)">{{ stop.name }}</span>
              <ChevronRight :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
            </NuxtLink>
          </div>
        </section>

        <!-- Empty state se tutto è vuoto -->
        <div
          v-if="!nearbyStops?.length && !favoriteStops?.length && !recentStops?.length && !showSearch"
          class="text-center py-12"
        >
          <div
            class="w-14 h-14 rounded-2xl flex items-center justify-center mx-auto mb-4"
            style="background-color: var(--bg-elevated)"
          >
            <Search :size="24" :stroke-width="1.75" style="color: var(--text-tertiary)" />
          </div>
          <p class="text-[15px] font-medium mb-1" style="color: var(--text-primary)">Cerca una fermata</p>
          <p class="text-sm" style="color: var(--text-secondary)">Usa la barra di ricerca qui sopra</p>
        </div>

      </div>
    </div>
  </AppLayout>
</template>
```

Aggiungi gli import Lucide nel `<script setup>`:
```typescript
import { Bus, Search, X, MapPin, Navigation, Star, Clock, ChevronRight } from 'lucide-vue-next'
```

- [ ] **Step 8.3: Commit**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/web && git add pages/index.vue && git commit -m "feat(web): home page redesign - hero, search, nearby/favorites/recents"
```

---

### Task 9: lines/index.vue — redesign lista linee

**Files:**
- Modify: `web/pages/lines/index.vue`

- [ ] **Step 9.1: Leggi il file corrente**

Leggi `web/pages/lines/index.vue` — mantieni tutta la logica, riscrivi il template.

- [ ] **Step 9.2: Riscrivi il template**

```html
<template>
  <AppLayout>
    <div class="max-w-lg mx-auto lg:max-w-2xl">
      <PageHeader title="Linee" />

      <div class="px-4 space-y-4 pb-8">
        <!-- Search -->
        <div
          class="flex items-center gap-3 px-4 rounded-2xl h-12"
          style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm); border: 1px solid var(--border)"
        >
          <Search :size="18" :stroke-width="1.75" style="color: var(--text-tertiary)" />
          <input
            v-model="query"
            type="search"
            placeholder="Cerca linee..."
            class="flex-1 bg-transparent text-[15px] outline-none"
            style="color: var(--text-primary); font-family: var(--font-sans)"
          />
          <button v-if="query" @click="query = ''" aria-label="Cancella">
            <X :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" />
          </button>
        </div>

        <!-- Filter chips -->
        <div class="flex gap-2 overflow-x-auto pb-1 scrollbar-hide -mx-4 px-4">
          <button
            v-for="type in availableTypes"
            :key="type.id"
            class="flex items-center gap-1.5 px-3.5 py-2 rounded-full text-sm font-medium whitespace-nowrap shrink-0 transition-all duration-200"
            :style="selectedType === type.id
              ? `background-color: var(--color-primary); color: var(--color-text-on-primary)`
              : `background-color: var(--bg-elevated); color: var(--text-secondary); border: 1px solid var(--border)`"
            @click="selectedType = selectedType === type.id ? null : type.id"
          >
            <component :is="type.icon" :size="15" :stroke-width="1.75" />
            {{ type.label }}
          </button>
        </div>

        <!-- Lines list -->
        <div
          v-if="filteredRoutes.length > 0"
          class="rounded-2xl overflow-hidden divide-y"
          style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm); divide-color: var(--border)"
        >
          <NuxtLink
            v-for="route in filteredRoutes"
            :key="route.routeId"
            :to="`/lines/${route.routeId}`"
            class="flex items-center gap-3 px-4 py-3.5 transition-colors duration-150 active:opacity-70"
          >
            <LineBadge :name="route.shortName" :color="route.color" :text-color="route.textColor" />
            <div class="flex-1 min-w-0">
              <p class="text-[15px] font-medium truncate" style="color: var(--text-primary)">{{ route.longName }}</p>
              <p v-if="route.headsigns?.[0]" class="text-xs truncate" style="color: var(--text-secondary)">
                Direzione {{ route.headsigns[0] }}
              </p>
            </div>
            <ChevronRight :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
          </NuxtLink>
        </div>

        <!-- Empty state -->
        <div v-else class="text-center py-12">
          <div
            class="w-14 h-14 rounded-2xl flex items-center justify-center mx-auto mb-4"
            style="background-color: var(--bg-elevated)"
          >
            <Route :size="24" :stroke-width="1.75" style="color: var(--text-tertiary)" />
          </div>
          <p class="text-[15px] font-medium mb-1" style="color: var(--text-primary)">Nessuna linea trovata</p>
          <p class="text-sm" style="color: var(--text-secondary)">Prova a modificare la ricerca</p>
        </div>
      </div>
    </div>
  </AppLayout>
</template>
```

Aggiungi import Lucide:
```typescript
import { Search, X, Bus, TramFront, TrainFront, Ship, ChevronRight, Route } from 'lucide-vue-next'

const TRANSIT_TYPE_OPTIONS = [
  { id: 'bus', label: 'Bus', icon: Bus },
  { id: 'tram', label: 'Tram', icon: TramFront },
  { id: 'rail', label: 'Treno', icon: TrainFront },
  { id: 'ferry', label: 'Nave', icon: Ship },
]
```

- [ ] **Step 9.3: Commit**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/web && git add pages/lines/index.vue && git commit -m "feat(web): lines list redesign - Lucide chips, refined list, no emoji"
```

---

### Task 10: lines/[lineId].vue — redesign dettaglio linea

**Files:**
- Modify: `web/pages/lines/[lineId].vue`

- [ ] **Step 10.1: Leggi il file corrente**

Leggi `web/pages/lines/[lineId].vue` — mantieni la logica, riscrivi il template.

- [ ] **Step 10.2: Riscrivi il template**

```html
<template>
  <AppLayout>
    <div class="max-w-lg mx-auto lg:max-w-2xl">
      <PageHeader
        :title="route?.shortName ?? '...'"
        back-to="/lines"
        back-label="Linee"
      >
        <template #action>
          <button
            v-if="shareUrl"
            @click="share"
            :aria-label="'Condividi fermata'"
            class="w-9 h-9 rounded-xl flex items-center justify-center transition-colors duration-150 active:opacity-70"
            style="color: var(--text-secondary)"
          >
            <Share2 :size="20" :stroke-width="1.75" />
          </button>
        </template>
      </PageHeader>

      <div class="px-4 pb-8 space-y-5">

        <!-- Line info header -->
        <div class="flex items-center gap-3 pt-2">
          <LineBadge
            :name="route?.shortName ?? ''"
            :color="route?.color"
            :text-color="route?.textColor"
            class="text-base px-3 py-1"
          />
          <div class="flex-1 min-w-0">
            <p class="text-[15px] font-semibold truncate" style="color: var(--text-primary)">{{ route?.longName }}</p>
          </div>
        </div>

        <!-- Direction switcher -->
        <div
          v-if="directions.length > 1"
          class="flex p-1 rounded-xl gap-1"
          style="background-color: var(--bg-elevated); border: 1px solid var(--border)"
        >
          <button
            v-for="dir in directions"
            :key="dir.id"
            class="flex-1 py-2 rounded-lg text-sm font-medium transition-all duration-200 truncate px-3"
            :style="selectedDirection === dir.id
              ? `background-color: var(--color-primary); color: var(--color-text-on-primary); box-shadow: var(--shadow-sm)`
              : `color: var(--text-secondary)`"
            @click="selectedDirection = dir.id"
          >
            {{ dir.label }}
          </button>
        </div>

        <!-- Stop list — timeline style -->
        <div v-if="stops.length > 0" class="relative">
          <!-- Vertical connector line -->
          <div
            class="absolute left-[27px] top-5 bottom-5 w-[2px]"
            style="background-color: var(--border)"
            aria-hidden="true"
          />
          <div class="space-y-0">
            <NuxtLink
              v-for="(stop, index) in stops"
              :key="stop.id"
              :to="`/stop/${stop.id}`"
              class="flex items-center gap-3 py-3 rounded-xl px-2 transition-colors duration-150 active:opacity-70 relative"
            >
              <!-- Timeline dot -->
              <div
                class="w-5 h-5 rounded-full border-2 bg-white dark:bg-[#1C1C24] shrink-0 z-10"
                :style="`border-color: ${index === 0 || index === stops.length - 1 ? 'var(--color-primary)' : 'var(--border)'}`"
                aria-hidden="true"
              />
              <span
                class="flex-1 text-[15px] truncate"
                :class="index === 0 || index === stops.length - 1 ? 'font-semibold' : 'font-medium'"
                style="color: var(--text-primary)"
              >
                {{ stop.name }}
              </span>
              <ChevronRight :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" class="shrink-0" />
            </NuxtLink>
          </div>
        </div>

        <!-- Skeleton loader -->
        <div v-else class="space-y-3 animate-pulse">
          <div
            v-for="i in 8"
            :key="i"
            class="h-12 rounded-xl"
            style="background-color: var(--bg-elevated)"
          />
        </div>

      </div>
    </div>
  </AppLayout>
</template>
```

Aggiungi import:
```typescript
import { Share2, ChevronRight } from 'lucide-vue-next'
```

- [ ] **Step 10.3: Commit**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/web && git add pages/lines/[lineId].vue && git commit -m "feat(web): line detail redesign - timeline stops, direction pill switcher"
```

---

### Task 11: stop/[stopId].vue — redesign pagina fermata (più importante)

**Files:**
- Modify: `web/pages/stop/[stopId].vue`

- [ ] **Step 11.1: Leggi il file corrente**

Leggi `web/pages/stop/[stopId].vue` — mantieni TUTTA la logica Vue (useRealtime, orari, composables), riscrivi solo il template.

- [ ] **Step 11.2: Riscrivi il template — sezione "Prossime partenze"**

```html
<template>
  <AppLayout>
    <div class="max-w-lg mx-auto lg:max-w-2xl">

      <!-- Navigation header -->
      <PageHeader
        :title="stop?.name ?? '...'"
        back-to="/"
        back-label="Home"
      >
        <template #action>
          <div class="flex items-center gap-1">
            <!-- Favorite button -->
            <button
              v-if="features?.enableFavorites"
              @click="toggleFavorite"
              class="w-9 h-9 rounded-xl flex items-center justify-center transition-colors duration-150 active:opacity-70"
              :style="isFavorite ? `color: var(--color-primary)` : `color: var(--text-secondary)`"
              :aria-label="isFavorite ? 'Rimuovi dai preferiti' : 'Aggiungi ai preferiti'"
              :aria-pressed="isFavorite"
            >
              <Star :size="20" :stroke-width="1.75" :fill="isFavorite ? 'currentColor' : 'none'" />
            </button>
            <!-- Share button -->
            <button
              @click="share"
              class="w-9 h-9 rounded-xl flex items-center justify-center transition-colors duration-150 active:opacity-70"
              style="color: var(--text-secondary)"
              aria-label="Condividi fermata"
            >
              <Share2 :size="20" :stroke-width="1.75" />
            </button>
          </div>
        </template>
      </PageHeader>

      <div class="px-4 pb-8 space-y-5">

        <!-- Stop name + meta -->
        <div class="pt-1">
          <h2 class="text-xl font-bold leading-tight" style="color: var(--text-primary)">{{ stop?.name }}</h2>
          <p v-if="stop?.description" class="text-sm mt-0.5" style="color: var(--text-secondary)">{{ stop.description }}</p>
        </div>

        <!-- Prossime partenze -->
        <section>
          <div class="flex items-center justify-between mb-3">
            <h3 class="text-xs font-semibold uppercase tracking-wider" style="color: var(--text-tertiary)">
              Prossime partenze
            </h3>
            <button
              @click="refreshRealtime"
              :aria-label="'Aggiorna'"
              class="flex items-center gap-1.5 text-xs font-medium px-2.5 py-1.5 rounded-lg transition-colors duration-150 active:opacity-70"
              style="color: var(--color-primary)"
              :disabled="isRefreshing"
            >
              <RefreshCw :size="14" :stroke-width="1.75" :class="{ 'animate-spin': isRefreshing }" />
              Aggiorna
            </button>
          </div>

          <!-- Departures list -->
          <div
            v-if="upcomingDepartures.length > 0"
            class="rounded-2xl overflow-hidden divide-y"
            style="background-color: var(--bg-elevated); box-shadow: var(--shadow-md); divide-color: var(--border)"
          >
            <DepartureRow
              v-for="dep in upcomingDepartures"
              :key="`${dep.tripId}-${dep.scheduledTime}`"
              :departure="dep"
              :show-countdown="true"
            />
          </div>

          <!-- Loading skeleton -->
          <div
            v-else-if="isLoading"
            class="rounded-2xl overflow-hidden divide-y animate-pulse"
            style="background-color: var(--bg-elevated); divide-color: var(--border)"
          >
            <div v-for="i in 4" :key="i" class="flex items-center gap-3 px-4 py-3.5">
              <div class="w-10 h-6 rounded" style="background-color: var(--bg-secondary)" />
              <div class="flex-1 h-4 rounded" style="background-color: var(--bg-secondary)" />
              <div class="w-12 h-4 rounded" style="background-color: var(--bg-secondary)" />
            </div>
          </div>

          <!-- Empty state -->
          <div
            v-else
            class="rounded-2xl px-4 py-10 text-center"
            style="background-color: var(--bg-elevated)"
          >
            <AlertCircle :size="28" :stroke-width="1.5" style="color: var(--text-tertiary)" class="mx-auto mb-3" />
            <p class="text-[15px] font-medium mb-1" style="color: var(--text-primary)">Nessuna partenza</p>
            <p class="text-sm" style="color: var(--text-secondary)">Non ci sono partenze previste per ora</p>
          </div>
        </section>

        <!-- Orario -->
        <section v-if="dayGroups.length > 0">
          <h3 class="text-xs font-semibold uppercase tracking-wider mb-3" style="color: var(--text-tertiary)">
            Orario
          </h3>

          <DayGroupTabs
            :groups="dayGroups"
            v-model:selectedId="selectedDayGroup"
            class="mb-4"
          />

          <div
            v-if="scheduledForGroup.length > 0"
            class="rounded-2xl overflow-hidden divide-y"
            style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm); divide-color: var(--border)"
            :id="`tabpanel-${selectedDayGroup}`"
            role="tabpanel"
          >
            <DepartureRow
              v-for="dep in scheduledForGroup"
              :key="`sched-${dep.tripId}-${dep.scheduledTime}`"
              :departure="dep"
              :show-countdown="false"
              :is-past="dep.isPast"
            />
          </div>
        </section>

      </div>
    </div>
  </AppLayout>
</template>
```

Aggiungi import:
```typescript
import { Star, Share2, RefreshCw, AlertCircle } from 'lucide-vue-next'
```

- [ ] **Step 11.3: Commit**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/web && git add pages/stop/[stopId].vue && git commit -m "feat(web): stop detail redesign - departures, schedule tabs, no emoji"
```

---

### Task 12: pages/[...slug].vue — rimozione emoji

**Files:**
- Modify: `web/pages/[...slug].vue`

- [ ] **Step 12.1: Leggi e aggiorna**

Leggi `web/pages/[...slug].vue`, sostituisci qualsiasi emoji con icone Lucide appropriate (es. `Bus` per 🚌, `AlertCircle` per errori). Wrappa con `<AppLayout>`.

- [ ] **Step 12.2: Commit**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/web && git add pages/[...slug].vue && git commit -m "feat(web): 404 page - Lucide icon, AppLayout wrapper"
```

---

## FASE 5 — UX Review con Playwright

### Task 13: Avvio dev server e screenshot iniziale

- [ ] **Step 13.1: Avvia il dev server**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/web && npm run dev &
```

Attendi che il server sia su `http://localhost:3000`.

- [ ] **Step 13.2: Prendi screenshot di tutte le pagine con Playwright**

Usa il tool `mcp__plugin_playwright_playwright__browser_navigate` + `browser_take_screenshot` per:
- `http://localhost:3000/` (Home)
- `http://localhost:3000/lines` (Lista linee)
- `http://localhost:3000/lines/[primo lineId disponibile]` (Dettaglio linea)
- `http://localhost:3000/stop/[primo stopId disponibile]` (Dettaglio fermata)
- `http://localhost:3000/settings` (Impostazioni)

Ogni screenshot va consegnato all'agente UX reviewer.

### Task 14: UX Review da agente senior designer

Dopo ogni batch di screenshot, un agente UX reviewer senior esegue una review spietata:
- Coerenza visiva con il design system
- Zero emoji rimaste
- Leggibilità su mobile
- Qualità tipografica (Plus Jakarta Sans caricato correttamente)
- Dark mode funzionante
- Bottom tab bar visible e funzionante
- Spacing e allineamenti
- Lucide icons stroke width 1.75

Ogni problema viene categorizzato come CRITICAL / MAJOR / MINOR.

---

## Note importanti per gli agenti

1. **Leggere sempre i file prima di modificarli** — usare Read tool
2. **Mantenere tutta la logica Vue** — composables, computed, watch — modificare SOLO il template e aggiungere import Lucide
3. **Zero emoji** — grep per emoji in ogni file modificato prima di committare
4. **CSS variables** — usare sempre `var(--bg-primary)` etc via `style=""` inline o classi Tailwind definite in tailwind.config.ts
5. **AppLayout** — ogni pagina deve essere wrappata con `<AppLayout>`
6. **Lucide stroke-width sempre 1.75** — non usare il default 2
7. **Testing con Playwright** — dopo ogni fase, screenshot del browser reale

```bash
# Comando per verificare emoji rimaste
grep -r "[\x{1F300}-\x{1F9FF}]" /Users/andreatoffanello/GitHub/transit-engine/web/pages /Users/andreatoffanello/GitHub/transit-engine/web/components --include="*.vue" -l
```
