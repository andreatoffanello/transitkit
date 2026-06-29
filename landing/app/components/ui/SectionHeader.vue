<script setup lang="ts">
defineProps<{ eyebrow?: string; title: string; align?: 'left' | 'center' }>()
</script>

<template>
  <header class="sh" :class="`sh--${align ?? 'left'}`">
    <Reveal v-if="eyebrow" as="p" class="sh__eyebrow">
      <span class="sh__dot" />{{ eyebrow }}
    </Reveal>
    <Reveal as="h2" variant="mask" class="sh__title" :delay="60">{{ title }}</Reveal>
    <slot />
  </header>
</template>

<style scoped>
.sh { display: flex; flex-direction: column; gap: 14px; }
.sh--center { align-items: center; text-align: center; }
.sh__eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-family: 'Geist Mono', monospace;
  font-size: 0.75rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-3);
}
.sh__dot { width: 5px; height: 5px; border-radius: 50%; background: #fff; opacity: 0.6; }
.sh__title {
  font-size: clamp(1.85rem, 4vw, 2.85rem);
  font-weight: 700;
  letter-spacing: -0.035em;
  line-height: 1.05;
  max-width: 20ch;
}
.sh--center .sh__title { max-width: 24ch; }
</style>
