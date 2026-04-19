<template>
  <AppLayout>
    <PageHeader
      :title="config?.accessibility ? ml(config.accessibility.title) : 'Accessibilità'"
      back-to="/info"
      back-label="Info"
    />

    <div v-if="config?.accessibility" class="max-w-lg mx-auto px-4 pb-10 space-y-8">

      <!-- Descrizione -->
      <section>
        <div
          class="rounded-2xl px-4 py-3.5"
          :style="{ backgroundColor: 'var(--bg-elevated)', boxShadow: 'var(--shadow-sm)' }"
        >
          <p class="text-sm leading-relaxed" style="color: var(--text-primary)">
            {{ ml(config.accessibility.description) }}
          </p>
        </div>
      </section>

      <!-- Bullet points -->
      <section v-if="config.accessibility.bullets?.length">
        <h2 class="text-xs font-semibold uppercase tracking-widest mb-3" style="color: var(--text-tertiary)">
          Caratteristiche
        </h2>
        <div
          class="rounded-2xl overflow-hidden divide-app"
          :style="{ backgroundColor: 'var(--bg-elevated)', boxShadow: 'var(--shadow-sm)', borderColor: 'var(--border)' }"
        >
          <div
            v-for="(bullet, i) in config.accessibility.bullets"
            :key="i"
            class="flex items-start gap-3 px-4 py-3.5"
            style="border-color: var(--border)"
          >
            <CheckCircle2
              :size="16"
              :stroke-width="1.75"
              class="shrink-0 mt-0.5"
              style="color: var(--color-primary)"
              aria-hidden="true"
            />
            <p class="text-sm leading-relaxed" style="color: var(--text-primary)">{{ ml(bullet) }}</p>
          </div>
        </div>
      </section>

      <!-- Link maggiori info -->
      <section v-if="config.accessibility.moreUrl">
        <a
          :href="config.accessibility.moreUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="flex items-center justify-center w-full py-3.5 rounded-2xl text-sm font-semibold transition-opacity duration-150 active:opacity-75"
          :style="{ backgroundColor: 'var(--bg-elevated)', boxShadow: 'var(--shadow-sm)', color: 'var(--color-primary)' }"
        >
          Maggiori informazioni
          <ExternalLink :size="14" :stroke-width="2" class="ml-2" aria-hidden="true" />
        </a>
      </section>

    </div>

    <div v-else class="max-w-lg mx-auto px-4 py-16 text-center">
      <p class="text-sm" style="color: var(--text-secondary)">Informazioni non disponibili.</p>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { CheckCircle2, ExternalLink } from 'lucide-vue-next'
import type { MultiLangString } from '~/types'

const { config } = await useOperator()

const locale = computed(() => config.value?.locale?.[0] ?? 'en')

function ml(field: MultiLangString | undefined): string {
  if (!field) return ''
  return (field as any)[locale.value] ?? (field as any)['en'] ?? ''
}

useHead({
  title: computed(() => {
    const title = config.value?.accessibility ? ml(config.value.accessibility.title) : 'Accessibilità'
    return `${title} — ${config.value?.name ?? ''}`
  }),
})
useOperatorHead(config)
</script>
