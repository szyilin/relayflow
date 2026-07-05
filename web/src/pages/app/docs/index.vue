<script setup lang="ts">
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'
</script>

<route lang="yaml">
meta:
  layout: workspace
</route>

<template>
  <WorkspaceShell>
    <template #panel>
      <div class="border-b border-[var(--ws-border-subtle)] px-4 py-3 font-semibold">
        云文档
      </div>
      <div class="space-y-1 p-2 text-sm">
        <button type="button" class="workspace-list-item w-full px-3 py-2 text-left" data-active="true">
          最近
        </button>
        <button type="button" class="workspace-list-item w-full px-3 py-2 text-left text-[var(--ws-text-muted)]">
          与我共享
        </button>
        <button type="button" class="workspace-list-item w-full px-3 py-2 text-left text-[var(--ws-text-muted)]">
          星标
        </button>
      </div>
    </template>

    <div class="flex h-full items-center justify-center p-8">
      <UEmpty
        icon="i-lucide-file-text"
        title="云文档"
        description="文档协作模块将在后续版本接入；当前展示工作台壳层与导航结构"
      />
    </div>
  </WorkspaceShell>
</template>
