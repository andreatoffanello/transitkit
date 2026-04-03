import { test, expect } from '@playwright/test'

test('transitType filter ?type=bus persists after page reload', async ({ page }) => {
  // Navigate to /lines
  await page.goto('/lines')

  // Wait for the filter chips to appear (only shown when multiple transit types exist)
  // and for the "Bus" chip to be available
  const busChip = page.getByRole('button', { name: /bus/i })
  await expect(busChip).toBeVisible({ timeout: 20000 })

  // Click the Bus chip to activate the filter
  await busChip.click()

  // URL should now contain ?type=bus
  await expect(page).toHaveURL(/[?&]type=bus/)

  // Reload the page to verify persistence
  await page.reload()

  // The Bus chip should be pressed after reload
  const busChipAfterReload = page.getByRole('button', { name: /bus/i })
  await expect(busChipAfterReload).toHaveAttribute('aria-pressed', 'true', { timeout: 20000 })

  // URL should still contain ?type=bus
  await expect(page).toHaveURL(/[?&]type=bus/)
})
