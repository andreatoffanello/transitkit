<template>
  <header
    class="sticky top-0 z-10 flex items-center gap-3 px-4 py-3 -mx-4"
    :class="marginBottom"
    style="background-color: var(--color-primary, #003366)"
  >
    <NuxtLink
      :to="backTo"
      :aria-label="backLabel"
      class="text-sm opacity-70 mr-2 shrink-0"
      style="color: var(--color-text-on-primary, #ffffff)"
    >
      ← {{ backText }}
    </NuxtLink>
    <span
      v-if="title"
      class="text-lg font-bold flex-1 text-center truncate"
      style="color: var(--color-text-on-primary, #ffffff)"
    >
      {{ title }}
    </span>
  </header>
</template>

<script setup lang="ts">
defineProps<{
  backTo: string
  backText?: string
  backLabel?: string
  title?: string
  marginBottom?: string
}>()
</script>
