<template>
  <div class="min-h-screen bg-gray-50 dark:bg-[#080C18] text-gray-900 dark:text-gray-100">
    <NuxtErrorBoundary>
      <NuxtPage />
      <template #error="{ error, clearError }">
        <div class="min-h-screen flex flex-col items-center justify-center px-6 text-center bg-gray-50 dark:bg-gray-950">
          <div class="max-w-sm w-full">
            <div class="text-4xl mb-4" aria-hidden="true">🚌</div>
            <h1 class="text-xl font-bold text-gray-800 dark:text-gray-100 mb-2">
              {{ error.statusCode === 404 ? 'Pagina non trovata' : 'Servizio temporaneamente non disponibile' }}
            </h1>
            <p class="text-sm text-gray-500 dark:text-gray-400 mb-6">
              {{ error.statusCode === 502 || error.statusCode === 503
                ? 'Impossibile caricare gli orari. Riprova tra qualche minuto.'
                : 'Si è verificato un errore imprevisto.' }}
            </p>
            <button
              class="px-5 py-2.5 rounded-xl bg-blue-600 text-white text-sm font-medium hover:bg-blue-700 focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 dark:focus-visible:ring-offset-gray-950 outline-none transition-colors"
              @click="clearError({ redirect: '/' })"
            >
              Riprova
            </button>
          </div>
        </div>
      </template>
    </NuxtErrorBoundary>
  </div>
</template>

<script setup lang="ts">
useHead({
  htmlAttrs: { lang: 'it' },
  meta: [
    { name: 'viewport', content: 'width=device-width, initial-scale=1' },
    { name: 'theme-color', content: '#003366' },
  ],
})
</script>
