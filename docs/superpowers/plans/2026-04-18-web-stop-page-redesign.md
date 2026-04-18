# Web Stop Page Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the web stop page to be visually consistent with the iOS/Android apps — map header, iOS-style departure rows, line filter chips, app download banner.

**Architecture:** Single scroll page (remove tab switcher), MapLibre GL JS map header (OpenFreeMap tiles, no API key), `AppDownloadBanner` fixed-top Teleport, `StopMapHeader` client-only component, `DepartureRow` redesigned to match iOS (transit icon rounded square + LineBadge + headsign + time).

**Tech Stack:** Vue 3 / Nuxt 3, MapLibre GL JS, OpenFreeMap vector tiles (`https://tiles.openfreemap.org/styles/bright`), Tailwind CSS, Lucide Vue Next icons.

---

## File Map

| Action | Path | Responsibility |
|--------|------|---------------|
| Modify | `web/types/index.ts` | Add optional `appLinks` to `OperatorConfig` |
| Modify | `shared/operators/appalcart/config.json` | Add placeholder `appLinks` |
| Create | `web/components/AppDownloadBanner.vue` | Fixed-top banner "Scarica app", dismissable |
| Create | `web/components/StopMapHeader.vue` | MapLibre GL map header, client-only |
| Modify | `web/components/DepartureRow.vue` | iOS-style row: transit icon + badge + headsign + time |
| Modify | `web/pages/stop/[stopId].vue` | Full page redesign: map header, iOS layout, filter chips |
| Modify | `web/components/AppLayout.vue` | Add `AppDownloadBanner`, compensate top padding |

---

## Task 1: Add `appLinks` type + install MapLibre

**Files:**
- Modify: `web/types/index.ts`
- Modify: `shared/operators/appalcart/config.json`
- Modify: `web/package.json` (via npm install)

- [ ] **Step 1: Add `appLinks` to `OperatorConfig`**

In `web/types/index.ts`, after the `StoreConfig` interface, add:
```ts
export interface AppLinks {
  ios?: string   // App Store URL
  android?: string  // Play Store URL
}
```
And add `appLinks?: AppLinks` to `OperatorConfig`.

- [ ] **Step 2: Add placeholder appLinks to appalcart config**

In `shared/operators/appalcart/config.json`, add after `"store"`:
```json
"appLinks": {
  "ios": "https://apps.apple.com/app/id000000000",
  "android": "https://play.google.com/store/apps/details?id=com.transitkit.appalcart"
}
```

