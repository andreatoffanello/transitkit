<script setup lang="ts">
import type { SectionsContent } from '~/types/content'
defineProps<{ data: SectionsContent['how'] }>()
</script>

<template>
  <section id="how" class="section" data-testid="how">
    <div class="shell">
      <SectionHeader :eyebrow="data.eyebrow" :title="data.title" />

      <div class="how__grid">
        <Reveal
          v-for="(s, i) in data.steps"
          :key="s.n"
          class="step"
          :class="{ 'step--end': i === data.steps.length - 1 }"
          :delay="i * 110"
        >
          <span v-if="i < data.steps.length - 1" class="step__seg" aria-hidden="true" />
          <span class="step__node" aria-hidden="true" />
          <span class="step__n">{{ s.n }}</span>
          <h3 class="step__title">{{ s.title }}</h3>
          <p class="step__body">{{ s.body }}</p>
        </Reveal>
      </div>

      <Reveal as="p" class="how__note" :delay="120">{{ data.note }}</Reveal>
    </div>
  </section>
</template>

<style scoped>
.how__grid {
  position: relative;
  margin-top: 56px;
  display: grid;
  gap: 30px;
  grid-template-columns: 1fr;
}
/* a transit line threads the steps — the route the rider takes through onboarding */
.step { position: relative; padding-left: 36px; }
.step__node {
  position: absolute;
  left: 0;
  top: 4px;
  width: 15px;
  height: 15px;
  border-radius: 50%;
  background: rgb(var(--cyan));
  box-shadow: 0 0 0 4px rgba(var(--cyan), 0.13), 0 0 16px 1px rgba(var(--cyan), 0.6);
  transform: scale(0);
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1) calc(var(--reveal-delay, 0s) + 0.12s);
}
.step.is-revealed .step__node { transform: scale(1); }
/* the route's terminus — a double halo reads as the arrival station */
.step--end .step__node {
  box-shadow: 0 0 0 4px rgba(var(--cyan), 0.13), 0 0 0 8px rgba(var(--cyan), 0.08), 0 0 16px 1px rgba(var(--cyan), 0.6);
}
/* the rail segment to the next station — draws as each step arrives */
.step__seg {
  position: absolute;
  left: 7px;
  top: 12px;
  width: 2px;
  height: calc(100% + 30px);
  background: linear-gradient(180deg, rgba(var(--cyan), 0.5), rgba(var(--cyan), 0.1));
  transform-origin: top;
  transform: scaleY(0);
  transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1) calc(var(--reveal-delay, 0s) + 0.2s);
}
.step.is-revealed .step__seg { transform: scaleY(1); }
.step__n {
  font-family: 'Geist Mono', monospace;
  font-size: 0.8125rem;
  color: var(--text-3);
  letter-spacing: 0.05em;
}
.step__title { font-size: 1.25rem; font-weight: 650; letter-spacing: -0.02em; margin: 12px 0 10px; }
.step__body { color: var(--text-2); font-size: 1rem; line-height: 1.55; max-width: 34ch; }
.how__note {
  margin-top: 48px;
  padding: 18px 22px;
  border: 1px dashed var(--hairline-strong);
  border-radius: 12px;
  color: var(--text-2);
  font-size: 0.96875rem;
  background: var(--surface);
}
@media (min-width: 820px) {
  .how__grid { grid-template-columns: repeat(3, 1fr); gap: 40px; }
  .step { padding-left: 0; padding-top: 36px; }
  .step__node { top: 0; }
  .step__seg {
    left: 7px;
    top: 7px;
    width: calc(100% + 40px);
    height: 2px;
    background: linear-gradient(90deg, rgba(var(--cyan), 0.6), rgba(var(--cyan), 0.08));
    transform-origin: left;
    transform: scaleX(0);
    transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1) calc(var(--reveal-delay, 0s) + 0.2s);
  }
  .step.is-revealed .step__seg { transform: scaleX(1); }
}
@media (prefers-reduced-motion: reduce) {
  .step__node { transform: scale(1); }
  .step__seg { transform: none; }
}
</style>
