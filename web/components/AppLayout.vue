<template>
  <div class="min-h-screen" style="background-color: var(--bg-secondary)">
    <AppSidebar />
    <main class="app-main lg:ml-60 min-h-screen" style="background-color: var(--bg-secondary)">
      <slot />
    </main>
    <AppTabBar />
  </div>
</template>
