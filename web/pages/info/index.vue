<template>
  <AppLayout>
    <PageHeader :title="s.tabInfo" />

    <div class="max-w-lg mx-auto md:max-w-xl lg:max-w-2xl px-4 pb-10 space-y-8">

      <!-- Servizi -->
      <section v-if="config?.services?.length">
        <h2 class="text-xs font-semibold uppercase tracking-widest mb-3" style="color: var(--text-tertiary)">
          {{ s.infoServices }}
        </h2>
        <div
          class="rounded-2xl overflow-hidden divide-app"
          :style="{ backgroundColor: 'var(--bg-elevated)', boxShadow: 'var(--shadow-sm)', borderColor: 'var(--border)' }"
        >
          <NuxtLink
            v-for="service in config.services"
            :key="service.id"
            :to="`/info/services/${service.id}`"
            class="flex items-center gap-3 px-4 py-3.5 transition-opacity duration-150 active:opacity-70 hover-row"
            style="border-color: var(--border)"
          >
            <div
              class="w-8 h-8 rounded-lg flex items-center justify-center shrink-0"
              style="background-color: color-mix(in srgb, var(--color-primary) 12%, transparent)"
            >
              <component
                :is="resolveServiceIcon(service.icon)"
                :size="18"
                :stroke-width="1.75"
                style="color: var(--color-primary)"
                aria-hidden="true"
              />
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium truncate" style="color: var(--text-primary)">
                {{ ml(service.title) }}
              </p>
              <p class="text-xs mt-0.5 line-clamp-2" style="color: var(--text-secondary)">
                {{ ml(service.subtitle) }}
              </p>
            </div>
            <ChevronRight :size="16" :stroke-width="1.75" class="shrink-0" style="color: var(--text-tertiary)" aria-hidden="true" />
          </NuxtLink>
        </div>
      </section>

      <!-- Tariffe -->
      <section v-if="config?.fares">
        <h2 class="text-xs font-semibold uppercase tracking-widest mb-3" style="color: var(--text-tertiary)">
          {{ s.infoFares }}
        </h2>
        <div
          class="rounded-2xl overflow-hidden divide-app"
          :style="{ backgroundColor: 'var(--bg-elevated)', boxShadow: 'var(--shadow-sm)', borderColor: 'var(--border)' }"
        >
          <NuxtLink
            to="/info/fares"
            class="flex items-center gap-3 px-4 py-3.5 transition-opacity duration-150 active:opacity-70 hover-row"
            style="border-color: var(--border)"
          >
            <div
              class="w-8 h-8 rounded-lg flex items-center justify-center shrink-0"
              style="background-color: color-mix(in srgb, var(--color-primary) 12%, transparent)"
            >
              <Ticket :size="18" :stroke-width="1.75" style="color: var(--color-primary)" aria-hidden="true" />
            </div>
            <span class="flex-1 text-sm font-medium" style="color: var(--text-primary)">{{ s.infoFaresTitle }}</span>
            <ChevronRight :size="16" :stroke-width="1.75" class="shrink-0" style="color: var(--text-tertiary)" aria-hidden="true" />
          </NuxtLink>
        </div>
      </section>

      <!-- Accessibilità -->
      <section v-if="config?.accessibility">
        <h2 class="text-xs font-semibold uppercase tracking-widest mb-3" style="color: var(--text-tertiary)">
          {{ s.infoAccessibility }}
        </h2>
        <div
          class="rounded-2xl overflow-hidden divide-app"
          :style="{ backgroundColor: 'var(--bg-elevated)', boxShadow: 'var(--shadow-sm)', borderColor: 'var(--border)' }"
        >
          <NuxtLink
            to="/info/accessibility"
            class="flex items-center gap-3 px-4 py-3.5 transition-opacity duration-150 active:opacity-70 hover-row"
            style="border-color: var(--border)"
          >
            <div
              class="w-8 h-8 rounded-lg flex items-center justify-center shrink-0"
              style="background-color: color-mix(in srgb, var(--color-primary) 12%, transparent)"
            >
              <Accessibility :size="18" :stroke-width="1.75" style="color: var(--color-primary)" aria-hidden="true" />
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium truncate" style="color: var(--text-primary)">
                {{ ml(config.accessibility.title) }}
              </p>
            </div>
            <ChevronRight :size="16" :stroke-width="1.75" class="shrink-0" style="color: var(--text-tertiary)" aria-hidden="true" />
          </NuxtLink>
        </div>
      </section>

      <!-- Contatti -->
      <section v-if="config?.contact && hasContact">
        <h2 class="text-xs font-semibold uppercase tracking-widest mb-3" style="color: var(--text-tertiary)">
          {{ s.infoContacts }}
        </h2>
        <div
          class="rounded-2xl overflow-hidden divide-app"
          :style="{ backgroundColor: 'var(--bg-elevated)', boxShadow: 'var(--shadow-sm)', borderColor: 'var(--border)' }"
        >
          <a
            v-if="config.contact.phone"
            :href="`tel:${config.contact.phone}`"
            class="flex items-center gap-3 px-4 py-3.5 transition-opacity duration-150 active:opacity-70 hover-row"
            style="border-color: var(--border)"
          >
            <div
              class="w-8 h-8 rounded-lg flex items-center justify-center shrink-0"
              style="background-color: color-mix(in srgb, var(--color-primary) 12%, transparent)"
            >
              <Phone :size="18" :stroke-width="1.75" style="color: var(--color-primary)" aria-hidden="true" />
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-xs font-medium uppercase tracking-wide" style="color: var(--text-tertiary)">{{ s.infoPhone }}</p>
              <p class="text-sm font-medium" style="color: var(--text-primary)">{{ config.contact.phone }}</p>
            </div>
            <ExternalLink :size="14" :stroke-width="1.75" class="shrink-0" style="color: var(--text-tertiary)" aria-hidden="true" />
          </a>
          <a
            v-if="config.contact.email"
            :href="`mailto:${config.contact.email}`"
            class="flex items-center gap-3 px-4 py-3.5 transition-opacity duration-150 active:opacity-70 hover-row"
            style="border-color: var(--border)"
          >
            <div
              class="w-8 h-8 rounded-lg flex items-center justify-center shrink-0"
              style="background-color: color-mix(in srgb, var(--color-primary) 12%, transparent)"
            >
              <Mail :size="18" :stroke-width="1.75" style="color: var(--color-primary)" aria-hidden="true" />
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-xs font-medium uppercase tracking-wide" style="color: var(--text-tertiary)">{{ s.infoEmail }}</p>
              <p class="text-sm font-medium truncate" style="color: var(--text-primary)">{{ config.contact.email }}</p>
            </div>
            <ExternalLink :size="14" :stroke-width="1.75" class="shrink-0" style="color: var(--text-tertiary)" aria-hidden="true" />
          </a>
        </div>
      </section>

    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ChevronRight, Ticket, Accessibility, Phone, Mail, ExternalLink, Bus, MapPin, Train, Ship, Info, Globe, Wrench } from 'lucide-vue-next'
import type { Component } from 'vue'
import type { MultiLangString } from '~/types'

const { config } = await useOperator()
const s = useStrings(config)

const locale = computed(() => config.value?.locale?.[0] ?? 'en')

function ml(field: MultiLangString | undefined): string {
  if (!field) return ''
  return (field as any)[locale.value] ?? (field as any)['en'] ?? ''
}

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

const hasContact = computed(() =>
  !!(config.value?.contact?.phone || config.value?.contact?.email)
)

useHead({
  title: computed(() => `${s.value.tabInfo} — ${config.value?.brandName ?? config.value?.name ?? ''}`),
})
useOperatorHead(config)
</script>
