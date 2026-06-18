// Web-only feature gates.
//
// `enableMap` in the operator config is SHARED with the native apps, where the
// map is a real, shipping feature. On the web the map page is still a placeholder
// (`pages/map.vue`), so we hide its nav tab independently of `enableMap` until the
// web map is real. Flip this to `true` once the web map ships.
export const WEB_MAP_READY = false
