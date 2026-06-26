import type { Ref } from 'vue'
import type { OperatorConfig } from '~/types'

export function useOperatorHead(config: Ref<OperatorConfig | null | undefined>) {
  const ogImageUrl = computed(() => {
    const color = encodeURIComponent(config.value?.theme.primaryColor ?? '#003366')
    const name = encodeURIComponent(config.value?.brandName ?? config.value?.name ?? 'Transit')
    return `/og.svg?color=${color}&name=${name}`
  })

  // Inject CSS variables in the <head> so both SSR and client hydration see the same
  // values for var(--color-primary) etc. without relying on dynamic :style bindings
  // that would create SSR/client mismatches.
  useHead({
    htmlAttrs: { lang: computed(() => config.value?.locale[0] ?? 'en') },
    style: [
      {
        innerHTML: computed(() => {
          // Parity con l'app nativa: il tint interattivo principale è
          // l'ACCENT dell'operatore (iOS usa `AppTheme.accent` ~130× come tint
          // globale; il primary è quasi inutilizzato). Il web mappa quindi il
          // suo `--color-primary` (usato ovunque come tinta) sull'accentColor,
          // e tiene il primaryColor come `--color-accent` per gli usi profondi.
          const accent = config.value?.theme.accentColor ?? '#06845c'
          const primary = config.value?.theme.primaryColor ?? '#165f9c'
          const textOnPrimary = config.value?.theme.textOnPrimary ?? '#ffffff'
          return `:root { --color-primary: ${accent}; --color-accent: ${primary}; --color-text-on-primary: ${textOnPrimary}; }`
        }),
      },
    ],
    meta: [
      { name: 'theme-color', content: computed(() => config.value?.theme.accentColor ?? '#06845c') },
      { name: 'theme-color', content: '#111827', media: '(prefers-color-scheme: dark)' },
      { name: 'apple-mobile-web-app-title', content: computed(() => config.value?.brandName ?? config.value?.name ?? 'Transit') },
      { property: 'og:image', content: ogImageUrl },
      { property: 'og:image:type', content: 'image/svg+xml' },
      { property: 'og:image:width', content: '1200' },
      { property: 'og:image:height', content: '630' },
    ],
  })
}
