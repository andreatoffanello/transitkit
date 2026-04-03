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
})
