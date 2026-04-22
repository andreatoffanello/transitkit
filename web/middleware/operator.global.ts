import { resolveOperatorId } from '~/utils/operators'

// Routes che non dipendono da un operatore (legali/standalone).
// Devono renderizzare anche quando l'host non matcha nessun operatore.
const OPERATOR_AGNOSTIC_ROUTES = new Set(['/privacy'])

export default defineNuxtRouteMiddleware((to) => {
  const host = useRequestURL().hostname
  const operatorId = resolveOperatorId(host)

  if (operatorId) {
    useState<string>('operatorId', () => operatorId).value = operatorId
    return
  }

  // Host sconosciuto: se la route è operator-agnostic, lascia passare senza operatorId.
  if (OPERATOR_AGNOSTIC_ROUTES.has(to.path)) {
    useState<string>('operatorId', () => '').value = ''
    return
  }

  throw createError({
    statusCode: 404,
    statusMessage: `No operator configured for host: ${host}`,
  })
})
