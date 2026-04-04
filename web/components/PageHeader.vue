<template>
  <!-- Variante Large: primo livello, nessun back button -->
  <header v-if="!backTo" class="px-5 pt-6 pb-3">
    <h1 class="text-xl font-bold leading-tight" style="color: var(--text-primary)">
      {{ title }}
    </h1>
    <p v-if="subtitle" class="text-sm mt-0.5" style="color: var(--text-secondary)">
      {{ subtitle }}
    </p>
  </header>

  <!-- Variante Navigation: pagine dettaglio con back button -->
  <header
    v-else
    class="sticky top-0 z-30 flex items-center h-[52px] px-2 transition-all duration-200"
    :class="scrolled ? 'border-b' : ''"
    :style="{
      backgroundColor: scrolled
        ? 'color-mix(in srgb, var(--bg-primary) 90%, transparent)'
        : 'transparent',
      backdropFilter: scrolled ? 'blur(16px) saturate(180%)' : 'none',
      WebkitBackdropFilter: scrolled ? 'blur(16px) saturate(180%)' : 'none',
      borderColor: 'var(--border)',
    }"
  >
    <!-- Back button -->
    <NuxtLink
      :to="backTo"
      class="flex items-center gap-1.5 pl-1 pr-3 py-2 rounded-lg -ml-1 transition-opacity duration-150 active:opacity-60"
      style="color: var(--color-primary); font-size: 14px; font-weight: 500; letter-spacing: -0.01em"
      :aria-label="`Torna indietro`"
    >
      <ChevronLeft :size="20" :stroke-width="1.75" />
      <span class="truncate">{{ backLabel ?? 'Indietro' }}</span>
    </NuxtLink>

    <!-- Title centrato -->
    <h1
      class="flex-1 text-center text-[15px] font-semibold truncate px-2"
      style="color: var(--text-primary)"
    >
      {{ title }}
    </h1>

    <!-- Slot azione destra (es. Share, Star) -->
    <div class="w-[80px] flex justify-end pr-1">
      <slot name="action" />
    </div>
  </header>
</template>

<script setup lang="ts">
import { ChevronLeft } from 'lucide-vue-next'

const props = defineProps<{
  title: string
  subtitle?: string
  backTo?: string
  backLabel?: string
}>()

const scrolled = ref(false)

onMounted(() => {
  if (props.backTo) {
    const handler = () => {
      scrolled.value = window.scrollY > 4
    }
    window.addEventListener('scroll', handler, { passive: true })
    onUnmounted(() => window.removeEventListener('scroll', handler))
  }
})
</script>
