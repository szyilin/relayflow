<script setup lang="ts">
import { computed } from 'vue'

export type WorkspaceSemanticKind =
  | 'official'
  | 'bot'
  | 'agent'
  | 'external'
  | 'role'
  | 'comingSoon'
  | 'info'
  | 'success'
  | 'warning'
  | 'danger'

const props = withDefaults(defineProps<{
  kind: WorkspaceSemanticKind
  size?: 'xs' | 'sm' | 'md'
}>(), {
  size: 'sm'
})

const kindToCss = {
  official: 'official',
  bot: 'bot',
  agent: 'agent',
  external: 'external',
  role: 'role',
  comingSoon: 'coming-soon',
  info: 'info',
  success: 'success',
  warning: 'warning',
  danger: 'danger'
} as const satisfies Record<WorkspaceSemanticKind, string>

const softStyle = computed(() => {
  const key = kindToCss[props.kind]
  return {
    backgroundColor: `var(--ws-tint-${key}-muted)`,
    color: `var(--ws-tint-${key})`
  }
})

const sizeClass = computed(() => {
  if (props.size === 'xs') {
    return 'px-1 py-px text-[10px] leading-4'
  }
  if (props.size === 'md') {
    return 'px-2 py-0.5 text-xs leading-5'
  }
  return 'px-1.5 py-px text-[11px] leading-4'
})
</script>

<template>
  <span
    class="inline-flex shrink-0 items-center rounded font-medium tracking-wide"
    :class="sizeClass"
    :style="softStyle"
  >
    <slot />
  </span>
</template>
