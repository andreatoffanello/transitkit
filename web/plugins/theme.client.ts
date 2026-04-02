import type { OperatorConfig } from '~/types'

const DEFAULT_THEME = {
  '--color-primary': '#003366',
  '--color-accent': '#0066CC',
  '--color-text-on-primary': '#FFFFFF',
}

function applyTheme(cfg: OperatorConfig | null | undefined): void {
  const root = document.documentElement
  if (!cfg?.theme) {
    // Reset ai default se nessun config trovato (es. pagina 404)
    for (const [key, value] of Object.entries(DEFAULT_THEME)) {
      root.style.setProperty(key, value)
    }
    return
  }
  root.style.setProperty('--color-primary', cfg.theme.primaryColor)
  root.style.setProperty('--color-accent', cfg.theme.accentColor)
  root.style.setProperty('--color-text-on-primary', cfg.theme.textOnPrimary)

  // Update browser chrome color (mobile browsers)
  const themeColorMeta = document.querySelector('meta[name="theme-color"]')
  if (themeColorMeta) {
    themeColorMeta.setAttribute('content', cfg?.theme.primaryColor ?? '#003366')
  }
}

export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.hook('page:finish', () => {
    const payload = nuxtApp.payload.data as Record<string, unknown> | undefined
    const configKey = Object.keys(payload ?? {}).find(k => k.startsWith('config-'))
    const cfg = configKey ? (payload?.[configKey] as OperatorConfig | null) : null
    applyTheme(cfg)
  })
})
