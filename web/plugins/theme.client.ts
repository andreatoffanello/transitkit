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
  }
  else {
    root.style.setProperty('--color-primary', cfg.theme.primaryColor)
    root.style.setProperty('--color-accent', cfg.theme.accentColor)
    root.style.setProperty('--color-text-on-primary', cfg.theme.textOnPrimary)
  }

  // Update browser chrome color (mobile)
  let themeColorMeta = document.querySelector<HTMLMetaElement>('meta[name="theme-color"]')
  if (!themeColorMeta) {
    themeColorMeta = document.createElement('meta')
    themeColorMeta.setAttribute('name', 'theme-color')
    document.head.appendChild(themeColorMeta)
  }
  themeColorMeta.setAttribute('content', cfg?.theme.primaryColor ?? DEFAULT_THEME['--color-primary'])
}

export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.hook('page:finish', () => {
    const payload = nuxtApp.payload.data as Record<string, unknown> | undefined
    const cfg = (payload?.['operator-config'] as OperatorConfig | null) ?? null
    applyTheme(cfg)
  })
})