- [ ] **Step 3: Install MapLibre GL**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/web && npm install maplibre-gl
```
Expected: maplibre-gl added to dependencies.

- [ ] **Step 4: Commit**

```bash
git add web/types/index.ts shared/operators/appalcart/config.json web/package.json web/package-lock.json
git commit -m "feat(web): add appLinks type + install maplibre-gl"
```

---

## Task 2: Create `AppDownloadBanner.vue`

**Files:**
- Create: `web/components/AppDownloadBanner.vue`

Banner is `Teleport to="body"` fixed top, 44px tall, operator primary color background, white text. Dismissed state stored in `localStorage` under key `app-banner-dismissed`. Shows App Store + Play Store icons only when respective URLs are in config. X button on right.

- [ ] **Step 1: Create `web/components/AppDownloadBanner.vue`**

```vue
<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="fixed top-0 left-0 right-0 z-50 flex items-center justify-between px-4 gap-3"
      style="height: 44px; background-color: var(--color-primary); color: var(--color-text-on-primary)"
      role="banner"
    >
      <span class="text-sm font-semibold flex-1 truncate">{{ label }}</span>
      <div class="flex items-center gap-3 shrink-0">
        <a
          v-if="appLinks?.ios"
          :href="appLinks.ios"
          target="_blank"
          rel="noopener noreferrer"
          aria-label="App Store"
          class="opacity-90 hover:opacity-100 transition-opacity"
        >
          <!-- Apple logo SVG -->
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M18.71 19.5C17.88 20.74 17 21.95 15.66 21.97C14.32 22 13.89 21.18 12.37 21.18C10.84 21.18 10.37 21.95 9.1 22C7.78 22.05 6.8 20.68 5.96 19.47C4.25 17 2.94 12.45 4.7 9.39C5.57 7.87 7.13 6.91 8.82 6.88C10.1 6.86 11.32 7.75 12.11 7.75C12.89 7.75 14.37 6.68 15.92 6.84C16.57 6.87 18.39 7.1 19.56 8.82C19.47 8.88 17.39 10.1 17.41 12.63C17.44 15.65 20.06 16.66 20.09 16.67C20.06 16.74 19.67 18.11 18.71 19.5ZM13 3.5C13.73 2.67 14.94 2.04 15.94 2C16.07 3.17 15.6 4.35 14.9 5.19C14.21 6.04 13.07 6.7 11.95 6.61C11.8 5.46 12.36 4.26 13 3.5Z"/>
          </svg>
        </a>
        <a
          v-if="appLinks?.android"
          :href="appLinks.android"
          target="_blank"
          rel="noopener noreferrer"
          aria-label="Google Play"
          class="opacity-90 hover:opacity-100 transition-opacity"
        >
          <!-- Play Store triangle SVG -->
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M3 20.5V3.5C3 2.91 3.34 2.39 3.84 2.15L13.69 12L3.84 21.85C3.34 21.6 3 21.09 3 20.5ZM16.81 15.12L6.05 21.34L14.54 12.85L16.81 15.12ZM20.16 10.81C20.5 11.08 20.75 11.5 20.75 12C20.75 12.5 20.53 12.9 20.18 13.18L17.89 14.5L15.39 12L17.89 9.5L20.16 10.81ZM6.05 2.66L16.81 8.88L14.54 11.15L6.05 2.66Z"/>
          </svg>
        </a>
        <button
          type="button"
          aria-label="Chiudi"
          class="p-1 opacity-80 hover:opacity-100 transition-opacity"
          @click="dismiss"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import type { AppLinks } from '~/types'

const props = defineProps<{
  appLinks?: AppLinks
  operatorName?: string
}>()

const STORAGE_KEY = 'app-banner-dismissed'
const visible = ref(false)

const label = computed(() => {
  const name = props.operatorName
  return name ? `Scarica ${name}` : 'Scarica app'
})

onMounted(() => {
  if (typeof localStorage !== 'undefined') {
    visible.value = localStorage.getItem(STORAGE_KEY) !== '1'
  }
})

function dismiss() {
  visible.value = false
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem(STORAGE_KEY, '1')
  }
}
</script>
```

- [ ] **Step 2: Commit**

```bash
git add web/components/AppDownloadBanner.vue
git commit -m "feat(web): add AppDownloadBanner component"
```

---

## Task 3: Create `StopMapHeader.vue`

**Files:**
- Create: `web/components/StopMapHeader.vue`

Client-only component (no SSR). Uses MapLibre GL JS with OpenFreeMap bright style. Shows stop annotation dot. Expand button opens native maps. Height 220px.

- [ ] **Step 1: Create `web/components/StopMapHeader.vue`**

```vue
<template>
  <div class="relative w-full overflow-hidden" style="height: 220px">
    <ClientOnly>
      <div ref="mapContainer" class="absolute inset-0" />
      <!-- Expand to native maps -->
      <a
        v-if="lat && lng"
        :href="`https://maps.apple.com/?q=${lat},${lng}`"
        target="_blank"
        rel="noopener noreferrer"
        aria-label="Apri in mappe"
        class="absolute bottom-3 right-3 flex items-center justify-center rounded-xl"
        style="width: 36px; height: 36px; background: rgba(255,255,255,0.85); backdrop-filter: blur(8px); box-shadow: 0 1px 4px rgba(0,0,0,0.18)"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#333" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <polyline points="15 3 21 3 21 9"/><polyline points="9 21 3 21 3 15"/>
          <line x1="21" y1="3" x2="14" y2="10"/><line x1="3" y1="21" x2="10" y2="14"/>
        </svg>
      </a>
      <template #fallback>
        <div class="absolute inset-0" style="background: var(--bg-secondary)" />
      </template>
    </ClientOnly>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  lat: number
  lng: number
  primaryColor?: string
}>()

const mapContainer = ref<HTMLElement | null>(null)
let mapInstance: unknown = null

