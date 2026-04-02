import { CDN_BASE } from '~/utils/operators'
import type { OperatorConfig, ScheduleData } from '~/types'

export async function useOperator() {
  const operatorId = useState<string>('operatorId')
  const id = operatorId.value

  const { data: config } = await useAsyncData<OperatorConfig>(
    `config-${id}`,
    () => $fetch<OperatorConfig>(`${CDN_BASE}/${id}/config.json`),
  )

  const { data: schedules } = await useAsyncData<ScheduleData>(
    `schedules-${id}`,
    () => $fetch<ScheduleData>(`${CDN_BASE}/${id}/schedules.json`),
  )

  return { operatorId, config, schedules }
}
