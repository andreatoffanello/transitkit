export interface RecentStop {
  stopId: string
  name: string
}

const STORAGE_KEY = 'recentStops'
const MAX_STOPS = 3
const recentStops = ref<RecentStop[]>([])

export function useRecentStops() {

  function load() {
    if (typeof window === 'undefined') return
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      recentStops.value = raw ? (JSON.parse(raw) as RecentStop[]) : []
    } catch {
      recentStops.value = []
    }
  }

  function addStop(stop: RecentStop) {
    if (typeof window === 'undefined') return
    const current = recentStops.value.filter(s => s.stopId !== stop.stopId)
    const updated = [stop, ...current].slice(0, MAX_STOPS)
    recentStops.value = updated
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(updated))
    } catch { /* storage full or unavailable */ }
  }

  return { recentStops, load, addStop }
}
