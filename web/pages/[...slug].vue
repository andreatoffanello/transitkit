<template>
  <div class="max-w-lg mx-auto px-4 pb-8">
    <PageHeader
      back-to="/"
      :back-text="config?.name ?? ''"
      :back-label="s.backToHome"
      :title="s.pageNotFound"
    />

    <div role="alert" class="text-center py-16 text-gray-400">
      <div class="text-5xl mb-4" aria-hidden="true">🚌</div>
      <p class="text-lg font-medium text-gray-800 dark:text-gray-100 mb-2">
        {{ s.pageNotFound }}
      </p>
      <p class="text-sm mb-6">{{ s.pageNotFoundHint }}</p>
      <NuxtLink
        to="/lines"
        class="inline-block px-5 py-2.5 rounded-xl text-sm font-medium"
        style="background-color: var(--color-primary, #003366); color: var(--color-text-on-primary, #ffffff)"
      >
        {{ s.backToLines }}
      </NuxtLink>
    </div>
  </div>
</template>

<script setup lang="ts">
const { config } = await useOperator()
const s = useStrings(config)

useHead({
  title: computed(() => `${s.value.pageNotFound} — ${config.value?.name ?? ''}`),
})
useOperatorHead(config)
</script>
