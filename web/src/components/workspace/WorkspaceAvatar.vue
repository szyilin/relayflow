<script setup lang="ts">
import { computed, useAttrs } from 'vue'
import { letterAvatarStyle } from '../../utils/avatar'

defineOptions({ inheritAttrs: false })

const props = withDefaults(defineProps<{
  src?: string | null
  text?: string | null
  alt?: string
  /** Prefer userId; falls back to text/alt for stable hash. */
  seed?: string | number | null
  icon?: string
  size?: '3xs' | '2xs' | 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '3xl'
}>(), {
  src: null,
  text: null,
  alt: undefined,
  seed: null,
  icon: undefined,
  size: 'md'
})

const attrs = useAttrs()

const resolvedSrc = computed(() => {
  const value = props.src?.trim()
  return value || undefined
})

const gradientStyle = computed(() =>
  letterAvatarStyle(props.seed ?? props.text ?? props.alt, Boolean(resolvedSrc.value)))
</script>

<template>
  <UAvatar
    :src="resolvedSrc"
    :alt="alt"
    :text="text || undefined"
    :icon="icon"
    :size="size"
    :style="gradientStyle"
    v-bind="attrs"
  />
</template>
