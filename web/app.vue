<template>
  <div class="min-h-screen bg-gray-50 dark:bg-[#080C18] text-gray-900 dark:text-gray-100">
    <NuxtPage />
  </div>
</template>

<script setup lang="ts">
useHead({
  htmlAttrs: { lang: 'it' },
  meta: [
    { name: 'viewport', content: 'width=device-width, initial-scale=1' },
    { name: 'theme-color', content: '#003366' },
  ],
})
</script>
