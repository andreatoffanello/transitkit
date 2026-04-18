<template>
  <div class="min-h-screen" style="background-color: var(--bg-secondary)">
    <AppDownloadBanner
      :app-links="config?.appLinks"
      :operator-name="config?.name"
    />
    <AppSidebar />
    <main
      class="app-main lg:ml-60 min-h-screen"
      :class="bannerVisible ? 'pt-[44px]' : ''"
      style="background-color: var(--bg-secondary)"
    >
      <slot />
    </main>
    <AppTabBar />
  </div>
</template>

<script setup lang="ts">
const { config } = await useOperator()

const bannerVisible = ref(false)

const onBannerDismissed = () => { bannerVisible.value = false }

onMounted(() => {
  const hasLink = !!(config.value?.appLinks?.ios || config.value?.appLinks?.android)
  bannerVisible.value = hasLink && localStorage.getItem('app-banner-dismissed') !== '1'
  window.addEventListener('app-banner-dismissed', onBannerDismissed)
})

onUnmounted(() => {
  window.removeEventListener('app-banner-dismissed', onBannerDismissed)
})
</script>
