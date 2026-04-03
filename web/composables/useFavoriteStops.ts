export interface FavoriteStop {
  stopId: string
  name: string
}

const STORAGE_KEY = 'favoriteStops'
const MAX_FAVORITES = 10
const favoriteStops = ref<FavoriteStop[]>([])

export function useFavoriteStops() {
  function load() {
    if (typeof window === 'undefined') return
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      favoriteStops.value = raw ? (JSON.parse(raw) as FavoriteStop[]) : []
    } catch {
      favoriteStops.value = []
    }
  }

  function toggleFavorite(stop: FavoriteStop) {
    if (typeof window === 'undefined') return
    const idx = favoriteStops.value.findIndex(s => s.stopId === stop.stopId)
    const updated = idx === -1
      ? [...favoriteStops.value, stop].slice(0, MAX_FAVORITES)
      : favoriteStops.value.filter(s => s.stopId !== stop.stopId)
    favoriteStops.value = updated
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(updated))
    } catch { /* storage full */ }
  }

  function isFavorite(stopId: string): boolean {
    return favoriteStops.value.some(s => s.stopId === stopId)
  }

  return { favoriteStops, load, toggleFavorite, isFavorite }
}
