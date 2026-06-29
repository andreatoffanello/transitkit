# TransitKit Landing Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the TransitKit marketing landing page — a dark-only, single-page Nuxt 4 site with a top-tier three.js "living network" hero, EN/IT i18n, and transparent two-tier pricing — that converts transit operators into booked demos.

**Architecture:** Standalone Nuxt 4 app at `transit-engine/landing/` (sibling of `web/`), deployed as its own Vercel project on `transitkit.app`. Section copy lives in Nuxt Content (per-locale); UI strings + routing in `@nuxtjs/i18n`. The hero is a raw three.js scene encapsulated in a composable, driven by a Pinia graphics-quality store that degrades to a static poster on low-power/reduced-motion. Dark-only Tailwind design system built from the real logo (pure black + white→silver sheen, no chromatic accent).

**Tech Stack:** Nuxt 4, Vue 3.5 `<script setup>`, `@nuxtjs/i18n`, `@nuxt/content`, Pinia, `@nuxtjs/tailwindcss` (Tailwind 3), three.js (raw), Geist + Geist Mono (self-hosted), Vercel (nitro preset), vitest (logic only), Playwright (smoke + screenshots).

**Spec:** `docs/superpowers/specs/2026-06-15-transitkit-landing-design.md`

**Verification model:** Each visual task ends with (a) `pnpm build` clean and (b) a `designer-eye-qa` pass on the relevant viewport(s). Only genuine logic (graphics-tier resolution, scene lifecycle) gets vitest. Don't write fake unit tests for presentational markup.

---

## File structure (decomposition)

```
landing/
  nuxt.config.ts                      # modules, i18n, content, tailwind, nitro vercel
  tailwind.config.ts                  # dark-only tokens
  app.vue                             # NuxtLayout + NuxtPage
  app.config.ts                       # site meta (name, contact, store URLs)
  assets/
    brand/                            # logo.svg, silhouette.svg + derived
    css/main.css                      # CSS vars (tokens), font-face, base
    fonts/                            # Geist*.woff2, GeistMono*.woff2
  layouts/default.vue                 # SiteNav + slot + SiteFooter
  components/
    site/SiteNav.vue                  # TK mark + nav + LocaleSwitch + CTA
    site/SiteFooter.vue
    site/LocaleSwitch.vue
    ui/CtaButton.vue                  # primary/ghost, magnetic micro-interaction
    ui/Reveal.vue                     # scroll-reveal wrapper (spring)
    hero/NetworkCanvas.client.vue     # mounts three.js scene / poster fallback
    sections/HeroSection.vue
    sections/TrapSection.vue
    sections/PillarsSection.vue
    sections/HowItWorksSection.vue
    sections/PositioningSection.vue
    sections/PricingSection.vue
    sections/LiveProofSection.vue
    sections/FaqSection.vue
    sections/FinalCtaSection.vue
  composables/
    useNetworkScene.ts                # three.js build/animate/teardown
    useReveal.ts                      # IntersectionObserver reveal helper
  stores/
    graphics.ts                       # useGraphicsStore: high|low|static tier
  content/
    en/sections.yml                   # all section copy, EN
    it/sections.yml                   # all section copy, IT
    en/faq.yml  it/faq.yml
  pages/index.vue                     # assembles sections in order
  public/                             # og.png, robots.txt, favicon outputs
  i18n/locales/en.json  it.json       # UI strings (nav, CTA labels, aria)
  tests/graphics.spec.ts              # vitest: tier resolution
  tests/scene.spec.ts                 # vitest: scene lifecycle (jsdom + stub WebGL)
  e2e/smoke.spec.ts                   # playwright: loads, sections present, screenshots
  vitest.config.ts  playwright.config.ts
  package.json  tsconfig.json  vercel.json
```

---

## Task 0: Scaffold the Nuxt app

**Files:**
- Create: `landing/package.json`, `landing/nuxt.config.ts`, `landing/tsconfig.json`, `landing/app.vue`, `landing/.gitignore`

