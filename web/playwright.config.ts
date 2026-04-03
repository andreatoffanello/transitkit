import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  workers: 1,
  use: {
    baseURL: 'http://localhost:3000',
    headless: true,
  },
  webServer: {
    command: 'CDN_BASE=http://localhost:3000/mock npx nuxt dev',
    url: 'http://localhost:3000/robots.txt',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
})
