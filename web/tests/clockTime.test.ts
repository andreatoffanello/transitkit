import { describe, it, expect } from 'vitest'
import { formatClockTime, formatClockHour } from '~/utils/clockTime'

describe('formatClockTime', () => {
  // === 24-hour (non-en locale) ===
  it('returns HH:MM unchanged for undefined locale', () => {
    expect(formatClockTime('13:33')).toBe('13:33')
  })

  it('returns HH:MM unchanged for IT locale', () => {
    expect(formatClockTime('13:33', 'it')).toBe('13:33')
  })

  it('returns HH:MM unchanged for it-IT locale tag', () => {
    expect(formatClockTime('08:05', 'it-IT')).toBe('08:05')
  })

  // === 12-hour (en locale) ===
  it('formats afternoon time in 12h for en locale', () => {
    expect(formatClockTime('13:33', 'en')).toBe('1:33 PM')
  })

  it('formats morning time without leading zero for en locale', () => {
    expect(formatClockTime('09:07', 'en')).toBe('9:07 AM')
  })

  it('formats midnight (00:05) as 12:05 AM for en locale', () => {
    expect(formatClockTime('00:05', 'en')).toBe('12:05 AM')
  })

  it('formats noon (12:00) as 12:00 PM for en locale', () => {
    expect(formatClockTime('12:00', 'en')).toBe('12:00 PM')
  })

  it('formats 12:30 as 12:30 PM for en locale', () => {
    expect(formatClockTime('12:30', 'en')).toBe('12:30 PM')
  })

  it('formats 23:59 as 11:59 PM for en locale', () => {
    expect(formatClockTime('23:59', 'en')).toBe('11:59 PM')
  })

  it('formats 01:00 as 1:00 AM for en locale', () => {
    expect(formatClockTime('01:00', 'en')).toBe('1:00 AM')
  })

  // === GTFS overflow (>24h) ===
  it('handles GTFS 25:10 as 1:10 AM for en locale (next-day)', () => {
    expect(formatClockTime('25:10', 'en')).toBe('1:10 AM')
  })

  it('handles GTFS 24:00 as 12:00 AM for en locale', () => {
    expect(formatClockTime('24:00', 'en')).toBe('12:00 AM')
  })

  it('handles GTFS 26:30 as 02:30 for it locale', () => {
    expect(formatClockTime('26:30', 'it')).toBe('02:30')
  })

  // === en-US locale tag ===
  it('formats time in 12h for en-US locale tag', () => {
    expect(formatClockTime('15:45', 'en-US')).toBe('3:45 PM')
  })

  // === HH:MM:SS input ===
  it('handles HH:MM:SS input, strips seconds', () => {
    expect(formatClockTime('14:05:30', 'en')).toBe('2:05 PM')
  })

  it('handles HH:MM:SS input for 24-hour locale', () => {
    expect(formatClockTime('14:05:30', 'it')).toBe('14:05')
  })

  // === Edge cases ===
  it('returns empty string unchanged', () => {
    expect(formatClockTime('')).toBe('')
  })

  it('returns malformed string unchanged', () => {
    expect(formatClockTime('not-a-time', 'en')).toBe('not-a-time')
  })
})

describe('formatClockHour (schedule hour dividers)', () => {
  it('en → 12-hour with period, no minutes', () => {
    expect(formatClockHour('13', 'en')).toBe('1 PM')
    expect(formatClockHour('00', 'en')).toBe('12 AM')
    expect(formatClockHour('12', 'en')).toBe('12 PM')
    expect(formatClockHour('07', 'en')).toBe('7 AM')
    expect(formatClockHour('23', 'en')).toBe('11 PM')
  })

  it('non-en → 24-hour zero-padded', () => {
    expect(formatClockHour('13', 'it')).toBe('13')
    expect(formatClockHour('7', 'it')).toBe('07')
    expect(formatClockHour('0', undefined)).toBe('00')
  })

  it('wraps GTFS overflow hours (25 → 1 AM / 01)', () => {
    expect(formatClockHour('25', 'en')).toBe('1 AM')
    expect(formatClockHour('24', 'en')).toBe('12 AM')
    expect(formatClockHour('25', 'it')).toBe('01')
  })

  it('accepts a number and tolerates garbage', () => {
    expect(formatClockHour(15, 'en')).toBe('3 PM')
    expect(formatClockHour('xx', 'en')).toBe('xx')
  })
})
