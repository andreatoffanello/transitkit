<script setup lang="ts">
import type { SectionsContent } from '~/types/content'
import { X } from 'lucide-vue-next'
defineProps<{ data: SectionsContent['trap'] }>()
</script>

<template>
  <section class="trap section" data-testid="trap">
    <div class="shell trap__grid">
      <SectionHeader :eyebrow="data.eyebrow" :title="data.title">
        <Reveal as="p" class="trap__body" :delay="120">{{ data.body }}</Reveal>
      </SectionHeader>

      <Reveal as="ul" class="trap__points" :delay="160">
        <li v-for="(p, i) in data.points" :key="i">
          <span class="trap__x"><X :size="15" :stroke-width="2.5" /></span>
          {{ p }}
        </li>
      </Reveal>
    </div>
  </section>
</template>

<style scoped>
.trap__grid {
  display: grid;
  gap: 40px;
  align-items: start;
}
.trap__body { margin-top: 6px; color: var(--text-2); font-size: 1.0625rem; line-height: 1.55; max-width: 46ch; }
.trap__points { display: flex; flex-direction: column; gap: 14px; }
.trap__points li {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 1.0625rem;
  color: var(--text);
  padding: 16px 18px;
  border: 1px solid var(--hairline);
  border-radius: 12px;
  background: var(--surface);
}
.trap__x {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  flex: none;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.06);
  color: var(--text-2);
}
@media (min-width: 900px) {
  .trap__grid { grid-template-columns: 1fr 1fr; gap: 72px; }
}
</style>
