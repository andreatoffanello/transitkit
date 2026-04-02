import type { Ref } from 'vue'
import type { Departure } from '~/types'

/**
 * Pure merge function — exported for unit testing.
 * Maps tripId → delay (seconds) onto the static departures list.
 * Returns a new array; does not mutate the input.
 */
export function mergeRealtimeDelays(
  departures: Departure[],
  delays: Record<string, number>,
): Departure[] {
  return departures.map(dep => {
    if (!dep.tripId || !(dep.tripId in delays)) return dep
    return {
      ...dep,
      realtimeDelay: delays[dep.tripId],
      isRealtime: true,
    }
  })
}

// Cache the FeedMessage type — loaded once, reused on every poll
let feedMessagePromise: Promise<import('protobufjs').Type> | null = null

function getFeedMessage(): Promise<import('protobufjs').Type> {
  if (!feedMessagePromise) {
    feedMessagePromise = import('protobufjs').then(protobuf =>
      protobuf.load('/proto/gtfs-realtime.proto').then(root =>
        root.lookupType('transit_realtime.FeedMessage'),
      ),
    )
  }
  return feedMessagePromise
}

/**
 * Client-side composable for GTFS-RT polling.
 * Polls every 30s; falls back silently on CORS/404/parse errors.
 *
 * @param departures - Reactive ref of static departures (input)
 * @param gtfsRtUrl  - URL for the trip_updates protobuf feed (may be undefined)
 */
export function useRealtime(
  departures: Ref<Departure[]>,
  gtfsRtUrl: string | undefined,
) {
  const isLive = ref(false)
  const merged = ref<Departure[]>([...departures.value])

  // Track the last known delay map so the watch can re-apply it
  let lastDelays: Record<string, number> = {}

  // Keep merged in sync with static departures; re-apply delays when live
  watch(departures, (deps) => {
    merged.value = isLive.value
      ? mergeRealtimeDelays(deps, lastDelays)
      : deps
  })

  if (!gtfsRtUrl || import.meta.server) {
    return { departures: merged, isLive }
  }

  async function poll() {
    try {
      const res = await fetch(gtfsRtUrl, { mode: 'cors' })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const buffer = await res.arrayBuffer()

      const FeedMessage = await getFeedMessage()
      const feed = FeedMessage.decode(new Uint8Array(buffer)) as unknown as GtfsRtFeed

      const delays: Record<string, number> = {}
      for (const entity of feed.entity ?? []) {
        const tu = entity.tripUpdate
        if (!tu?.trip?.tripId) continue
        const updates = tu.stopTimeUpdate
        const lastUpdate = updates?.[updates.length - 1]
        const delay = lastUpdate?.departure?.delay ?? lastUpdate?.arrival?.delay ?? 0
        delays[tu.trip.tripId] = delay
      }

      lastDelays = delays
      merged.value = mergeRealtimeDelays(departures.value, delays)
      isLive.value = true
    } catch {
      // CORS, 404, parse error — degrade silently
      isLive.value = false
      lastDelays = {}
      merged.value = departures.value
    }
  }

  let timer: ReturnType<typeof setInterval> | undefined
  onMounted(async () => {
    await poll()
    timer = setInterval(poll, 30_000)
  })
  onUnmounted(() => {
    if (timer !== undefined) clearInterval(timer)
  })

  return { departures: merged, isLive }
}

// Minimal GTFS-RT types
interface GtfsRtFeed {
  entity?: Array<{
    tripUpdate?: {
      trip?: { tripId?: string }
      stopTimeUpdate?: Array<{
        departure?: { delay?: number }
        arrival?: { delay?: number }
      }>
    }
  }>
}
