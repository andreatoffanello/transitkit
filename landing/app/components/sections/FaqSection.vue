<script setup lang="ts">
import type { FaqContent } from '~/types/content'
import { Plus } from 'lucide-vue-next'
const props = defineProps<{ data: FaqContent; title: string }>()
const open = ref<number | null>(0)
function toggle(i: number) { open.value = open.value === i ? null : i }
</script>

<template>
  <section id="faq" class="section" data-testid="faq">
    <div class="shell faq">
      <Reveal as="h2" class="faq__title">{{ title }}</Reveal>
      <ul class="faq__list">
        <Reveal v-for="(item, i) in data.items" :key="i" as="li" class="faq__item" :delay="i * 50">
          <button
            class="faq__q"
            :aria-expanded="open === i"
            @click="toggle(i)"
          >
            <span>{{ item.q }}</span>
            <Plus :size="18" :stroke-width="2" class="faq__icon" :class="{ 'is-open': open === i }" />
          </button>
          <div class="faq__a" :class="{ 'is-open': open === i }">
            <p>{{ item.a }}</p>
          </div>
        </Reveal>
      </ul>
    </div>
  </section>
</template>

<style scoped>
.faq { max-width: 760px; }
.faq__title { font-size: clamp(1.85rem, 4vw, 2.6rem); font-weight: 700; letter-spacing: -0.035em; margin-bottom: 40px; }
.faq__list { display: flex; flex-direction: column; }
.faq__item { border-top: 1px solid var(--hairline); }
.faq__item:last-child { border-bottom: 1px solid var(--hairline); }
.faq__q {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 22px 4px;
  text-align: left;
  font-size: 1.0625rem;
  font-weight: 550;
  letter-spacing: -0.01em;
  color: var(--text);
  cursor: pointer;
}
.faq__icon { flex: none; color: var(--text-3); transition: transform 0.3s, color 0.2s; }
.faq__icon.is-open { transform: rotate(45deg); color: var(--text); }
.faq__q:hover .faq__icon { color: var(--text-2); }
.faq__a {
  display: grid;
  grid-template-rows: 0fr;
  transition: grid-template-rows 0.32s cubic-bezier(0.16, 1, 0.3, 1);
}
.faq__a.is-open { grid-template-rows: 1fr; }
.faq__a > p {
  overflow: hidden;
  color: var(--text-2);
  font-size: 1rem;
  line-height: 1.6;
  max-width: 60ch;
  padding-right: 36px;
}
.faq__a.is-open > p { padding-bottom: 22px; }
</style>
