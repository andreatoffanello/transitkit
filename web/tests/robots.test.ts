import { describe, it, expect } from 'vitest'
import { buildRobotsTxt } from '../server/routes/robots.txt'

describe('buildRobotsTxt', () => {
  it('always includes User-agent and Allow', () => {
    const txt = buildRobotsTxt('example.com', false)
    expect(txt).toContain('User-agent: *')
    expect(txt).toContain('Allow: /')
  })

  it('includes Sitemap for known operator', () => {
    const txt = buildRobotsTxt('appalcart.transitkit.app', true)
    expect(txt).toContain('Sitemap: https://appalcart.transitkit.app/sitemap.xml')
  })

  it('excludes Sitemap for unknown host', () => {
    const txt = buildRobotsTxt('unknown.com', false)
    expect(txt).not.toContain('Sitemap')
  })

  it('ends with newline', () => {
    const txt = buildRobotsTxt('example.com', true)
    expect(txt.endsWith('\n')).toBe(true)
  })
})
