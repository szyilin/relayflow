<script setup lang="ts">
import { ref } from 'vue'
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'
import { mockContacts } from '../../../mocks/workspace/data'

const keyword = ref('')
</script>

<route lang="yaml">
meta:
  layout: workspace
</route>

<template>
  <WorkspaceShell>
    <template #panel>
      <div class="border-b border-[var(--ws-border)] px-4 py-3 font-semibold">
        通讯录
      </div>
      <div class="p-3">
        <UInput v-model="keyword" placeholder="搜索同事" icon="i-lucide-search" class="workspace-search" />
      </div>
      <div class="space-y-0.5 px-2 pb-3">
        <div
          v-for="person in mockContacts"
          :key="person.id"
          class="workspace-list-item flex items-center gap-3 px-3 py-2"
        >
          <UAvatar :text="person.avatarText" size="sm" />
          <div>
            <p class="text-sm font-medium">
              {{ person.name }}
            </p>
            <p class="text-xs text-[var(--ws-text-muted)]">
              {{ person.dept }}
            </p>
          </div>
        </div>
      </div>
    </template>

    <div class="flex h-full items-center justify-center p-8">
      <UEmpty icon="i-lucide-user-round" title="选择同事" description="从左侧列表选择联系人，查看资料与快捷操作（原型占位）" />
    </div>
  </WorkspaceShell>
</template>
