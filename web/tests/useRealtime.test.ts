import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mergeRealtimeDelays } from '~/composables/useRealtime'
import type { Departure } from '~/types'

const baseDepartures: Departure[] = [
  {
    id: '07:35_3_Centro_',
    time: '07:35',
    lineName: '3',
    routeId: 'route-3',
    headsign: 'Centro',
    color: '#FF0000',
    textColor: '#FFFFFF',
    transitType: 'bus',
    dock: '',
    tripId: 'trip-001',
    minutesFromMidnight: 455,
  },
  {
    id: '08:00_7_Stazione_',
    time: '08:00',
    lineName: '7',
    routeId: 'route-7',
    headsign: 'Stazione',
    color: '#0000FF',
    textColor: '#FFFFFF',
    transitType: 'bus',
    dock: '',
    tripId: 'trip-002',
    minutesFromMidnight: 480,
  },
]

describe('mergeRealtimeDelays', () => {
  it('applies delay to departure with matching tripId', () => {
    const delays: Record<string, number> = { 'trip-001': 120 }
    const result = mergeRealtimeDelays(baseDepartures, delays)
    expect(result[0]?.realtimeDelay).toBe(120)
    expect(result[0]?.isRealtime).toBe(true)
  })

  it('does not modify departure without tripId in delay map', () => {
    const delays: Record<string, number> = {}
    const result = mergeRealtimeDelays(baseDepartures, delays)
    expect(result[0]?.realtimeDelay).toBeUndefined()
    expect(result[0]?.isRealtime).toBeUndefined()
  })

  it('does not mutate the original array', () => {
    const delays: Record<string, number> = { 'trip-001': 60 }
    mergeRealtimeDelays(baseDepartures, delays)
    expect(baseDepartures[0]?.realtimeDelay).toBeUndefined()
  })

  it('only marks departure with delay as isRealtime, others unchanged', () => {
    const delays: Record<string, number> = { 'trip-001': 180 }
    const result = mergeRealtimeDelays(baseDepartures, delays)
    expect(result[1]?.isRealtime).toBeUndefined()
    expect(result[1]?.realtimeDelay).toBeUndefined()
  })

  it('sets isRealtime: true when delay is 0 (on-time, tracked)', () => {
    const delays: Record<string, number> = { 'trip-001': 0 }
    const result = mergeRealtimeDelays(baseDepartures, delays)
    expect(result[0]?.realtimeDelay).toBe(0)
    expect(result[0]?.isRealtime).toBe(true)
  })

  it('handles negative delay (early departure)', () => {
    const delays: Record<string, number> = { 'trip-001': -120 }
    const result = mergeRealtimeDelays(baseDepartures, delays)
    expect(result[0]?.realtimeDelay).toBe(-120)
    expect(result[0]?.isRealtime).toBe(true)
  })

  it('delay = -60 (1 min early) — realtimeDelay is -60, minutesFromMidnight unchanged', () => {
    const delays: Record<string, number> = { 'trip-001': -60 }
    const result = mergeRealtimeDelays(baseDepartures, delays)
    expect(result[0]?.realtimeDelay).toBe(-60)
    expect(result[0]?.isRealtime).toBe(true)
    // minutesFromMidnight is NOT adjusted by mergeRealtimeDelays — it remains 455 (07:35)
    expect(result[0]?.minutesFromMidnight).toBe(455)
  })

  it('delay = 0 (on time, tracked) — realtimeDelay is 0, minutesFromMidnight unchanged', () => {
    const delays: Record<string, number> = { 'trip-001': 0 }
    const result = mergeRealtimeDelays(baseDepartures, delays)
    expect(result[0]?.realtimeDelay).toBe(0)
    expect(result[0]?.isRealtime).toBe(true)
    expect(result[0]?.minutesFromMidnight).toBe(455)
  })

  it('delay = -600 (10 min early) — realtimeDelay is -600, minutesFromMidnight unchanged', () => {
    const delays: Record<string, number> = { 'trip-001': -600 }
    const result = mergeRealtimeDelays(baseDepartures, delays)
    expect(result[0]?.realtimeDelay).toBe(-600)
    expect(result[0]?.isRealtime).toBe(true)
    // minutesFromMidnight is NOT adjusted — static schedule value (07:35 = 455) is preserved
    expect(result[0]?.minutesFromMidnight).toBe(455)
  })

  it('handles departure with no tripId — not marked realtime', () => {
    const noTripDep: Departure[] = [
      {
        id: '09:00_1_Airport_',
        time: '09:00',
        lineName: '1',
        routeId: 'route-1',
        headsign: 'Airport',
        color: '#000',
        textColor: '#FFF',
        transitType: 'bus',
        dock: '',
        minutesFromMidnight: 540,
        // tripId intentionally absent
      },
    ]
    const delays: Record<string, number> = { 'some-trip': 60 }
    const result = mergeRealtimeDelays(noTripDep, delays)
    expect(result[0]?.isRealtime).toBeUndefined()
  })
})

