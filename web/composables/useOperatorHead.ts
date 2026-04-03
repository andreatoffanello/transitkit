import type { Ref } from 'vue'
import type { OperatorConfig } from '~/types'

export function useOperatorHead(config: Ref<OperatorConfig | null>) {
  const ogImageUrl = computed(() => {
    const color = encodeURIComponent(config.value?.theme.primaryColor ?? '#003366')
    const name = encodeURIComponent(config.value?.fullName ?? config.value?.name ?? 'Transit')
    return `/og.svg?color=${color}&name=${name}`
  })

  useHead({
    htmlAttrs: { lang: computed(() => config.value?.locale[0] ?? 'it') },
    meta: [
      { name: 'theme-color', content: computed(() => config.value?.theme.primaryColor ?? '#003366') },
      { name: 'theme-color', content: '#111827', media: '(prefers-color-scheme: dark)' },
      { name: 'apple-mobile-web-app-title', content: computed(() => config.value?.name ?? 'Transit') },
      { property: 'og:image', content: ogImageUrl },
      { property: 'og:image:type', content: 'image/svg+xml' },
      { property: 'og:image:width', content: '1200' },
      { property: 'og:image:height', content: '630' },
    ],
  })
}
