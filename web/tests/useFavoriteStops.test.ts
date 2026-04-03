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
})
