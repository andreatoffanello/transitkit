import { test, expect } from '@playwright/test'

test('home page loads with operator name', async ({ page }) => {
  await page.goto('/')
  await expect(page.locator('h1')).toBeVisible()
})

test('navigate to lines page', async ({ page }) => {
  await page.goto('/')
  await page.getByRole('link', { name: /linee/i }).first().click()
  await expect(page).toHaveURL('/lines')
  await expect(page.locator('main, [class*="max-w"]').first()).toBeVisible()
})

test('lines page shows at least one line', async ({ page }) => {
  await page.goto('/lines')
  // Wait for content to load (skeleton → real content)
  await page.waitForTimeout(2000)
  await expect(page.locator('a[href^="/lines/"]').first()).toBeVisible()
})

test('stop page loads with stop name', async ({ page }) => {
  await page.goto('/stop/stop-A')
  // Wait for content (past skeleton)
  await page.waitForSelector('h1, h2', { timeout: 10000 })
  const heading = page.locator('h1, h2').first()
  await expect(heading).toBeVisible()
})

test('stop page shows departures section', async ({ page }) => {
  await page.goto('/stop/stop-A')
  await page.waitForTimeout(2000)
  // Should show either upcoming departures or schedule section
  const hasSection = await page.locator('text=/prossime partenze|orari di oggi|nessuna partenza/i').count()
  expect(hasSection).toBeGreaterThan(0)
})

test('stop page shows line back button when navigating from line', async ({ page }) => {
  // Navigate directly to stop with ?from=route-1 (simulates coming from line detail)
  await page.goto('/stop/stop-A?from=route-1')
  await page.waitForTimeout(1500)
  // Back link should point to the line, not home
  const backLink = page.locator('a[href*="/lines/route-1"]').first()
  await expect(backLink).toBeVisible()
})

test('stop page shows home back button without from param', async ({ page }) => {
  await page.goto('/stop/stop-A')
  await page.waitForTimeout(1500)
  // Back link should point to home
  const backLink = page.locator('a[href="/"]').first()
  await expect(backLink).toBeVisible()
})
