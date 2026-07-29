<script setup lang="ts">
/**
 * Stable scroll region for workspace UModal bodies.
 * Caps height so content growth scrolls inside instead of resizing the shell.
 */
withDefaults(defineProps<{
  /** Tailwind min-height class for loading/empty/list parity */
  minHeightClass?: string
  /** Tailwind max-height class (viewport-capped) */
  maxHeightClass?: string
}>(), {
  minHeightClass: 'min-h-[16rem]',
  maxHeightClass: 'max-h-[min(70vh,36rem)]'
})
</script>

<template>
  <div
    class="overscroll-contain overflow-y-auto"
    :class="[minHeightClass, maxHeightClass]"
  >
    <slot />
  </div>
</template>
