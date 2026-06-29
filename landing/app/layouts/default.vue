<template>
  <div class="site">
    <div class="site__field" aria-hidden="true" />
    <SiteNav />
    <main>
      <slot />
    </main>
    <SiteFooter />
  </div>
</template>

<style scoped>
.site { position: relative; min-height: 100vh; isolation: isolate; }
/* the hero's dither, as a faint engineered film across the whole page — gives the
   dark scroll a continuous texture instead of a flat void. Neutral (cyan stays a
   signal accent); the hero canvas paints over its own area. */
.site__field {
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background-image: radial-gradient(rgba(255, 255, 255, 0.5) 0.5px, transparent 0.85px);
  background-size: 4px 4px;
  opacity: 0.15;
}
</style>
