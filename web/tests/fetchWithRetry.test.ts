import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

// Must import AFTER stubbing fetch
let fetchWithRetry: typeof import('~/utils/fetchWithRetry').fetchWithRetry

beforeEach(async () => {
  vi.useFakeTimers()
  vi.stubGlobal('fetch', vi.fn())
  // Re-import to get a clean module (avoid module caching issues with mocks)
  const mod = await import('~/utils/fetchWithRetry')
  fetchWithRetry = mod.fetchWithRetry
})

afterEach(() => {
  vi.useRealTimers()
  vi.restoreAllMocks()
})

function makeResponse(status: number): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve({}),
  } as unknown as Response
}

describe('fetchWithRetry', () => {
  it('succeeds on first attempt without retrying', async () => {
    const fetchMock = vi.mocked(globalThis.fetch)
    fetchMock.mockResolvedValueOnce(makeResponse(200))

    const resultPromise = fetchWithRetry('https://example.com/data.json')
    await vi.runAllTimersAsync()
    const result = await resultPromise

    expect(result.status).toBe(200)
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('retries on 5xx and succeeds on second attempt', async () => {
    const fetchMock = vi.mocked(globalThis.fetch)
    fetchMock
      .mockResolvedValueOnce(makeResponse(503))
      .mockResolvedValueOnce(makeResponse(200))

    const resultPromise = fetchWithRetry('https://example.com/data.json', 3, 1000)
    // First attempt returns 503, triggers 1000ms delay
    await vi.advanceTimersByTimeAsync(1000)
    const result = await resultPromise

    expect(result.status).toBe(200)
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('retries on network error and succeeds on third attempt', async () => {
    const fetchMock = vi.mocked(globalThis.fetch)
    fetchMock
      .mockRejectedValueOnce(new Error('Network error'))
      .mockRejectedValueOnce(new Error('Network error'))
      .mockResolvedValueOnce(makeResponse(200))

    const resultPromise = fetchWithRetry('https://example.com/data.json', 3, 1000)
    // Attempt 1 fails → delay 1000ms
    await vi.advanceTimersByTimeAsync(1000)
    // Attempt 2 fails → delay 2000ms
    await vi.advanceTimersByTimeAsync(2000)
    const result = await resultPromise

    expect(result.status).toBe(200)
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })

  it('throws after maxAttempts exhausted on network error', async () => {
    const fetchMock = vi.mocked(globalThis.fetch)
    fetchMock
      .mockRejectedValueOnce(new Error('Network failure'))
      .mockRejectedValueOnce(new Error('Network failure'))
      .mockRejectedValueOnce(new Error('Network failure'))

    let caughtError: unknown
    const resultPromise = fetchWithRetry('https://example.com/data.json', 3, 1000).catch(
      err => { caughtError = err },
    )
    // Attempt 1 fails → delay 1000ms
    await vi.advanceTimersByTimeAsync(1000)
    // Attempt 2 fails → delay 2000ms
    await vi.advanceTimersByTimeAsync(2000)
    await resultPromise

    expect(caughtError).toBeInstanceOf(Error)
    expect((caughtError as Error).message).toBe('Network failure')
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })

  it('returns 4xx immediately without retrying', async () => {
    const fetchMock = vi.mocked(globalThis.fetch)
    fetchMock.mockResolvedValueOnce(makeResponse(404))

    const resultPromise = fetchWithRetry('https://example.com/missing.json', 3, 1000)
    await vi.runAllTimersAsync()
    const result = await resultPromise

    expect(result.status).toBe(404)
    // Only one attempt — no retry on 4xx
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('returns 5xx response on last attempt rather than throwing', async () => {
    const fetchMock = vi.mocked(globalThis.fetch)
    fetchMock
      .mockResolvedValueOnce(makeResponse(500))
      .mockResolvedValueOnce(makeResponse(500))
      .mockResolvedValueOnce(makeResponse(500))

    const resultPromise = fetchWithRetry('https://example.com/data.json', 3, 100)
    await vi.advanceTimersByTimeAsync(100)
    await vi.advanceTimersByTimeAsync(200)
    const result = await resultPromise

    expect(result.status).toBe(500)
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })

  it('AbortError is re-thrown immediately without retry', async () => {
    const fetchMock = vi.mocked(globalThis.fetch)
    const abortError = new DOMException('The operation was aborted.', 'AbortError')
    fetchMock.mockRejectedValueOnce(abortError)

    let caughtError: unknown
    const resultPromise = fetchWithRetry('https://example.com/data.json', 3, 1000).catch(
      err => { caughtError = err },
    )
    await vi.runAllTimersAsync()
    await resultPromise

    expect(caughtError).toBe(abortError)
    // Only one attempt — AbortError is not retried
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('passes signal to fetch', async () => {
    const fetchMock = vi.mocked(globalThis.fetch)
    fetchMock.mockResolvedValueOnce(makeResponse(200))

    const controller = new AbortController()
    const resultPromise = fetchWithRetry('https://example.com/data.json', 3, 1000, controller.signal)
    await vi.runAllTimersAsync()
    await resultPromise

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(fetchMock).toHaveBeenCalledWith(
      'https://example.com/data.json',
      { signal: controller.signal },
    )
  })

  it('uses exponential backoff delays: 1000ms then 2000ms', async () => {
    const fetchMock = vi.mocked(globalThis.fetch)
    const setTimeoutSpy = vi.spyOn(globalThis, 'setTimeout')

    fetchMock
      .mockRejectedValueOnce(new Error('fail'))
      .mockRejectedValueOnce(new Error('fail'))
      .mockResolvedValueOnce(makeResponse(200))

    const resultPromise = fetchWithRetry('https://example.com/data.json', 3, 1000)
    await vi.advanceTimersByTimeAsync(1000)
    await vi.advanceTimersByTimeAsync(2000)
    await resultPromise

    // Check that delays were 1000 and 2000 (2^0 * 1000, 2^1 * 1000)
    const delayArgs = setTimeoutSpy.mock.calls
      .map(call => call[1])
      .filter(ms => ms !== undefined && ms > 0)
      .sort((a, b) => (a as number) - (b as number))
    expect(delayArgs).toEqual([1000, 2000])
  })
})
