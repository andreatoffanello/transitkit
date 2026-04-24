import type { OperatorConfig, ScheduleData } from '~/types'
import { fetchOperatorConfig } from './useOperatorConfig'
import { fetchOperatorSchedules } from './useOperatorSchedule'

// Module-level controller: aborts in-flight CDN fetches on rapid re-navigation.
let currentAbortController: AbortController | null = null

export async function useOperator() {
  const operatorId = useState<string>('operatorId')
  const id = operatorId.value
  const { public: { cdnBase } } = useRuntimeConfig()

  // Cancel any in-flight fetch session from a previous navigation
  currentAbortController?.abort()
  currentAbortController = new AbortController()
  const { signal } = currentAbortController

  // Use stable keys that don't depend on hostname or dynamic state.
  // The operator ID is scoped per-request via the CDN URL inside the fetcher;
  // using a fixed key ensures the SSR payload key always matches the client
  // hydration key, preventing hydration style mismatches on theme :style bindings.
  const nuxtApp = useNuxtApp()
  const getCachedData = <T>(key: string) =>
    (nuxtApp.payload.data as Record<string, T>)[key] ?? (nuxtApp.static.data as Record<string, T>)[key]

  const configAsync = useAsyncData<OperatorConfig>(
    'operator-config',
    () => fetchOperatorConfig(cdnBase, id, signal),
    { getCachedData: key => getCachedData<OperatorConfig>(key) },
  )
  const schedulesAsync = useAsyncData<ScheduleData>(
    'operator-schedules',
    () => fetchOperatorSchedules(cdnBase, id, signal),
    { getCachedData: key => getCachedData<ScheduleData>(key) },
  )
  const { data: config, error: configError } = await configAsync
  const { data: schedules, error: schedulesError } = await schedulesAsync

  // If either critical fetch failed, surface a proper error page.
  // AbortError means navigation moved on — silently ignore, no error page.
  if (configError.value || schedulesError.value) {
    const err = configError.value || schedulesError.value
    if (err instanceof Error && err.name === 'AbortError') {
      currentAbortController = null
      const pending = computed(() => !config.value || !schedules.value)
      return { operatorId, config, schedules, pending }
    }
    const status = (err as { status?: number }).status
    const statusCode = status === 404 ? 404 : 502
    const statusMessage = statusCode === 404
      ? 'Operator not found'
      : 'Impossibile caricare i dati. Riprova tra qualche minuto.'
    throw createError({ statusCode, statusMessage })
  }

  currentAbortController = null

  const pending = computed(() => !config.value || !schedules.value)

  return { operatorId, config, schedules, pending }
}
