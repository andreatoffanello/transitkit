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
import { normalizeHex } from '~/utils/color'
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
const fgColor = computed(() =>
  props.textColor ? normalizeHex(props.textColor) : 'var(--color-text-on-primary)'
)
const ariaLabel = computed(() => `${getStrings(props.locale).lineLabel} ${props.name}`)
</script>
