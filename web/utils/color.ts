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

/**
 * WCAG relative luminance (0 = black … 1 = white) of a hex color.
 * Accepts 3- or 6-digit hex with or without '#'. Returns NaN on unparseable input.
 */
export function relativeLuminance(color: string): number {
  const clean = normalizeHex(color).replace('#', '')
  const full = clean.length === 3 ? clean.split('').map(c => c + c).join('') : clean
  if (full.length !== 6) return NaN
  const chan = (i: number) => {
    const c = parseInt(full.slice(i, i + 2), 16) / 255
    return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4)
  }
  const r = chan(0), g = chan(2), b = chan(4)
  if (!Number.isFinite(r) || !Number.isFinite(g) || !Number.isFinite(b)) return NaN
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

/**
 * Returns `dark` or `light` — whichever has the higher WCAG contrast ratio
 * against `bg`. Use for line badges / chips when the GTFS feed provides no
 * explicit `route_text_color` (many operators leave it blank): white-on-light
 * routes are otherwise illegible (e.g. white on `#B9CCE2` ≈ 1.6:1).
 */
export function readableTextColor(bg: string, dark = '#000000', light = '#ffffff'): string {
  const L = relativeLuminance(bg)
  if (!Number.isFinite(L)) return light
  const contrastWhite = 1.05 / (L + 0.05)
  const contrastBlack = (L + 0.05) / 0.05
  return contrastBlack >= contrastWhite ? dark : light
}
