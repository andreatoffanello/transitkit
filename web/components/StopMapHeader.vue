<template>
  <div class="relative w-full overflow-hidden" style="height: 220px; background: var(--bg-secondary)">
    <div ref="mapContainer" class="absolute inset-0" style="height: 220px" />
    <!-- Expand to native maps -->
    <a
      v-if="lat && lng"
      :href="`https://maps.apple.com/?q=${lat},${lng}`"
      target="_blank"
      rel="noopener noreferrer"
      aria-label="Apri in mappe"
      class="absolute bottom-3 right-3 flex items-center justify-center rounded-xl z-10"
      style="width: 36px; height: 36px; background: rgba(255,255,255,0.88); backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px); box-shadow: 0 1px 6px rgba(0,0,0,0.18)"
    >
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#333" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <polyline points="15 3 21 3 21 9"/>
        <polyline points="9 21 3 21 3 15"/>
        <line x1="21" y1="3" x2="14" y2="10"/>
        <line x1="3" y1="21" x2="10" y2="14"/>
      </svg>
    </a>
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

  const { default: maplibregl } = await import('maplibre-gl')

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
    const el = document.createElement('div')
    const color = props.primaryColor ?? '#165F9C'
    el.style.cssText = `
      width: 26px;
      height: 26px;
      border-radius: 50%;
      background: ${color};
      border: 3px solid white;
      box-shadow: 0 2px 8px rgba(0,0,0,0.28);
    `
    new maplibregl.Marker({ element: el })
      .setLngLat([props.lng, props.lat])
      .addTo(map)
  })
})

onUnmounted(() => {
  if (mapInstance) {
    (mapInstance as { remove: () => void }).remove()
    mapInstance = null
  }
})
</script>
