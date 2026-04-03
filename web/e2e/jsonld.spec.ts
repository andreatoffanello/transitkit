import { test, expect } from '@playwright/test'

test.describe('JSON-LD structured data in SSR HTML', () => {
  test('home page has TransitAgency JSON-LD', async ({ page }) => {
    await page.goto('/')
    const scripts = await page.locator('script[type="application/ld+json"]').allTextContents()
    const parsed = scripts.map(s => JSON.parse(s)).filter(d => Object.keys(d).length > 0)
    const agency = parsed.find(d => d['@type'] === 'TransitAgency')
    expect(agency).toBeDefined()
    expect(agency['@context']).toBe('https://schema.org')
  })

  test('/lines/:id has BusRoute JSON-LD', async ({ page }) => {
    await page.goto('/lines/route-1')
    const scripts = await page.locator('script[type="application/ld+json"]').allTextContents()
    const parsed = scripts.map(s => JSON.parse(s)).filter(d => Object.keys(d).length > 0)
    const route = parsed.find(d => d['@type'] === 'BusRoute')
    expect(route).toBeDefined()
    expect(route['identifier']).toBe('route-1')
  })

  test('/stop/:id has BusStop JSON-LD', async ({ page }) => {
    await page.goto('/stop/stop-A')
    const scripts = await page.locator('script[type="application/ld+json"]').allTextContents()
    const parsed = scripts.map(s => JSON.parse(s)).filter(d => Object.keys(d).length > 0)
    const stop = parsed.find(d => d['@type'] === 'BusStop')
    expect(stop).toBeDefined()
    expect(stop['identifier']).toBe('stop-A')
  })
})