- [ ] **Step 1: Init Nuxt 4 + install deps**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine
pnpm dlx nuxi@latest init landing --packageManager pnpm --gitInit false
cd landing
pnpm add @nuxtjs/i18n @nuxt/content pinia @pinia/nuxt @nuxtjs/tailwindcss three
pnpm add -D @types/three vitest @vitest/ui @playwright/test @vue/test-utils jsdom
```

- [ ] **Step 2: Configure `nuxt.config.ts`**

```ts
export default defineNuxtConfig({
  compatibilityDate: '2025-01-01',
  devtools: { enabled: true },
  modules: ['@nuxtjs/tailwindcss', '@pinia/nuxt', '@nuxt/content', '@nuxtjs/i18n'],
  css: ['~/assets/css/main.css'],
  nitro: { preset: 'vercel' },
  i18n: {
    locales: [
      { code: 'en', language: 'en-US', name: 'English' },
      { code: 'it', language: 'it-IT', name: 'Italiano' },
    ],
    defaultLocale: 'en',
    strategy: 'prefix_except_default',
    bundle: { optimizeTranslationDirective: false },
  },
  app: {
    head: {
      htmlAttrs: { class: 'dark', lang: 'en' },
      meta: [{ name: 'color-scheme', content: 'dark' }],
    },
  },
  typescript: { strict: true },
})
```

- [ ] **Step 3: Verify dev server boots**

Run: `pnpm dev` then `curl -s http://localhost:3000 | head -5`
Expected: HTML response, no module-resolution errors in console. Stop the server.

- [ ] **Step 4: Commit** (only if Andrea has authorized commits this session — otherwise leave staged/working and note it)

```bash
git add landing && git commit -m "feat(landing): scaffold Nuxt 4 app with i18n, content, pinia, tailwind"
```

---

## Task 1: Design tokens, fonts, dark-only Tailwind

**Files:**
- Create: `landing/assets/css/main.css`, `landing/tailwind.config.ts`, `landing/assets/fonts/*`

- [ ] **Step 1: Fetch self-hosted Geist + Geist Mono woff2**

```bash
cd landing && pnpm add @fontsource/geist @fontsource/geist-mono
# copy the woff2 we use into assets/fonts (400/500/600/700/800 + mono 400/500)
```
(Alternatively `@import` the fontsource css in main.css — pick whichever keeps only the weights we use.)

- [ ] **Step 2: Write `assets/css/main.css` with tokens**

```css
@tailwind base; @tailwind components; @tailwind utilities;
:root {
  --bg: #000000; --surface: #0A0B0D; --surface-2: #101216;
  --hairline: rgba(255,255,255,0.08);
  --text: #FFFFFF; --text-2: rgba(255,255,255,0.64); --text-3: rgba(255,255,255,0.40);
}
@layer base {
  html { background: var(--bg); color: var(--text); }
  body { font-family: 'Geist', -apple-system, sans-serif; -webkit-font-smoothing: antialiased; }
  ::selection { background: #fff; color: #000; }
}
.text-sheen { background: linear-gradient(135deg,#fff 0%,#ccc 100%); -webkit-background-clip: text; background-clip: text; color: transparent; }
```

- [ ] **Step 3: Write `tailwind.config.ts` (dark-only, tokens mapped)**

```ts
import type { Config } from 'tailwindcss'
export default {
  darkMode: 'class',
  content: ['./components/**/*.{vue,ts}', './layouts/**/*.vue', './pages/**/*.vue', './app.vue'],
  theme: { extend: {
    fontFamily: { sans: ['Geist', 'sans-serif'], mono: ['Geist Mono', 'monospace'] },
    colors: { bg: 'var(--bg)', surface: 'var(--surface)', 'surface-2': 'var(--surface-2)',
              hairline: 'var(--hairline)', text: 'var(--text)', 'text-2': 'var(--text-2)', 'text-3': 'var(--text-3)' },
  }},
  plugins: [],
} satisfies Config
```

- [ ] **Step 4: Verify** — add a throwaway `<h1 class="text-sheen font-bold text-7xl">TK</h1>` to `app.vue`, run `pnpm dev`, screenshot, confirm pure-black bg + silver sheen heading. Remove the throwaway. `pnpm build` clean.

---

## Task 2: Brand assets + favicons/og from silhouette

