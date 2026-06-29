/**
 * Pointer-follow glow: writes --mx / --my / --glow CSS vars on the hovered element
 * so a card's ::after radial highlight tracks the cursor. Cheap, CSS-driven.
 */
export function usePointerGlow() {
  function onMove(e: PointerEvent) {
    const t = e.currentTarget as HTMLElement
    const r = t.getBoundingClientRect()
    t.style.setProperty('--mx', `${e.clientX - r.left}px`)
    t.style.setProperty('--my', `${e.clientY - r.top}px`)
    t.style.setProperty('--glow', '1')
  }
  function onLeave(e: PointerEvent) {
    ;(e.currentTarget as HTMLElement).style.setProperty('--glow', '0')
  }
  return { onMove, onLeave }
}
