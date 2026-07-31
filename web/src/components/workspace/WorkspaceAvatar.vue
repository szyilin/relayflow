<script setup lang="ts">
import { computed, useAttrs } from 'vue'
import type { CSSProperties, StyleValue } from 'vue'
import { letterAvatarCssVars, letterAvatarStyle, resolveAvatarUrl } from '../../utils/avatar'

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

/** Accept file id or URL; empty → letter/icon fallback. */
const resolvedSrc = computed(() => resolveAvatarUrl(props.src))

const seedKey = computed(() => props.seed ?? props.text ?? props.alt)

const isLetter = computed(() => !resolvedSrc.value)

const rootStyle = computed((): StyleValue => {
  const fromAttrs = attrs.style as StyleValue | undefined
  if (!isLetter.value) {
    return fromAttrs
  }
  const letter: CSSProperties = {
    ...letterAvatarStyle(seedKey.value, false),
    ...letterAvatarCssVars(seedKey.value)
  }
  if (!fromAttrs) {
    return letter
  }
  if (Array.isArray(fromAttrs)) {
    return [...fromAttrs, letter]
  }
  return [fromAttrs, letter]
})

const rootClass = computed(() => {
  const fromAttrs = attrs.class
  if (!isLetter.value) {
    return fromAttrs
  }
  return [fromAttrs, 'workspace-letter-avatar']
})

const displayIcon = computed(() => {
  if (resolvedSrc.value) {
    return undefined
  }
  return props.icon || undefined
})
</script>

<template>
  <UAvatar
    v-bind="{ ...attrs, class: undefined, style: undefined }"
    :src="resolvedSrc"
    :alt="alt"
    :text="text || undefined"
    :icon="displayIcon"
    :size="size"
    :class="rootClass"
    :style="rootStyle"
    :ui="isLetter ? { fallback: 'text-white', icon: 'text-white' } : undefined"
  />
</template>
