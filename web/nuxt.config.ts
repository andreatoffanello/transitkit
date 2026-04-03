export default defineNuxtConfig({
  compatibilityDate: '2025-01-01',
  devtools: { enabled: true },
  modules: ['@nuxtjs/tailwindcss'],

  css: ['~/assets/css/main.css'],

  routeRules: {
    '/': { isr: 3600, headers: { 'cache-control': 'public, max-age=60, stale-while-revalidate=120' } },
    '/lines': { isr: 3600, headers: { 'cache-control': 'public, max-age=300, stale-while-revalidate=600' } },
    '/lines/**': { isr: 3600, headers: { 'cache-control': 'public, max-age=300, stale-while-revalidate=600' } },
    '/stop/**': { isr: 3600, headers: { 'cache-control': 'public, max-age=60, stale-while-revalidate=120' } },
  },

  nitro: {
    preset: 'vercel',
  },

  typescript: {
    strict: true,
  },

  experimental: {
    payloadExtraction: false,
  },
})
