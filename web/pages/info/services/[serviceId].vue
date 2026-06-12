<template>
  <AppLayout>
    <PageHeader
      :title="service ? ml(service.title) : s.serviceFallbackTitle"
      back-to="/info"
      back-label="Info"
    />

    <div v-if="service" class="max-w-lg mx-auto px-4 pb-10 space-y-8">

      <!-- Hero icon + subtitle -->
      <div class="flex flex-col items-center pt-2 pb-2">
        <div
          class="w-16 h-16 rounded-2xl flex items-center justify-center mb-3"
          style="background-color: color-mix(in srgb, var(--color-primary) 12%, transparent)"
        >
          <component
            :is="resolveServiceIcon(service.icon)"
            :size="32"
            :stroke-width="1.5"
            style="color: var(--color-primary)"
            aria-hidden="true"
          />
        </div>
        <p class="text-sm text-center" style="color: var(--text-secondary)">
          {{ ml(service.subtitle) }}
        </p>
      </div>

      <!-- Descrizione -->
      <section v-if="service.description">
        <h2 class="text-xs font-semibold uppercase tracking-widest mb-3" style="color: var(--text-tertiary)">
          {{ s.serviceDescription }}
        </h2>
        <div
          class="rounded-2xl px-4 py-3.5"
          :style="{ backgroundColor: 'var(--bg-elevated)', boxShadow: 'var(--shadow-sm)' }"
        >
          <p class="text-sm leading-relaxed" style="color: var(--text-primary)">{{ ml(service.description) }}</p>
        </div>
      </section>

      <!-- Dettagli (audience, hours, fare, serviceArea) -->
      <section v-if="service.audience || service.hours || service.fare || service.serviceArea">
        <h2 class="text-xs font-semibold uppercase tracking-widest mb-3" style="color: var(--text-tertiary)">
          {{ s.serviceDetails }}
        </h2>
        <div
          class="rounded-2xl overflow-hidden divide-app"
          :style="{ backgroundColor: 'var(--bg-elevated)', boxShadow: 'var(--shadow-sm)', borderColor: 'var(--border)' }"
        >
          <div
            v-if="service.audience"
            class="flex items-start gap-3 px-4 py-3.5"
            style="border-color: var(--border)"
          >
            <Users :size="16" :stroke-width="1.75" class="shrink-0 mt-0.5" style="color: var(--text-tertiary)" aria-hidden="true" />
            <div>
              <p class="text-xs font-medium mb-0.5" style="color: var(--text-tertiary)">{{ s.serviceAudience }}</p>
              <p class="text-sm" style="color: var(--text-primary)">{{ ml(service.audience) }}</p>
            </div>
          </div>
          <div
            v-if="service.hours"
            class="flex items-start gap-3 px-4 py-3.5"
            style="border-color: var(--border)"
          >
            <Clock :size="16" :stroke-width="1.75" class="shrink-0 mt-0.5" style="color: var(--text-tertiary)" aria-hidden="true" />
            <div>
              <p class="text-xs font-medium mb-0.5" style="color: var(--text-tertiary)">{{ s.serviceHours }}</p>
              <p class="text-sm" style="color: var(--text-primary)">{{ ml(service.hours) }}</p>
            </div>
          </div>
          <div
            v-if="service.fare"
            class="flex items-start gap-3 px-4 py-3.5"
            style="border-color: var(--border)"
          >
            <Ticket :size="16" :stroke-width="1.75" class="shrink-0 mt-0.5" style="color: var(--text-tertiary)" aria-hidden="true" />
            <div>
              <p class="text-xs font-medium mb-0.5" style="color: var(--text-tertiary)">{{ s.serviceFare }}</p>
              <p class="text-sm" style="color: var(--text-primary)">{{ ml(service.fare) }}</p>
            </div>
          </div>
          <div
            v-if="service.serviceArea"
            class="flex items-start gap-3 px-4 py-3.5"
            style="border-color: var(--border)"
          >
            <MapPin :size="16" :stroke-width="1.75" class="shrink-0 mt-0.5" style="color: var(--text-tertiary)" aria-hidden="true" />
            <div>
              <p class="text-xs font-medium mb-0.5" style="color: var(--text-tertiary)">{{ s.serviceArea }}</p>
              <p class="text-sm" style="color: var(--text-primary)">{{ ml(service.serviceArea) }}</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Come funziona (steps) -->
      <section v-if="service.steps?.length">
        <h2 class="text-xs font-semibold uppercase tracking-widest mb-3" style="color: var(--text-tertiary)">
          {{ s.serviceHowItWorks }}
        </h2>
        <div
          class="rounded-2xl overflow-hidden divide-app"
          :style="{ backgroundColor: 'var(--bg-elevated)', boxShadow: 'var(--shadow-sm)', borderColor: 'var(--border)' }"
        >
          <div
            v-for="(step, i) in service.steps"
            :key="i"
            class="flex items-start gap-3 px-4 py-3.5"
            style="border-color: var(--border)"
          >
            <span
              class="shrink-0 w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold mt-0.5"
              style="background-color: color-mix(in srgb, var(--color-primary) 12%, transparent); color: var(--color-primary)"
            >
              {{ i + 1 }}
            </span>
            <p class="text-sm leading-relaxed" style="color: var(--text-primary)">{{ ml(step) }}</p>
          </div>
        </div>
      </section>

      <!-- Note -->
      <section v-if="service.notes?.length">
        <h2 class="text-xs font-semibold uppercase tracking-widest mb-3" style="color: var(--text-tertiary)">
          {{ s.serviceNotes }}
        </h2>
        <div
          class="rounded-2xl overflow-hidden divide-app"
          :style="{ backgroundColor: 'var(--bg-elevated)', boxShadow: 'var(--shadow-sm)', borderColor: 'var(--border)' }"
        >
          <div
            v-for="(note, i) in service.notes"
            :key="i"
            class="flex items-start gap-3 px-4 py-3.5"
            style="border-color: var(--border)"
          >
            <span class="shrink-0 mt-2 w-1.5 h-1.5 rounded-full" style="background-color: var(--text-tertiary)" aria-hidden="true" />
            <p class="text-sm leading-relaxed" style="color: var(--text-secondary)">{{ ml(note) }}</p>
          </div>
        </div>
      </section>

      <!-- Link utili -->
      <section v-if="service.links?.length">
        <h2 class="text-xs font-semibold uppercase tracking-widest mb-3" style="color: var(--text-tertiary)">
          {{ s.serviceUsefulLinks }}
        </h2>
        <div
          class="rounded-2xl overflow-hidden divide-app"
          :style="{ backgroundColor: 'var(--bg-elevated)', boxShadow: 'var(--shadow-sm)', borderColor: 'var(--border)' }"
        >
          <a
            v-for="link in service.links"
            :key="link.url"
            :href="link.url"
            target="_blank"
            rel="noopener noreferrer"
            class="flex items-center gap-3 px-4 py-3.5 transition-opacity duration-150 active:opacity-70 hover-row"
            style="border-color: var(--border)"
          >
            <Globe :size="16" :stroke-width="1.75" class="shrink-0" style="color: var(--color-primary)" aria-hidden="true" />
            <span class="flex-1 text-sm font-medium" style="color: var(--color-primary)">{{ ml(link.label) }}</span>
            <ExternalLink :size="14" :stroke-width="1.75" class="shrink-0" style="color: var(--text-tertiary)" aria-hidden="true" />
          </a>
        </div>
      </section>

      <!-- CTA -->
      <div v-if="service.cta" class="pt-2 pb-4">
        <NuxtLink
          v-if="service.cta.type === 'internal'"
          :to="service.cta.value"
          class="flex items-center justify-center w-full py-3.5 rounded-2xl text-sm font-semibold transition-opacity duration-150 active:opacity-75"
          :style="{ backgroundColor: 'var(--color-primary)', color: 'var(--color-text-on-primary)' }"
        >
          {{ ml(service.cta.label) }}
        </NuxtLink>
        <a
          v-else-if="service.cta.type === 'phone'"
          :href="`tel:${service.cta.value}`"
          class="flex items-center justify-center w-full py-3.5 rounded-2xl text-sm font-semibold transition-opacity duration-150 active:opacity-75"
          :style="{ backgroundColor: 'var(--color-primary)', color: 'var(--color-text-on-primary)' }"
        >
          <Phone :size="16" :stroke-width="2" class="mr-2" aria-hidden="true" />
          {{ ml(service.cta.label) }}
        </a>
        <a
          v-else
          :href="service.cta.value"
          target="_blank"
          rel="noopener noreferrer"
          class="flex items-center justify-center w-full py-3.5 rounded-2xl text-sm font-semibold transition-opacity duration-150 active:opacity-75"
          :style="{ backgroundColor: 'var(--color-primary)', color: 'var(--color-text-on-primary)' }"
        >
          {{ ml(service.cta.label) }}
          <ExternalLink :size="14" :stroke-width="2" class="ml-2" aria-hidden="true" />
        </a>
      </div>

    </div>

    <!-- Not found -->
    <div v-else class="max-w-lg mx-auto px-4 py-16 text-center">
      <p class="text-sm" style="color: var(--text-secondary)">{{ s.serviceNotFound }}</p>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { Clock, Users, Ticket, MapPin, Globe, ExternalLink, Phone, Bus, Train, Ship, Info, Accessibility, Wrench } from 'lucide-vue-next'
import type { Component } from 'vue'
import type { MultiLangString } from '~/types'

const { config } = await useOperator()
const s = useStrings(config)
const route = useRoute()

const locale = computed(() => config.value?.locale?.[0] ?? 'en')

function ml(field: MultiLangString | undefined): string {
  if (!field) return ''
  return (field as any)[locale.value] ?? (field as any)['en'] ?? ''
}

const service = computed(() =>
  config.value?.services?.find(s => s.id === route.params.serviceId)
)

const ICON_MAP: Record<string, Component> = {
  bus: Bus,
  'map-pin': MapPin,
  train: Train,
  ship: Ship,
  accessibility: Accessibility,
  phone: Phone,
  globe: Globe,
  wrench: Wrench,
  info: Info,
}

function resolveServiceIcon(name: string): Component {
  return ICON_MAP[name] ?? Info
}

useHead({
  title: computed(() => {
    const title = service.value ? ml(service.value.title) : s.value.serviceFallbackTitle
    return `${title} — ${config.value?.name ?? ''}`
  }),
})
useOperatorHead(config)
</script>
