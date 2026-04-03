import { test, expect } from '@playwright/test'

/**
 * T324 — Verifica il parametro ?stop= nel flusso back button: stop page → line page.
 *
 * Flusso testato:
 * 1. Naviga su /lines/route-1
 * 2. Clicca sul link della fermata stop-B
 * 3. Verifica che la stop page carichi e il back button contenga il nome linea
 * 4. Clicca il back button
 * 5. Verifica che l'URL contenga ?stop=stop-B
 * 6. Verifica che il link della fermata stop-B abbia aria-current="location"
 */
test('back button from stop page returns to line page with ?stop= param and aria-current', async ({ page }) => {
  // Step 1: naviga sulla pagina dettaglio linea route-1
  await page.goto('/lines/route-1')

  // Aspetta che i link delle fermate siano visibili.
  // NuxtLink renders with query params: /stop/stop-B?from=route-1
  const stopBLink = page.locator(`a[href*="/stop/stop-B"]`).first()
  await expect(stopBLink).toBeVisible({ timeout: 20000 })

  // Step 2: clicca sul link della fermata stop-B
  await stopBLink.click()

  // Step 3: verifica che la stop page carichi (URL corretta)
  await expect(page).toHaveURL(/\/stop\/stop-B/, { timeout: 15000 })

  // Verifica che il back button punti a /lines/route-1?stop=stop-B
  const backButton = page.locator(`a[href*="/lines/route-1"][href*="stop=stop-B"]`).first()
  await expect(backButton).toBeVisible({ timeout: 10000 })

  // Step 4: clicca il back button
  await backButton.click()

  // Step 5: verifica che l'URL contenga ?stop=stop-B
  await expect(page).toHaveURL(/\/lines\/route-1/, { timeout: 10000 })
  await expect(page).toHaveURL(/[?&]stop=stop-B/)

  // Step 6: verifica che il link della fermata stop-B abbia aria-current="location"
  const highlightedStop = page.locator(`a[href*="/stop/stop-B"][aria-current="location"]`).first()
  await expect(highlightedStop).toBeVisible({ timeout: 10000 })
})
