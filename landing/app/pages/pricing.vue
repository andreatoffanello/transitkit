<script setup lang="ts">
import type { SectionsContent } from '~/types/content'

const { locale, t } = useI18n()
const { site } = useAppConfig()

const { data } = await useAsyncData(
  `pricing-${locale.value}`,
  async () => {
    const sections = await queryCollection('sections').all()
    const loc = locale.value
    const pick = <T,>(rows: { stem?: string }[]) =>
      (rows.find((r) => r.stem?.startsWith(`${loc}/`)) ?? rows.find((r) => r.stem?.startsWith('en/'))) as T
    return { sections: pick<SectionsContent>(sections) }
  },
  { watch: [locale] },
)

const pricing = computed(() => data.value?.sections?.pricing)

useHead({ htmlAttrs: { lang: locale } })
useSeoMeta({
  title: () => `${t('nav.pricing')} — ${site.name}`,
  description: () => pricing.value?.sub ?? '',
  ogTitle: () => `${t('nav.pricing')} — ${site.name}`,
  ogDescription: () => pricing.value?.sub ?? '',
})
</script>

<template>
  <div v-if="pricing" class="pricingpage">
    <PricingSection :data="pricing" />
  </div>
</template>

<style scoped>
/* clear the fixed nav and give the standalone page a little air above the grid */
.pricingpage { padding-top: clamp(40px, 6vw, 80px); }
</style>
