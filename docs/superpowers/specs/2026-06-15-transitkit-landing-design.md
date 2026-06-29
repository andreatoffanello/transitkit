# TransitKit — Landing Page Design Spec

> Date: 2026-06-15 · Status: approved (design), pre-implementation
> Owner: Andrea Toffanello

## Goal

Marketing landing page for **TransitKit** — the product that ships branded native
iOS + Android apps (plus web stop pages, QR, push) for small transit operators on
top of their existing GTFS / GTFS-RT feeds.

One job: convert a transit operator's executive (GM / director) into a booked demo.
The emotional pitch is **ownership and control**: stop being a tenant in an
aggregator (Moovit, Transit) and take back your rider-facing channel — at
big-vendor quality, no upfront, $499/mo.

## Locked decisions

| Topic | Decision |
|---|---|
| Location | New `landing/` directory inside the `transit-engine` monorepo (sibling of `web/`). Independent Nuxt app, separate Vercel project, root domain `transitkit.app`. |
| Language | **EN default**, **IT** second locale. `@nuxtjs/i18n`. |
| Stack | Nuxt 4 (latest), Vue 3.5 `<script setup>`, Pinia, Nuxt Content (per-locale copy), `@nuxtjs/i18n`, Tailwind (dark-only, own tokens), **three.js (raw)** for the hero. |
| Theme | **Dark-only.** Pure black base, monochrome-luminous accent (the logo's white→silver sheen). **No color accent.** |
| 3D | **Concept A — "The living network"** (see below). |
| Pricing | **Standard $499/mo · Pro $799/mo (+ payments module)**. $0 setup, 3-week go-live, cancel anytime. |
| Deploy | Vercel, new project, `transitkit.app` root. `console.` and `rt.` subdomains unchanged. |

## Brand facts (from real logo)

Source files: `~/GitHub/transitkit/transitkit/design/{logo,silhouette}.{svg,png}`
(copied into `landing/assets/brand/`).

- Mark: bold geometric **"TK"** monogram, heavy condensed letterforms.
- Fill: linear gradient **#FFFFFF → #CCCCCC** (top-left → bottom-right) — a metallic
  white→silver *sheen*, NOT a chromatic gradient.
- Background in `logo.{svg,png}`: true black `#000000`.
- `silhouette.svg` = same path, single-color, transparent → source for derived assets
  (favicon, og-image, nav mark, 3D texture, monochrome masks).

### Design tokens

```
--bg:            #000000      /* true black, matches logo bg */
--surface:       #0A0B0D      /* cards, panels */
--surface-2:     #101216      /* elevated */
--hairline:      rgba(255,255,255,0.08)
--text:          #FFFFFF      /* primary, opacity 1 */
--text-2:        rgba(255,255,255,0.64)
--text-3:        rgba(255,255,255,0.40)
--sheen:         linear-gradient(135deg, #FFFFFF 0%, #CCCCCC 100%)  /* H1 display, mark, pulse cores */
```

No functional accent color. CTA = high-contrast white fill on black; secondary = hairline-bordered ghost.

### Type

- Display + body: **Geist** (self-hosted woff2). Heavy weight for H1 to echo the mark.
- Technical accents (GTFS, feed labels, codes, pricing figures): **Geist Mono**.
- Scale: system 4/8/16/24/32 spacing; optical alignment; body contrast AA+.

## 3D — Concept A: "The living network"

Abstract transit network as a 3D constellation. Coherent because GTFS *is* a graph;
reads as "transit" without drawing a bus; "signal flowing through the network" maps
1:1 onto realtime + push + "own your channel".

- **Scene:** `#000` background, exponential fog for depth.
- **Nodes (stops):** ~40–60 points in loose organic 3D layout, instanced. Small glowing dots, sheen-tinted.
- **Edges (routes):** curved tubes/lines (CatmullRom/quadratic bezier) connecting near nodes; a subset forms coherent "routes".
- **Pulses (realtime signal):** bright sprites travel along edges (animated offset), white core → silver falloff, additive blending + bloom. These are the only "alive" elements.
- **Camera:** slow continuous drift + damped pointer parallax. Hero only; subtle depth shift tied to scroll on the first viewport.
- **Post:** `UnrealBloomPass` (restrained). Optional cheap DoF.
- **Performance & fallbacks:**
  - Instanced geometry; `pixelRatio` capped at 2.
  - `IntersectionObserver` → stop the render loop when hero is off-screen; pause on tab blur.
  - Pinia `useGraphicsStore` resolves a runtime quality tier: `high | low | static`.
    - `prefers-reduced-motion` → render one static frame (poster).
    - Coarse pointer / low `deviceMemory` / no WebGL → static gradient + poster image, no live canvas.
  - Hero ships at 60fps or it doesn't ship. Must not dent perceived speed.

## Page structure (single page, narrative scroll)

1. **Hero** — full-bleed 3D canvas. TK mark. H1 *"Take back control of your rider experience."* Subhead. Primary CTA *"Get your apps in 3 weeks"*, secondary *"See it live → AppalRider"*. Trust line: native iOS + Android · web + QR · push you control.
2. **The trap** — *"You're a tenant in someone else's app."* Contrast vs aggregators: their brand, zero channel control, no push of your own.
3. **What you own** — four pillars, each with a purposeful micro-interaction:
   - Branded native iOS + Android apps (your logo, colors, icon).
   - Push you control — alert-automated **and** direct messages, even non-service comms. Your direct channel to riders.
   - Web stop-departure pages + **QR generator** to manage in-stop QR codes.
   - Ordinary maintenance & bugfixing included (custom/evolutive work quoted separately).
4. **How it works** — 3 steps: *You publish clean GTFS(-RT) → we build & brand → live in 3 weeks.* Zero upfront.
5. **Same quality, none of the lock-in** — positioning. Anchor = the cost of a custom enterprise build (5–6 figures upfront) vs TransitKit $499/mo, no upfront, it's *yours*. Comparison table.
6. **Pricing** — two tiers, transparent (anti-incumbent weapon):
   - **Standard $499/mo** ($4,990/yr, 2 months free): native apps + push + web/QR + realtime + console + maintenance.
   - **Pro $799/mo**: everything in Standard **+ payment/fare management module**. Replaces two vendors.
   - Always: $0 setup · 3-week go-live · cancel anytime.
7. **Live proof** — AppalRider showcase (App Store + Google Play links, app screenshots).
8. **FAQ** — reuse from `docs/business/outreach/one-pager.md` (vs aggregators, fare vendor coexistence, source escrow, contract terms, GDPR/CCPA, public procurement).
9. **Final CTA + footer** — *"Get your branded transit apps."* Contact (andrea@transitkit.app), links (console, privacy, one-pager.pdf), locale switch.

## Content & i18n architecture

- UI strings + routing via `@nuxtjs/i18n` (EN default, IT secondary, `prefix_except_default` strategy).
- Section copy (hero, pillars, FAQ, pricing rows) in **Nuxt Content** markdown/yaml, per locale: `content/en/*`, `content/it/*`. Copy editable without touching components.
- Pinia stores: `useGraphicsStore` (3D quality tier), `useUiStore` (locale/theme bookkeeping if needed). Kept minimal — present because requested, not over-applied.

## Tech / file layout

```
landing/
  nuxt.config.ts        # i18n, content, tailwind, nitro vercel preset
  tailwind.config.ts    # dark-only tokens
  app.vue
  assets/
    brand/              # logo.svg, silhouette.svg, derived favicons/og
    css/main.css        # tokens, fonts
    fonts/              # Geist, Geist Mono woff2
  components/
    hero/NetworkCanvas.client.vue   # three.js scene (client-only)
    sections/*.vue                  # one per page section
    ui/*.vue                        # CTA, LocaleSwitch, ...
  composables/
    useNetworkScene.ts  # three.js setup/teardown, quality tiers
  content/{en,it}/*     # section copy
  stores/               # pinia
  pages/index.vue
  public/               # og image, robots, etc.
```

## Out of scope (this iteration)

- Live billing/checkout (Paddle) — CTAs go to demo/contact only; billing wired later.
- Blog / CMS beyond the marketing page.
- Multi-page site (legal/privacy can be a stub or link to existing).

## Open items / to confirm during build

- Exact AppalRider App Store / Play URLs and screenshots for the Live proof section.
- Final og-image composition (generated from silhouette).
- IT copy: ship EN first, IT translation can land in the same PR or a follow-up.
