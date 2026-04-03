<template>
  <div class="min-h-screen" style="background-color: var(--bg-secondary)">
    <AppSidebar />
    <main class="lg:ml-60 pb-20 lg:pb-8 min-h-screen" style="background-color: var(--bg-secondary)">
      <slot />
    </main>
    <AppTabBar />
  </div>
</template>
