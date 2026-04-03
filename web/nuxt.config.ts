export default defineNuxtConfig({
  compatibilityDate: '2025-01-01',
  devtools: { enabled: true },
  modules: ['@nuxtjs/tailwindcss'],

  css: ['~/assets/css/main.css'],

  routeRules: {
    '/': { isr: 3600 },
    '/lines': { isr: 3600 },
    '/lines/**': { isr: 3600 },
    '/stop/**': { isr: 3600 },
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
