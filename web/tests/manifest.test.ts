import { describe, it, expect } from 'vitest'
import { buildManifest } from '../server/routes/manifest.json'
import type { OperatorConfig } from '../types'

const mockConfig: OperatorConfig = {
  id: 'testop',
  name: 'TestOp',
  fullName: 'Test Operator Full Name',
  url: 'https://example.com',
  region: 'Test Region',
  country: 'IT',
  timezone: 'Europe/Rome',
  locale: ['it', 'en'],
  theme: {
    primaryColor: '#FF5500',
    accentColor: '#0055FF',
    textOnPrimary: '#FFFFFF',
  },
  store: { title: 'TestOp', subtitle: 'Orari', keywords: 'bus' },
  map: { centerLat: 45.0, centerLng: 10.0, defaultZoom: 13 },
  features: {
    enableMap: true,
    enableGeolocation: true,
    enableFavorites: false,
    enableNotifications: false,
  },
}

describe('buildManifest', () => {
  it('null config → returns TransitKit fallback name', () => {
    const manifest = buildManifest(null)
    expect(manifest.name).toBe('TransitKit')
    expect(manifest.short_name).toBe('Transit')
    expect(manifest.theme_color).toBe('#003366')
  })

  it('config with fullName → uses fullName as name', () => {
    const manifest = buildManifest(mockConfig)
    expect(manifest.name).toBe('Test Operator Full Name')
    expect(manifest.short_name).toBe('TestOp')
  })

  it('config without fullName (undefined) → uses name field', () => {
    // fullName is required in the type, but at runtime it may be absent from CDN data
    const configNoFull = { ...mockConfig, fullName: undefined as unknown as string }
    const manifest = buildManifest(configNoFull)
    // fullName is undefined → nullish coalescing falls back to name
    expect(manifest.name).toBe('TestOp')
  })

  it('theme_color comes from config.theme.primaryColor', () => {
    const manifest = buildManifest(mockConfig)
    expect(manifest.theme_color).toBe('#FF5500')
  })

  it('icons array is always present', () => {
    const manifestNull = buildManifest(null)
    const manifestConfig = buildManifest(mockConfig)

    expect(Array.isArray(manifestNull.icons)).toBe(true)
    expect((manifestNull.icons as unknown[]).length).toBeGreaterThan(0)

    expect(Array.isArray(manifestConfig.icons)).toBe(true)
    expect((manifestConfig.icons as unknown[]).length).toBeGreaterThan(0)
  })
})