describe('useRealtime — visibilitychange behavior', () => {
  // Capture lifecycle callbacks registered by the composable
  let mountedCallback: (() => Promise<void>) | undefined
  let unmountedCallback: (() => void) | undefined

  // Track event listeners added to our mock document
  type Listener = EventListenerOrEventListenerObject
  const listeners: Map<string, Listener[]> = new Map()

  let visibilityState = 'visible'

  const mockDocument = {
    get hidden() { return visibilityState === 'hidden' },
    get visibilityState() { return visibilityState },
    addEventListener(type: string, fn: Listener) {
      if (!listeners.has(type)) listeners.set(type, [])
      listeners.get(type)!.push(fn)
    },
    removeEventListener(type: string, fn: Listener) {
      const arr = listeners.get(type) ?? []
      const idx = arr.indexOf(fn)
      if (idx !== -1) arr.splice(idx, 1)
    },
  }

  function dispatchVisibility(state: 'visible' | 'hidden') {
    visibilityState = state
    const arr = listeners.get('visibilitychange') ?? []
    for (const fn of arr) {
      if (typeof fn === 'function') fn(new Event('visibilitychange'))
      else fn.handleEvent(new Event('visibilitychange'))
    }
  }

  beforeEach(async () => {
    vi.resetModules()
    listeners.clear()
    visibilityState = 'visible'
    mountedCallback = undefined
    unmountedCallback = undefined

    // Re-stub Nuxt auto-imports that setup.ts normally provides (lost after resetModules)
    vi.stubGlobal('ref', vi.fn((v: unknown) => ({ value: v })))
    vi.stubGlobal('watch', vi.fn())
    vi.stubGlobal('computed', vi.fn((fn: () => unknown) => ({ value: fn() })))

    // Override lifecycle globals to capture callbacks
    vi.stubGlobal('onMounted', vi.fn((cb: () => Promise<void>) => { mountedCallback = cb }))
    vi.stubGlobal('onUnmounted', vi.fn((cb: () => void) => { unmountedCallback = cb }))

    // Provide a mock document in the node environment
    vi.stubGlobal('document', mockDocument)
  })

  afterEach(() => {
    // Restore globals stubbed in this describe block
    vi.unstubAllGlobals()
  })

  it('fetches immediately and restarts polling when tab becomes visible', async () => {
    // Reimport after resetModules so lifecycle stubs are in effect
    const { useRealtime } = await import('~/composables/useRealtime')

    const mockFetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 404,
    } as Response)
    vi.stubGlobal('fetch', mockFetch)

    const deps = { value: [] as Departure[] }
    // Call composable — this registers onMounted/onUnmounted
    useRealtime(deps as any, 'https://example.com/gtfs-rt')

    // Simulate mount: this should call poll() once and start polling
    await mountedCallback!()
    const callsAfterMount = mockFetch.mock.calls.length

    // Simulate tab becoming visible (e.g. after being hidden)
    dispatchVisibility('visible')

    // poll() should have been called again (fetch count increases)
    expect(mockFetch.mock.calls.length).toBeGreaterThan(callsAfterMount)
  })

  it('stops polling (clears interval) when tab becomes hidden', async () => {
    const clearIntervalSpy = vi.spyOn(globalThis, 'clearInterval')

    const { useRealtime } = await import('~/composables/useRealtime')

    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 404,
    } as Response))

    const deps = { value: [] as Departure[] }
    useRealtime(deps as any, 'https://example.com/gtfs-rt')

    await mountedCallback!()

    // Simulate tab going hidden
    dispatchVisibility('hidden')

    // clearInterval should have been called (pausing the polling timer)
    expect(clearIntervalSpy).toHaveBeenCalled()

    clearIntervalSpy.mockRestore()
  })

  it('removes visibilitychange listener on unmount', async () => {
    const { useRealtime } = await import('~/composables/useRealtime')

    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 404,
    } as Response))

    const deps = { value: [] as Departure[] }
    useRealtime(deps as any, 'https://example.com/gtfs-rt')

    await mountedCallback!()

    // Listener should be registered
    expect(listeners.get('visibilitychange')?.length).toBe(1)

    // Unmount
    unmountedCallback!()

    // Listener should have been removed
    expect(listeners.get('visibilitychange')?.length).toBe(0)
  })

  it('poll() skips fetch when document.hidden is true', async () => {
    const { useRealtime } = await import('~/composables/useRealtime')

    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)

    // Set the tab as hidden before mount so poll() sees document.hidden === true
    visibilityState = 'hidden'

    const deps = { value: [] as Departure[] }
    useRealtime(deps as any, 'https://example.com/gtfs-rt')

    // Simulate the onMounted lifecycle — poll() should exit early due to document.hidden
    await mountedCallback!()

    expect(fetchMock).not.toHaveBeenCalled()
  })
})

