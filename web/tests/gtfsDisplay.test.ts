import { describe, it, expect } from 'vitest'
import {
  normalizeDirectionSuffix,
  normalizeAcronyms,
  normalizeGtfsDisplay,
  GTFS_ACRONYMS,
} from '~/utils/gtfsDisplay'

describe('normalizeDirectionSuffix', () => {
  it('uppercases (up)', () => {
    expect(normalizeDirectionSuffix('Main St (up)')).toBe('Main St (Up)')
  })

  it('uppercases (down)', () => {
    expect(normalizeDirectionSuffix('Main St (down)')).toBe('Main St (Down)')
  })

  it('handles already-correct casing idempotently', () => {
    expect(normalizeDirectionSuffix('Main St (Up)')).toBe('Main St (Up)')
    expect(normalizeDirectionSuffix('Main St (Down)')).toBe('Main St (Down)')
  })

  it('handles ALL-CAPS variant', () => {
    expect(normalizeDirectionSuffix('Route 1 (UP)')).toBe('Route 1 (Up)')
  })

  it('handles mixed casing', () => {
    expect(normalizeDirectionSuffix('Loop (dOwN)')).toBe('Loop (Down)')
  })

  it('does not touch unrelated parentheses', () => {
    expect(normalizeDirectionSuffix('Stop (A)')).toBe('Stop (A)')
  })

  it('handles suffix with spaces inside parens', () => {
    expect(normalizeDirectionSuffix('Stop ( up )')).toBe('Stop (Up)')
  })
})

describe('normalizeAcronyms', () => {
  it('uppercases Asu → ASU', () => {
    expect(normalizeAcronyms('Asu Src')).toBe('ASU SRC')
  })

  it('does not touch partial word matches (Sassy, Ascii)', () => {
    expect(normalizeAcronyms('Sassy Ascii')).toBe('Sassy Ascii')
  })

  it('is idempotent when already uppercase', () => {
    expect(normalizeAcronyms('ASU Campus')).toBe('ASU Campus')
  })

  it('handles lowercase version', () => {
    expect(normalizeAcronyms('asu campus')).toBe('ASU campus')
  })

  it('accepts a custom acronym list', () => {
    expect(normalizeAcronyms('Foo bar', ['FOO'])).toBe('FOO bar')
  })
})

describe('normalizeGtfsDisplay', () => {
  it('applies both normalizations: "Asu Src (down)" → "ASU SRC (Down)"', () => {
    expect(normalizeGtfsDisplay('Asu Src (down)')).toBe('ASU SRC (Down)')
  })

  it('real-world example: "Asu Src (Up)" stays "ASU SRC (Up)"', () => {
    expect(normalizeGtfsDisplay('Asu Src (Up)')).toBe('ASU SRC (Up)')
  })

  it('passes through clean strings unchanged', () => {
    expect(normalizeGtfsDisplay('Boone Mall')).toBe('Boone Mall')
  })
})

describe('GTFS_ACRONYMS list', () => {
  it('contains ASU and SRC at minimum', () => {
    expect(GTFS_ACRONYMS).toContain('ASU')
    expect(GTFS_ACRONYMS).toContain('SRC')
  })
})
