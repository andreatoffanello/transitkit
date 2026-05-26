<template>
  <div
    class="relative w-full overflow-hidden"
    style="height: 220px; background: var(--bg-secondary); border-bottom-left-radius: 20px; border-bottom-right-radius: 20px"
  >
    <div ref="mapContainer" class="absolute inset-0" style="height: 220px" />
    <!-- Expand → opens external maps via geo: URI (iOS + Android) -->
    <a
      v-if="lat && lng"
      :href="`geo:${lat},${lng}?q=${lat},${lng}`"
      :aria-label="ariaOpenInMaps"
      class="absolute bottom-3 right-3 flex items-center justify-center rounded-xl z-10 transition-opacity active:opacity-70"
      style="width: 36px; height: 36px; background: rgba(255,255,255,0.92); backdrop-filter: blur(10px) saturate(180%); -webkit-backdrop-filter: blur(10px) saturate(180%); box-shadow: 0 2px 8px rgba(0,0,0,0.18); color: #1a1a1a"
    >
      <Maximize2 :size="15" :stroke-width="2" />
    </a>
  </div>
</template>

<script setup lang="ts">
import { Maximize2 } from 'lucide-vue-next'

const props = defineProps<{
  lat: number
  lng: number
  primaryColor?: string
  ariaOpenInMaps?: string
}>()

const mapContainer = ref<HTMLElement | null>(null)
let mapInstance: unknown = null
let marker: unknown = null

// Lucide Signpost — inline SVG così il marker non dipende dal component tree
// e si può iniettare via innerHTML su un DOM element creato per MapLibre Marker.
// Glyph identico a quello usato dalle native iOS (LucideIcon.signpost) e
// Android (StopSymbolLayer signpost.png). Source: lucide.dev/icons/signpost (MIT)
const SIGNPOST_SVG = `<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.25" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 13v8"/><path d="M12 3v3"/><path d="M18 6a2 2 0 0 1 1.387.56l2.307 2.22a1 1 0 0 1 0 1.44l-2.307 2.22A2 2 0 0 1 18 13H6a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z"/></svg>`

function buildMarkerEl(color: string): HTMLElement {
  const el = document.createElement('div')
  el.style.cssText = `
    width: 30px;
    height: 30px;
    border-radius: 50%;
    background: ${color};
    border: 3px solid white;
    box-shadow: 0 2px 10px rgba(0,0,0,0.32);
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
  `
  el.innerHTML = SIGNPOST_SVG
  return el
}

onMounted(async () => {
  if (!mapContainer.value) return

  // Usa la build CSP-compliant di MapLibre + worker URL esplicito.
  // Necessario perché il pattern `URL.createObjectURL(blob)` del UMD bundle
  // di default si rompe con il bundling Vite/Nuxt (worker spawn fallisce,
  // canvas resta uniforme, zero tile request).
  const [{ default: maplibregl }, workerUrlMod] = await Promise.all([
    import('maplibre-gl/dist/maplibre-gl-csp.js'),
    import('maplibre-gl/dist/maplibre-gl-csp-worker.js?url'),
  ])
  // @ts-expect-error setWorkerUrl exists in CSP build
  maplibregl.setWorkerUrl(workerUrlMod.default)

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
  // @ts-expect-error dev debug
  if (import.meta.dev || (typeof window !== 'undefined' && window.location?.hostname?.includes('appalcart'))) (window as unknown as { __map: unknown }).__map = map
  map.on('error', (e) => console.error('[StopMapHeader] map error', (e as unknown as { error?: { message?: string } })?.error?.message ?? e))

  map.on('load', () => {
    const accent = props.primaryColor ?? '#165F9C'
    marker = new maplibregl.Marker({ element: buildMarkerEl(accent), anchor: 'bottom' })
      .setLngLat([props.lng, props.lat])
      .addTo(map)
  })
})

// Re-color marker if primaryColor arrives late (async config resolution)
watch(() => props.primaryColor, (color) => {
  if (!marker || !color) return
  const el = (marker as { getElement: () => HTMLElement }).getElement()
  if (el) el.style.background = color
})

onUnmounted(() => {
  if (mapInstance) {
    (mapInstance as { remove: () => void }).remove()
    mapInstance = null
    marker = null
  }
})
</script>
