<template>
  <AppLayout>
    <div class="max-w-lg mx-auto px-4 pt-8">
      <h1 class="text-xl font-bold mb-1" style="color: var(--text-primary)">Mappa</h1>
      <p class="text-sm mb-8" style="color: var(--text-secondary)">{{ config?.fullName ?? config?.name }}</p>

      <div
        class="rounded-3xl flex flex-col items-center justify-center py-16 px-6 text-center"
        style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm)"
      >
        <div
          class="w-16 h-16 rounded-3xl flex items-center justify-center mb-5"
          style="background-color: var(--bg-secondary)"
        >
          <Map :size="28" :stroke-width="1.5" style="color: var(--text-tertiary)" />
        </div>
        <p class="text-base font-semibold mb-2" style="color: var(--text-primary)">Mappa in arrivo</p>
        <p class="text-sm max-w-[260px]" style="color: var(--text-secondary)">
          Visualizza fermate e percorsi sulla mappa interattiva. Disponibile a breve.
        </p>

        <div class="mt-8 w-full space-y-2">
          <NuxtLink
            to="/lines"
            class="flex items-center justify-center gap-2 w-full py-3 rounded-2xl text-sm font-semibold transition-opacity duration-150 active:opacity-70"
            style="background-color: var(--color-primary); color: var(--color-text-on-primary)"
          >
            <Route :size="16" :stroke-width="1.75" />
            Sfoglia le linee
          </NuxtLink>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { Map, Route } from 'lucide-vue-next'
const { config } = await useOperator()

useHead({ title: computed(() => `Mappa — ${config.value?.name ?? ''}`) })
</script>
