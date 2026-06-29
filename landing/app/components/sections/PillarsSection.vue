<script setup lang="ts">
import type { SectionsContent } from '~/types/content'
import { Smartphone, BellRing, QrCode, Wrench } from 'lucide-vue-next'
import type { Component } from 'vue'

defineProps<{ data: SectionsContent['pillars'] }>()

const icons: Record<string, Component> = { smartphone: Smartphone, 'bell-ring': BellRing, 'qr-code': QrCode, wrench: Wrench }
const glow = usePointerGlow()
</script>

<template>
  <section id="pillars" class="section" data-testid="pillars">
    <div class="shell">
      <SectionHeader :eyebrow="data.eyebrow" :title="data.title" />

      <div class="pillars__grid">
        <Reveal
          v-for="(item, i) in data.items"
          :key="item.title"
          class="pillar"
          :delay="i * 80"
          @pointermove="glow.onMove"
          @pointerleave="glow.onLeave"
        >
          <span class="pillar__field" aria-hidden="true" />
          <span class="pillar__icon">
            <component :is="icons[item.icon]" :size="22" :stroke-width="1.75" />
          </span>
          <h3 class="pillar__title">{{ item.title }}</h3>
          <p class="pillar__body">{{ item.body }}</p>
        </Reveal>
      </div>
    </div>
  </section>
</template>

<style scoped>
.pillars__grid {
  margin-top: 56px;
  display: grid;
  gap: 16px;
  grid-template-columns: 1fr;
}
.pillar {
  position: relative;
  overflow: hidden;
  padding: 24px;
  border: 1px solid var(--hairline);
  border-radius: 16px;
  background: linear-gradient(180deg, var(--surface), rgba(10, 11, 13, 0.4));
  transition: border-color 0.3s, transform 0.3s, background 0.3s;
}
/* dithered cyan field bleeding from the top-right corner — the hero's texture,
   turning a plain icon card into a panel that belongs to the product */
.pillar__field {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background-image: radial-gradient(rgba(var(--cyan), 0.58) 0.5px, transparent 0.85px);
  background-size: 3px 3px;
  opacity: 0.62;
  -webkit-mask-image: radial-gradient(165px 135px at 100% 0%, #000, transparent 74%);
          mask-image: radial-gradient(165px 135px at 100% 0%, #000, transparent 74%);
}
.pillar__icon, .pillar__title, .pillar__body { position: relative; z-index: 1; }
.pillar:hover {
  border-color: var(--hairline-strong);
  transform: translateY(-3px);
  background: linear-gradient(180deg, var(--surface-2), var(--surface));
}
/* interior: a whisper of depth under the cursor */
.pillar::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  background: radial-gradient(260px circle at var(--mx, 50%) var(--my, 50%), rgba(150, 222, 255, 0.05), transparent 60%);
  opacity: var(--glow, 0);
  transition: opacity 0.35s ease;
}
/* edge-light: the 1px rim catches cyan light toward the cursor — the page's own
   signal, not the stock radial fill every SaaS ships */
.pillar::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  padding: 1px;
  box-sizing: border-box;
  pointer-events: none;
  background: radial-gradient(220px circle at var(--mx, 50%) var(--my, 50%), rgba(120, 220, 255, 0.8), transparent 55%);
  -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
          mask-composite: exclude;
  opacity: var(--glow, 0);
  transition: opacity 0.35s ease;
}
.pillar__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid var(--hairline);
  color: #fff;
  margin-bottom: 20px;
}
.pillar__title { font-size: 1.1875rem; font-weight: 650; letter-spacing: -0.02em; margin-bottom: 9px; }
.pillar__body { color: var(--text-2); font-size: 0.96875rem; line-height: 1.55; }
@media (min-width: 640px) { .pillars__grid { grid-template-columns: 1fr 1fr; } }
@media (min-width: 1000px) { .pillars__grid { grid-template-columns: repeat(4, 1fr); } }
</style>
