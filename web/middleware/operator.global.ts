import { resolveOperatorId } from '~/utils/operators'

export default defineNuxtRouteMiddleware(() => {
  const host = useRequestURL().hostname
  const operatorId = resolveOperatorId(host)

  if (!operatorId) {
    throw createError({
      statusCode: 404,
      statusMessage: `No operator configured for host: ${host}`,
    })
  }

  // Rende operatorId disponibile in tutto il render tree via useState
  useState<string>('operatorId', () => operatorId).value = operatorId
})