**Files:**
- Create: `landing/assets/brand/{logo.svg,silhouette.svg}`, `landing/public/{favicon.ico,icon.svg,apple-touch-icon.png,og.png}`

- [ ] **Step 1: Copy source logos**

```bash
cp ~/GitHub/transitkit/transitkit/design/logo.svg landing/assets/brand/logo.svg
cp ~/GitHub/transitkit/transitkit/design/silhouette.svg landing/assets/brand/silhouette.svg
```

- [ ] **Step 2: Generate favicon set + apple-touch from silhouette** (white mark on black, padded)

```bash
# rsvg-convert or sharp; produce 32/180/512 PNG on #000, then favicon.ico
node scripts/gen-favicons.mjs   # writes public/icon.svg, apple-touch-icon.png, favicon.ico
```
(`gen-favicons.mjs`: render silhouette white, centered at ~66% on black, sizes 32/180/192/512.)

- [ ] **Step 3: Generate `public/og.png`** (1200×630): black bg, TK mark + "Branded transit apps for operators." in Geist, sheen treatment.

- [ ] **Step 4: Wire head links** in `nuxt.config.ts` `app.head.link` (icon, apple-touch) and default og/twitter meta in `app.config.ts`.

- [ ] **Step 5: Verify** — `pnpm build`, check `public/` outputs exist and og.png renders correctly (open it). designer-eye-qa on the og composition.

---

## Task 3: App shell — layout, nav, footer

**Files:**
- Create: `landing/layouts/default.vue`, `landing/components/site/SiteNav.vue`, `landing/components/site/SiteFooter.vue`, `landing/components/site/LocaleSwitch.vue`, `landing/components/ui/CtaButton.vue`, `landing/app.config.ts`
- Modify: `landing/app.vue`

- [ ] **Step 1: `app.config.ts`** — site constants (no copy, just structured data):

```ts
export default defineAppConfig({
  site: {
    name: 'TransitKit', contactEmail: 'andrea@transitkit.app',
    consoleUrl: 'https://console.transitkit.app',
    appStoreUrl: '', playStoreUrl: '', // fill from AppalRider during Task 7
  },
})
```

- [ ] **Step 2: `CtaButton.vue`** — props `variant: 'primary' | 'ghost'`, `to?`. Primary = white fill / black text; ghost = hairline border. Magnetic micro-interaction (translate toward pointer, damped, ≤6px), pressed state, respects `prefers-reduced-motion`. `.accessibilityIdentifier` equivalent: stable `data-testid`.

- [ ] **Step 3: `SiteNav.vue`** — fixed top, blur/vibrancy over content, TK mark (silhouette.svg, explicit `width`/`height` per icon rule), anchor links, `LocaleSwitch`, primary CTA "Book a demo". Hide nav-link row on mobile → compact.

- [ ] **Step 4: `LocaleSwitch.vue`** — EN/IT toggle via `useSwitchLocalePath()`.

- [ ] **Step 5: `SiteFooter.vue`** — contact email, console link, privacy stub link, one-pager.pdf link, locale switch, © line.

- [ ] **Step 6: `layouts/default.vue`** = `<SiteNav/> <slot/> <SiteFooter/>`. `app.vue` = `<NuxtLayout><NuxtPage/></NuxtLayout>`.

- [ ] **Step 7: Verify** — `pnpm dev`, screenshot mobile + desktop: nav blur, mark crisp, CTA hover/press feel right. `pnpm build` clean. designer-eye-qa.

---

## Task 4: i18n strings + Nuxt Content copy (EN)

**Files:**
- Create: `landing/i18n/locales/en.json`, `landing/i18n/locales/it.json`, `landing/content/en/sections.yml`, `landing/content/en/faq.yml`, `landing/content/it/sections.yml`, `landing/content/it/faq.yml`

- [ ] **Step 1: `i18n/locales/en.json`** — UI chrome strings only (nav labels, CTA labels, aria, locale names). Mirror keys in `it.json` (IT translated; EN may be placeholder-equal at first, flagged).

