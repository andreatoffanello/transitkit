<script setup lang="ts">
const props = withDefaults(defineProps<{
  to: string
  variant?: 'primary' | 'ghost'
}>(), { variant: 'primary' })

const el = ref<HTMLElement | null>(null)
const tx = ref(0)
const ty = ref(0)

// "#book-demo" (optionally "#book-demo/Standard") opens the shared lead modal
// instead of navigating — it is the single capture point for every demo CTA.
const isLead = computed(() => props.to === '#book-demo' || props.to.startsWith('#book-demo/'))
const leadPlan = computed(() => {
  const slash = props.to.indexOf('/')
  return slash === -1 ? '' : decodeURIComponent(props.to.slice(slash + 1))
})
const lead = useLeadStore()
function onLeadClick(e: MouseEvent) {
  e.preventDefault()
  lead.show(leadPlan.value)
}

const isHash = computed(() => !isLead.value && props.to.startsWith('#'))
const isExternal = computed(() => /^(https?:|mailto:|tel:)/.test(props.to))

let reduced = false
onMounted(() => {
  reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
})

function onMove(e: PointerEvent) {
  if (reduced || !el.value) return
  const r = el.value.getBoundingClientRect()
  const dx = e.clientX - (r.left + r.width / 2)
  const dy = e.clientY - (r.top + r.height / 2)
  tx.value = Math.max(-6, Math.min(6, dx * 0.18))
  ty.value = Math.max(-6, Math.min(6, dy * 0.3))
}
function reset() { tx.value = 0; ty.value = 0 }

const router = useRouter()
const localePath = useLocalePath()

function onHashClick(e: MouseEvent) {
  if (!isHash.value) return
  e.preventDefault()
  const target = document.querySelector(props.to)
  if (target) {
    target.scrollIntoView({ behavior: reduced ? 'auto' : 'smooth' })
  } else {
    // The target section only lives on the homepage; from a sub-page (e.g. the
    // "Book a demo" CTA in the shared nav on /terms or /pricing) navigate to the
    // localized homepage + hash instead of silently doing nothing.
    router.push(`${localePath('/')}${props.to}`)
  }
}

const cls = computed(() => [
  'cta',
  props.variant === 'primary' ? 'cta--primary' : 'cta--ghost',
])
const style = computed(() => ({ transform: `translate(${tx.value}px, ${ty.value}px)` }))
</script>

<template>
  <button
    v-if="isLead"
    ref="el"
    type="button"
    :class="cls"
    :style="style"
    @click="onLeadClick"
    @pointermove="onMove"
    @pointerleave="reset"
  >
    <span><slot /></span>
  </button>
  <a
    v-else-if="isExternal"
    ref="el"
    :href="to"
    :class="cls"
    :style="style"
    @pointermove="onMove"
    @pointerleave="reset"
  >
    <span><slot /></span>
  </a>
  <a
    v-else-if="isHash"
    ref="el"
    :href="to"
    :class="cls"
    :style="style"
    @click="onHashClick"
    @pointermove="onMove"
    @pointerleave="reset"
  >
    <span><slot /></span>
  </a>
  <NuxtLinkLocale
    v-else
    ref="el"
    :to="to"
    :class="cls"
    :style="style"
    @pointermove="onMove"
    @pointerleave="reset"
  >
    <span><slot /></span>
  </NuxtLinkLocale>
</template>

<style scoped>
.cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  height: 2.875rem;
  padding: 0 1.25rem;
  border-radius: 0.625rem;
  font-weight: 600;
  font-size: 0.9375rem;
  font-family: inherit;
  letter-spacing: -0.01em;
  white-space: nowrap;
  /* button/anchor parity: variants below (same specificity, later) reset bg/border */
  border: 0;
  background: transparent;
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
  transition: transform 0.25s cubic-bezier(0.2, 0.9, 0.2, 1), background 0.2s, border-color 0.2s, opacity 0.2s;
  will-change: transform;
}
.cta:active { transform: scale(0.97) !important; }
.cta--primary {
  background: #fff;
  color: #000;
}
.cta--primary:hover { background: #f0f0f0; }
.cta--ghost {
  border: 1px solid var(--hairline-strong);
  color: var(--text);
  background: transparent;
}
.cta--ghost:hover { border-color: rgba(255, 255, 255, 0.28); background: rgba(255, 255, 255, 0.04); }
</style>
