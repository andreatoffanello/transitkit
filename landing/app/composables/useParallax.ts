/**
 * Scroll-linked parallax: writes a `--py` px offset on the element based on its
 * distance from the viewport centre. Compose it into the element's transform,
 * e.g. translate(x, calc(-46% + var(--py, 0px))). No-op under reduced motion.
 */
export function useParallax(speed = 0.08) {
  const el = ref<HTMLElement | null>(null)
  let raf = 0

  function update() {
    const node = el.value
    if (!node) return
    const r = node.getBoundingClientRect()
    const center = r.top + r.height / 2
    const offset = (center - window.innerHeight / 2) * speed
    node.style.setProperty('--py', `${offset.toFixed(1)}px`)
  }
  function onScroll() {
    cancelAnimationFrame(raf)
    raf = requestAnimationFrame(update)
  }

  onMounted(() => {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
    update()
    window.addEventListener('scroll', onScroll, { passive: true })
    window.addEventListener('resize', onScroll)
  })
  onBeforeUnmount(() => {
    window.removeEventListener('scroll', onScroll)
    window.removeEventListener('resize', onScroll)
    cancelAnimationFrame(raf)
  })

  return { el }
}
