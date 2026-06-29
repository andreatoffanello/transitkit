import { defineStore } from 'pinia'

export type Tier = 'high' | 'low' | 'static'

export interface Caps {
  reducedMotion: boolean
  webgl: boolean
  deviceMemory: number
  coarsePointer: boolean
}

/** Pure resolver — unit tested. */
export function resolveTier(c: Caps): Tier {
  if (c.reducedMotion || !c.webgl) return 'static'
  if (c.coarsePointer || c.deviceMemory < 4) return 'low'
  return 'high'
}

function detectWebgl(): boolean {
  try {
    const canvas = document.createElement('canvas')
    return !!(canvas.getContext('webgl2') || canvas.getContext('webgl'))
  } catch {
    return false
  }
}

export const useGraphicsStore = defineStore('graphics', {
  state: () => ({ tier: 'static' as Tier, detected: false }),
  actions: {
    detect() {
      if (typeof window === 'undefined') return
      this.tier = resolveTier({
        reducedMotion: window.matchMedia('(prefers-reduced-motion: reduce)').matches,
        webgl: detectWebgl(),
        deviceMemory: (navigator as unknown as { deviceMemory?: number }).deviceMemory ?? 8,
        coarsePointer: window.matchMedia('(pointer: coarse)').matches,
      })
      this.detected = true
    },
  },
})