onMounted(async () => {
  if (!mapContainer.value) return
  const [{ default: maplibregl }, _css] = await Promise.all([
    import('maplibre-gl'),
    import('maplibre-gl/dist/maplibre-gl.css'),
  ])

  mapInstance = new maplibregl.Map({
    container: mapContainer.value,
    style: 'https://tiles.openfreemap.org/styles/bright',
    center: [props.lng, props.lat],
    zoom: 15.5,
    pitch: 0,
    bearing: 0,
    attributionControl: false,
    interactive: false,
  })

  const map = mapInstance as InstanceType<typeof maplibregl.Map>

  map.on('load', () => {
    // Stop marker
    const el = document.createElement('div')
    el.style.cssText = `
      width: 28px; height: 28px; border-radius: 50%;
      background: ${props.primaryColor ?? '#165F9C'};
      border: 3px solid white;
      box-shadow: 0 2px 8px rgba(0,0,0,0.3);
    `
    new maplibregl.Marker({ element: el })
      .setLngLat([props.lng, props.lat])
      .addTo(map)
  })
})

onUnmounted(() => {
  if (mapInstance) {
    (mapInstance as InstanceType<typeof import('maplibre-gl')['default']['Map']>).remove()
    mapInstance = null
  }
})
</script>
```

- [ ] **Step 2: Commit**

```bash
git add web/components/StopMapHeader.vue
git commit -m "feat(web): add StopMapHeader with MapLibre GL + OpenFreeMap"
```

---

## Task 4: Redesign `DepartureRow.vue`

**Files:**
- Modify: `web/components/DepartureRow.vue`

New layout matches iOS: `[transit icon square][LineBadge][headsign flex-1][time / countdown]`. "Prossima" badge is handled by the parent, not this component. Remove border-left highlight. Keep all existing countdown/delay logic.

Transit type → Lucide icon mapping:
- `bus` → `Bus`
- `tram` → `Tram`
- `metro` / `rail` → `Train`  
- `ferry` → `Ship`
- default → `Bus`

- [ ] **Step 1: Rewrite `web/components/DepartureRow.vue`**

```vue
<template>
  <div
    class="flex items-center gap-3 py-3 px-4 relative transition-colors duration-150"
    :class="{ 'opacity-40': isPast }"
    :aria-label="rowAriaLabel"
  >
    <!-- Transit type icon square -->
    <div
      class="w-8 h-8 rounded-lg flex items-center justify-center shrink-0"
      :style="{ backgroundColor: bgColor }"
      aria-hidden="true"
    >
      <component :is="transitIcon" :size="16" color="white" :stroke-width="2" />
    </div>

    <!-- Line badge -->
    <LineBadge
      :name="departure.lineName"
      :color="departure.color"
      :text-color="departure.textColor"
      :locale="locale"
    />

    <!-- Headsign -->
    <span
      class="flex-1 text-[15px] font-medium truncate"
      style="color: var(--text-primary)"
    >
      {{ departure.headsign }}
      <span
        v-if="departure.dock"
        class="text-xs ml-1"
        style="color: var(--text-tertiary)"
      >{{ s.dockPrefix }}{{ departure.dock }}</span>
    </span>

    <!-- Time / countdown -->
    <div class="flex items-center gap-1.5 shrink-0">
      <span
        v-if="departure.isRealtime"
        class="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse shrink-0"
        :aria-label="s.ariaRealtimeData"
        role="img"
      />
      <div class="text-right">
        <template v-if="hasDelay">
          <span class="block text-[12px] line-through" style="color: var(--text-tertiary); font-variant-numeric: tabular-nums">{{ departure.time }}</span>
          <span class="block text-[15px] font-semibold text-orange-500" style="font-variant-numeric: tabular-nums">{{ realtimeTime }}</span>
        </template>
        <template v-else>
          <span
            v-if="showCountdown && typeof effectiveMinutes === 'number' && effectiveMinutes >= 0 && effectiveMinutes <= 1"
            class="inline-flex items-center justify-center h-6 px-2.5 rounded-md text-xs font-bold"
            style="background-color: #16a34a; color: #fff"
          >{{ s.now }}</span>
          <span
            v-else-if="showCountdown && typeof effectiveMinutes === 'number' && effectiveMinutes > 1 && effectiveMinutes <= 30"
            class="text-[17px] font-bold leading-none"
            style="color: var(--color-primary); font-variant-numeric: tabular-nums; letter-spacing: -0.03em"
          >{{ effectiveMinutes }}<span class="text-[11px] font-medium ml-0.5">min</span></span>
          <span
            v-else
            class="text-[15px] font-semibold"
            style="font-variant-numeric: tabular-nums; letter-spacing: -0.02em; color: var(--text-primary)"
          >{{ departure.time }}</span>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Bus, Tram, Train, Ship } from 'lucide-vue-next'
