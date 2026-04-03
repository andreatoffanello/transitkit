/**
 * Fetch with exponential backoff retry.
 * Retries on network errors and 5xx responses.
 * Does NOT retry on 4xx (e.g., 404 = operator not found = permanent).
 * Pass an AbortSignal to cancel all attempts mid-flight.
 */
export async function fetchWithRetry(
  url: string,
  maxAttempts = 3,
  baseDelayMs = 1000,
  signal?: AbortSignal,
): Promise<Response> {
  let lastError: unknown

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    let response: Response | undefined

    try {
      response = await fetch(url, signal ? { signal } : undefined)
    } catch (err) {
      // Propagate AbortError immediately — no retry
      if (err instanceof Error && err.name === 'AbortError') throw err
      // Network error (fetch threw)
      lastError = err
      if (attempt < maxAttempts) {
        await delay(baseDelayMs * Math.pow(2, attempt - 1))
        continue
      }
      throw lastError
    }

    // 4xx: return immediately, no retry
    if (response.status >= 400 && response.status < 500) {
      return response
    }

    // 5xx: retry if attempts remain
    if (response.status >= 500) {
      lastError = new Error(`HTTP ${response.status}`)
      if (attempt < maxAttempts) {
        await delay(baseDelayMs * Math.pow(2, attempt - 1))
        continue
      }
      // Last attempt: return the 5xx response so caller can decide
      return response
    }

    // 2xx / 3xx: success
    return response
  }

  // Should never reach here, but TypeScript needs a return path
  throw lastError
}

function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}
