// TransitKit landing — dark-only marketing site
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },

  modules: ['@nuxtjs/tailwindcss', '@pinia/nuxt', '@nuxt/content', '@nuxtjs/i18n'],

  // flat component names regardless of subfolder (CtaButton, HeroSection, SiteNav…)
  components: [{ path: '~/components', pathPrefix: false }],

  // tailwind module owns main.css (which holds our @tailwind directives + tokens)
  tailwindcss: { cssPath: '~/assets/css/main.css' },

  i18n: {
    locales: [
      { code: 'en', language: 'en-US', name: 'English', file: 'en.json' },
      { code: 'it', language: 'it-IT', name: 'Italiano', file: 'it.json' },
    ],
    defaultLocale: 'en',
    strategy: 'prefix_except_default',
    lazy: true,
    detectBrowserLanguage: false, // '/' always EN; '/it' is explicit
    bundle: { optimizeTranslationDirective: false },
  },

  nitro: { preset: 'vercel' },

  routeRules: {
    '/': { prerender: true },
    '/it': { prerender: true },
    '/privacy': { prerender: true },
    '/it/privacy': { prerender: true },
  },

  app: {
    head: {
      htmlAttrs: { class: 'dark', lang: 'en' },
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'color-scheme', content: 'dark' },
        { name: 'theme-color', content: '#000000' },
      ],
      link: [
        { rel: 'icon', type: 'image/svg+xml', href: '/icon.svg' },
        { rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' },
        { rel: 'apple-touch-icon', href: '/apple-touch-icon.png' },
      ],
    },
  },

  typescript: { strict: true },
})
