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
 */
export const CDN_BASE = 'https://andreatoffanello.github.io/transitkit-data'

export function resolveOperatorId(host: string): string | null {
  return OPERATOR_HOSTS[host] ?? null
}
