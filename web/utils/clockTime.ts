/**
 * formatClockTime — convert a GTFS "HH:MM" or "HH:MM:SS" string to a
 * locale-appropriate clock display.
 *
 * Rules:
 *  - locale starting with 'en' → 12-hour AM/PM, no leading zero ("1:33 PM",
 *    "12:05 AM"). Hour=0 maps to "12:…AM", hour=12 maps to "12:…PM".
 *  - other locales → unchanged "HH:MM" (24-hour).
 *  - GTFS times can exceed 24 h (e.g. "25:10" = next-day 01:10) — handled
 *    by wrapping the hour modulo 24 before displaying.
 *
 * @param hhmm   GTFS time string: "HH:MM" or "HH:MM:SS".
 * @param locale Optional BCP-47 locale or language tag.  Defaults to 24-hour.
 */
export function formatClockTime(hhmm: string, locale?: string): string {
  if (!hhmm) return hhmm

  // Parse — accept "HH:MM" and "HH:MM:SS"
  const parts = hhmm.split(':')
  const rawHour = parseInt(parts[0] ?? '', 10)
  const rawMin = parseInt(parts[1] ?? '', 10)

  if (Number.isNaN(rawHour) || Number.isNaN(rawMin)) return hhmm

  // Wrap GTFS overflow hours (25:10 → 01:10)
  const hour24 = rawHour % 24
  const mm = String(rawMin).padStart(2, '0')

  const lang = (locale ?? '').split('-')[0]?.toLowerCase()

  if (lang === 'en') {
    const period = hour24 < 12 ? 'AM' : 'PM'
    const hour12 = hour24 % 12 === 0 ? 12 : hour24 % 12
    return `${hour12}:${mm} ${period}`
  }

  // Non-English: return 24-hour "HH:MM"
  return `${String(hour24).padStart(2, '0')}:${mm}`
}
