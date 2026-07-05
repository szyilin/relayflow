<script setup lang="ts">
defineProps<{
  active?: boolean
}>()

const emit = defineEmits<{
  start: [event: PointerEvent]
}>()
</script>

<template>
  <div
    class="workspace-resize-handle group absolute inset-y-0 -right-1.5 z-10 w-3 cursor-col-resize"
    role="separator"
    aria-orientation="vertical"
    aria-label="拖动调整列表宽度"
    @pointerdown="emit('start', $event)"
  >
    <div
      class="absolute inset-y-3 left-1/2 w-0.5 -translate-x-1/2 rounded-full transition-colors"
      :class="active ? 'bg-primary' : 'bg-transparent group-hover:bg-[var(--ws-resize-hover)]'"
    />
  </div>
</template>
