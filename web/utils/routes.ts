import type { Route } from '~/types'

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
  const q = searchQuery.trim().toLowerCase()
  if (q) {
    result = result.filter(
      r =>
        r.name.toLowerCase().includes(q) ||
        r.longName?.toLowerCase().includes(q),
    )
  }
  return result
}
