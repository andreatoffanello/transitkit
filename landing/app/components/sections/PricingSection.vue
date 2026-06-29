<script setup lang="ts">
import type { SectionsContent } from '~/types/content'
import { Check } from 'lucide-vue-next'
defineProps<{ data: SectionsContent['pricing'] }>()
const glow = usePointerGlow()
</script>

<template>
  <section id="pricing" class="section" data-testid="pricing">
    <div class="shell">
      <SectionHeader :eyebrow="data.eyebrow" :title="data.title" align="center">
        <Reveal as="p" class="price__sub" :delay="100">{{ data.sub }}</Reveal>
      </SectionHeader>

      <div class="price__grid">
        <Reveal
          v-for="(tier, i) in data.tiers"
          :key="tier.name"
          class="tier"
          :class="{ 'tier--hl': tier.highlighted }"
          :delay="i * 90"
          @pointermove="glow.onMove"
          @pointerleave="glow.onLeave"
        >
          <span v-if="tier.badge" class="tier__badge">{{ tier.badge }}</span>
          <div class="tier__top">
            <h3 class="tier__name">{{ tier.name }}</h3>
            <p class="tier__tag">{{ tier.tagline }}</p>
          </div>
          <div class="tier__price">
            <span class="tier__amount">{{ tier.price }}</span>
            <span v-if="tier.period" class="tier__period">{{ tier.period }}</span>
          </div>
          <p v-if="tier.priceNote" class="tier__pricenote">{{ tier.priceNote }}</p>
          <CtaButton :to="tier.cta.to || '#'" :variant="tier.highlighted ? 'primary' : 'ghost'" class="tier__cta">
            {{ tier.cta.label }}
          </CtaButton>
          <ul class="tier__features">
            <li v-for="f in tier.features" :key="f">
              <Check :size="16" :stroke-width="2.25" class="tier__check" />{{ f }}
            </li>
          </ul>
        </Reveal>
      </div>

      <Reveal as="ul" class="price__always" :delay="120">
        <li v-for="a in data.always" :key="a">{{ a }}</li>
      </Reveal>
    </div>
  </section>
</template>

<style scoped>
.price__sub { margin-top: 6px; color: var(--text-2); font-size: 1.0625rem; text-align: center; }
.price__grid {
  margin-top: 56px;
  display: grid;
  gap: 18px;
  grid-template-columns: 1fr;
  max-width: 760px;
  margin-inline: auto;
}
.tier {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 32px;
  border: 1px solid var(--hairline);
  border-radius: 20px;
  background: var(--surface);
}
.tier--hl {
  border-color: rgba(255, 255, 255, 0.22);
  background: linear-gradient(180deg, #15171b, var(--surface));
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.05), 0 30px 80px -44px rgba(255, 255, 255, 0.32);
}
/* interior: a whisper of depth under the cursor */
.tier::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  background: radial-gradient(300px circle at var(--mx, 50%) var(--my, 50%), rgba(150, 222, 255, 0.05), transparent 60%);
  opacity: var(--glow, 0);
  transition: opacity 0.35s ease;
}
/* edge-light: the 1px rim catches cyan light toward the cursor — page signal, not stock fill */
.tier::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  padding: 1px;
  box-sizing: border-box;
  pointer-events: none;
  background: radial-gradient(240px circle at var(--mx, 50%) var(--my, 50%), rgba(120, 220, 255, 0.78), transparent 55%);
  -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
          mask-composite: exclude;
  opacity: var(--glow, 0);
  transition: opacity 0.35s ease;
}
/* the focal tier gets a stronger cyan rim + a touch more interior bloom */
.tier--hl::before {
  background: radial-gradient(300px circle at var(--mx, 50%) var(--my, 50%), rgba(41, 200, 255, 0.08), transparent 62%);
}
.tier--hl::after {
  background: radial-gradient(260px circle at var(--mx, 50%) var(--my, 50%), rgba(41, 200, 255, 0.95), transparent 55%);
}
.tier__badge {
  position: absolute;
  top: 18px;
  right: 18px;
  font-family: 'Geist Mono', monospace;
  font-size: 0.6875rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #000;
  background: #fff;
  padding: 4px 9px;
  border-radius: 999px;
  font-weight: 600;
}
.tier__name { font-size: 1.375rem; font-weight: 700; letter-spacing: -0.02em; }
.tier__tag { color: var(--text-2); font-size: 0.9375rem; margin-top: 6px; line-height: 1.45; min-height: 2.6em; }
.tier__price { display: flex; align-items: baseline; gap: 4px; margin: 22px 0 0; }
.tier__amount { font-size: 3rem; font-weight: 750; letter-spacing: -0.04em; font-family: 'Geist Mono', monospace; }
.tier__period { color: var(--text-3); font-family: 'Geist Mono', monospace; font-size: 1rem; }
.tier__pricenote {
  font-family: 'Geist Mono', monospace;
  font-size: 0.78rem;
  color: var(--text-2);
  margin: 6px 0 0;
}
.tier__cta { width: 100%; margin-top: 22px; }
.tier__features { margin-top: 24px; display: flex; flex-direction: column; gap: 12px; }
.tier__features li { display: flex; align-items: flex-start; gap: 11px; font-size: 0.96875rem; color: var(--text); line-height: 1.4; }
.tier__check { flex: none; margin-top: 2px; color: #fff; }
.tier--hl .tier__check { color: #fff; }
.price__always {
  margin: 40px auto 0;
  max-width: 760px;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px 22px;
}
.price__always li {
  position: relative;
  font-family: 'Geist Mono', monospace;
  font-size: 0.875rem;
  color: var(--text-2);
  letter-spacing: 0.01em;
  padding-left: 18px;
}
.price__always li::before { content: '✓'; position: absolute; left: 0; color: var(--text); }
@media (min-width: 720px) {
  .price__grid { grid-template-columns: 1fr 1fr; }
}
</style>
