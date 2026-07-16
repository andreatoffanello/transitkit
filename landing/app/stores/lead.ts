import { defineStore } from 'pinia'

// Shared state for the "Book a demo" lead modal. Every demo CTA on the site
// (nav, hero, pricing tiers, final CTA) opens the same single modal via show().
export const useLeadStore = defineStore('lead', () => {
  const open = ref(false)
  const plan = ref('') // optional pre-selected plan (e.g. "Standard"), shown as a chip

  function show(selectedPlan = '') {
    plan.value = selectedPlan
    open.value = true
  }
  function hide() {
    open.value = false
  }

  return { open, plan, show, hide }
})
