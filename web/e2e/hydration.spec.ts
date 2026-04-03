import { test, expect } from '@playwright/test'

/**
 * Hydration regression guard — T356
 *
 * Intercepts console errors/warnings on each main route and asserts that
 * none of them contain Vue's "Hydration" / "hydration mismatch" text.
 * This catches SSR ↔ client DOM mismatches before they reach production.
 */

const ROUTES = [
  { path: '/', label: 'home' },
  { path: '/lines', label: 'lines' },
  { path: '/stop/stop-A', label: 'stop detail' },
]

for (const { path, label } of ROUTES) {
  test(`no hydration warnings on ${label} (${path})`, async ({ page }) => {
    const hydrationMessages: string[] = []

    page.on('console', (msg) => {
      const type = msg.type()
      if (type !== 'error' && type !== 'warning') return
      const text = msg.text()
      if (text.toLowerCase().includes('hydration')) {
        hydrationMessages.push(`[${type}] ${text}`)
      }
    })

    await page.goto(path, { waitUntil: 'networkidle' })

    expect(
      hydrationMessages,
      `Hydration errors/warnings detected on ${path}:\n${hydrationMessages.join('\n')}`,
    ).toHaveLength(0)
  })
}
