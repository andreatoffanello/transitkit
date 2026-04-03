export function escapeHtml(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

export function normalizeForSearch(s: string): string {
  return s.normalize('NFD').replace(/\p{Mn}/gu, '').toLowerCase()
}

export function highlightMatch(text: string, query: string): string {
  if (!query) return escapeHtml(text)
  const normalizedText = normalizeForSearch(text)
  const normalizedQuery = normalizeForSearch(query.trim())
  if (!normalizedQuery) return escapeHtml(text)
  const start = normalizedText.indexOf(normalizedQuery)
  if (start === -1) return escapeHtml(text)
  const end = start + normalizedQuery.length
  return (
    escapeHtml(text.slice(0, start)) +
    '<span class="font-semibold text-gray-900 dark:text-white">' +
    escapeHtml(text.slice(start, end)) +
    '</span>' +
    escapeHtml(text.slice(end))
  )
}
