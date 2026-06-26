import type { Ref } from 'vue'

// Lazy-load + cache the GTFS-RT FeedMessage type (shared shape with useRealtime).
let feedMessagePromise: Promise<import('protobufjs').Type> | null = null
function getFeedMessage(): Promise<import('protobufjs').Type> {
  if (!feedMessagePromise) {
    feedMessagePromise = import('protobufjs')
      .then(protobuf => protobuf.load('/proto/gtfs-realtime.proto')
        .then(root => root.lookupType('transit_realtime.FeedMessage')))
      .catch(err => { feedMessagePromise = null; throw err })
  }
  return feedMessagePromise
}

/**
 * Client-side composable that polls the GTFS-RT vehicle-positions feed and
 * exposes how many live vehicles each route currently has — drives the
 * "N live" badge on the lines list (parity with iOS `vehicleStore.liveCount`).
 *
 * Vehicles are keyed by tripId (routeId in the feed is usually empty), so we
 * resolve trip → route via `tripRouteIndex`.
 *
 * @param vehiclePositionsUrl  proxy URL for vehicle-positions.pb (may be undefined)
 * @param tripRouteIndex       reactive tripId → routeId map (from buildTripRouteIndex)
 */
export function useLiveVehicles(
  vehiclePositionsUrl: string | undefined,
  tripRouteIndex: Ref<Map<string, string>>,
) {
  const liveCountByRoute = ref<Map<string, number>>(new Map())
  const isLive = ref(false)

  if (!vehiclePositionsUrl || import.meta.server) {
    return { liveCountByRoute, isLive }
  }
  const feedUrl: string = vehiclePositionsUrl

  async function poll() {
    if (typeof document !== 'undefined' && document.hidden) return
    try {
      const res = await fetch(feedUrl, { mode: 'cors' })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const buffer = await res.arrayBuffer()
      const FeedMessage = await getFeedMessage()
      const feed = FeedMessage.decode(new Uint8Array(buffer)) as unknown as GtfsRtVehicleFeed

      const counts = new Map<string, number>()
      const index = tripRouteIndex.value
      for (const entity of feed.entity ?? []) {
        const tripId = entity.vehicle?.trip?.tripId
        if (!tripId) continue
        const routeId = entity.vehicle?.trip?.routeId || index.get(tripId)
        if (!routeId) continue
        counts.set(routeId, (counts.get(routeId) ?? 0) + 1)
      }
      liveCountByRoute.value = counts
      isLive.value = true
    } catch {
      // CORS / 404 / parse error — degrade silently
      isLive.value = false
      liveCountByRoute.value = new Map()
    }
  }

  let timer: ReturnType<typeof setInterval> | undefined
  let onVisibility: (() => void) | undefined

  function restart() {
    if (timer !== undefined) clearInterval(timer)
    timer = setInterval(poll, 20_000)
  }

  onMounted(async () => {
    await poll()
    restart()
    onVisibility = () => {
      if (document.visibilityState === 'visible') { poll(); restart() }
      else if (timer !== undefined) clearInterval(timer)
    }
    document.addEventListener('visibilitychange', onVisibility)
  })
  onUnmounted(() => {
    if (timer !== undefined) clearInterval(timer)
    if (onVisibility) document.removeEventListener('visibilitychange', onVisibility)
  })

  return { liveCountByRoute, isLive }
}

interface GtfsRtVehicleFeed {
  entity?: Array<{
    vehicle?: {
      trip?: { tripId?: string; routeId?: string }
    }
  }>
}
