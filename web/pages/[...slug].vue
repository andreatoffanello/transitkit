<template>
  <AppLayout>
    <div class="max-w-lg mx-auto lg:max-w-2xl flex flex-col items-center justify-center min-h-[60vh] px-4 text-center">
      <div
        class="w-16 h-16 rounded-2xl flex items-center justify-center mx-auto mb-5"
        style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm)"
      >
        <MapPin :size="28" :stroke-width="1.5" style="color: var(--text-tertiary)" />
      </div>
      <h1 class="text-xl font-bold mb-2" style="color: var(--text-primary)">Pagina non trovata</h1>
      <p class="text-[15px] mb-6" style="color: var(--text-secondary)">
        La pagina che stai cercando non esiste o è stata spostata.
      </p>
      <NuxtLink
        to="/"
        class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-semibold transition-opacity duration-150 active:opacity-70"
        style="background-color: var(--color-primary); color: var(--color-text-on-primary)"
      >
        <Home :size="16" :stroke-width="1.75" />
        Torna alla home
      </NuxtLink>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { MapPin, Home } from 'lucide-vue-next'

const { config } = await useOperator()

useHead({
  title: computed(() => `Pagina non trovata — ${config.value?.name ?? 'TransitKit'}`),
})
useOperatorHead(config)
</script>
