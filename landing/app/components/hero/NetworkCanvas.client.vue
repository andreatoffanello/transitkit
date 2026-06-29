<script setup lang="ts">
import type { HeroSceneHandle } from '~/composables/useHeroScene'

const graphics = useGraphicsStore()
const canvas = ref<HTMLCanvasElement | null>(null)
const root = ref<HTMLElement | null>(null)
let handle: HeroSceneHandle | null = null
let io: IntersectionObserver | null = null
let visible = true
let starting = false

function updateProgress() {
  if (!handle || !root.value) return
  const r = root.value.getBoundingClientRect()
  // 0 when hero fills the viewport top, 1 after scrolling one hero-height up
  const p = r.height > 0 ? -r.top / r.height : 0
  handle.setProgress(p)
}

async function ensureRunning() {
  if (handle || starting || !canvas.value || !visible || graphics.tier === 'static') return
  starting = true
  try {
    handle = await startHeroScene(canvas.value, { tier: graphics.tier })
    updateProgress()
  } finally {
    starting = false
  }
}
function stopScene() {
  handle?.stop()
  handle = null
}
function onVisibility() {
  visible = !document.hidden
  if (!visible) stopScene()
  else ensureRunning()
}

onMounted(async () => {
  if (!graphics.detected) graphics.detect()
  if (graphics.tier === 'static') return

  await nextTick()
  io = new IntersectionObserver(
    (entries) => {
      for (const e of entries) {
        if (e.isIntersecting) { visible = true; ensureRunning() }
        else { visible = false; stopScene() }
      }
    },
    { threshold: 0.01 },
  )
  if (root.value) io.observe(root.value)
  document.addEventListener('visibilitychange', onVisibility)
  window.addEventListener('scroll', updateProgress, { passive: true })
  ensureRunning()
})

onBeforeUnmount(() => {
  stopScene()
  io?.disconnect()
  document.removeEventListener('visibilitychange', onVisibility)
  window.removeEventListener('scroll', updateProgress)
})
</script>

<template>
  <div ref="root" class="hero-canvas" aria-hidden="true">
    <canvas v-if="graphics.tier !== 'static'" ref="canvas" class="hero-canvas__gl" />
    <div v-else class="hero-canvas__poster" />
    <div class="hero-canvas__grain" />
    <div class="hero-canvas__vignette" />
  </div>
</template>

<style scoped>
.hero-canvas {
  position: absolute;
  inset: 0;
  overflow: hidden;
}
.hero-canvas__gl { display: block; width: 100%; height: 100%; }
.hero-canvas__poster {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(120% 90% at 62% 42%, rgba(255, 255, 255, 0.09), transparent 60%),
    #000;
}
.hero-canvas__poster::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 56px 56px;
  mask-image: radial-gradient(90% 70% at 62% 42%, #000, transparent 75%);
}
/* always-on fine film grain — the tactile "premium" tell over the field */
.hero-canvas__grain {
  position: absolute;
  inset: -50%;
  pointer-events: none;
  opacity: 0.5;
  mix-blend-mode: overlay;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='160' height='160'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  animation: grain 7s steps(6) infinite;
}
@keyframes grain {
  0% { transform: translate(0, 0); }
  20% { transform: translate(-4%, 3%); }
  40% { transform: translate(3%, -4%); }
  60% { transform: translate(-3%, 2%); }
  80% { transform: translate(2%, 4%); }
  100% { transform: translate(0, 0); }
}
@media (prefers-reduced-motion: reduce) {
  .hero-canvas__grain { animation: none; }
}
.hero-canvas__vignette {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(120% 85% at 62% 40%, transparent 46%, rgba(0, 0, 0, 0.42) 100%),
    linear-gradient(to bottom, transparent 60%, #000 99%);
}
</style>