import type { Departure } from '~/types'
import { getStrings } from '~/utils/strings'
import { normalizeHex } from '~/utils/color'

const props = defineProps<{
  departure: Departure
  now?: number
  locale?: string
  showCountdown?: boolean
  isPast?: boolean
  isNext?: boolean
}>()

const s = computed(() => getStrings(props.locale))

const transitIcon = computed(() => {
  switch (props.departure.transitType) {
    case 'tram': return Tram
    case 'metro':
    case 'rail':
    case 'monorail': return Train
    case 'ferry': return Ship
    default: return Bus
  }
})

// Use line color for icon background, fallback to primary
const bgColor = computed(() =>
  props.departure.color ? normalizeHex(props.departure.color) : 'var(--color-primary)'
)

const delayMinutes = computed(() =>
  props.departure.realtimeDelay !== undefined
    ? Math.round(props.departure.realtimeDelay / 60)
    : 0
)
const hasDelay = computed(() => delayMinutes.value > 0)

const realtimeTime = computed(() => {
  if (!hasDelay.value) return props.departure.time
  const [h = 0, m = 0] = props.departure.time.split(':').map(Number)
  const totalMin = h * 60 + m + delayMinutes.value
  const rh = Math.floor(totalMin / 60) % 24
  const rm = totalMin % 60
  return `${String(rh).padStart(2, '0')}:${String(rm).padStart(2, '0')}`
})

function computeEffectiveMinutes(nowMs: number): number {
  const midnight = new Date(nowMs)
  midnight.setHours(0, 0, 0, 0)
  const nowMin = Math.floor((nowMs - midnight.getTime()) / 60_000)
  let diffMin = props.departure.minutesFromMidnight - nowMin
  if (props.departure.realtimeDelay !== undefined) diffMin += delayMinutes.value
  return diffMin
}

const effectiveMinutes = computed(() => computeEffectiveMinutes(props.now ?? Date.now()))

