import { test, expect } from '@playwright/test'

/**
 * Regressione: il banner mostrava solo due icone da 20px cliccabili — il resto
 * della riga, testo incluso, non era un link. Su mobile deve essere tutta
 * tappabile e puntare allo store del dispositivo.
 */

const IPHONE_UA =
  'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1'
const ANDROID_UA =
  'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36'

test.describe('download banner — iOS', () => {
  test.use({ userAgent: IPHONE_UA, viewport: { width: 390, height: 844 }, hasTouch: true })

  test('the whole banner links to the App Store', async ({ page }) => {
    await page.goto('/stop/stop-A')
    const cta = page.locator('.banner-cta')
    await expect(cta).toBeVisible()
    await expect(cta).toHaveAttribute('href', /apps\.apple\.com/)
    // Nessun link a Play su un dispositivo iOS.
    await expect(page.locator('a[href*="play.google.com"]')).toHaveCount(0)
  })
})

test.describe('download banner — Android', () => {
  test.use({ userAgent: ANDROID_UA, viewport: { width: 412, height: 915 }, hasTouch: true })

  test('the whole banner links to Google Play', async ({ page }) => {
    await page.goto('/stop/stop-A')
    const cta = page.locator('.banner-cta')
    await expect(cta).toBeVisible()
    await expect(cta).toHaveAttribute('href', /play\.google\.com/)
    await expect(page.locator('a[href*="apps.apple.com"]')).toHaveCount(0)
  })
})

test.describe('download banner — desktop', () => {
  test('keeps both store choices', async ({ page }) => {
    await page.goto('/stop/stop-A')
    await expect(page.locator('a[href*="apps.apple.com"]')).toBeVisible()
    await expect(page.locator('a[href*="play.google.com"]')).toBeVisible()
  })
})
