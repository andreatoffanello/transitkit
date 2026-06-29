<script setup lang="ts">
import type { SectionsContent } from '~/types/content'
import { Check, Minus } from 'lucide-vue-next'
defineProps<{ data: SectionsContent['positioning'] }>()
</script>

<template>
  <section class="section" data-testid="positioning">
    <div class="shell">
      <SectionHeader :eyebrow="data.eyebrow" :title="data.title">
        <Reveal as="p" class="pos__body" :delay="120">{{ data.body }}</Reveal>
      </SectionHeader>

      <div class="pos__table">
        <Reveal as="div" class="pos__head" :delay="40">
          <span class="pos__col pos__col--them">{{ data.colThem }}</span>
          <span class="pos__col pos__col--us">{{ data.colUs }}</span>
        </Reveal>
        <Reveal v-for="(row, i) in data.rows" :key="i" as="div" class="pos__row" :delay="140 + i * 110">
          <span class="pos__cell pos__cell--them">
            <Minus :size="15" :stroke-width="2.5" class="pos__ico pos__ico--them" />{{ row.them }}
          </span>
          <span class="pos__cell pos__cell--us">
            <Check :size="15" :stroke-width="2.5" class="pos__ico pos__ico--us" />{{ row.us }}
          </span>
        </Reveal>
      </div>

      <Reveal as="p" class="pos__anchor" :delay="80">{{ data.anchor }}</Reveal>
    </div>
  </section>
</template>

<style scoped>
.pos__body { margin-top: 6px; color: var(--text-2); font-size: 1.0625rem; line-height: 1.55; max-width: 56ch; }
.pos__table {
  position: relative;
  margin-top: 48px;
  border: 1px solid var(--hairline);
  border-radius: 16px;
  overflow: hidden;
  background: var(--surface);
}
/* a cyan signal-rail runs the full height of the TransitKit column — the eye is
   pulled to your side; the hero's signal, marking the route that wins */
.pos__table::after {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 2px;
  transform: translateX(-1px);
  background: linear-gradient(180deg, rgba(var(--cyan), 0.6), rgba(var(--cyan), 0.14));
  box-shadow: 0 0 12px rgba(var(--cyan), 0.32);
  pointer-events: none;
  z-index: 1;
}
.pos__head, .pos__row { display: grid; grid-template-columns: 1fr 1fr; }
.pos__head { border-bottom: 1px solid var(--hairline-strong); }
.pos__col {
  padding: 16px 22px;
  font-family: 'Geist Mono', monospace;
  font-size: 0.75rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}
.pos__col--them { color: var(--text-3); }
.pos__col--us { color: var(--text); }
.pos__row { border-top: 1px solid var(--hairline); }
.pos__row:first-of-type { border-top: 0; }
.pos__cell {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 18px 22px;
  font-size: 0.9375rem;
  line-height: 1.4;
}
.pos__cell--them { color: var(--text-2); }
.pos__cell--us {
  position: relative;
  overflow: hidden;
  color: var(--text);
  background: linear-gradient(90deg, rgba(var(--cyan), 0.02), rgba(var(--cyan), 0.07));
}
/* the "TransitKit" column lights up cyan as each row reveals */
.pos__cell--us::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(41, 200, 255, 0.3) 50%, transparent);
  background-size: 220% 100%;
  background-position: -40% 0;
  opacity: 0;
  pointer-events: none;
}
/* each row's cyan band SWEEPS left→right, trailing its own reveal delay → a cascade down the column */
.pos__row.is-revealed .pos__cell--us::before {
  animation: usFlare 1.3s cubic-bezier(0.16, 1, 0.3, 1) var(--reveal-delay, 0.15s);
}
@keyframes usFlare {
  0% { opacity: 0; background-position: -40% 0; }
  22% { opacity: 0.9; }
  100% { opacity: 0; background-position: 140% 0; }
}
.pos__ico { flex: none; }
.pos__ico--them { color: var(--text-3); }
.pos__ico--us { color: #fff; }
@media (prefers-reduced-motion: reduce) {
  .pos__row.is-revealed .pos__cell--us::before { animation: none; }
}
.pos__anchor {
  margin-top: 28px;
  font-size: 1.0625rem;
  line-height: 1.55;
  color: var(--text-2);
  max-width: 62ch;
}
@media (max-width: 560px) {
  .pos__col, .pos__cell { padding-inline: 14px; font-size: 0.875rem; }
}
</style>
