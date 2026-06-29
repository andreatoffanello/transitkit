<script setup lang="ts">
import type { SectionsContent } from '~/types/content'
defineProps<{ data: SectionsContent['hero'] }>()

const entered = ref(false)
const { el: contentEl } = useParallax(-0.05)
onMounted(() => {
  requestAnimationFrame(() => requestAnimationFrame(() => { entered.value = true }))
})
</script>

<template>
  <section class="hero" data-testid="hero">
    <ClientOnly>
      <NetworkCanvas />
    </ClientOnly>

    <div class="hero__scrim" aria-hidden="true" />
    <div ref="contentEl" class="hero__content" :class="{ 'is-in': entered }">
      <h1 class="hero__h1">
        <span class="text-sheen">{{ data.h1 }}</span>
      </h1>
      <p class="hero__sub">{{ data.sub }}</p>
      <div class="hero__cta">
        <CtaButton :to="data.ctaPrimary.to || '#pricing'" variant="primary">{{ data.ctaPrimary.label }}</CtaButton>
        <CtaButton :to="data.ctaSecondary.to || '#proof'" variant="ghost">{{ data.ctaSecondary.label }}</CtaButton>
      </div>
      <p class="hero__trust">{{ data.trustline }}</p>
    </div>
  </section>
</template>

<style scoped>
.hero {
  position: relative;
  min-height: 100svh;
  display: flex;
  align-items: center;
  overflow: hidden;
}
.hero__scrim {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background: linear-gradient(105deg, rgba(0, 0, 0, 0.88) 0%, rgba(0, 0, 0, 0.66) 40%, rgba(0, 0, 0, 0.28) 60%, transparent 76%);
}
@media (max-width: 768px) {
  .hero__scrim {
    background: linear-gradient(180deg, rgba(0, 0, 0, 0.35) 0%, rgba(0, 0, 0, 0.72) 42%, rgba(0, 0, 0, 0.86) 70%, #000 100%);
  }
}
.hero__content {
  position: relative;
  z-index: 2;
  transform: translateY(var(--py, 0px));
  max-width: 1120px;
  width: 100%;
  margin: 0 auto;
  padding: 120px 24px 80px;
  display: flex;
  flex-direction: column;
  gap: 26px;
  align-items: flex-start;
}
/* on-load entrance — staggered spring rise */
.hero__content > * {
  opacity: 0;
  transform: translateY(24px);
  transition:
    opacity 0.9s cubic-bezier(0.16, 1, 0.3, 1),
    transform 0.9s cubic-bezier(0.16, 1, 0.3, 1);
}
.hero__content.is-in > * { opacity: 1; transform: none; }
.hero__content > *:nth-child(1) { transition-delay: 0.08s; }
.hero__content > *:nth-child(2) { transition-delay: 0.2s; }
.hero__content > *:nth-child(3) { transition-delay: 0.32s; }
.hero__content > *:nth-child(4) { transition-delay: 0.42s; }
@media (prefers-reduced-motion: reduce) {
  .hero__content > * { opacity: 1; transform: none; transition: none; }
}
.hero__h1 {
  font-size: clamp(2.6rem, 7vw, 5rem);
  font-weight: 800;
  letter-spacing: -0.045em;
  line-height: 0.98;
  max-width: 16ch;
}
.hero__sub {
  font-size: clamp(1.05rem, 1.6vw, 1.3rem);
  line-height: 1.5;
  color: rgba(255, 255, 255, 0.85); /* over the field — keep AA comfortable */
  max-width: 52ch;
}
.hero__cta { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 4px; }
.hero__trust {
  font-family: 'Geist Mono', monospace;
  font-size: 0.8125rem;
  letter-spacing: 0.02em;
  color: var(--text-3);
  margin-top: 6px;
}
</style>
