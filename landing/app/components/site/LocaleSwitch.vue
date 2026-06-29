<script setup lang="ts">
const { locale, locales } = useI18n()
const switchLocalePath = useSwitchLocalePath()
const { t } = useI18n()

const available = computed(() => locales.value.map((l) => (typeof l === 'string' ? { code: l } : l)))
</script>

<template>
  <nav class="locale" :aria-label="t('a11y.switchLanguage')">
    <NuxtLink
      v-for="l in available"
      :key="l.code"
      :to="switchLocalePath(l.code)"
      class="locale__item"
      :class="{ 'is-active': l.code === locale }"
    >
      {{ l.code.toUpperCase() }}
    </NuxtLink>
  </nav>
</template>

<style scoped>
.locale {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px;
  border: 1px solid var(--hairline);
  border-radius: 8px;
}
.locale__item {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: var(--text-3);
  padding: 0.25rem 0.5rem;
  border-radius: 6px;
  transition: color 0.2s, background 0.2s;
}
.locale__item:hover { color: var(--text-2); }
.locale__item.is-active { color: #000; background: #fff; }
</style>