- [ ] **Step 2: `content/en/sections.yml`** — all marketing copy keyed by section, sourced from the spec + `docs/business/outreach/one-pager.md`:
  - hero: h1 "Take back control of your rider experience.", subhead, ctaPrimary "Get your apps in 3 weeks", ctaSecondary "See it live → AppalRider", trustline.
  - trap, pillars[4], howItWorks[3 steps], positioning (+comparison rows), pricing (two tiers + features + always-line), liveProof, finalCta.
- [ ] **Step 3: `content/en/faq.yml`** — 6 Q/A from one-pager (aggregators, fare vendor, escrow, contract, GDPR, procurement).
- [ ] **Step 4: IT files** — translate; if time-boxed, ship EN values with a `# TODO it` marker and track as open item (spec allows IT in follow-up). Do NOT leave empty keys.
- [ ] **Step 5: Verify** — query content in a scratch page, confirm both locales resolve. `pnpm build` clean.

---

## Task 5: Pinia graphics-quality store (with tests)

**Files:**
- Create: `landing/stores/graphics.ts`, `landing/tests/graphics.spec.ts`, `landing/vitest.config.ts`

- [ ] **Step 1: Write failing test `tests/graphics.spec.ts`**

```ts
import { describe, it, expect } from 'vitest'
import { resolveTier } from '~/stores/graphics'

describe('resolveTier', () => {
  it('returns static when reduced motion preferred', () => {
    expect(resolveTier({ reducedMotion: true, webgl: true, deviceMemory: 8, coarsePointer: false })).toBe('static')
  })
  it('returns static when no webgl', () => {
    expect(resolveTier({ reducedMotion: false, webgl: false, deviceMemory: 8, coarsePointer: false })).toBe('static')
  })
  it('returns low on coarse pointer or low memory', () => {
    expect(resolveTier({ reducedMotion: false, webgl: true, deviceMemory: 2, coarsePointer: false })).toBe('low')
    expect(resolveTier({ reducedMotion: false, webgl: true, deviceMemory: 8, coarsePointer: true })).toBe('low')
  })
  it('returns high on capable desktop', () => {
    expect(resolveTier({ reducedMotion: false, webgl: true, deviceMemory: 8, coarsePointer: false })).toBe('high')
  })
})
```

- [ ] **Step 2: Run → fails** — `pnpm vitest run tests/graphics.spec.ts` (Expected: `resolveTier` not exported).

- [ ] **Step 3: Implement `stores/graphics.ts`**

```ts
import { defineStore } from 'pinia'
export type Tier = 'high' | 'low' | 'static'
export interface Caps { reducedMotion: boolean; webgl: boolean; deviceMemory: number; coarsePointer: boolean }
export function resolveTier(c: Caps): Tier {
  if (c.reducedMotion || !c.webgl) return 'static'
  if (c.coarsePointer || c.deviceMemory < 4) return 'low'
  return 'high'
}
export const useGraphicsStore = defineStore('graphics', {
  state: () => ({ tier: 'static' as Tier }),
  actions: {
    detect() {
      if (typeof window === 'undefined') return
      const gl = (() => { try { return !!document.createElement('canvas').getContext('webgl') } catch { return false } })()
      this.tier = resolveTier({
        reducedMotion: window.matchMedia('(prefers-reduced-motion: reduce)').matches,
        webgl: gl,
        deviceMemory: (navigator as any).deviceMemory ?? 8,
        coarsePointer: window.matchMedia('(pointer: coarse)').matches,
      })
    },
  },
})
```

- [ ] **Step 4: Run → passes** — `pnpm vitest run tests/graphics.spec.ts` (Expected: 4 passing).
- [ ] **Step 5: Commit** (if authorized).

---

## Task 6: three.js hero — "living network"

**Files:**
- Create: `landing/composables/useNetworkScene.ts`, `landing/components/hero/NetworkCanvas.client.vue`, `landing/tests/scene.spec.ts`

