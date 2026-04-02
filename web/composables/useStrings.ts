import { getStrings } from '~/utils/strings'
import type { AppStrings } from '~/utils/strings'
import type { OperatorConfig } from '~/types'

/**
 * Returns the app strings for the current operator's locale.
 * Call after useOperator() so config is populated.
 */
export function useStrings(config: Ref<OperatorConfig | null>): ComputedRef<AppStrings> {
  return computed(() => getStrings(config.value?.locale[0]))
}
