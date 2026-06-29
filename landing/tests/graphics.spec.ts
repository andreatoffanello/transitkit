import { describe, it, expect } from 'vitest'
import { resolveTier } from '../app/stores/graphics'

describe('resolveTier', () => {
  it('returns static when reduced motion is preferred', () => {
    expect(resolveTier({ reducedMotion: true, webgl: true, deviceMemory: 8, coarsePointer: false })).toBe('static')
  })
  it('returns static when WebGL is unavailable', () => {
    expect(resolveTier({ reducedMotion: false, webgl: false, deviceMemory: 8, coarsePointer: false })).toBe('static')
  })
  it('returns low on coarse pointer', () => {
    expect(resolveTier({ reducedMotion: false, webgl: true, deviceMemory: 8, coarsePointer: true })).toBe('low')
  })
  it('returns low on low device memory', () => {
    expect(resolveTier({ reducedMotion: false, webgl: true, deviceMemory: 2, coarsePointer: false })).toBe('low')
  })
  it('returns high on a capable desktop', () => {
    expect(resolveTier({ reducedMotion: false, webgl: true, deviceMemory: 8, coarsePointer: false })).toBe('high')
  })
})
