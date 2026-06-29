<script setup lang="ts">
const { t } = useI18n()
const { site } = useAppConfig()
const year = 2026
</script>

<template>
  <footer class="footer">
    <div class="footer__inner">
      <div class="footer__brand">
        <img src="~/assets/brand/mark.svg" alt="" width="34" height="21" />
        <p class="footer__tag">{{ t('footer.tagline') }}</p>
      </div>

      <nav class="footer__links" aria-label="Footer">
        <a :href="site.consoleUrl">{{ t('footer.console') }}</a>
        <NuxtLinkLocale to="/privacy">{{ t('footer.privacy') }}</NuxtLinkLocale>
        <a :href="`mailto:${site.contactEmail}`">{{ site.contactEmail }}</a>
        <LocaleSwitch />
      </nav>
    </div>

    <div class="footer__base">
      <span>© {{ year }} {{ site.name }}. {{ t('footer.rights') }}</span>
      <span class="footer__domain">{{ site.domain }}</span>
    </div>
  </footer>
</template>

<style scoped>
.footer {
  border-top: 1px solid var(--hairline);
  padding: 64px 24px 40px;
  margin-top: 80px;
}
.footer__inner {
  max-width: 1120px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 32px;
  justify-content: space-between;
}
.footer__brand { display: flex; flex-direction: column; gap: 14px; }
.footer__tag { color: var(--text-2); font-size: 0.9375rem; max-width: 22ch; }
.footer__links {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 20px;
}
.footer__links a { color: var(--text-2); font-size: 0.9375rem; transition: color 0.2s; }
.footer__links a:hover { color: var(--text); }
.footer__base {
  max-width: 1120px;
  margin: 40px auto 0;
  padding-top: 24px;
  border-top: 1px solid var(--hairline);
  display: flex;
  justify-content: space-between;
  color: var(--text-3);
  font-size: 0.8125rem;
}
.footer__domain { font-family: 'Geist Mono', monospace; letter-spacing: 0.04em; }
@media (min-width: 768px) {
  .footer__inner { flex-direction: row; align-items: flex-start; }
}
</style>
