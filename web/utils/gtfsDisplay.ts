/**
 * gtfsDisplay — Display normalization for raw GTFS strings.
 *
 * Two targeted interventions only (no generic title-casing):
 *   a) Normalize direction suffixes "(up)" / "(down)" → "(Up)" / "(Down)"
 *   b) Uppercase known acronyms that appear as isolated title-case words
 */

/** Acronyms that GTFS sometimes emits as title-case (e.g. "Asu", "Src"). */
export const GTFS_ACRONYMS: readonly string[] = ['ASU', 'SRC', 'UNC', 'YMCA', 'IRT', 'CBD', 'HOV', 'BRT']

/**
 * Normalize GTFS direction suffix casing.
 * "(up)" → "(Up)", "(down)" → "(Down)"
 * Suffixes already correctly cased are left untouched.
 */
export function normalizeDirectionSuffix(input: string): string {
  return input.replace(/\(\s*(up|down)\s*\)/gi, (_, dir: string) => {
    const normalized = dir.charAt(0).toUpperCase() + dir.slice(1).toLowerCase()
    return `(${normalized})`
  })
}

/**
 * Uppercase known acronyms that appear as isolated title-case words.
 * "Asu Src" → "ASU SRC", "Asu" → "ASU", "Sassy" (not in list) → "Sassy"
 * Match is word-boundary aware; case-insensitive for the source token.
 */
export function normalizeAcronyms(input: string, acronyms: readonly string[] = GTFS_ACRONYMS): string {
  let result = input
  for (const acronym of acronyms) {
    // Match the acronym as a whole word, case-insensitive
    const pattern = new RegExp(`\\b${acronym}\\b`, 'gi')
    result = result.replace(pattern, acronym)
  }
  return result
}

/**
 * Apply both normalizations in sequence.
 * Safe to call on any headsign / stop name string.
 */
export function normalizeGtfsDisplay(input: string): string {
  return normalizeAcronyms(normalizeDirectionSuffix(input))
}
