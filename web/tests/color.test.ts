import { describe, it, expect } from 'vitest'
import { normalizeHex } from '~/utils/color'

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
