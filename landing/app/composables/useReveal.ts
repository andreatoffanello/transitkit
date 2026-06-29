/**
 * Scroll-reveal via IntersectionObserver. Adds `is-revealed` once the element
 * enters the viewport. Fails OPEN — content is never left permanently hidden:
 * if the template ref hasn't bound yet it retries for a few frames, then reveals.
 * No-op (immediately revealed) under reduced motion.
 */
export function useReveal(options: { threshold?: number; rootMargin?: string } = {}) {
  const el = ref<HTMLElement | null>(null)
  const revealed = ref(false)
  let io: IntersectionObserver | null = null

  function begin() {
    const node = el.value
    if (!node) return false
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches || !('IntersectionObserver' in window)) {
      revealed.value = true
      return true
    }
    io = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            revealed.value = true
            io?.disconnect()
          }
        }
      },
      { threshold: options.threshold ?? 0.18, rootMargin: options.rootMargin ?? '0px 0px -8% 0px' },
    )
    io.observe(node)
    // already in view at mount? reveal right away
    const r = node.getBoundingClientRect()
    if (r.top < window.innerHeight && r.bottom > 0) revealed.value = true
    return true
  }

  onMounted(() => {
    let tries = 0
    function attempt() {
      if (begin()) return
      if (tries++ < 6) requestAnimationFrame(attempt)
      else revealed.value = true // ref never bound — show content rather than hide it forever
    }
    attempt()
  })
  onBeforeUnmount(() => io?.disconnect())

  return { el, revealed }
}
