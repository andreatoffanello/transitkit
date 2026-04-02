/**
 * Normalizes a color string to a valid CSS hex color.
 * Handles inputs with or without the '#' prefix.
 * Falls back to the provided fallback if input is falsy.
 */
export function normalizeHex(color: string | undefined | null, fallback = '#000000'): string {
  if (!color) return fallback
  const clean = color.replace('#', '')
  return `#${clean}`
}
