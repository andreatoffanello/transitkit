import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { useFavoriteStops } from '~/composables/useFavoriteStops'

const localStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: (key: string) => store[key] ?? null,
    setItem: (key: string, value: string) => { store[key] = value },
    removeItem: (key: string) => { delete store[key] },
    clear: () => { store = {} },
  }
})()

describe('useFavoriteStops', () => {
  beforeEach(() => {
    localStorageMock.clear()
    vi.stubGlobal('localStorage', localStorageMock)
    vi.stubGlobal('window', { localStorage: localStorageMock })
    const { load } = useFavoriteStops()
    load()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('load() initializes to empty array when localStorage is empty', () => {
    const { favoriteStops, load } = useFavoriteStops()
    load()
    expect(favoriteStops.value).toEqual([])
  })

  it('toggleFavorite() adds a stop when not already favorite', () => {
    const { favoriteStops, toggleFavorite } = useFavoriteStops()
    toggleFavorite({ stopId: 'a', name: 'Stop A' })
    expect(favoriteStops.value).toHaveLength(1)
    expect(favoriteStops.value[0]).toEqual({ stopId: 'a', name: 'Stop A' })
  })

  it('toggleFavorite() removes a stop when already favorite', () => {
    const { favoriteStops, toggleFavorite } = useFavoriteStops()
    toggleFavorite({ stopId: 'a', name: 'Stop A' })
    toggleFavorite({ stopId: 'a', name: 'Stop A' })
    expect(favoriteStops.value).toHaveLength(0)
  })

  it('isFavorite() returns true for favorited stop', () => {
    const { toggleFavorite, isFavorite } = useFavoriteStops()
    toggleFavorite({ stopId: 'x', name: 'X' })
    expect(isFavorite('x')).toBe(true)
  })

  it('isFavorite() returns false for non-favorited stop', () => {
    const { isFavorite } = useFavoriteStops()
    expect(isFavorite('nothere')).toBe(false)
  })

  it('toggleFavorite() caps at MAX_FAVORITES (10)', () => {
    const { favoriteStops, toggleFavorite } = useFavoriteStops()
    for (let i = 0; i < 12; i++) {
      toggleFavorite({ stopId: `s${i}`, name: `Stop ${i}` })
    }
    expect(favoriteStops.value).toHaveLength(10)
  })

  it('exactly 10 favorites are all saved at MAX_FAVORITES boundary', () => {
    const { favoriteStops, toggleFavorite } = useFavoriteStops()
    for (let i = 0; i < 10; i++) {
      toggleFavorite({ stopId: `s${i}`, name: `Stop ${i}` })
    }
    expect(favoriteStops.value).toHaveLength(10)
    expect(favoriteStops.value.map(s => s.stopId)).toEqual([
      's0', 's1', 's2', 's3', 's4', 's5', 's6', 's7', 's8', 's9'
    ])
  })

  it('11th favorite toggle is ignored when already at MAX_FAVORITES', () => {
    const { favoriteStops, toggleFavorite } = useFavoriteStops()
    for (let i = 0; i < 10; i++) {
      toggleFavorite({ stopId: `s${i}`, name: `Stop ${i}` })
    }
    // Attempt to add 11th favorite
    toggleFavorite({ stopId: 's10', name: 'Stop 10' })
    expect(favoriteStops.value).toHaveLength(10)
    expect(favoriteStops.value.map(s => s.stopId)).toEqual([
      's0', 's1', 's2', 's3', 's4', 's5', 's6', 's7', 's8', 's9'
    ])
  })

  it('isFavorite() returns true for item at position 10 (last before cap)', () => {
    const { toggleFavorite, isFavorite } = useFavoriteStops()
    for (let i = 0; i < 10; i++) {
      toggleFavorite({ stopId: `s${i}`, name: `Stop ${i}` })
    }
    // 10th item (index 9, stopId 's9') should be favorite
    expect(isFavorite('s9')).toBe(true)
    // 11th item that would exceed cap should not be favorite
    expect(isFavorite('s10')).toBe(false)
  })

  it('load() falls back to [] when localStorage contains invalid JSON', () => {
    const { favoriteStops, load } = useFavoriteStops()
    localStorageMock.setItem('favoriteStops', '{"broken":')
    load()
    expect(favoriteStops.value).toEqual([])
  })

  it('toggleFavorite() persists add to localStorage', () => {
    const { toggleFavorite } = useFavoriteStops()
    toggleFavorite({ stopId: 'b', name: 'Stop B' })
    const raw = localStorageMock.getItem('favoriteStops')
    expect(raw).not.toBeNull()
    const parsed = JSON.parse(raw!)
    expect(parsed).toEqual([{ stopId: 'b', name: 'Stop B' }])
  })

  it('load() restores favorites from localStorage (cold-start)', () => {
    const { toggleFavorite } = useFavoriteStops()
    toggleFavorite({ stopId: 'c', name: 'Stop C' })
    const saved = localStorageMock.getItem('favoriteStops')!
    // Simulate cold start: wipe storage → reset singleton to []
    localStorageMock.clear()
    const { load, favoriteStops } = useFavoriteStops()
    load()
    expect(favoriteStops.value).toHaveLength(0)
    // Restore persisted data (as if app re-opened with previous session)
    localStorageMock.setItem('favoriteStops', saved)
    load()
    expect(favoriteStops.value).toHaveLength(1)
    expect(favoriteStops.value[0]!.stopId).toBe('c')
  })
})
