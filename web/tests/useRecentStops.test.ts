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

  it('addStop() saves exactly 3 stops when exactly 3 are added', () => {
    const { recentStops, addStop } = useRecentStops()
    addStop({ stopId: 'a', name: 'Stop A' })
    addStop({ stopId: 'b', name: 'Stop B' })
    addStop({ stopId: 'c', name: 'Stop C' })
    expect(recentStops.value).toHaveLength(3)
    expect(recentStops.value[0]?.stopId).toBe('c')
    expect(recentStops.value[1]?.stopId).toBe('b')
    expect(recentStops.value[2]?.stopId).toBe('a')
  })

  it('addStop() removes oldest when 4th stop is added (4th stop becomes first)', () => {
    const { recentStops, addStop } = useRecentStops()
    addStop({ stopId: 'a', name: 'A' })
    addStop({ stopId: 'b', name: 'B' })
    addStop({ stopId: 'c', name: 'C' })
    addStop({ stopId: 'd', name: 'D' })
    expect(recentStops.value).toHaveLength(3)
    expect(recentStops.value[0]?.stopId).toBe('d')
    expect(recentStops.value[1]?.stopId).toBe('c')
    expect(recentStops.value[2]?.stopId).toBe('b')
    // 'a' is removed
    expect(recentStops.value.map(s => s.stopId)).not.toContain('a')
  })

  it('addStop() maintains length 3 after cap is reached (5th+ stops)', () => {
    const { recentStops, addStop } = useRecentStops()
    addStop({ stopId: 'a', name: 'A' })
    addStop({ stopId: 'b', name: 'B' })
    addStop({ stopId: 'c', name: 'C' })
    addStop({ stopId: 'd', name: 'D' })
    addStop({ stopId: 'e', name: 'E' })
    addStop({ stopId: 'f', name: 'F' })
    expect(recentStops.value).toHaveLength(3)
    expect(recentStops.value[0]?.stopId).toBe('f')
    expect(recentStops.value[1]?.stopId).toBe('e')
    expect(recentStops.value[2]?.stopId).toBe('d')
    // only 'd', 'e', 'f' remain
    expect(recentStops.value.map(s => s.stopId)).toEqual(['f', 'e', 'd'])
  })

  it('load() returns to empty length after multiple adds that cap out and dedup', () => {
    const { recentStops, addStop, load } = useRecentStops()
    // Fill to capacity
    addStop({ stopId: 'a', name: 'A' })
    addStop({ stopId: 'b', name: 'B' })
    addStop({ stopId: 'c', name: 'C' })
    expect(recentStops.value).toHaveLength(3)

    // Now try adding a duplicate of 'a' (should move to front, not exceed 3)
    addStop({ stopId: 'a', name: 'A Updated' })
    expect(recentStops.value).toHaveLength(3)
    expect(recentStops.value[0]?.stopId).toBe('a')
    expect(recentStops.value[0]?.name).toBe('A Updated')
  })

  it('load() falls back to [] when localStorage contains invalid JSON', () => {
    const { recentStops, load } = useRecentStops()
    localStorageMock.setItem('recentStops', 'not-valid-json{{{')
    load()
    expect(recentStops.value).toEqual([])
  })

  it('load() restores stops from localStorage after cold start', () => {
    const { addStop, load, recentStops } = useRecentStops()
    // Step 1: Populate storage
    addStop({ stopId: 'x', name: 'Stop X' })
    expect(recentStops.value).toHaveLength(1)

    // Step 2: Simulate cold start — clear in-memory state by loading from empty store
    const savedData = localStorageMock.getItem('recentStops')
    localStorageMock.clear()
    load() // now recentStops.value === []
    expect(recentStops.value).toHaveLength(0)

    // Step 3: Restore storage and reload — proves load() reads from storage
    if (savedData) localStorageMock.setItem('recentStops', savedData)
    load()
    expect(recentStops.value).toHaveLength(1)
    expect(recentStops.value[0]?.stopId).toBe('x')
  })
})
