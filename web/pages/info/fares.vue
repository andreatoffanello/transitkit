<template>
  <AppLayout>
    <PageHeader
      title="Tariffe e biglietti"
      back-to="/info"
      back-label="Info"
    />

    <div v-if="config?.fares" class="max-w-lg mx-auto px-4 pb-10 space-y-8">

      <!-- Tipi tariffa -->
      <section v-if="config.fares.types?.length">
        <h2 class="text-xs font-semibold uppercase tracking-widest mb-3" style="color: var(--text-tertiary)">
          Tariffe
        </h2>
        <div
          class="rounded-2xl overflow-hidden divide-app"
          :style="{ backgroundColor: 'var(--bg-elevated)', boxShadow: 'var(--shadow-sm)', borderColor: 'var(--border)' }"
        >
          <div
            v-for="fare in config.fares.types"
            :key="fare.name"
            class="flex items-center gap-3 px-4 py-3.5"
            style="border-color: var(--border)"
          >
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium" style="color: var(--text-primary)">{{ fare.name }}</p>
              <p v-if="fare.notes" class="text-xs mt-0.5" style="color: var(--text-secondary)">{{ fare.notes }}</p>
            </div>
            <span class="text-sm font-semibold shrink-0" style="color: var(--color-primary)">{{ fare.price }}</span>
          </div>
        </div>
      </section>

      <!-- Note generali -->
      <section v-if="config.fares.notes">
        <h2 class="text-xs font-semibold uppercase tracking-widest mb-3" style="color: var(--text-tertiary)">
          Note
        </h2>
        <div
          class="rounded-2xl px-4 py-3.5"
          :style="{ backgroundColor: 'var(--bg-elevated)', boxShadow: 'var(--shadow-sm)' }"
        >
          <p class="text-sm leading-relaxed" style="color: var(--text-secondary)">{{ config.fares.notes }}</p>
        </div>
      </section>

      <!-- Punti vendita -->
      <section v-if="config.pointsOfSale?.length">
        <h2 class="text-xs font-semibold uppercase tracking-widest mb-3" style="color: var(--text-tertiary)">
          Dove acquistare
        </h2>
        <div
          class="rounded-2xl overflow-hidden divide-app"
          :style="{ backgroundColor: 'var(--bg-elevated)', boxShadow: 'var(--shadow-sm)', borderColor: 'var(--border)' }"
        >
          <div
            v-for="pos in config.pointsOfSale"
            :key="pos.name"
            class="flex items-start gap-3 px-4 py-3.5"
            style="border-color: var(--border)"
          >
            <MapPin :size="16" :stroke-width="1.75" class="shrink-0 mt-0.5" style="color: var(--text-tertiary)" aria-hidden="true" />
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium" style="color: var(--text-primary)">{{ pos.name }}</p>
              <p v-if="pos.address" class="text-xs mt-0.5" style="color: var(--text-secondary)">{{ pos.address }}</p>
              <p v-if="pos.hours" class="text-xs mt-0.5" style="color: var(--text-tertiary)">{{ pos.hours }}</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Acquisto online -->
      <div v-if="config.fares.purchaseUrl" class="pt-2 pb-4">
        <a
          :href="config.fares.purchaseUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="flex items-center justify-center w-full py-3.5 rounded-2xl text-sm font-semibold transition-opacity duration-150 active:opacity-75"
          :style="{ backgroundColor: 'var(--color-primary)', color: 'var(--color-text-on-primary)' }"
        >
          Acquista online
          <ExternalLink :size="14" :stroke-width="2" class="ml-2" aria-hidden="true" />
        </a>
      </div>

    </div>

    <div v-else class="max-w-lg mx-auto px-4 py-16 text-center">
      <p class="text-sm" style="color: var(--text-secondary)">Informazioni tariffarie non disponibili.</p>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { MapPin, ExternalLink } from 'lucide-vue-next'

const { config } = await useOperator()

useHead({
  title: computed(() => `Tariffe — ${config.value?.name ?? ''}`),
})
useOperatorHead(config)
</script>
