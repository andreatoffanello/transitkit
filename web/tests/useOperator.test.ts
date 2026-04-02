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

const fetchMock = vi.fn()
vi.stubGlobal('$fetch', fetchMock)

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
    if (urlStr.includes('config.json')) return Promise.resolve(mockConfig)
    if (urlStr.includes('schedules.json')) return Promise.resolve(mockSchedules)
    return Promise.reject(new Error(`Unexpected URL: ${urlStr}`))
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
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('appalcart/config.json'),
    )
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('appalcart/schedules.json'),
    )
  })

  it('usa CDN_BASE nell\'URL', async () => {
    await useOperator()
    const calls = fetchMock.mock.calls.map(c => String(c[0]))
    expect(calls.some(u => u.startsWith('https://'))).toBe(true)
  })
})
