<script setup lang="ts">
import WorkspaceRail from './WorkspaceRail.vue'
import AdminColorModeToggle from '../admin/AdminColorModeToggle.vue'

defineProps<{
  showAside?: boolean
}>()
</script>

<template>
  <div class="workspace-shell relative flex h-svh w-full overflow-hidden">
    <div class="absolute right-3 top-3 z-20 flex items-center gap-1 rounded-lg bg-[var(--ws-panel-bg)]/80 p-0.5 shadow-sm backdrop-blur-sm">
      <AdminColorModeToggle />
    </div>

    <WorkspaceRail />

    <aside v-if="$slots.panel" class="workspace-panel flex h-full shrink-0 flex-col">
      <slot name="panel" />
    </aside>

    <main class="workspace-main flex min-w-0 flex-1 flex-col">
      <slot />
    </main>

    <aside v-if="showAside && $slots.aside" class="workspace-aside hidden h-full shrink-0 flex-col xl:flex">
      <slot name="aside" />
    </aside>
  </div>
</template>
