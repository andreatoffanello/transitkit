import { vi } from 'vitest'

// Stub Nuxt auto-imports usati nei composables
// I test che ne hanno bisogno possono sovrascrivere questi stub localmente
vi.stubGlobal('useState', vi.fn((_, init) => ({ value: init?.() })))
vi.stubGlobal('useAsyncData', vi.fn())
vi.stubGlobal('$fetch', vi.fn())
vi.stubGlobal('useRequestURL', vi.fn(() => new URL('http://appalcart.transitkit.app/')))
vi.stubGlobal('defineNuxtRouteMiddleware', vi.fn((fn) => fn))
vi.stubGlobal('createError', vi.fn((opts) => new Error(opts.statusMessage)))
vi.stubGlobal('useRoute', vi.fn(() => ({ params: {} })))
vi.stubGlobal('useHead', vi.fn())
vi.stubGlobal('computed', vi.fn((fn) => ({ value: fn() })))
vi.stubGlobal('ref', vi.fn((v) => ({ value: v })))
vi.stubGlobal('watch', vi.fn())
vi.stubGlobal('onMounted', vi.fn())
vi.stubGlobal('onUnmounted', vi.fn())
