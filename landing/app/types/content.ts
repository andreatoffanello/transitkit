export interface Cta { label: string; to?: string }

export interface SectionsContent {
  hero: { h1: string; sub: string; ctaPrimary: Cta; ctaSecondary: Cta; trustline: string }
  trap: { eyebrow: string; title: string; body: string; points: string[] }
  pillars: { eyebrow: string; title: string; items: { icon: string; title: string; body: string }[] }
  pedigree: {
    eyebrow: string
    title: string
    intro: string
    items: { icon: string; name: string; place: string; body: string; ios: string; android: string }[]
  }
  how: { eyebrow: string; title: string; steps: { n: string; title: string; body: string }[]; note: string }
  positioning: {
    eyebrow: string; title: string; body: string; colThem: string; colUs: string
    rows: { them: string; us: string }[]; anchor: string
  }
  pricing: {
    eyebrow: string; title: string; sub: string
    tiers: { name: string; price: string; period: string; priceNote?: string; tagline: string; features: string[]; cta: Cta; highlighted?: boolean; badge?: string }[]
    always: string[]
  }
  proof: { eyebrow: string; title: string; body: string }
  finalCta: { title: string; body: string; cta: Cta }
}

export interface FaqContent { items: { q: string; a: string }[] }
