import type { Ref } from 'vue'
import type { OperatorConfig } from '~/types'

export function useOperatorHead(config: Ref<OperatorConfig | null>) {
  useHead({
    meta: [
      { name: 'theme-color', content: computed(() => config.value?.theme.primaryColor ?? '#003366') },
      { name: 'apple-mobile-web-app-title', content: computed(() => config.value?.name ?? 'Transit') },
    ],
  })
}
