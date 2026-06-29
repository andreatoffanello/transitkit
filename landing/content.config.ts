import { defineContentConfig, defineCollection, z } from '@nuxt/content'

// Section copy lives per-locale as data files: content/<locale>/sections.yml + faq.yml.
// Components query by id (e.g. 'en/sections') so copy is editable without touching markup.

const cta = z.object({ label: z.string(), to: z.string().optional() })

const sections = defineCollection({
  type: 'data',
  source: '**/sections.yml',
  schema: z.object({
    hero: z.object({
      h1: z.string(),
      sub: z.string(),
      ctaPrimary: cta,
      ctaSecondary: cta,
      trustline: z.string(),
    }),
    trap: z.object({
      eyebrow: z.string(),
      title: z.string(),
      body: z.string(),
      points: z.array(z.string()),
    }),
    pillars: z.object({
      eyebrow: z.string(),
      title: z.string(),
      items: z.array(z.object({ icon: z.string(), title: z.string(), body: z.string() })),
    }),
    pedigree: z.object({
      eyebrow: z.string(),
      title: z.string(),
      intro: z.string(),
      items: z.array(
        z.object({
          icon: z.string(),
          name: z.string(),
          place: z.string(),
          body: z.string(),
          ios: z.string(),
          android: z.string(),
        }),
      ),
    }),
    how: z.object({
      eyebrow: z.string(),
      title: z.string(),
      steps: z.array(z.object({ n: z.string(), title: z.string(), body: z.string() })),
      note: z.string(),
    }),
    positioning: z.object({
      eyebrow: z.string(),
      title: z.string(),
      body: z.string(),
      colThem: z.string(),
      colUs: z.string(),
      rows: z.array(z.object({ them: z.string(), us: z.string() })),
      anchor: z.string(),
    }),
    pricing: z.object({
      eyebrow: z.string(),
      title: z.string(),
      sub: z.string(),
      tiers: z.array(
        z.object({
          name: z.string(),
          price: z.string(),
          period: z.string(),
          priceNote: z.string().optional(),
          tagline: z.string(),
          features: z.array(z.string()),
          cta: cta,
          highlighted: z.boolean().optional(),
          badge: z.string().optional(),
        }),
      ),
      always: z.array(z.string()),
    }),
    proof: z.object({
      eyebrow: z.string(),
      title: z.string(),
      body: z.string(),
    }),
    finalCta: z.object({
      title: z.string(),
      body: z.string(),
      cta: cta,
    }),
  }),
})

const faq = defineCollection({
  type: 'data',
  source: '**/faq.yml',
  schema: z.object({
    items: z.array(z.object({ q: z.string(), a: z.string() })),
  }),
})

export default defineContentConfig({
  collections: { sections, faq },
})
