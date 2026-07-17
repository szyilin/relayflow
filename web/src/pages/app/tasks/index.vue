<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TaskDetailPanel from '../../../components/workspace/TaskDetailPanel.vue'
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'
import { useTasksStore } from '../../../stores/tasks'

const route = useRoute()
const router = useRouter()
const tasksStore = useTasksStore()
const tab = ref<'list' | 'board'>('list')
const createOpen = ref(false)
const toast = useToast()

const createForm = reactive({
  title: '',
  dueTime: ''
})

async function applyTaskIdFromRoute() {
  const raw = route.query.taskId
  const taskId = typeof raw === 'string' ? raw : Array.isArray(raw) ? raw[0] : null
  const id = taskId?.trim() || null
  await tasksStore.selectTask(id)
}

async function openTask(id: string) {
  await router.replace({ query: { ...route.query, taskId: id } })
  await tasksStore.selectTask(id)
}

async function closeDetail() {
  const q = { ...route.query }
  delete q.taskId
  await router.replace({ query: q })
  await tasksStore.selectTask(null)
}

onMounted(async () => {
  tasksStore.hydrateLocal()
  await tasksStore.fetchMyTasks()
  await applyTaskIdFromRoute()
})

watch(() => route.query.taskId, () => {
  void applyTaskIdFromRoute()
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

function subtaskHint(task: { subtaskDoneCount?: number, subtaskTotal?: number }) {
  const total = task.subtaskTotal ?? 0
  if (!total) {
    return null
  }
  return `${task.subtaskDoneCount ?? 0}/${total}`
}

async function handleCreate() {
  const title = createForm.title.trim()
  if (!title) {
    toast.add({ title: '请输入任务标题', color: 'error' })
    return
  }
  try {
    const id = await tasksStore.addTask({
      title,
      dueTime: createForm.dueTime ? new Date(createForm.dueTime).toISOString() : null
    })
    createForm.title = ''
    createForm.dueTime = ''
    createOpen.value = false
    toast.add({ title: '任务已创建', color: 'success' })
    if (id) {
      await openTask(id)
    }
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
    if (tasksStore.selectedId === id) {
      await closeDetail()
    }
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

    <div class="flex h-full min-h-0">
      <div class="flex min-w-0 flex-1 flex-col">
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
                role="button"
                tabindex="0"
                class="flex cursor-pointer items-center gap-3 rounded-lg border px-4 py-3 transition-colors"
                :class="tasksStore.selectedId === task.id
                  ? 'border-primary/50 bg-primary/5 ring-1 ring-primary/20'
                  : 'border-[var(--ws-border-subtle)] bg-[var(--ws-panel-bg)] hover:bg-[var(--ws-rail-hover)]/40'"
                @click="openTask(task.id)"
                @keydown.enter.prevent="openTask(task.id)"
              >
                <UCheckbox
                  :model-value="task.status === 'DONE'"
                  @click.stop
                  @update:model-value="(value: boolean | 'indeterminate') => handleToggle(task.id, value === true)"
                />
                <div class="min-w-0 flex-1">
                  <p
                    class="font-medium"
                    :class="task.status === 'DONE' ? 'line-through text-[var(--ws-text-muted)]' : ''"
                  >
                    {{ task.title }}
                  </p>
                  <p class="mt-0.5 flex flex-wrap gap-x-3 text-xs text-[var(--ws-text-muted)]">
                    <span v-if="formatDue(task.dueTime)">截止 {{ formatDue(task.dueTime) }}</span>
                    <span v-if="subtaskHint(task)">子任务 {{ subtaskHint(task) }}</span>
                  </p>
                </div>
                <UButton
                  color="neutral"
                  variant="ghost"
                  icon="i-lucide-trash-2"
                  size="xs"
                  aria-label="删除"
                  @click.stop="handleDelete(task.id)"
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

      <div
        class="hidden w-[min(100%,22rem)] shrink-0 md:block lg:w-[26rem]"
        :class="tasksStore.selectedId ? '' : 'opacity-90'"
      >
        <TaskDetailPanel
          :task="tasksStore.selectedDetail"
          :subtasks="tasksStore.selectedSubtasks"
          :loading="tasksStore.detailLoading"
          @close="closeDetail"
        />
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
