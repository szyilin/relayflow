<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <div class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
    <div class="space-y-1">
      <h1 class="text-xl font-semibold text-highlighted">
        {{ title }}
      </h1>
      <p v-if="description" class="text-sm text-muted">
        {{ description }}
      </p>
    </div>

    <div v-if="$slots.actions" class="flex flex-wrap items-center gap-2">
      <slot name="actions" />
    </div>
  </div>
</template>
