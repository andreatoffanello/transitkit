<script setup lang="ts">
import type { SectionsContent } from '~/types/content'
import { Apple, Play } from 'lucide-vue-next'
defineProps<{ data: SectionsContent['proof'] }>()
const { site } = useAppConfig()
const { el: frontEl } = useParallax(-0.05)
const { el: backEl } = useParallax(0.07)
</script>

<template>
  <section id="proof" class="section" data-testid="proof">
    <div class="shell">
      <div class="proof">
        <div class="proof__copy">
          <SectionHeader :eyebrow="data.eyebrow" :title="data.title">
            <Reveal as="p" class="proof__body" :delay="120">{{ data.body }}</Reveal>
          </SectionHeader>
          <Reveal class="proof__badges" :delay="160">
            <a :href="site.appStoreUrl" class="badge" target="_blank" rel="noopener">
              <Apple :size="18" :stroke-width="1.75" /> App Store
            </a>
            <a :href="site.playStoreUrl" class="badge" target="_blank" rel="noopener">
              <Play :size="17" :stroke-width="1.75" /> Google Play
            </a>
          </Reveal>
        </div>

        <Reveal class="proof__device" :delay="80">
          <div class="phones">
            <figure ref="backEl" class="phone phone--back">
              <img src="/showcase/appalrider-routes.png" alt="AppalRider — live routes for the Boone network" loading="lazy" />
            </figure>
            <figure ref="frontEl" class="phone phone--front">
              <img src="/showcase/appalrider-follow.png" alt="AppalRider — following a live bus on the Red line in 3D" loading="lazy" />
            </figure>
          </div>
        </Reveal>
      </div>
    </div>
  </section>
</template>

<style scoped>
.proof { display: grid; gap: 48px; align-items: center; }
.proof__body { margin-top: 6px; color: var(--text-2); font-size: 1.0625rem; line-height: 1.55; max-width: 44ch; }
.proof__badges { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 28px; }
.badge {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  height: 46px;
  padding: 0 18px;
  border: 1px solid var(--hairline-strong);
  border-radius: 11px;
  font-weight: 550;
  font-size: 0.9375rem;
  color: var(--text);
  transition: border-color 0.2s, background 0.2s;
}
.badge:hover { border-color: rgba(255, 255, 255, 0.3); background: rgba(255, 255, 255, 0.04); }

.proof__device { display: flex; justify-content: center; }
.phones {
  position: relative;
  width: 100%;
  max-width: 440px;
  height: clamp(360px, 64vw, 500px);
}
.phone {
  position: absolute;
  top: 50%;
  left: 50%;
  width: clamp(168px, 44%, 214px);
  margin: 0;
  padding: 7px;
  border-radius: 34px;
  background: linear-gradient(160deg, #1a1c1f, #060606);
  border: 1px solid var(--hairline-strong);
  box-shadow: 0 50px 110px -50px rgba(255, 255, 255, 0.22), inset 0 0 0 1px rgba(255, 255, 255, 0.05);
}
.phone img { display: block; width: 100%; border-radius: 27px; }
.phone--back {
  transform: translate(-26%, calc(-54% + var(--py, 0px))) rotate(4deg);
  z-index: 1;
  filter: brightness(0.9) saturate(0.95);
}
.phone--front {
  transform: translate(-72%, calc(-46% + var(--py, 0px))) rotate(-3deg);
  z-index: 2;
}
@media (min-width: 880px) { .proof { grid-template-columns: 1fr 1fr; } }
</style>