const rowAriaLabel = computed(() => {
  const diffMin = effectiveMinutes.value
  let timeDescription: string
  if (diffMin === 0) timeDescription = s.value.now
  else if (diffMin > 0 && diffMin < 60) timeDescription = `${diffMin} ${s.value.minutes}`
  else timeDescription = props.departure.time
  return `${s.value.lineLabel} ${props.departure.lineName}, ${props.departure.headsign}, ${timeDescription}`
})
</script>
```

- [ ] **Step 2: Commit**

```bash
git add web/components/DepartureRow.vue
git commit -m "feat(web): redesign DepartureRow to match iOS (transit icon + badge + time)"
```

---

## Task 5: Redesign `stop/[stopId].vue`

**Files:**
- Modify: `web/pages/stop/[stopId].vue`

New structure (top to bottom):
1. `StopMapHeader` (full width, 220px, no PageHeader above it)
2. Stop identity row: stop name H1 bold + bookmark icon
3. Line badges horizontal scroll (from `servingRoutes`)
4. Segmented control: Prossime / Orario (iOS pill style — gray bg, white active, no primary color)
5. **Prossime section**: line filter chips (Tutti + per linea, primary color active), departure rows (dividers, no card shadow), "prossima" badge on first, "Nessuna partenza" empty state
6. **Orario section**: DayGroupTabs + DepartureRow list
7. Footer compact: Google Maps text link + share icon (no pill buttons)

The `AppDownloadBanner` is wired up in `AppLayout` (Task 6), not here.

- [ ] **Step 1: Rewrite `web/pages/stop/[stopId].vue`**

Full replacement — see implementation in code below. Keep all existing `<script setup>` logic (composables, computed values). Only replace the `<template>`.

```vue
<template>
  <AppLayout>
    <div class="max-w-lg mx-auto lg:max-w-2xl">
      <!-- Map header (client-only, no SSR) -->
      <template v-if="!pending && stop">
        <StopMapHeader
          :lat="stop.lat"
          :lng="stop.lng"
          :primary-color="config?.theme?.primaryColor"
        />
      </template>
      <div v-else-if="pending" class="w-full skeleton-shimmer" style="height: 220px" />

      <!-- Stop identity -->
      <div class="px-4 pt-4 pb-3">
        <template v-if="pending">
          <div class="h-7 rounded-lg w-3/4 skeleton-shimmer mb-2" />
          <div class="flex gap-2">
            <div class="h-6 w-12 rounded skeleton-shimmer" />
            <div class="h-6 w-12 rounded skeleton-shimmer" />
          </div>
        </template>
        <template v-else-if="stop">
          <div class="flex items-start justify-between gap-2">
            <h1 class="text-[22px] font-bold leading-tight" style="color: var(--text-primary)">
              {{ stop.name }}
            </h1>
            <div class="flex items-center gap-0.5 mt-0.5 shrink-0">
              <button
                v-if="config?.features?.enableFavorites"
                type="button"
                :aria-label="isFavorite(stopId) ? s.removeFromFavorites : s.addToFavorites"
                class="p-2 rounded-lg transition-opacity active:opacity-60"
                :style="{ color: isFavorite(stopId) ? 'var(--color-primary)' : 'var(--text-tertiary)' }"
                @click="toggleFavorite({ stopId: stopId, name: stop.name })"
              >
                <Bookmark :size="20" :stroke-width="1.75" :fill="isFavorite(stopId) ? 'currentColor' : 'none'" />
              </button>
              <button
                v-if="canShare"
                type="button"
                :aria-label="s.shareStop"
                class="p-2 rounded-lg transition-opacity active:opacity-60"
                style="color: var(--text-tertiary)"
                @click="shareStop"
              >
                <Share2 :size="20" :stroke-width="1.75" />
              </button>
              <button
                v-else
                type="button"
                :aria-label="s.copyLink"
                class="p-2 rounded-lg transition-opacity active:opacity-60"
                style="color: var(--text-tertiary)"
                @click="copyLink"
              >
                <component :is="copied ? Check : Copy" :size="20" :stroke-width="1.75" />
              </button>
            </div>
          </div>
          <!-- Line badges scroll -->
          <div
            v-if="servingRoutes.length"
            class="flex gap-1.5 mt-2 overflow-x-auto scrollbar-none pb-0.5"
            role="list"
            :aria-label="s.servingLines ?? 'Linee'"
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
        </template>
      </div>

      <!-- Segmented control: Prossime / Orario -->
      <div
        v-if="!pending && stop"
        class="mx-4 mb-4 flex p-1 rounded-xl gap-1"
        style="background-color: rgba(120,120,128,0.12)"
      >
        <button
          class="flex-1 py-1.5 px-3 rounded-[10px] text-sm font-semibold transition-all duration-150 active:opacity-70"
          :style="activeTab === 'prossime'
            ? { backgroundColor: 'var(--bg-elevated)', color: 'var(--text-primary)', boxShadow: '0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.08)' }
            : { color: 'var(--text-secondary)' }"
          @click="activeTab = 'prossime'"
        >
          Prossime
        </button>
        <button
          class="flex-1 py-1.5 px-3 rounded-[10px] text-sm font-semibold transition-all duration-150 active:opacity-70 flex items-center justify-center gap-1.5"
          :style="activeTab === 'orario'
            ? { backgroundColor: 'var(--bg-elevated)', color: 'var(--text-primary)', boxShadow: '0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.08)' }
            : { color: 'var(--text-secondary)' }"
          @click="activeTab = 'orario'"
        >
          Orario
          <span v-if="isLive" class="w-1.5 h-1.5 rounded-full bg-green-500 shrink-0" aria-hidden="true" />
        </button>
      </div>

      <template v-if="!pending && stop">
        <!-- ── Prossime partenze ── -->
        <section v-show="activeTab === 'prossime'" class="px-4 mb-6" aria-labelledby="section-adesso">
          <!-- Section header -->
          <div class="flex items-center justify-between mb-3">
            <h2 id="section-adesso" class="text-[17px] font-bold" style="color: var(--text-primary)">
              Prossime partenze
              <span
                v-if="isLive"
                class="inline-block w-2 h-2 rounded-full bg-green-500 animate-pulse ml-1.5 align-middle"
                aria-hidden="true"
              />
            </h2>
            <button
              type="button"
              aria-label="Aggiorna"
              class="flex items-center gap-1 text-xs font-semibold px-2.5 py-1.5 rounded-lg transition-opacity active:opacity-60"
              style="color: var(--color-primary); background-color: color-mix(in srgb, var(--color-primary) 10%, transparent)"
              @click="refreshRealtime"
            >
              <RefreshCw :size="13" :stroke-width="1.75" :class="{ 'animate-spin': realtimeLoading }" />
              Aggiorna
            </button>
          </div>

          <!-- Line filter chips -->
          <div
            v-if="servingRoutes.length > 1"
            class="flex gap-1.5 mb-3 overflow-x-auto scrollbar-none pb-0.5"
            role="group"
            aria-label="Filtra per linea"
          >
            <button
              class="shrink-0 h-7 px-3 rounded-full text-xs font-semibold transition-all duration-150 active:opacity-70"
              :style="filterLine === null
                ? { backgroundColor: 'var(--color-primary)', color: 'var(--color-text-on-primary)' }
                : { backgroundColor: 'var(--bg-elevated)', color: 'var(--text-secondary)', border: '1px solid var(--border)' }"
              @click="filterLine = null"
            >
              Tutti
            </button>
            <button
              v-for="r in servingRoutes"
              :key="r.id"
              class="shrink-0 h-7 px-3 rounded-full text-xs font-bold transition-all duration-150 active:opacity-70"
              :style="filterLine === r.name
                ? { backgroundColor: r.color || 'var(--color-primary)', color: r.textColor || 'var(--color-text-on-primary)' }
                : { backgroundColor: 'var(--bg-elevated)', color: 'var(--text-secondary)', border: '1px solid var(--border)' }"
              @click="filterLine = r.name"
            >
              {{ r.name }}
            </button>
          </div>

          <!-- Realtime updated at -->
          <span v-if="isLive && realtimeLastUpdated" role="status" aria-live="polite" class="block text-xs mb-2 tabular-nums" style="color: var(--text-tertiary)">
            {{ s.updatedAt }} {{ realtimeLastUpdated }}
          </span>

          <!-- Departures list -->
          <div
            v-if="filteredUpcomingDepartures.length"
            class="overflow-hidden divide-y"
            style="background-color: var(--bg-elevated); border-radius: 16px; border: 1px solid var(--border)"
          >
            <!-- "prossima" badge on first row -->
            <div class="relative">
              <span
                class="absolute top-2 left-3 text-[10px] font-bold px-1.5 py-0.5 rounded"
                style="background-color: #16a34a; color: #fff; line-height: 1; z-index: 1"
                aria-hidden="true"
              >prossima</span>
              <DepartureRow
                :departure="filteredUpcomingDepartures[0]!"
                :now="now"
                :locale="config?.locale[0]"
                :show-countdown="true"
                :is-next="true"
                class="pt-7"
              />
            </div>
            <DepartureRow
              v-for="dep in filteredUpcomingDepartures.slice(1, showAllUpcoming ? undefined : 5)"
              :key="dep.id"
              :departure="dep"
              :now="now"
              :locale="config?.locale[0]"
              :show-countdown="true"
            />
            <!-- Show more link -->
            <button
              v-if="!showAllUpcoming && filteredUpcomingDepartures.length > 5"
              type="button"
              class="w-full py-3 text-sm font-semibold flex items-center justify-center gap-1 transition-opacity active:opacity-60"
              style="color: var(--color-primary)"
              @click="showAllUpcoming = true"
            >
              <ChevronDown :size="15" :stroke-width="2" />
              Mostra altri {{ filteredUpcomingDepartures.length - 5 }}
            </button>
          </div>

          <!-- Empty state -->
          <div
            v-else
            class="rounded-2xl px-5 py-8 text-center"
            style="background-color: var(--bg-elevated); border: 1px solid var(--border)"
          >
            <div
              class="w-12 h-12 rounded-2xl flex items-center justify-center mx-auto mb-3"
              style="background-color: color-mix(in srgb, var(--color-primary) 8%, var(--bg-secondary))"
            >
              <Clock :size="24" :stroke-width="1.5" style="color: var(--color-primary); opacity: 0.7" />
            </div>
            <p class="text-[15px] font-semibold mb-1" style="color: var(--text-primary)">
              {{ s.noDepartures ?? 'Nessuna partenza nelle prossime 2 ore' }}
            </p>
            <div v-if="nextDepartureTodayData" class="flex items-center justify-center gap-2 mt-2">
              <span class="text-sm" style="color: var(--text-secondary)">{{ s.nextDepartureToday }}:</span>
              <LineBadge
                :name="nextDepartureTodayData.lineName"
                :color="nextDepartureTodayData.lineColor"
                :text-color="nextDepartureTodayData.lineTextColor"
                :locale="config?.locale[0]"
              />
              <span class="text-sm font-semibold tabular-nums" style="color: var(--text-primary)">{{ nextDepartureTodayData.time }}</span>
            </div>
            <button
              type="button"
              class="inline-flex items-center gap-1 text-sm font-semibold mt-3"
              style="color: var(--color-primary)"
              @click="activeTab = 'orario'"
            >
              Vedi orario completo
              <ChevronDown :size="14" :stroke-width="1.75" />
            </button>
          </div>

          <!-- Realtime indicator -->
          <p v-if="isLive" class="text-xs text-green-600 dark:text-green-400 mt-2 flex items-center gap-1.5" role="status">
            <span class="w-1.5 h-1.5 rounded-full bg-green-500 shrink-0" />
            {{ s.updatedRealtime }}
          </p>
          <div aria-live="polite" class="sr-only">{{ isLive ? s.updatedRealtime : '' }}</div>
        </section>

        <!-- ── Orario ── -->
        <section v-show="activeTab === 'orario'" class="px-4 mb-6" aria-labelledby="section-orari">
          <h2 id="section-orari" class="text-[17px] font-bold mb-3" style="color: var(--text-primary)">
            Orario
          </h2>
          <div
            v-if="config?.gtfsRt && !isLive"
            class="text-xs text-center text-amber-600 dark:text-amber-400 py-1.5 px-3 rounded-lg mb-3"
            style="background: rgba(245,158,11,0.1)"
            role="alert"
          >
            {{ s.schedulesNotLive }}
          </div>
          <DayGroupTabs
            :day-groups="dayGroups"
            :departures-by-day-group="departuresByGroup"
            :initial-key="todayKey ?? undefined"
            :strings="s"
          >
            <template #default="{ departures }">
              <div
                v-if="departures.length"
                class="overflow-hidden divide-y"
                style="background-color: var(--bg-elevated); border-radius: 16px; border: 1px solid var(--border)"
              >
                <DepartureRow
                  v-for="(dep, index) in departures"
                  :key="dep.id"
                  :departure="dep"
                  :now="now"
                  :locale="config?.locale[0]"
                  :data-departure-future="dep.minutesFromMidnight >= nowMin && departures.findIndex((d: Departure) => d.minutesFromMidnight >= nowMin) === index ? 'true' : undefined"
                />
              </div>
              <div
                v-else
                class="rounded-2xl px-5 py-12 text-center"
                style="background-color: var(--bg-elevated); border: 1px solid var(--border)"
              >
                <p class="font-semibold mb-1" style="color: var(--text-primary)">{{ s.noDeparturesToday }}</p>
                <p class="text-sm" style="color: var(--text-secondary)">{{ s.noDeparturesHint }}</p>
                <p v-if="nextServiceLabel" class="text-sm mt-1" style="color: var(--text-tertiary)">
                  {{ s.nextServiceDay }}: {{ nextServiceLabel }}
                </p>
              </div>
            </template>
          </DayGroupTabs>
        </section>

        <!-- ── Footer ── -->
        <footer class="px-4 pb-8 flex items-center gap-4" style="color: var(--text-tertiary)">
          <a
            v-if="stop.lat && stop.lng"
            :href="`https://maps.google.com/?q=${stop.lat},${stop.lng}`"
            target="_blank"
            rel="noopener noreferrer"
            class="flex items-center gap-1.5 text-sm transition-opacity active:opacity-60"
            style="color: var(--text-tertiary)"
          >
            <MapPin :size="14" :stroke-width="1.75" />
            Google Maps
          </a>
          <button
            v-if="canShare"
            type="button"
            class="flex items-center gap-1.5 text-sm transition-opacity active:opacity-60"
            style="color: var(--text-tertiary)"
            @click="shareStop"
          >
            <Share2 :size="14" :stroke-width="1.75" />
            Condividi
          </button>
          <button
            v-else
            type="button"
            class="flex items-center gap-1.5 text-sm transition-opacity active:opacity-60"
            style="color: var(--text-tertiary)"
            @click="copyLink"
          >
            <component :is="copied ? Check : Copy" :size="14" :stroke-width="1.75" />
            {{ copied ? 'Copiato' : 'Copia link' }}
          </button>
        </footer>
      </template>

      <!-- Error: stop not found -->
      <div v-else-if="!pending" role="alert" class="text-center py-16 px-4">
        <p class="text-lg font-semibold" style="color: var(--text-primary)">{{ s.stopNotFound }}</p>
        <p class="text-sm mt-1" style="color: var(--text-tertiary)">ID: {{ stopId }}</p>
        <NuxtLink to="/" class="mt-4 inline-block text-sm underline" style="color: var(--color-primary)">
          {{ s.backToHome }}
        </NuxtLink>
      </div>
    </div>
  </AppLayout>
