import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { OperatorConfig } from '~/types'

// useHead is stubbed globally in setup.ts — we just need to reference it
// and capture what it was called with after each composable invocation.

const mockConfig: OperatorConfig = {
  id: 'appalcart',
  name: 'AppalCART',
  fullName: 'AppalCART Regional Transportation',
  url: '',
  region: 'Boone NC',
  country: 'US',
  timezone: 'America/New_York',
  locale: ['en'],
  theme: { primaryColor: '#1b3a6b', accentColor: '#f5a623', textOnPrimary: '#ffffff' },
  store: { title: '', subtitle: '', keywords: '' },
  map: { centerLat: 36.2, centerLng: -81.6, defaultZoom: 13 },
  features: { enableMap: true, enableGeolocation: false, enableFavorites: false, enableNotifications: false },
}

// Resolve the content of a possibly-computed value
function resolveValue(v: unknown): unknown {
  if (v !== null && typeof v === 'object' && 'value' in v) return (v as { value: unknown }).value
  return v
}

describe('useOperatorHead', () => {
  let useHeadMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    useHeadMock = vi.fn()
    vi.stubGlobal('useHead', useHeadMock)
    // computed is already stubbed in setup.ts as fn => ({ value: fn() })
  })

  it('calls useHead with theme-color meta containing the primary color from config', async () => {
    const { useOperatorHead } = await import('~/composables/useOperatorHead')

    const config = { value: mockConfig }
    useOperatorHead(config as any)

    expect(useHeadMock).toHaveBeenCalledOnce()

    const [headOptions] = useHeadMock.mock.calls[0] as [Record<string, unknown>]
    const meta = headOptions.meta as Array<Record<string, unknown>>

    const themeColorMeta = meta.find(
      m => m.name === 'theme-color' && !m.media,
    )
    expect(themeColorMeta).toBeDefined()
    expect(resolveValue(themeColorMeta!.content)).toBe('#1b3a6b')
  })

  it('calls useHead with default theme-color #003366 when config is null', async () => {
    vi.resetModules()
    vi.stubGlobal('useHead', useHeadMock)
    vi.stubGlobal('computed', vi.fn((fn: () => unknown) => ({ value: fn() })))

    const { useOperatorHead } = await import('~/composables/useOperatorHead')

    const config = { value: null }
    // Must not throw
    expect(() => useOperatorHead(config as any)).not.toThrow()

    expect(useHeadMock).toHaveBeenCalledOnce()

    const [headOptions] = useHeadMock.mock.calls[0] as [Record<string, unknown>]
    const meta = headOptions.meta as Array<Record<string, unknown>>

    const themeColorMeta = meta.find(
      m => m.name === 'theme-color' && !m.media,
    )
    expect(themeColorMeta).toBeDefined()
    expect(resolveValue(themeColorMeta!.content)).toBe('#003366')
  })

  it('passes apple-mobile-web-app-title containing config.name', async () => {
    vi.resetModules()
    vi.stubGlobal('useHead', useHeadMock)
    vi.stubGlobal('computed', vi.fn((fn: () => unknown) => ({ value: fn() })))

    const { useOperatorHead } = await import('~/composables/useOperatorHead')

    const config = { value: mockConfig }
    useOperatorHead(config as any)

    const [headOptions] = useHeadMock.mock.calls[0] as [Record<string, unknown>]
    const meta = headOptions.meta as Array<Record<string, unknown>>

    const appTitleMeta = meta.find(m => m.name === 'apple-mobile-web-app-title')
    expect(appTitleMeta).toBeDefined()
    // The composable uses config.value?.name ?? 'Transit'
    expect(resolveValue(appTitleMeta!.content)).toBe('AppalCART')
  })

  it('style block innerHTML contains --color-primary with the primary color', async () => {
    vi.resetModules()
    vi.stubGlobal('useHead', useHeadMock)
    vi.stubGlobal('computed', vi.fn((fn: () => unknown) => ({ value: fn() })))

    const { useOperatorHead } = await import('~/composables/useOperatorHead')

    const config = { value: mockConfig }
    useOperatorHead(config as any)

    const [headOptions] = useHeadMock.mock.calls[0] as [Record<string, unknown>]
    const styles = headOptions.style as Array<Record<string, unknown>>

    expect(styles).toHaveLength(1)
    const innerHTML = resolveValue(styles[0]!.innerHTML) as string
    expect(innerHTML).toContain('--color-primary')
    expect(innerHTML).toContain('#1b3a6b')
  })

  it('style block innerHTML contains --color-primary with default #003366 when config is null', async () => {
    vi.resetModules()
    vi.stubGlobal('useHead', useHeadMock)
    vi.stubGlobal('computed', vi.fn((fn: () => unknown) => ({ value: fn() })))

    const { useOperatorHead } = await import('~/composables/useOperatorHead')

    const config = { value: null }
    useOperatorHead(config as any)

    const [headOptions] = useHeadMock.mock.calls[0] as [Record<string, unknown>]
    const styles = headOptions.style as Array<Record<string, unknown>>
    const innerHTML = resolveValue(styles[0]!.innerHTML) as string
    expect(innerHTML).toContain('--color-primary')
    expect(innerHTML).toContain('#003366')
  })
})
