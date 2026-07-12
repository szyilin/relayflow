<script setup lang="ts">
import { useWorkspaceNav } from '../../composables/useWorkspaceNav'
import WorkspaceRailHeader from './WorkspaceRailHeader.vue'

const { railItems, activeRailId } = useWorkspaceNav()
</script>

<template>
  <aside class="workspace-rail workspace-card flex h-full shrink-0 flex-col overflow-hidden">
    <WorkspaceRailHeader />

    <nav class="flex-1 space-y-0.5 overflow-y-auto p-2">
      <RouterLink
        v-for="item in railItems"
        :key="item.id"
        :to="item.to"
        class="workspace-rail-btn relative flex items-center gap-3 rounded-xl px-3 py-2.5"
        :data-active="activeRailId === item.id"
      >
        <UIcon :name="item.icon" class="size-5 shrink-0" />
        <span class="truncate text-sm font-medium">{{ item.label }}</span>
        <span
          v-if="item.badge"
          class="ml-auto flex size-5 shrink-0 items-center justify-center rounded-full bg-error text-[10px] font-medium text-white"
        >
          {{ item.badge }}
        </span>
      </RouterLink>
    </nav>
  </aside>
</template>
