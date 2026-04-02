import { CDN_BASE } from '~/utils/operators'
import type { OperatorConfig, ScheduleData } from '~/types'

export async function useOperator() {
  const operatorId = useState<string>('operatorId')
  const id = operatorId.value

  const { data: config, error: configError } = await useAsyncData<OperatorConfig>(
    `config-${id}`,
    () => $fetch(`${CDN_BASE}/${id}/config.json`),
  )

  const { data: schedules, error: schedulesError } = await useAsyncData<ScheduleData>(
    `schedules-${id}`,
    () => $fetch(`${CDN_BASE}/${id}/schedules.json`),
  )

  // If either critical fetch failed, surface a proper error page
  if (configError.value || schedulesError.value) {
    throw createError({
      statusCode: 502,
      statusMessage: 'Impossibile caricare i dati. Riprova tra qualche minuto.',
      fatal: true,
    })
  }

  const pending = computed(() => !config.value || !schedules.value)

  return { operatorId, config, schedules, pending }
}
