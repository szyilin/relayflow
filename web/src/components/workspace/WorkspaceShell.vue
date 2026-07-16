<script setup lang="ts">
import WorkspaceRail from './WorkspaceRail.vue'
import WorkspaceResizeHandle from './WorkspaceResizeHandle.vue'
import { useTenantSwitchReload } from '../../composables/useTenantSwitchReload'
import { useWorkspaceImRealtime } from '../../composables/useWorkspaceImRealtime'
import { useWorkspacePanelResize } from '../../composables/useWorkspacePanelResize'

defineProps<{
  showAside?: boolean
}>()

const { panelWidth, isResizing, startResize } = useWorkspacePanelResize()
useTenantSwitchReload()
useWorkspaceImRealtime()
</script>

<template>
  <div class="workspace-shell relative flex h-svh w-full overflow-hidden">
    <div class="workspace-shell-inner flex min-h-0 min-w-0 flex-1 gap-[var(--ws-shell-gap)] p-[var(--ws-shell-gap)]">
      <WorkspaceRail />

      <div
        v-if="$slots.panel"
        class="workspace-panel-wrap relative shrink-0"
        :style="{ width: `${panelWidth}px` }"
      >
        <aside class="workspace-panel workspace-card flex h-full flex-col overflow-hidden">
          <slot name="panel" />
        </aside>
        <WorkspaceResizeHandle :active="isResizing" @start="startResize" />
      </div>

      <main class="workspace-main workspace-card flex min-w-0 flex-1 flex-col overflow-hidden">
        <slot />
      </main>

      <aside
        v-if="showAside && $slots.aside"
        class="workspace-aside workspace-card hidden h-full shrink-0 flex-col overflow-hidden xl:flex"
      >
        <slot name="aside" />
      </aside>
    </div>
  </div>
</template>
