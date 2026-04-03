import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { OperatorConfig, ScheduleData } from '~/types'

// Sovrascriviamo gli stub globali di setup.ts con versioni specifiche per questo test
const stateMap = new Map<string, { value: unknown }>()

vi.stubGlobal('useState', (key: string, init?: () => unknown) => {
  if (!stateMap.has(key)) stateMap.set(key, { value: init?.() })
  return stateMap.get(key)!
})

vi.stubGlobal('useAsyncData', async (_key: string, fn: () => Promise<unknown>) => {
  const data = await fn()
  return { data: { value: data }, error: { value: null }, pending: { value: false } }
})

// Mock native fetch (useOperator now uses fetchWithRetry → fetch)
const fetchMock = vi.fn()
vi.stubGlobal('fetch', fetchMock)

function makeResponse(body: unknown, status = 200): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  } as unknown as Response
}

// Import DOPO i mock
const { useOperator } = await import('~/composables/useOperator')

const mockConfig: OperatorConfig = {
  id: 'appalcart',
  name: 'AppalCART',
  fullName: 'AppalCART Regional Transportation',
  url: '',
  region: 'Boone NC',
  country: 'US',
  timezone: 'America/New_York',
  locale: ['en'],
  theme: { primaryColor: '#1B3A6B', accentColor: '#F5A623', textOnPrimary: '#FFFFFF' },
  store: { title: '', subtitle: '', keywords: '' },
  map: { centerLat: 36.2, centerLng: -81.6, defaultZoom: 13 },
  features: { enableMap: true, enableGeolocation: false, enableFavorites: false, enableNotifications: false },
}

const mockSchedules: ScheduleData = {
  operator: { id: 'appalcart', name: 'AppalCART', url: '' },
  lastUpdated: '2026-01-01',
  validUntil: '2026-12-31',
  headsigns: [],
  lineNames: [],
  routeIds: [],
  tripIds: [],
  routes: [],
  stops: [],
}

beforeEach(() => {
  stateMap.clear()
  stateMap.set('operatorId', { value: 'appalcart' })
  fetchMock.mockImplementation((url: unknown) => {
    const urlStr = String(url)
    if (urlStr.includes('config.json')) return Promise.resolve(makeResponse(mockConfig))
    if (urlStr.includes('schedules.json')) return Promise.resolve(makeResponse(mockSchedules))
    return Promise.reject(new Error(`Unexpected URL: ${urlStr}`))
  })
})

// useAsyncData stub that propagates fetcher errors into error.value (mirrors Nuxt behaviour)
function makeErrorPropagatingAsyncData() {
  return async (_key: string, fn: () => Promise<unknown>) => {
    try {
      const data = await fn()
      return { data: { value: data }, error: { value: null }, pending: { value: false } }
    }
    catch (err) {
      return { data: { value: null }, error: { value: err }, pending: { value: false } }
    }
  }
}

function makeCreateErrorMock() {
  return vi.fn((opts: { statusCode: number; statusMessage: string }) => {
    const e = new Error(opts.statusMessage) as Error & { statusCode: number }
    e.statusCode = opts.statusCode
    return e
  })
}

describe('useOperator — error paths', () => {
  it('lancia createError 404 quando il CDN risponde con 404', async () => {
    fetchMock.mockImplementation((url: unknown) => {
      const urlStr = String(url)
      if (urlStr.includes('config.json')) return Promise.resolve(makeResponse({}, 404))
      if (urlStr.includes('schedules.json')) return Promise.resolve(makeResponse(mockSchedules))
      return Promise.reject(new Error(`Unexpected URL: ${urlStr}`))
    })
    vi.stubGlobal('useAsyncData', makeErrorPropagatingAsyncData())
    const createErrorMock = makeCreateErrorMock()
    vi.stubGlobal('createError', createErrorMock)

    await expect(useOperator()).rejects.toThrow('Operator not found')
    expect(createErrorMock).toHaveBeenCalledWith(
      expect.objectContaining({ statusCode: 404 }),
    )
  })

  it('lancia createError 502 quando il CDN risponde con 503', async () => {
    fetchMock.mockImplementation((url: unknown) => {
      const urlStr = String(url)
      if (urlStr.includes('config.json')) return Promise.resolve(makeResponse({}, 503))
      if (urlStr.includes('schedules.json')) return Promise.resolve(makeResponse(mockSchedules))
      return Promise.reject(new Error(`Unexpected URL: ${urlStr}`))
    })
    vi.stubGlobal('useAsyncData', makeErrorPropagatingAsyncData())
    const createErrorMock = makeCreateErrorMock()
    vi.stubGlobal('createError', createErrorMock)

    await expect(useOperator()).rejects.toThrow()
    expect(createErrorMock).toHaveBeenCalledWith(
      expect.objectContaining({ statusCode: 502 }),
    )
  })

  it('lancia createError 502 quando schedules fallisce (config OK)', async () => {
    fetchMock.mockImplementation((url: unknown) => {
      const urlStr = String(url)
      if (urlStr.includes('config.json')) return Promise.resolve(makeResponse(mockConfig))
      if (urlStr.includes('schedules.json')) return Promise.resolve(makeResponse({}, 503))
      return Promise.reject(new Error(`Unexpected URL: ${urlStr}`))
    })
    vi.stubGlobal('useAsyncData', makeErrorPropagatingAsyncData())
    const createErrorMock = makeCreateErrorMock()
    vi.stubGlobal('createError', createErrorMock)

    await expect(useOperator()).rejects.toThrow()
    expect(createErrorMock).toHaveBeenCalledWith(
      expect.objectContaining({ statusCode: 502 }),
    )
  })
})

describe('useOperator', () => {
  it('carica config dal CDN', async () => {
    const { config } = await useOperator()
    expect(config.value?.id).toBe('appalcart')
    expect(config.value?.name).toBe('AppalCART')
  })

  it('carica schedules dal CDN', async () => {
    const { schedules } = await useOperator()
    expect(schedules.value?.operator.id).toBe('appalcart')
  })

  it('usa operatorId per costruire URL CDN corretto', async () => {
    await useOperator()
    const urls = fetchMock.mock.calls.map((c: unknown[]) => String(c[0]))
    expect(urls.some((u: string) => u.includes('appalcart/config.json'))).toBe(true)
    expect(urls.some((u: string) => u.includes('appalcart/schedules.json'))).toBe(true)
  })

  it('usa CDN_BASE nell\'URL', async () => {
    await useOperator()
    const calls = fetchMock.mock.calls.map(c => String(c[0]))
    expect(calls.some(u => u.startsWith('https://'))).toBe(true)
  })

  it('legge correttamente locale da config.json', async () => {
    const { config } = await useOperator()
    expect(config.value?.locale).toEqual(['en'])
  })

  it('non crasha se config.json non ha locale', async () => {
    const configWithoutLocale = { ...mockConfig }
    delete (configWithoutLocale as Partial<typeof mockConfig>).locale
    fetchMock.mockImplementation((url: unknown) => {
      const urlStr = String(url)
      if (urlStr.includes('config.json')) return Promise.resolve(makeResponse(configWithoutLocale))
      if (urlStr.includes('schedules.json')) return Promise.resolve(makeResponse(mockSchedules))
      return Promise.reject(new Error(`Unexpected URL: ${urlStr}`))
    })
    const { config } = await useOperator()
    expect(config.value).toBeDefined()
    expect(config.value?.locale).toBeUndefined()
  })
})
