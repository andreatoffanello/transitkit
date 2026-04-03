import { CDN_BASE } from '~/utils/operators'
import { fetchWithRetry } from '~/utils/fetchWithRetry'
import type { OperatorConfig, ScheduleData } from '~/types'

async function fetchJson<T>(url: string): Promise<T> {
  const res = await fetchWithRetry(url)
  if (!res.ok) {
    throw new Error(`HTTP ${res.status} fetching ${url}`)
  }
  return res.json() as Promise<T>
}

export async function useOperator() {
  const operatorId = useState<string>('operatorId')
  const id = operatorId.value

  const { data: config, error: configError } = await useAsyncData<OperatorConfig>(
    `config-${id}`,
    () => fetchJson<OperatorConfig>(`${CDN_BASE}/${id}/config.json`),
  )

  const { data: schedules, error: schedulesError } = await useAsyncData<ScheduleData>(
    `schedules-${id}`,
    () => fetchJson<ScheduleData>(`${CDN_BASE}/${id}/schedules.json`),
  )

  // If either critical fetch failed, surface a proper error page
  if (configError.value || schedulesError.value) {
    throw createError({
      statusCode: 502,
      statusMessage: 'Impossibile caricare i dati. Riprova tra qualche minuto.',
    })
  }

  const pending = computed(() => !config.value || !schedules.value)

  return { operatorId, config, schedules, pending }
}
