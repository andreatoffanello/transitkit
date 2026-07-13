import { describe, it, expect } from 'vitest'
import { normalizeHex, relativeLuminance, readableTextColor } from '~/utils/color'

describe('normalizeHex', () => {
  it('returns color with # when input already has #', () => {
    expect(normalizeHex('#FF5733')).toBe('#FF5733')
  })

  it('adds # prefix when input has no #', () => {
    expect(normalizeHex('FF5733')).toBe('#FF5733')
  })

  it('handles shorthand hex without #', () => {
    expect(normalizeHex('FFF')).toBe('#FFF')
  })

  it('handles shorthand hex with #', () => {
    expect(normalizeHex('#FFF')).toBe('#FFF')
  })

  it('returns default fallback when input is undefined', () => {
    expect(normalizeHex(undefined)).toBe('#000000')
  })

  it('returns default fallback when input is null', () => {
    expect(normalizeHex(null)).toBe('#000000')
  })

  it('returns default fallback when input is empty string', () => {
    expect(normalizeHex('')).toBe('#000000')
  })

  it('uses custom fallback when input is falsy', () => {
    expect(normalizeHex(undefined, '#FFFFFF')).toBe('#FFFFFF')
    expect(normalizeHex(null, '#123456')).toBe('#123456')
    expect(normalizeHex('', '#AABBCC')).toBe('#AABBCC')
  })

  it('handles lowercase hex without #', () => {
    expect(normalizeHex('ff5733')).toBe('#ff5733')
  })

  it('handles lowercase hex with #', () => {
    expect(normalizeHex('#ff5733')).toBe('#ff5733')
  })
})

describe('readableTextColor (WCAG legible fg on line badges)', () => {
  // The real AppalCART route colors that previously rendered illegible white text.
  it('picks BLACK on light route backgrounds', () => {
    expect(readableTextColor('#B9CCE2')).toBe('#000000') // E (Express) — was 1.64:1 white
    expect(readableTextColor('B9CCE2')).toBe('#000000')  // tolerates missing #
    expect(readableTextColor('#F0A73E')).toBe('#000000') // O (Orange)
  })

  it('picks WHITE on dark route backgrounds', () => {
    expect(readableTextColor('#000000')).toBe('#ffffff')
    expect(readableTextColor('#58595B')).toBe('#ffffff') // GY (Gray)
  })

  it('supports custom dark/light tokens', () => {
    expect(readableTextColor('#ffffff', '#111', '#eee')).toBe('#111')
  })

  it('falls back to light on unparseable input (never throws)', () => {
    expect(readableTextColor('nope')).toBe('#ffffff')
    expect(readableTextColor('#12')).toBe('#ffffff')
  })

  it('relativeLuminance: black=0, white=1, NaN on garbage', () => {
    expect(relativeLuminance('#000000')).toBeCloseTo(0, 5)
    expect(relativeLuminance('#ffffff')).toBeCloseTo(1, 5)
    expect(Number.isNaN(relativeLuminance('zzz'))).toBe(true)
  })
})
