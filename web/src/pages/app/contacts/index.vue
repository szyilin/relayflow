<script setup lang="ts">
import { computed, ref } from 'vue'
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'

interface WorkspaceContact {
  id: string
  name: string
  dept: string
  avatarText: string
}

const keyword = ref('')
const contacts = ref<WorkspaceContact[]>([])

const filtered = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  if (!q) {
    return contacts.value
  }
  return contacts.value.filter(person =>
    person.name.toLowerCase().includes(q) || person.dept.toLowerCase().includes(q))
})
</script>

<route lang="yaml">
meta:
  layout: workspace
</route>

<template>
  <WorkspaceShell>
    <template #panel>
      <div class="border-b border-[var(--ws-border-subtle)] px-4 py-3 font-semibold">
        通讯录
      </div>
      <div class="p-3">
        <UInput v-model="keyword" placeholder="搜索同事" icon="i-lucide-search" class="workspace-search" />
      </div>
      <div v-if="filtered.length" class="space-y-0.5 px-2 pb-3">
        <div
          v-for="person in filtered"
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
      <div v-else class="flex flex-1 flex-col items-center justify-center p-6">
        <UEmpty
          icon="i-lucide-users"
          title="暂无联系人"
          description="通讯录将在后续切片接入"
        />
      </div>
    </template>

    <div class="flex h-full items-center justify-center p-8">
      <UEmpty icon="i-lucide-user-round" title="选择同事" description="从左侧列表选择联系人，查看资料与快捷操作" />
    </div>
  </WorkspaceShell>
</template>