describe('useRealtime — isLoading and refresh()', () => {
  let mountedCallback: (() => Promise<void>) | undefined

  beforeEach(async () => {
    vi.resetModules()
    mountedCallback = undefined

    // Re-stub Nuxt auto-imports
    vi.stubGlobal('ref', vi.fn((v: unknown) => ({ value: v })))
    vi.stubGlobal('watch', vi.fn())
    vi.stubGlobal('computed', vi.fn((fn: () => unknown) => ({ value: fn() })))
    vi.stubGlobal('onMounted', vi.fn((cb: () => Promise<void>) => { mountedCallback = cb }))
    vi.stubGlobal('onUnmounted', vi.fn())

    // Provide a minimal mock document for the composable
    vi.stubGlobal('document', {
      hidden: false,
      visibilityState: 'visible',
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('isLoading is false initially (before any fetch)', async () => {
    const { useRealtime } = await import('~/composables/useRealtime')
    const deps = { value: [] as Departure[] }

    const { isLoading } = useRealtime(deps as any, 'https://example.com/gtfs-rt')

    expect(isLoading.value).toBe(false)
  })

  it('isLoading is false initially when no gtfsRtUrl is provided', async () => {
    const { useRealtime } = await import('~/composables/useRealtime')
    const deps = { value: [] as Departure[] }

    const { isLoading } = useRealtime(deps as any, undefined)

    expect(isLoading.value).toBe(false)
  })

  it('isLoading becomes true during fetch, then false after (fetch fails)', async () => {
    const { useRealtime } = await import('~/composables/useRealtime')

    // Track isLoading.value while fetch is in-flight
    let isLoadingDuringFetch: boolean | undefined

    const mockFetch = vi.fn().mockImplementation(async () => {
      // At this point poll() has already set isLoading.value = true
      isLoadingDuringFetch = result.isLoading.value
      return { ok: false, status: 503 } as Response
    })
    vi.stubGlobal('fetch', mockFetch)

    const deps = { value: [] as Departure[] }
    const result = useRealtime(deps as any, 'https://example.com/gtfs-rt')

    // Before mount: isLoading should be false
    expect(result.isLoading.value).toBe(false)

    // Simulate mount (triggers poll())
    await mountedCallback!()

    // During fetch, isLoading was true
    expect(isLoadingDuringFetch).toBe(true)

    // After fetch completes (failed), isLoading is reset to false
    expect(result.isLoading.value).toBe(false)
  })

  it('isLoading is false after a successful fetch (arrayBuffer parse fails gracefully)', async () => {
    const { useRealtime } = await import('~/composables/useRealtime')

    // Return a valid ok response but with arrayBuffer that returns empty buffer;
    // protobufjs will fail to load in node env, so the catch block runs — isLoading ends false
    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      arrayBuffer: () => Promise.resolve(new ArrayBuffer(0)),
    } as unknown as Response)
    vi.stubGlobal('fetch', mockFetch)

    const deps = { value: [] as Departure[] }
    const result = useRealtime(deps as any, 'https://example.com/gtfs-rt')

    await mountedCallback!()

    expect(result.isLoading.value).toBe(false)
  })

  it('refresh() does not throw when no gtfsRtUrl is configured', async () => {
    const { useRealtime } = await import('~/composables/useRealtime')
    const deps = { value: [] as Departure[] }

    const { refresh } = useRealtime(deps as any, undefined)

    // Should resolve without throwing
    await expect(refresh()).resolves.toBeUndefined()
  })

  it('refresh() triggers a fetch call', async () => {
    const { useRealtime } = await import('~/composables/useRealtime')

    const mockFetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 404,
    } as Response)
    vi.stubGlobal('fetch', mockFetch)

    const deps = { value: [] as Departure[] }
    const { refresh } = useRealtime(deps as any, 'https://example.com/gtfs-rt')

    // fetch should not have been called yet (onMounted not triggered)
    expect(mockFetch).not.toHaveBeenCalled()

    await refresh()

    expect(mockFetch).toHaveBeenCalledTimes(1)
    expect(mockFetch).toHaveBeenCalledWith(
      'https://example.com/gtfs-rt',
      { mode: 'cors' },
    )
  })

  it('refresh() resets isLoading to false after completion even on fetch error', async () => {
    const { useRealtime } = await import('~/composables/useRealtime')

    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('network error')))

    const deps = { value: [] as Departure[] }
    const { refresh, isLoading } = useRealtime(deps as any, 'https://example.com/gtfs-rt')

    await refresh()

    expect(isLoading.value).toBe(false)
  })
})

