export default defineNuxtConfig({
  compatibilityDate: '2025-01-01',
  devtools: { enabled: true },
  modules: ['@nuxtjs/tailwindcss'],

  app: {
    pageTransition: { name: 'page-fade', mode: 'out-in' },
    head: {
      link: [
        {
          rel: 'stylesheet',
          href: 'https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:ital,wght@0,400;0,500;0,600;0,700;0,800&display=swap',
        },
      ],
    },
  },

  css: ['~/assets/css/main.css'],

  runtimeConfig: {
    public: {
      cdnBase: process.env.CDN_BASE ?? 'https://andreatoffanello.github.io/transitkit-data',
    },
  },

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

  experimental: {},
})
