<script setup lang="ts">
const props = withDefaults(defineProps<{ delay?: number; as?: string; variant?: 'rise' | 'mask' }>(), {
  delay: 0,
  as: 'div',
  variant: 'rise',
})
const { el, revealed } = useReveal()
</script>

<template>
  <component
    :is="props.as"
    ref="el"
    class="reveal"
    :class="[`reveal--${props.variant}`, { 'is-revealed': revealed }]"
    :style="{ '--reveal-delay': `${props.delay}ms` }"
  >
    <!-- mask wipe lives on an INNER element so the IO target (root) is never
         clip-zeroed — clip-path on the observed node makes IO report 0 forever -->
    <span v-if="props.variant === 'mask'" class="reveal__inner"><slot /></span>
    <slot v-else />
  </component>
</template>

<style scoped>
/* rise: opacity + transform — both IO-safe */
.reveal--rise {
  opacity: 0;
  transform: translateY(20px);
  transition:
    opacity 0.8s cubic-bezier(0.16, 1, 0.3, 1) var(--reveal-delay),
    transform 0.8s cubic-bezier(0.16, 1, 0.3, 1) var(--reveal-delay);
}
.reveal--rise.is-revealed {
  opacity: 1;
  transform: none;
}

/* mask: root stays geometrically full (IO-safe); inner does the clip wipe */
.reveal--mask .reveal__inner {
  display: block;
  opacity: 0;
  transform: translateY(14px);
  clip-path: inset(0 0 100% 0);
  transition:
    opacity 0.8s cubic-bezier(0.16, 1, 0.3, 1) var(--reveal-delay),
    transform 0.85s cubic-bezier(0.16, 1, 0.3, 1) var(--reveal-delay),
    clip-path 0.9s cubic-bezier(0.16, 1, 0.3, 1) var(--reveal-delay);
}
.reveal--mask.is-revealed .reveal__inner {
  opacity: 1;
  transform: none;
  clip-path: inset(0 0 -8% 0);
}

@media (prefers-reduced-motion: reduce) {
  .reveal--rise,
  .reveal--mask .reveal__inner {
    opacity: 1 !important;
    transform: none !important;
    clip-path: none !important;
    transition: none;
  }
}
</style>
