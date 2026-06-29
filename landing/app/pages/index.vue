<script setup lang="ts">
import type { SectionsContent, FaqContent } from '~/types/content'

const { locale, t } = useI18n()
const { site } = useAppConfig()

const { data } = await useAsyncData(
  // locale-specific key so each locale prerenders its own content (a static key
  // makes the /it prerender reuse the cached English payload from /)
  `landing-content-${locale.value}`,
  async () => {
    const [sections, faq] = await Promise.all([
      queryCollection('sections').all(),
      queryCollection('faq').all(),
    ])
    const loc = locale.value
    const pick = <T,>(rows: { stem?: string }[]) =>
      (rows.find((r) => r.stem?.startsWith(`${loc}/`)) ?? rows.find((r) => r.stem?.startsWith('en/'))) as T
    return {
      sections: pick<SectionsContent>(sections),
      faq: pick<FaqContent>(faq),
    }
  },
  { watch: [locale] },
)

const s = computed(() => data.value?.sections)

useSeoMeta({
  title: () => t('seo.title'),
  description: () => s.value?.hero.sub ?? '',
  ogTitle: () => t('seo.title'),
  ogDescription: () => s.value?.hero.sub ?? '',
  ogImage: '/og.png',
  ogType: 'website',
  twitterCard: 'summary_large_image',
  twitterImage: '/og.png',
})
</script>

<template>
  <div v-if="data?.sections">
    <HeroSection :data="data.sections.hero" />
    <TrapSection :data="data.sections.trap" />
    <PedigreeSection :data="data.sections.pedigree" />
    <PillarsSection :data="data.sections.pillars" />
    <HowItWorksSection :data="data.sections.how" />
    <PositioningSection :data="data.sections.positioning" />
    <LiveProofSection :data="data.sections.proof" />
    <PricingSection :data="data.sections.pricing" />
    <FaqSection v-if="data.faq" :data="data.faq" :title="t('faq.heading')" />
    <FinalCtaSection :data="data.sections.finalCta" />
  </div>
</template>
