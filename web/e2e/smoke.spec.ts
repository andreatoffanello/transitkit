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
