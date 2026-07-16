<template>
  <div class="min-h-screen" style="background-color: var(--bg-secondary)">
    <AppDownloadBanner
      :app-links="config?.appLinks"
      :operator-name="config?.name"
      :locale="config?.locale?.[0]"
    />
    <AppTopBar />
    <main
      class="app-main lg:pt-4 min-h-screen"
      style="background-color: var(--bg-secondary); padding-bottom: var(--download-banner-offset, 0px)"
    >
      <slot />
    </main>
    <AppTabBar />
  </div>
</template>

<script setup lang="ts">
const { config } = await useOperator()
</script>