- [ ] **Step 1: `useNetworkScene.ts`** — pure-ish factory returning `{ start(canvas, opts), stop(), setTier(tier) }`. Responsibilities:
  - Build scene: `#000` bg, exponential fog.
  - Nodes: 40–60 points in organic 3D layout (seeded, deterministic — no `Math.random()` at module load; seed via a passed value), `THREE.Points` instanced, sheen-tinted.
  - Edges: subset of near-node pairs as `QuadraticBezierCurve3` → `TubeGeometry` (thin) or line segments; cohere into "routes".
  - Pulses: sprites travelling along edges via animated curve param `t`, additive blending.
  - Post: `EffectComposer` + `UnrealBloomPass` (restrained strength). Skip composer on `low` tier (raw render, no bloom, fewer nodes).
  - Camera: slow drift + damped pointer parallax.
  - Loop: `requestAnimationFrame`; expose `stop()` that cancels RAF + disposes geometries/materials/renderer.
  - `pixelRatio` capped at `min(devicePixelRatio, 2)`; on `low`, cap at 1.5 and halve node/pulse counts.

- [ ] **Step 2: `NetworkCanvas.client.vue`** (client-only by filename):
  - On mount: `useGraphicsStore().detect()`. If `tier === 'static'` → render `<img>` poster (a pre-baked frame of the network, generate to `public/hero-poster.webp`) — no WebGL.
  - Else: create canvas, `useNetworkScene().start(canvas, { tier })`.
  - `IntersectionObserver`: `stop()`/`start()` as hero enters/leaves viewport. `visibilitychange`: pause on hidden.
  - `onBeforeUnmount`: `stop()`.

- [ ] **Step 3: `tests/scene.spec.ts`** (jsdom + stubbed WebGL) — assert `start()` then `stop()` cancels RAF and nulls the renderer without throwing; assert `low` tier produces fewer nodes than `high` (expose counts for test). Run → red → implement count exposure → green.

- [ ] **Step 4: Generate `public/hero-poster.webp`** — capture one high-tier frame (Playwright screenshot of the canvas) and save as the static/reduced-motion poster.

- [ ] **Step 5: Verify** — `pnpm dev` on desktop: 60fps drift, pulses with restrained bloom, pointer parallax. Throttle to mobile emulation → confirm `low`/`static` path. Force `prefers-reduced-motion` → poster shows, no canvas. designer-eye-qa: "does the hero read as a living transit network, premium, not a generic blob?" `pnpm build` clean.

---

## Task 7: Section components + assemble page

**Files:**
- Create: `landing/components/sections/*.vue` (Hero, Trap, Pillars, HowItWorks, Positioning, Pricing, LiveProof, Faq, FinalCta), `landing/components/ui/Reveal.vue`, `landing/composables/useReveal.ts`
- Modify: `landing/pages/index.vue`

- [ ] **Step 1: `useReveal.ts` + `Reveal.vue`** — IntersectionObserver-driven reveal; spring-feel transform/opacity on enter; no-op under `prefers-reduced-motion`.
- [ ] **Step 2: `HeroSection.vue`** — `<NetworkCanvas/>` full-bleed behind; H1 (`text-sheen`), subhead, CTA row (primary + ghost), trustline. Copy from content `sections.hero`.
- [ ] **Step 3: `TrapSection.vue`** — "tenant in someone else's app"; aggregator contrast.
- [ ] **Step 4: `PillarsSection.vue`** — 4 pillar cards (`v-for` over content), each with a small purposeful micro-interaction on hover. Lucide icons via `lucide-vue-next` with explicit `:size`/width+height.
- [ ] **Step 5: `HowItWorksSection.vue`** — 3 steps, mono step numbers, "live in 3 weeks", "$0 upfront".
- [ ] **Step 6: `PositioningSection.vue`** — comparison table (custom enterprise build vs TransitKit), anchored on ownership + no upfront.
- [ ] **Step 7: `PricingSection.vue`** — two cards: **Standard $499/mo**, **Pro $799/mo (+ payments)**. Mono price figures. Feature lists from content. Always-line: $0 setup · 3-week go-live · cancel anytime. Pro card subtly emphasized (hairline glow), not loud.
- [ ] **Step 8: `LiveProofSection.vue`** — AppalRider: app screenshots + App Store/Play badges (fill real URLs into `app.config.ts`). If URLs unknown at build, use placeholder buttons disabled + flag.
- [ ] **Step 9: `FaqSection.vue`** — accordion from `faq.yml`, accessible (`button`/`aria-expanded`), spring height.
- [ ] **Step 10: `FinalCtaSection.vue`** — "Get your branded transit apps." + primary CTA + contact.
- [ ] **Step 11: `pages/index.vue`** — import + order all sections; set per-page SEO (`useSeoMeta`) from content + og.
- [ ] **Step 12: Verify** — `pnpm dev`, full-page screenshots mobile + desktop + tablet(768). `pnpm build` clean. **designer-eye-qa full pass** (in-scope criteria + out-of-scope defects: alignment, contrast AA+, spacing rhythm, type hierarchy).

