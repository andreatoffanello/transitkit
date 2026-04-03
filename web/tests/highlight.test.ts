import { describe, it, expect } from 'vitest'
import { highlightMatch, escapeHtml } from '~/utils/highlight'

describe('highlightMatch', () => {
  it('wraps exact match in span', () => {
    const result = highlightMatch('Centro Storico', 'centro')
    expect(result).toContain('<span')
    expect(result).toContain('Centro')
  })

  it('returns original text when no match', () => {
    const result = highlightMatch('Linea 3', 'xyz')
    expect(result).toBe('Linea 3')
  })

  it('empty query returns original text', () => {
    const result = highlightMatch('Linea 3', '')
    expect(result).toBe('Linea 3')
  })

  it('NFD: query with accent finds text without accent', () => {
    const result = highlightMatch('Estero', 'éster')
    expect(result).toContain('<span')
  })

  it('NFD: query without accent finds text with accent', () => {
    const result = highlightMatch('Été', 'ete')
    expect(result).toContain('<span')
  })

  it('query longer than text returns original text unchanged', () => {
    const result = highlightMatch('ABC', 'ABCDEFG')
    expect(result).toBe('ABC')
    expect(result).not.toContain('<span')
  })

  it('query equals entire text wraps everything in span', () => {
    const result = highlightMatch('Centro', 'centro')
    expect(result).toBe('<span class="font-semibold text-gray-900 dark:text-white">Centro</span>')
  })

  it('empty text with non-empty query returns empty string', () => {
    const result = highlightMatch('', 'test')
    expect(result).toBe('')
    expect(result).not.toContain('<span')
  })
})

describe('escapeHtml', () => {
  it('escapes < and >', () => {
    expect(escapeHtml('<b>')).toBe('&lt;b&gt;')
  })

  it('escapes &', () => {
    expect(escapeHtml('a & b')).toBe('a &amp; b')
  })

  it('XSS: text with < outside match is escaped in highlightMatch output', () => {
    const result = highlightMatch('<script>alert(1)</script> Centro', 'centro')
    expect(result).not.toContain('<script>')
    expect(result).toContain('&lt;script&gt;')
  })
})
