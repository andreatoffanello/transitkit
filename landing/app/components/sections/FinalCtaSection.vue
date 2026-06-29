<script setup lang="ts">
import type { SectionsContent } from '~/types/content'
defineProps<{ data: SectionsContent['finalCta'] }>()
</script>

<template>
  <section id="contact" class="section" data-testid="final-cta">
    <div class="shell">
      <Reveal class="final">
        <div class="final__glow" aria-hidden="true" />
        <h2 class="final__title text-sheen">{{ data.title }}</h2>
        <p class="final__body">{{ data.body }}</p>
        <CtaButton :to="data.cta.to || '#'" variant="primary" class="final__cta">{{ data.cta.label }}</CtaButton>
      </Reveal>
    </div>
  </section>
</template>

<style scoped>
.final {
  position: relative;
  overflow: hidden;
  text-align: center;
  padding: clamp(56px, 8vw, 92px) 24px;
  border: 1px solid var(--hairline);
  border-radius: 28px;
  background: linear-gradient(180deg, var(--surface-2), #000);
}
.final__glow {
  position: absolute;
  top: -40%; left: 50%; transform: translateX(-50%);
  width: 680px; height: 420px;
  background: radial-gradient(50% 50% at 50% 50%, rgba(255, 255, 255, 0.1), transparent 70%);
  pointer-events: none;
}
.final__title {
  position: relative;
  font-size: clamp(2rem, 5vw, 3.4rem);
  font-weight: 800;
  letter-spacing: -0.04em;
  line-height: 1.02;
  max-width: 18ch;
  margin-inline: auto;
}
.final__body {
  position: relative;
  margin: 20px auto 32px;
  color: var(--text-2);
  font-size: 1.0625rem;
  line-height: 1.55;
  max-width: 48ch;
}
.final__cta { position: relative; }
</style>