describe('useRealtime — lastUpdated', () => {
  let mountedCallback: (() => Promise<void>) | undefined

  beforeEach(async () => {
    vi.resetModules()
    mountedCallback = undefined

    // Re-stub Nuxt auto-imports
    vi.stubGlobal('ref', vi.fn((v: unknown) => ({ value: v })))
    vi.stubGlobal('watch', vi.fn())
    vi.stubGlobal('computed', vi.fn((fn: () => unknown) => ({ value: fn() })))
    vi.stubGlobal('onMounted', vi.fn((cb: () => Promise<void>) => { mountedCallback = cb }))
    vi.stubGlobal('onUnmounted', vi.fn())

    // Provide a minimal mock document for the composable
    vi.stubGlobal('document', {
      hidden: false,
      visibilityState: 'visible',
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('lastUpdated.value is null before any poll is called', async () => {
    const { useRealtime } = await import('~/composables/useRealtime')
    const deps = { value: [] as Departure[] }

    const { lastUpdated } = useRealtime(deps as any, 'https://example.com/gtfs-rt')

    expect(lastUpdated.value).toBeNull()
  })

  it('lastUpdated.value matches HH:MM format after poll() resolves', async () => {
    const { useRealtime } = await import('~/composables/useRealtime')

    // fetch fails with a non-ok response; lastUpdated is still set in the finally block
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 503,
    } as Response))

    const deps = { value: [] as Departure[] }
    const result = useRealtime(deps as any, 'https://example.com/gtfs-rt')

    // Trigger the mounted lifecycle to run poll()
    await mountedCallback!()

    expect(result.lastUpdated.value).toMatch(/^\d{2}:\d{2}$/)
  })
})
