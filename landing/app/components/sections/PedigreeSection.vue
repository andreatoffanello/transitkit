<script setup lang="ts">
import type { SectionsContent } from '~/types/content'
import { Apple, Play } from 'lucide-vue-next'
defineProps<{ data: SectionsContent['pedigree'] }>()
const glow = usePointerGlow()
</script>

<template>
  <section class="section" data-testid="pedigree">
    <div class="shell">
      <SectionHeader :eyebrow="data.eyebrow" :title="data.title">
        <Reveal as="p" class="ped__intro" :delay="120">{{ data.intro }}</Reveal>
      </SectionHeader>

      <div class="ped__grid">
        <Reveal
          v-for="(item, i) in data.items"
          :key="item.name"
          class="ped__card"
          :delay="i * 90"
          @pointermove="glow.onMove"
          @pointerleave="glow.onLeave"
        >
          <div class="ped__head">
            <img class="ped__icon" :src="`/showcase/icons/${item.icon}`" :alt="`${item.name} app icon`" width="56" height="56" loading="lazy" />
            <div class="ped__id">
              <span class="ped__name">{{ item.name }}</span>
              <span class="ped__place">{{ item.place }}</span>
            </div>
          </div>
          <p class="ped__body">{{ item.body }}</p>
          <div class="ped__links">
            <a :href="item.ios" class="ped__store" target="_blank" rel="noopener">
              <Apple :size="15" :stroke-width="1.9" /> App Store
            </a>
            <a :href="item.android" class="ped__store" target="_blank" rel="noopener">
              <Play :size="14" :stroke-width="1.9" /> Google Play
            </a>
          </div>
        </Reveal>
      </div>
    </div>
  </section>
</template>

<style scoped>
.ped__intro { margin-top: 6px; color: var(--text-2); font-size: 1.0625rem; line-height: 1.55; max-width: 54ch; }
.ped__grid {
  margin-top: 56px;
  display: grid;
  gap: 16px;
  grid-template-columns: 1fr;
}
.ped__card {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 24px;
  border: 1px solid var(--hairline);
  border-radius: 18px;
  background: linear-gradient(180deg, var(--surface), rgba(10, 11, 13, 0.4));
  transition: border-color 0.3s, transform 0.3s;
}
.ped__card:hover { border-color: var(--hairline-strong); transform: translateY(-3px); }
/* interior: a whisper of depth under the cursor */
.ped__card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  background: radial-gradient(260px circle at var(--mx, 50%) var(--my, 50%), rgba(150, 222, 255, 0.05), transparent 60%);
  opacity: var(--glow, 0);
  transition: opacity 0.35s ease;
}
/* edge-light: the 1px rim catches cyan light toward the cursor */
.ped__card::after {
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
.ped__head { display: flex; align-items: center; gap: 14px; }
.ped__icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  border: 1px solid var(--hairline);
  flex: none;
}
.ped__id { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.ped__name { font-size: 1.0625rem; font-weight: 650; letter-spacing: -0.02em; }
.ped__place { font-family: 'Geist Mono', monospace; font-size: 0.75rem; color: var(--text-3); letter-spacing: 0.02em; }
.ped__body { margin-top: 18px; color: var(--text-2); font-size: 0.9375rem; line-height: 1.55; flex: 1; }
.ped__links { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 20px; }
.ped__store {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 36px;
  padding: 0 13px;
  border: 1px solid var(--hairline);
  border-radius: 9px;
  font-size: 0.8125rem;
  font-weight: 550;
  color: var(--text-2);
  transition: border-color 0.2s, color 0.2s, background 0.2s;
}
.ped__store:hover { color: var(--text); border-color: var(--hairline-strong); background: rgba(255, 255, 255, 0.03); }
@media (min-width: 760px) {
  .ped__grid { grid-template-columns: repeat(3, 1fr); }
}
</style>