---

## Task 8: Motion & interaction polish

**Files:**
- Modify: section components, `CtaButton.vue`, `Reveal.vue`

- [ ] **Step 1:** Calibrate scroll-reveal stagger (spring damping, ≤120ms stagger) — never linear/ease-generic.
- [ ] **Step 2:** CTA magnetic + pressed feedback final tuning; ensure no motion when reduced-motion.
- [ ] **Step 3:** Hero→page continuity: subtle camera depth shift as first section scrolls out (hero only, cheap).
- [ ] **Step 4:** Verify perceived speed: no animation blocks first paint; hero canvas lazy/`client`. designer-eye-qa: motion "significant, not decorative".

---

## Task 9: Performance, a11y, SEO pass

**Files:**
- Create: `landing/e2e/smoke.spec.ts`, `landing/playwright.config.ts`, `landing/public/robots.txt`
- Modify: `nuxt.config.ts` (routeRules / prerender)

- [ ] **Step 1:** `nuxt.config.ts` → prerender the index (`routeRules: { '/': { prerender: true }, '/it': { prerender: true } }`), static og/robots.
- [ ] **Step 2:** `e2e/smoke.spec.ts` (Playwright): page loads, all 9 section testids present, locale switch routes to `/it`, no console errors. Captures full-page screenshots (desktop/mobile) as artifacts.
- [ ] **Step 3:** Run Lighthouse (or `pnpm dlx unlighthouse` / Playwright + lighthouse) → Performance ≥ 90 mobile, a11y ≥ 95. Fix offenders (font preload, image dimensions, contrast).
- [ ] **Step 4:** Verify — `pnpm vitest run` (all green), `pnpm exec playwright test` (green), `pnpm build` clean.

---

## Task 10: Vercel deploy

**Files:**
- Create: `landing/vercel.json`

- [ ] **Step 1:** `vercel.json` — framework nuxt, security headers (CSP allowing self + fonts, `X-Content-Type-Options`, etc.), cache headers for static.
- [ ] **Step 2:** Link a **new** Vercel project (root dir `landing/`) — do NOT reuse the `web/` project. `pnpm dlx vercel link` + `vercel --prod=false` for a preview URL first.
- [ ] **Step 3:** Verify preview URL renders identically to local (hero, all sections, both locales). Hand the preview URL to Andrea for sign-off before attaching `transitkit.app`.
- [ ] **Step 4:** Attach `transitkit.app` apex domain to the project (Andrea confirms DNS). Production deploy.

---

## Self-review notes

- **Spec coverage:** hero/3D (Task 6), trap+pillars+how+positioning+pricing+proof+faq+finalCTA (Task 7), i18n+content (Task 4), tokens/type/dark-only (Task 1), brand-from-silhouette (Task 2), pricing 499/799 (Task 7 Step 7), perf/fallbacks (Tasks 6, 9), deploy (Task 10). Pinia (Task 5), Nuxt Content (Task 4), i18n (Tasks 0/4) all present per request.
- **No-color-accent** enforced in Task 1 tokens (no accent color defined) and Task 3 CTA (white/ghost only).
- **Icon rule:** explicit width/height called out in Task 3 Step 3 and Task 7 Step 4.
- **No-branch rule:** plan works in-place on `main`; commit steps gated on Andrea's authorization (his rule: commit only when asked).
- **Open items** (from spec): real AppalRider URLs (Task 7 Step 8), og composition (Task 2), IT copy may follow (Task 4 Step 4).
```
