<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'
import { useTasksStore } from '../../../stores/tasks'

const tasksStore = useTasksStore()
const tab = ref<'list' | 'board'>('list')
const createOpen = ref(false)
const toast = useToast()

const createForm = reactive({
  title: '',
  dueTime: ''
})

onMounted(() => {
  void tasksStore.fetchMyTasks()
})

function formatDue(iso?: string | null) {
  if (!iso) {
    return undefined
  }
  return new Date(iso).toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function handleCreate() {
  const title = createForm.title.trim()
  if (!title) {
    toast.add({ title: '请输入任务标题', color: 'error' })
    return
  }
  try {
    await tasksStore.addTask({
      title,
      dueTime: createForm.dueTime ? new Date(createForm.dueTime).toISOString() : null
    })
    createForm.title = ''
    createForm.dueTime = ''
    createOpen.value = false
    toast.add({ title: '任务已创建', color: 'success' })
  } catch {
    toast.add({ title: '创建失败', color: 'error' })
  }
}

async function handleToggle(id: string, done: boolean) {
  try {
    await tasksStore.setTaskDone(id, done)
  } catch {
    toast.add({ title: '更新失败', color: 'error' })
  }
}

async function handleDelete(id: string) {
  try {
    await tasksStore.removeTask(id)
    toast.add({ title: '已删除', color: 'success' })
  } catch {
    toast.add({ title: '删除失败', color: 'error' })
  }
}
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
        <UTooltip text="即将推出">
          <button
            type="button"
            disabled
            class="workspace-list-item w-full cursor-not-allowed px-3 py-2 text-left text-sm text-[var(--ws-text-muted)] opacity-60"
          >
            我关注的
          </button>
        </UTooltip>
        <UTooltip text="即将推出">
          <button
            type="button"
            disabled
            class="workspace-list-item w-full cursor-not-allowed px-3 py-2 text-left text-sm text-[var(--ws-text-muted)] opacity-60"
          >
            动态
          </button>
        </UTooltip>
      </div>
    </template>

    <div class="flex h-full flex-col">
      <header class="flex items-center gap-3 border-b border-[var(--ws-border-subtle)] px-5 py-3">
        <h1 class="flex-1 text-lg font-semibold">
          我负责的
        </h1>
        <UButton color="primary" icon="i-lucide-plus" @click="createOpen = true">
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
        <div v-if="tab === 'list'">
          <div v-if="tasksStore.loading" class="flex justify-center py-12">
            <UIcon name="i-lucide-loader-circle" class="size-6 animate-spin text-[var(--ws-text-muted)]" />
          </div>
          <div v-else-if="tasksStore.items.length" class="space-y-2">
            <div
              v-for="task in tasksStore.items"
              :key="task.id"
              class="flex items-center gap-3 rounded-lg border border-[var(--ws-border-subtle)] bg-[var(--ws-panel-bg)] px-4 py-3"
            >
              <UCheckbox
                :model-value="task.status === 'DONE'"
                @update:model-value="(value: boolean | 'indeterminate') => handleToggle(task.id, value === true)"
              />
              <div class="min-w-0 flex-1">
                <p
                  class="font-medium"
                  :class="task.status === 'DONE' ? 'line-through text-[var(--ws-text-muted)]' : ''"
                >
                  {{ task.title }}
                </p>
                <p v-if="formatDue(task.dueTime)" class="text-xs text-[var(--ws-text-muted)]">
                  截止 {{ formatDue(task.dueTime) }}
                </p>
              </div>
              <UButton
                color="neutral"
                variant="ghost"
                icon="i-lucide-trash-2"
                size="xs"
                aria-label="删除"
                @click="handleDelete(task.id)"
              />
            </div>
          </div>
          <UEmpty
            v-else
            icon="i-lucide-list-todo"
            title="暂无任务"
            description="点击右上角「新建」添加第一条任务"
          />
        </div>
        <UEmpty v-else icon="i-lucide-kanban-square" title="看板视图" description="看板将在后续切片实现" />
      </div>
    </div>

    <UModal v-model:open="createOpen" title="新建任务">
      <template #body>
        <form class="space-y-4" @submit.prevent="handleCreate">
          <UFormField label="标题" required>
            <UInput v-model="createForm.title" placeholder="任务标题" autofocus />
          </UFormField>
          <UFormField label="截止时间">
            <UInput v-model="createForm.dueTime" type="datetime-local" />
          </UFormField>
          <div class="flex justify-end gap-2">
            <UButton color="neutral" variant="soft" @click="createOpen = false">
              取消
            </UButton>
            <UButton type="submit" color="primary" :loading="tasksStore.saving">
              创建
            </UButton>
          </div>
        </form>
      </template>
    </UModal>
  </WorkspaceShell>
</template>