</template>
```

`<script setup>` needs two small additions:
1. `const filterLine = ref<string | null>(null)` — for line filter chips
2. `const showAllUpcoming = ref(false)` — for "mostra altri"
3. `const filteredUpcomingDepartures = computed(...)` — filters `upcomingDepartures` by `filterLine`
4. Import `Bookmark` instead of `Star` from lucide-vue-next

- [ ] **Step 2: Update script section — add filterLine, showAllUpcoming, filteredUpcomingDepartures, swap Star→Bookmark**

- [ ] **Step 3: Commit**

```bash
git add web/pages/stop/[stopId].vue
git commit -m "feat(web): redesign stop page — map header, iOS layout, line filter chips"
```

---

## Task 6: Update `AppLayout.vue`

**Files:**
- Modify: `web/components/AppLayout.vue`

Add `AppDownloadBanner` and compensate `pt-[44px]` on main content when banner is visible. Banner receives `appLinks` and `operatorName` from `useOperator()`.

- [ ] **Step 1: Rewrite `web/components/AppLayout.vue`**

```vue
<template>
  <div class="min-h-screen" style="background-color: var(--bg-secondary)">
    <AppDownloadBanner
      :app-links="config?.appLinks"
      :operator-name="config?.name"
    />
    <AppSidebar />
    <main
      class="app-main lg:ml-60 min-h-screen"
      :class="bannerVisible ? 'pt-[44px]' : ''"
      style="background-color: var(--bg-secondary)"
    >
      <slot />
    </main>
    <AppTabBar />
  </div>
