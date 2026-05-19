<template>
  <div class="relative w-full overflow-hidden stop-map-hero">
    <div ref="mapContainer" class="absolute inset-0" />
    <!-- Expand → opens external maps -->
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

// Lucide Signpost path — kept inline so the marker doesn't depend on the component tree.
// Source: https://lucide.dev/icons/signpost  (MIT)
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

  const { default: maplibregl } = await import('maplibre-gl')

  const accent = props.primaryColor ?? '#165F9C'

  mapInstance = new maplibregl.Map({
    container: mapContainer.value,
    style: 'https://tiles.openfreemap.org/styles/bright',
    center: [props.lng, props.lat],
    zoom: 14.5,
    pitch: 35,
    bearing: 0,
    attributionControl: false,
    interactive: false,
  })

  const map = mapInstance as InstanceType<typeof maplibregl.Map>

  map.on('load', () => {
    marker = new maplibregl.Marker({ element: buildMarkerEl(accent), anchor: 'bottom' })
      .setLngLat([props.lng, props.lat])
      .addTo(map)

    // Fly-in cinematico (parity native: iOS pitch 60° + Android pitch 50°).
    // Manteniamo 50° per leggibilità web e per non perdere il pin sul tilt.
    setTimeout(() => {
      map.flyTo({
        center: [props.lng, props.lat],
        zoom: 17,
        pitch: 50,
        bearing: 0,
        duration: 1200,
        essential: true,
        curve: 1.42,
      })
    }, 500)
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

<style scoped>
.stop-map-hero {
  height: 240px;
  background: var(--bg-secondary);
  border-bottom-left-radius: 20px;
  border-bottom-right-radius: 20px;
}
@media (min-width: 640px) {
  .stop-map-hero {
    height: 280px;
  }
}
</style>
