<script setup lang="ts">
withDefaults(defineProps<{
  title: string
  description?: string
  icon?: string
  /** `hero`：大插画空态（无小色块底）；`compact`：默认图标/小插画 */
  variant?: 'compact' | 'hero'
}>(), {
  description: undefined,
  icon: 'i-lucide-inbox',
  variant: 'compact'
})
</script>

<template>
  <div
    class="flex flex-col items-center justify-center text-center"
    :class="variant === 'hero' ? 'gap-4 px-8 py-12' : 'gap-3 px-6 py-10'"
  >
    <div
      v-if="variant === 'hero'"
      class="flex w-[min(100%,14rem)] items-center justify-center sm:w-56"
      aria-hidden="true"
    >
      <slot name="illustration">
        <UIcon
          :name="icon"
          class="size-16 text-[var(--ui-primary)] opacity-90"
        />
      </slot>
    </div>
    <div
      v-else
      class="workspace-empty-illust flex size-16 items-center justify-center rounded-2xl"
      aria-hidden="true"
    >
      <slot name="illustration">
        <UIcon
          :name="icon"
          class="size-8 text-[var(--ui-primary)] opacity-90"
        />
      </slot>
    </div>
    <div class="space-y-1">
      <p
        class="font-semibold text-[var(--ws-text)]"
        :class="variant === 'hero' ? 'text-sm' : 'text-sm'"
      >
        {{ title }}
      </p>
      <p
        v-if="description"
        class="max-w-xs text-xs leading-relaxed text-[var(--ws-text-muted)]"
      >
        {{ description }}
      </p>
    </div>
    <div
      v-if="$slots.actions"
      class="flex flex-wrap items-center justify-center gap-2 pt-1"
    >
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.workspace-empty-illust {
  background:
    radial-gradient(ellipse 80% 70% at 30% 20%, color-mix(in oklab, var(--ui-primary) 28%, transparent), transparent 55%),
    radial-gradient(ellipse 70% 60% at 80% 80%, color-mix(in oklab, var(--ws-tint-agent) 18%, transparent), transparent 50%),
    color-mix(in oklab, var(--ws-tint-info-muted) 70%, var(--ws-panel-bg));
  box-shadow: inset 0 0 0 1px color-mix(in oklab, var(--ui-primary) 12%, transparent);
}
</style>
