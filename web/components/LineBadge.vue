<template>
  <span
    :aria-label="ariaLabel"
    class="inline-flex items-center justify-center rounded-md px-2 py-0.5 text-xs font-bold leading-tight"
    :style="{ backgroundColor: normalizedBg, color: normalizedText }"
  >
    {{ name }}
  </span>
</template>

<script setup lang="ts">
import { normalizeHex } from '~/utils/color'
import { getStrings } from '~/utils/strings'

const props = defineProps<{
  name: string
  color: string
  textColor: string
  locale?: string
}>()

const normalizedBg = computed(() => normalizeHex(props.color))
const normalizedText = computed(() => normalizeHex(props.textColor))
const ariaLabel = computed(() => `${getStrings(props.locale).lineLabel} ${props.name}`)
</script>
