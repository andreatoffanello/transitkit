import type { Route } from '~/types'

function normalize(s: string): string {
  return s.normalize('NFD').replace(/\p{Mn}/gu, '').toLowerCase()
}

/**
 * Pure function for filtering routes by transit type and/or search query.
 * Used by lines/index.vue and its unit tests.
 */
export function filterRoutes(
  routes: Route[],
  selectedType: string | null,
  searchQuery: string,
): Route[] {
  let result = routes
  if (selectedType) {
    result = result.filter(r => r.transitType === selectedType)
  }
  const q = normalize(searchQuery.trim())
  if (q) {
    result = result.filter(
      r =>
        normalize(r.name).includes(q) ||
        (r.longName !== undefined && normalize(r.longName).includes(q)),
    )
  }
  return result
}

export function sortRoutes(routes: Route[], locale?: string): Route[] {
  return [...routes].sort((a, b) =>
    a.name.localeCompare(b.name, locale, { numeric: true, sensitivity: 'base' })
  )
}
