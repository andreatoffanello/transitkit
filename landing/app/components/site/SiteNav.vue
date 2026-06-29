<script setup lang="ts">
const { t } = useI18n()
const localePath = useLocalePath()
const scrolled = ref(false)

const links = [
  { href: '#pillars', key: 'nav.product' },
  { href: '#how', key: 'nav.how' },
  { href: '#pricing', key: 'nav.pricing' },
  { href: '#faq', key: 'nav.faq' },
]

const activeIndex = ref(-1) // -1 = above the first section (hero)
const linksEl = ref<HTMLElement | null>(null)
const dot = reactive({ x: 0, visible: false })

// one shared indicator that SLIDES between items — measured from the live DOM so it
// always lands dead-centre under the active link, regardless of label width/locale
function updateDot() {
  const wrap = linksEl.value
  if (!wrap || activeIndex.value < 0) { dot.visible = false; return }
  const a = wrap.querySelectorAll<HTMLElement>('a')[activeIndex.value]
  if (!a) { dot.visible = false; return }
  dot.x = a.offsetLeft + a.offsetWidth / 2
  dot.visible = true
}

// scroll-position driven (NOT IO band-crossing): pick the last section whose top has
// crossed a reference line at 42% viewport. Deterministic — never lags on fast scroll.
let sections: HTMLElement[] = []
let ticking = false
function compute() {
  ticking = false
  scrolled.value = window.scrollY > 12
  const line = window.innerHeight * 0.42
  let idx = -1
  for (let i = 0; i < sections.length; i++) {
    if (sections[i]!.getBoundingClientRect().top <= line) idx = i
    else break
  }
  if (idx !== activeIndex.value) { activeIndex.value = idx; updateDot() }
}
function onScroll() {
  if (ticking) return
  ticking = true
  requestAnimationFrame(compute)
}
function onResize() { updateDot() }

onMounted(() => {
  sections = links.map((l) => document.getElementById(l.href.slice(1))).filter(Boolean) as HTMLElement[]
  compute()
  updateDot()
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('resize', onResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('resize', onResize)
})

function go(href: string, e: MouseEvent) {
  e.preventDefault()
  document.querySelector(href)?.scrollIntoView({ behavior: 'smooth' })
}
</script>

<template>
  <header class="nav" :class="{ 'is-scrolled': scrolled }">
    <div class="nav__inner">
      <NuxtLink :to="localePath('/')" class="nav__brand" aria-label="TransitKit home">
        <img src="~/assets/brand/mark.svg" alt="" width="30" height="19" class="nav__mark" />
        <span class="nav__word">TransitKit</span>
      </NuxtLink>

      <nav ref="linksEl" class="nav__links" aria-label="Primary">
        <a
          v-for="(l, i) in links"
          :key="l.href"
          :href="l.href"
          :class="{ 'is-active': activeIndex === i }"
          @click="go(l.href, $event)"
        >{{ t(l.key) }}</a>
        <span
          class="nav__dot"
          :class="{ 'is-visible': dot.visible }"
          :style="{ transform: `translateX(${dot.x}px)` }"
          aria-hidden="true"
        />
      </nav>

      <div class="nav__right">
        <LocaleSwitch />
        <CtaButton to="#pricing" variant="primary" class="nav__cta">{{ t('nav.cta') }}</CtaButton>
      </div>
    </div>
  </header>
</template>

<style scoped>
.nav {
  position: fixed;
  inset: 0 0 auto 0;
  z-index: 50;
  transition: background 0.3s, border-color 0.3s, backdrop-filter 0.3s;
  border-bottom: 1px solid transparent;
}
.nav.is-scrolled {
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: saturate(180%) blur(16px);
  -webkit-backdrop-filter: saturate(180%) blur(16px);
  border-bottom-color: var(--hairline);
}
.nav__inner {
  max-width: var(--max, 1120px);
  margin: 0 auto;
  height: 64px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
@media (min-width: 480px) {
  .nav__inner { padding: 0 24px; gap: 24px; }
}
.nav__brand { display: inline-flex; align-items: center; gap: 10px; }
.nav__mark { display: block; }
.nav__word { display: none; font-weight: 700; letter-spacing: -0.02em; font-size: 1.0625rem; }
@media (min-width: 400px) { .nav__word { display: inline; } }
.nav__links { position: relative; display: none; gap: 28px; }
.nav__links a {
  position: relative;
  font-size: 0.9375rem;
  color: var(--text-2);
  transition: color 0.25s ease;
}
.nav__links a:hover { color: var(--text); }
.nav__links a.is-active { color: var(--text); }
/* one shared dot that travels between items — the cyan signal of the hero, in the nav */
.nav__dot {
  position: absolute;
  left: 0;
  bottom: -9px;
  margin-left: -3px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(41, 200, 255, 0.95);
  box-shadow: 0 0 10px 1px rgba(41, 200, 255, 0.55);
  opacity: 0;
  transform: translateX(0);
  transition:
    transform 0.46s cubic-bezier(0.16, 1, 0.3, 1),
    opacity 0.3s ease;
  pointer-events: none;
}
.nav__dot.is-visible { opacity: 1; }
@media (prefers-reduced-motion: reduce) {
  .nav__dot { transition: opacity 0.2s linear; }
}
.nav__right { display: flex; align-items: center; gap: 12px; }
.nav__cta { display: inline-flex; height: 2.5rem; padding: 0 0.95rem; }
@media (min-width: 768px) {
  .nav__links { display: flex; }
  .nav__cta { height: 2.875rem; padding: 0 1.25rem; }
}
</style>
