<template>
  <span
    :aria-label="ariaLabel"
    class="inline-flex items-center justify-center px-2 py-0.5 text-xs font-bold leading-none shrink-0"
    style="border-radius: 4px; min-width: 2rem;"
    :style="{
      backgroundColor: bgColor,
      color: fgColor,
    }"
  >
    {{ name }}
  </span>
</template>

<script setup lang="ts">
import { normalizeHex, readableTextColor } from '~/utils/color'
import { getStrings } from '~/utils/strings'

const props = defineProps<{
  name: string
  color?: string | null
  textColor?: string | null
  locale?: string
}>()

const bgColor = computed(() =>
  props.color ? normalizeHex(props.color) : 'var(--color-primary)'
)
// Prefer the feed's route_text_color; if it's blank (common — every AppalCART
// route leaves it empty) derive a legible black/white from the background
// luminance instead of blindly using white, which fails WCAG on light routes.
const fgColor = computed(() => {
  if (props.textColor) return normalizeHex(props.textColor)
  if (props.color) return readableTextColor(props.color)
  return 'var(--color-text-on-primary)'
})
const ariaLabel = computed(() => `${getStrings(props.locale).lineLabel} ${props.name}`)
</script>
