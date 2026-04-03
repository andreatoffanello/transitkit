import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { useRecentStops } from '~/composables/useRecentStops'

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: (key: string) => store[key] ?? null,
    setItem: (key: string, value: string) => { store[key] = value },
    removeItem: (key: string) => { delete store[key] },
    clear: () => { store = {} },
  }
})()

describe('useRecentStops', () => {
  beforeEach(() => {
    localStorageMock.clear()
    vi.stubGlobal('localStorage', localStorageMock)
    // Ensure typeof window !== 'undefined' so composable guard passes in Node env
    vi.stubGlobal('window', { localStorage: localStorageMock })
    // Reset singleton by loading empty storage
    const { load } = useRecentStops()
    load()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('load() initializes to empty array when localStorage is empty', () => {
    const { recentStops, load } = useRecentStops()
    load()
    expect(recentStops.value).toEqual([])
  })

  it('addStop() prepends new stop', () => {
    const { recentStops, addStop } = useRecentStops()
    addStop({ stopId: 'a', name: 'Stop A' })
    addStop({ stopId: 'b', name: 'Stop B' })
    expect(recentStops.value[0]).toEqual({ stopId: 'b', name: 'Stop B' })
    expect(recentStops.value[1]).toEqual({ stopId: 'a', name: 'Stop A' })
  })

  it('addStop() deduplicates by stopId (moves to front)', () => {
    const { recentStops, addStop } = useRecentStops()
    addStop({ stopId: 'a', name: 'Stop A' })
    addStop({ stopId: 'b', name: 'Stop B' })
    addStop({ stopId: 'a', name: 'Stop A Updated' })
    expect(recentStops.value).toHaveLength(2)
    expect(recentStops.value[0]).toEqual({ stopId: 'a', name: 'Stop A Updated' })
  })

  it('addStop() caps at MAX_STOPS (3)', () => {
    const { recentStops, addStop } = useRecentStops()
    addStop({ stopId: 'a', name: 'A' })
    addStop({ stopId: 'b', name: 'B' })
    addStop({ stopId: 'c', name: 'C' })
    addStop({ stopId: 'd', name: 'D' })
    expect(recentStops.value).toHaveLength(3)
    expect(recentStops.value[0]?.stopId).toBe('d')
  })

  it('load() restores persisted stops from localStorage', () => {
    const { addStop, load, recentStops } = useRecentStops()
    addStop({ stopId: 'x', name: 'Stop X' })
    // Simulate fresh load (singleton already updated, just verify persistence)
    load()
    expect(recentStops.value[0]?.stopId).toBe('x')
  })
})
