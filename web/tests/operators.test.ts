import { describe, it, expect } from 'vitest'
import { resolveOperatorId, CDN_BASE } from '~/utils/operators'

describe('resolveOperatorId', () => {
  it('risolve host noto', () => {
    expect(resolveOperatorId('appalcart.transitkit.app')).toBe('appalcart')
  })

  it('risolve host custom (CNAME cliente)', () => {
    expect(resolveOperatorId('fermate.appalcart.com')).toBe('appalcart')
  })

  it('restituisce null per host sconosciuto', () => {
    expect(resolveOperatorId('unknown.example.com')).toBeNull()
  })

  it('restituisce null per stringa vuota', () => {
    expect(resolveOperatorId('')).toBeNull()
  })

  it('CDN_BASE è una stringa https://', () => {
    expect(CDN_BASE).toBeTruthy()
    expect(CDN_BASE.startsWith('https://')).toBe(true)
  })
})
