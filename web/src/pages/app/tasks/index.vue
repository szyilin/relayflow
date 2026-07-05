<script setup lang="ts">
import { ref } from 'vue'
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'
import { mockTasks } from '../../../mocks/workspace/data'

const tab = ref<'list' | 'board'>('list')
</script>

<route lang="yaml">
meta:
  layout: workspace
</route>

<template>
  <WorkspaceShell>
    <template #panel>
      <div class="border-b border-[var(--ws-border-subtle)] px-4 py-3">
        <h2 class="font-semibold">
          任务
        </h2>
      </div>
      <div class="space-y-1 p-2">
        <button type="button" class="workspace-list-item w-full px-3 py-2 text-left text-sm font-medium" data-active="true">
          我负责的
        </button>
        <button type="button" class="workspace-list-item w-full px-3 py-2 text-left text-sm text-[var(--ws-text-muted)]">
          我关注的
        </button>
        <button type="button" class="workspace-list-item w-full px-3 py-2 text-left text-sm text-[var(--ws-text-muted)]">
          动态
        </button>
      </div>
    </template>

    <div class="flex h-full flex-col">
      <header class="flex items-center gap-3 border-b border-[var(--ws-border-subtle)] px-5 py-3">
        <h1 class="flex-1 text-lg font-semibold">
          我负责的
        </h1>
        <UButton color="primary" icon="i-lucide-plus">
          新建
        </UButton>
      </header>

      <div class="flex gap-4 border-b border-[var(--ws-border-subtle)] px-5">
        <button
          type="button"
          class="border-b-2 py-2 text-sm"
          :class="tab === 'list' ? 'border-primary text-primary font-medium' : 'border-transparent text-[var(--ws-text-muted)]'"
          @click="tab = 'list'"
        >
          任务列表
        </button>
        <button
          type="button"
          class="border-b-2 py-2 text-sm text-[var(--ws-text-muted)]"
          :class="tab === 'board' ? 'border-primary text-primary font-medium' : 'border-transparent'"
          @click="tab = 'board'"
        >
          看板
        </button>
      </div>

      <div class="flex-1 overflow-y-auto p-5">
        <div v-if="tab === 'list'" class="space-y-2">
          <div
            v-for="task in mockTasks"
            :key="task.id"
            class="flex items-center gap-3 rounded-lg border border-[var(--ws-border-subtle)] bg-[var(--ws-panel-bg)] px-4 py-3"
          >
            <UCheckbox :model-value="task.done" disabled />
            <div class="min-w-0 flex-1">
              <p class="font-medium" :class="task.done ? 'line-through text-[var(--ws-text-muted)]' : ''">
                {{ task.title }}
              </p>
              <p v-if="task.due" class="text-xs text-[var(--ws-text-muted)]">
                截止 {{ task.due }}
              </p>
            </div>
            <UBadge color="neutral" variant="subtle">
              {{ task.list }}
            </UBadge>
          </div>
        </div>
        <UEmpty v-else icon="i-lucide-kanban-square" title="看板视图" description="原型占位，后续切片实现" />
      </div>
    </div>
  </WorkspaceShell>
</template>
