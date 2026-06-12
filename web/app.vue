<template>
  <div class="min-h-screen bg-gray-50 dark:bg-[#080C18] text-gray-900 dark:text-gray-100">
    <NuxtLoadingIndicator :color="'#1e40af'" />
    <a
      href="#main-content"
      class="sr-only focus:not-sr-only focus:absolute focus:top-2 focus:left-2 focus:z-50 focus:px-4 focus:py-2 focus:rounded-lg focus:bg-white focus:text-gray-900 focus:shadow-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
    >
      {{ appStrings.errorSkipToContent }}
    </a>
    <NuxtErrorBoundary>
      <main id="main-content" tabindex="-1">
        <NuxtPage />
      </main>
      <template #error="{ error, clearError }">
        <div class="min-h-screen flex flex-col items-center justify-center px-6 text-center bg-gray-50 dark:bg-gray-950">
          <div class="max-w-sm w-full">
            <div class="text-4xl mb-4" aria-hidden="true"></div>
            <h1 class="text-xl font-bold text-gray-800 dark:text-gray-100 mb-2">
              {{ errorStatusCode(error) === 404 ? appStrings.errorPageNotFound : appStrings.errorServiceUnavailable }}
            </h1>
            <p class="text-sm text-gray-500 dark:text-gray-400 mb-6">
              {{ errorStatusCode(error) === 502 || errorStatusCode(error) === 503
                ? appStrings.errorLoadSchedules
                : appStrings.errorUnexpected }}
            </p>
            <button
              class="px-5 py-2.5 rounded-xl bg-blue-600 text-white text-sm font-medium hover:bg-blue-700 focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 dark:focus-visible:ring-offset-gray-950 outline-none transition-colors"
              @click="retryClearError(clearError)"
            >
              {{ appStrings.errorRetry }}
            </button>
          </div>
        </div>
      </template>
    </NuxtErrorBoundary>
  </div>
</template>

<script setup lang="ts">
import { getStrings } from '~/utils/strings'
import type { OperatorConfig } from '~/types'

const { initTheme } = useTheme()
onMounted(() => { initTheme() })

// Resolve operator locale for lang attribute and error boundary strings.
// useNuxtData reads from the same async data cache as useOperator's
// useAsyncData('operator-config', ...) call — zero extra fetches.
const { data: operatorConfig } = useNuxtData<OperatorConfig>('operator-config')

// Best-effort locale: defaults to 'en' when config not yet loaded (SSR cold start)
const lang = computed(() => {
  const locale = operatorConfig.value?.locale?.[0]
  if (!locale) return 'en'
  return locale.split('-')[0]?.toLowerCase() ?? 'en'
})

const appStrings = computed(() => getStrings(lang.value))

type NuxtBoundaryError = Error & { statusCode?: number }

function errorStatusCode(error: Error): number | undefined {
  return (error as NuxtBoundaryError).statusCode
}

function retryClearError(clearError: (() => void) | ((opts: { redirect: string }) => void)): void {
  ;(clearError as (opts: { redirect: string }) => void)({ redirect: '/' })
}

useHead({
  htmlAttrs: { lang: computed(() => lang.value) },
  link: [
    { rel: 'manifest', href: '/manifest.json' },
    { rel: 'apple-touch-icon', href: '/icons/icon-180.png' },
  ],
  meta: [
    { name: 'viewport', content: 'width=device-width, initial-scale=1' },
    { name: 'theme-color', content: '#003366' },
    { name: 'apple-mobile-web-app-capable', content: 'yes' },
    { name: 'apple-mobile-web-app-status-bar-style', content: 'black-translucent' },
    { name: 'apple-mobile-web-app-title', content: 'Transit' },
    { name: 'mobile-web-app-capable', content: 'yes' },
  ],
})
</script>