</template>

<script setup lang="ts">
const { config } = await useOperator()

// Mirror dismissal state so we can remove top padding when banner is gone
const bannerVisible = ref(false)
onMounted(() => {
  bannerVisible.value = localStorage.getItem('app-banner-dismissed') !== '1'
  // Listen for custom dismiss event from banner
  window.addEventListener('app-banner-dismissed', () => { bannerVisible.value = false })
})
</script>
```

Also update `AppDownloadBanner.vue` to emit a `window` event `'app-banner-dismissed'` when dismissed, so AppLayout can react:
```js
function dismiss() {
  visible.value = false
  localStorage.setItem(STORAGE_KEY, '1')
  window.dispatchEvent(new Event('app-banner-dismissed'))
}
```

- [ ] **Step 2: Commit**

```bash
git add web/components/AppLayout.vue web/components/AppDownloadBanner.vue
git commit -m "feat(web): integrate AppDownloadBanner into AppLayout"
```

---

## Task 7: Visual review

- [ ] **Step 1: Open browser at stop page, 390px width**

Navigate to `http://localhost:3002/stop/stop-A`.

- [ ] **Step 2: Take before/after screenshots, verify against iOS simulator screenshot**

Compare visually: map header visible, iOS departure row style, line filter chips, segmented control, app banner.

- [ ] **Step 3: Fix any regressions found**
