// web/plugins/theme.client.ts
export default defineNuxtPlugin((nuxtApp) => {
  // Applica il tema leggendo l'operatorConfig già nel payload di Nuxt
  // Il payload viene popolato da useAsyncData nei componenti/pagine
  nuxtApp.hook('page:finish', () => {
    const payload = nuxtApp.payload.data
    const configKey = Object.keys(payload ?? {}).find(k => k.startsWith('config-'))
    if (!configKey) return
    const cfg = (payload as Record<string, unknown>)[configKey] as import('~/types').OperatorConfig | null
    if (!cfg?.theme) return
    const root = document.documentElement
    root.style.setProperty('--color-primary', cfg.theme.primaryColor)
    root.style.setProperty('--color-accent', cfg.theme.accentColor)
    root.style.setProperty('--color-text-on-primary', cfg.theme.textOnPrimary)
  })
})
