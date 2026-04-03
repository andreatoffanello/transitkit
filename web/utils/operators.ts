/**
 * Mappa host → operatorId.
 * Aggiungere una riga per ogni nuovo operatore o dominio custom.
 * Ogni modifica richiede un redeploy Vercel.
 */
const OPERATOR_HOSTS: Record<string, string> = {
  'appalcart.transitkit.app': 'appalcart',
  'fermate.appalcart.com': 'appalcart',
  // 'fermate.tcat.org': 'tcat',
}

/**
 * URL base del CDN dati (GitHub Pages transitkit-data repo).
 * In development, set CDN_BASE env var to override (e.g. http://localhost:3010/mock).
 */
export const CDN_BASE = process.env.CDN_BASE ?? 'https://andreatoffanello.github.io/transitkit-data'

export function resolveOperatorId(host: string): string | null {
  if (OPERATOR_HOSTS[host]) return OPERATOR_HOSTS[host] ?? null

  // In development: fall back to NUXT_OPERATOR env var, or the first registered operator
  if (import.meta.dev) {
    const devOp = process.env.NUXT_OPERATOR
    if (devOp) return devOp
    const first = Object.values(OPERATOR_HOSTS)[0]
    return first ?? null
  }

  return null
}
