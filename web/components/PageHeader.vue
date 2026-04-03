<template>
  <header
    class="sticky top-0 z-10 flex items-center gap-3 px-4 py-3 -mx-4"
    :class="marginBottom"
    :style="{ backgroundColor: primaryColor }"
  >
    <NuxtLink
      :to="backTo"
      :aria-label="backLabel"
      class="text-sm opacity-70 mr-2 shrink-0"
      :style="{ color: textColor }"
    >
      ← {{ backText }}
    </NuxtLink>
    <span
      v-if="title"
      class="text-lg font-bold flex-1 text-center truncate"
      :style="{ color: textColor }"
    >
      {{ title }}
    </span>
  </header>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  primaryColor?: string
  textColor?: string
  backTo: string
  backText?: string
  backLabel?: string
  title?: string
  marginBottom?: string
}>(), {
  primaryColor: '#003366',
  textColor: '#FFFFFF',
  backText: '',
  backLabel: undefined,
  title: undefined,
  marginBottom: 'mb-4',
})
</script>
