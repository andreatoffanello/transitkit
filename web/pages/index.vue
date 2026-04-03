<template>
  <div>
    <!-- Hero -->
    <div
      class="flex flex-col items-center justify-center text-center px-6 py-16 min-h-[40vh]"
      :style="{ backgroundColor: config?.theme.primaryColor, color: config?.theme.textOnPrimary }"
    >
      <h1 class="text-3xl font-bold mb-2">{{ config?.fullName ?? config?.name }}</h1>
      <p v-if="config?.region" class="text-sm opacity-70 mb-8">{{ config.region }}</p>

      <NuxtLink
        to="/lines"
        class="px-6 py-3 rounded-2xl font-semibold text-sm"
        :style="{
          backgroundColor: config?.theme.textOnPrimary,
          color: config?.theme.primaryColor,
        }"
      >
        {{ s.linesAndSchedules }}
      </NuxtLink>
    </div>

    <!-- Body -->
    <div class="max-w-lg mx-auto px-4 py-8 space-y-4">
      <!-- Store info (app link) -->
      <div v-if="config?.store" class="bg-white dark:bg-white/5 rounded-2xl p-4">
        <p class="text-xs font-semibold uppercase tracking-wider text-gray-400 mb-2">{{ s.officialApp }}</p>
        <p class="font-semibold">{{ config.store.title }}</p>
        <p class="text-sm text-gray-400">{{ config.store.subtitle }}</p>
      </div>

      <!-- Contacts -->
      <div v-if="config?.contact?.phone || config?.contact?.email" class="bg-white dark:bg-white/5 rounded-2xl p-4 space-y-3">
        <p class="text-xs font-semibold uppercase tracking-wider text-gray-400 mb-1">{{ s.contacts }}</p>
        <a
          v-if="config.contact.phone"
          :href="`tel:${config.contact.phone}`"
          class="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-200"
        >
          📞 {{ config.contact.phone }}
        </a>
        <a
          v-if="config.contact.email"
          :href="`mailto:${config.contact.email}`"
          class="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-200"
        >
          ✉️ {{ config.contact.email }}
        </a>
      </div>

      <!-- Official website -->
      <a
        v-if="config?.url"
        :href="config.url"
        target="_blank"
        rel="noopener noreferrer"
        class="flex items-center justify-center gap-2 py-3 rounded-xl bg-gray-100 dark:bg-white/10 text-sm font-medium text-gray-700 dark:text-gray-200 hover:bg-gray-200 dark:hover:bg-white/20 transition-colors"
      >
        🌐 {{ s.officialWebsite }}
      </a>

      <!-- Fermate recenti -->
      <div v-if="recentStops.length" class="bg-white dark:bg-white/5 rounded-2xl p-4">
        <p class="text-xs font-semibold uppercase tracking-wider text-gray-400 mb-3">{{ s.recentStops }}</p>
        <div class="space-y-2">
          <NuxtLink
            v-for="stop in recentStops"
            :key="stop.stopId"
            :to="`/stop/${stop.stopId}`"
            class="flex items-center justify-between py-1.5"
          >
            <span class="text-sm text-gray-900 dark:text-gray-100">{{ stop.name }}</span>
            <span class="text-gray-400 text-sm" aria-hidden="true">›</span>
          </NuxtLink>
        </div>
      </div>

      <!-- Privacy link -->
      <a
        v-if="config?.privacyUrl"
        :href="config.privacyUrl"
        target="_blank"
        rel="noopener noreferrer"
        class="block text-center text-xs text-gray-400 underline"
      >
        {{ s.privacy }}
      </a>
    </div>
  </div>
</template>

<script setup lang="ts">
const { config } = await useOperator()
const s = useStrings(config)

const { recentStops, load } = useRecentStops()
onMounted(() => { load() })

useHead({
  title: computed(() => `${config.value?.fullName ?? config.value?.name ?? ''} — Orari e linee`),
  meta: [
    {
      name: 'description',
      content: computed(() => {
        const name = config.value?.fullName ?? config.value?.name ?? ''
        const region = config.value?.region
        const location = region ? ` — ${region}` : ''
        return `${name}${location}. Orari, linee e fermate. Bus schedule and timetables.`
      }),
    },
    {
      property: 'og:title',
      content: computed(() => config.value?.fullName ?? config.value?.name ?? ''),
    },
    {
      property: 'og:description',
      content: computed(() => {
        const name = config.value?.fullName ?? config.value?.name ?? ''
        const region = config.value?.region
        const location = region ? ` — ${region}` : ''
        return `${name}${location}. Orari, linee e fermate. Bus schedule and timetables.`
      }),
    },
  ],
})
useOperatorHead(config)
</script>
