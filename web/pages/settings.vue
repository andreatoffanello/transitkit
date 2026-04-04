<template>
  <AppLayout>
    <div class="max-w-lg mx-auto lg:max-w-2xl flex flex-col" style="min-height: calc(100vh - 4rem)">
      <PageHeader title="Impostazioni" />

      <div class="px-4 space-y-6 pb-8 flex-1">

        <!-- Aspetto -->
        <section>
          <h2 class="text-xs font-semibold uppercase tracking-widest px-1 mb-2" style="color: var(--text-tertiary)">
            Aspetto
          </h2>
          <div class="rounded-2xl overflow-hidden" style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm)">
            <button
              class="flex items-center gap-4 w-full px-4 py-4 transition-opacity duration-150 active:opacity-70"
              @click="toggleTheme"
              :aria-pressed="isDark"
              aria-label="Attiva/disattiva tema scuro"
            >
              <div class="w-9 h-9 rounded-xl flex items-center justify-center shrink-0" style="background-color: var(--bg-secondary)">
                <component :is="isDark ? Sun : Moon" :size="18" :stroke-width="1.75" style="color: var(--text-secondary)" />
              </div>
              <div class="flex-1 text-left">
                <p class="text-[15px] font-medium" style="color: var(--text-primary)">Tema scuro</p>
                <p class="text-sm" style="color: var(--text-secondary)">{{ isDark ? 'Scuro' : 'Chiaro' }}</p>
              </div>
              <!-- Toggle visivo -->
              <div
                class="relative w-12 h-7 rounded-full transition-colors duration-300 shrink-0"
                :style="isDark ? 'background-color: var(--color-primary)' : 'background-color: var(--text-tertiary)'"
              >
                <div
                  class="absolute top-0.5 w-6 h-6 bg-white rounded-full shadow-sm transition-transform duration-300"
                  :class="isDark ? 'translate-x-5' : 'translate-x-0.5'"
                />
              </div>
            </button>
          </div>
        </section>

        <!-- Informazioni -->
        <section>
          <h2 class="text-xs font-semibold uppercase tracking-widest px-1 mb-2" style="color: var(--text-tertiary)">
            Informazioni
          </h2>
          <div
            class="rounded-2xl overflow-hidden"
            style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm)"
          >
            <div class="flex items-center gap-4 px-4 py-4" style="border-bottom: 1px solid var(--border)">
              <div class="w-9 h-9 rounded-xl flex items-center justify-center shrink-0" style="background-color: var(--bg-secondary)">
                <Bus :size="18" :stroke-width="1.75" style="color: var(--text-secondary)" />
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-[15px] font-medium" style="color: var(--text-primary)">{{ config?.name ?? 'TransitKit' }}</p>
                <p v-if="config?.fullName" class="text-sm truncate" style="color: var(--text-secondary)">{{ config.fullName }}</p>
              </div>
            </div>

            <a
              v-if="config?.privacyUrl"
              :href="config.privacyUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="flex items-center gap-4 px-4 py-4 transition-opacity duration-150 active:opacity-70"
            >
              <div class="w-9 h-9 rounded-xl flex items-center justify-center shrink-0" style="background-color: var(--bg-secondary)">
                <Shield :size="18" :stroke-width="1.75" style="color: var(--text-secondary)" />
              </div>
              <span class="flex-1 text-[15px] font-medium" style="color: var(--text-primary)">Privacy</span>
              <ExternalLink :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" />
            </a>

            <a
              v-if="config?.url"
              :href="config.url"
              target="_blank"
              rel="noopener noreferrer"
              class="flex items-center gap-4 px-4 py-4 transition-opacity duration-150 active:opacity-70"
            >
              <div class="w-9 h-9 rounded-xl flex items-center justify-center shrink-0" style="background-color: var(--bg-secondary)">
                <Globe :size="18" :stroke-width="1.75" style="color: var(--text-secondary)" />
              </div>
              <span class="flex-1 text-[15px] font-medium" style="color: var(--text-primary)">Sito ufficiale</span>
              <ExternalLink :size="16" :stroke-width="1.75" style="color: var(--text-tertiary)" />
            </a>
          </div>
        </section>

        <!-- Dati orari -->
        <section v-if="schedules?.validUntil">
          <h2 class="text-xs font-semibold uppercase tracking-widest px-1 mb-2" style="color: var(--text-tertiary)">
            Dati orari
          </h2>
          <div class="rounded-2xl overflow-hidden" style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm)">
            <div class="flex items-center gap-4 px-4 py-4">
              <div class="w-9 h-9 rounded-xl flex items-center justify-center shrink-0" style="background-color: var(--bg-secondary)">
                <CalendarDays :size="18" :stroke-width="1.75" style="color: var(--text-secondary)" />
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-[15px] font-medium" style="color: var(--text-primary)">Orari validi fino al</p>
                <p class="text-sm" style="color: var(--text-secondary)">{{ formatValidUntil(schedules.validUntil) }}</p>
              </div>
              <span
                v-if="schedulesExpiringSoon"
                class="text-xs font-semibold px-2 py-0.5 rounded-full"
                style="background-color: rgba(245,158,11,0.12); color: #d97706"
              >In scadenza</span>
            </div>
          </div>
        </section>

      </div>

      <!-- Version footer anchored to bottom -->
      <div class="px-4 py-5 text-center" style="border-top: 1px solid var(--border)">
        <p class="text-xs" style="color: var(--text-tertiary)">
          Powered by transit-engine
        </p>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { Sun, Moon, Bus, Shield, ExternalLink, Globe, CalendarDays } from 'lucide-vue-next'

const { isDark, toggleTheme } = useTheme()
const { config, schedules } = await useOperator()

const schedulesExpiringSoon = computed(() => {
  if (!schedules.value?.validUntil) return false
  const exp = new Date(schedules.value.validUntil)
  const daysLeft = (exp.getTime() - Date.now()) / (1000 * 60 * 60 * 24)
  return daysLeft < 30
})

function formatValidUntil(dateStr: string): string {
  const d = new Date(dateStr)
  return d.toLocaleDateString(config.value?.locale?.[0] ?? 'it-IT', { day: 'numeric', month: 'long', year: 'numeric' })
}

useHead({ title: 'Impostazioni' })
</script>
