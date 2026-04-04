<template>
  <AppLayout>
    <div class="max-w-lg mx-auto px-4">
      <div class="px-1 pt-6 pb-4">
        <h1 class="text-xl font-bold mb-0.5" style="color: var(--text-primary)">Mappa</h1>
        <p class="text-sm" style="color: var(--text-secondary)">{{ config?.fullName ?? config?.name }}</p>
      </div>

      <div
        class="rounded-3xl flex flex-col items-center justify-center py-14 px-6 text-center"
        style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm)"
      >
        <!-- Animated illustration -->
        <div
          class="relative w-24 h-24 rounded-3xl flex items-center justify-center mb-6"
          style="background: linear-gradient(135deg, color-mix(in srgb, var(--color-primary) 10%, var(--bg-secondary)) 0%, color-mix(in srgb, var(--color-primary) 6%, var(--bg-secondary)) 100%)"
        >
          <div
            class="absolute inset-0 rounded-3xl animate-pulse"
            style="background: radial-gradient(circle at 35% 40%, color-mix(in srgb, var(--color-primary) 20%, transparent), transparent 70%); opacity: 0.6"
          />
          <Map :size="44" :stroke-width="1.5" style="color: var(--color-primary); position: relative; z-index: 1; opacity: 0.85" />
        </div>

        <h2 class="text-lg font-semibold mb-2" style="color: var(--text-primary)">Prossimamente</h2>
        <p class="text-sm leading-relaxed max-w-[260px]" style="color: var(--text-secondary)">
          Mappa interattiva con fermate, percorsi e tempi di percorrenza. In sviluppo.
        </p>

        <div class="mt-8 w-full max-w-xs space-y-2.5">
          <NuxtLink
            to="/lines"
            class="flex items-center justify-center gap-2 w-full py-3 rounded-2xl text-sm font-semibold transition-opacity duration-150 active:opacity-70"
            style="background-color: var(--color-primary); color: var(--color-text-on-primary)"
          >
            <Route :size="16" :stroke-width="1.75" />
            Sfoglia le linee
          </NuxtLink>
          <NuxtLink
            to="/"
            class="flex items-center justify-center gap-2 w-full py-3 rounded-2xl text-sm font-semibold transition-opacity duration-150 active:opacity-70"
            style="background-color: var(--bg-secondary); color: var(--text-secondary); border: 1px solid var(--border)"
          >
            <Home :size="16" :stroke-width="1.75" />
            Torna alla home
          </NuxtLink>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { Map, Route, Home } from 'lucide-vue-next'
const { config } = await useOperator()

useHead({ title: computed(() => `Mappa — ${config.value?.name ?? ''}`) })
</script>
