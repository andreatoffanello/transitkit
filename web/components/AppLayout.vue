<template>
  <div class="min-h-screen" style="background-color: var(--bg-secondary)">
    <AppDownloadBanner
      :app-links="config?.appLinks"
      :operator-name="config?.name"
      :locale="config?.locale?.[0]"
    />
    <AppSidebar />
    <main
      class="app-main lg:ml-60 min-h-screen"
      style="background-color: var(--bg-secondary)"
    >
      <slot />
    </main>
    <AppTabBar />
  </div>
</template>

<script setup lang="ts">
const { config } = await useOperator()
</script>
