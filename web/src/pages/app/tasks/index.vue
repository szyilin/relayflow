<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TaskDetailPanel from '../../../components/workspace/TaskDetailPanel.vue'
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'
import { useUserPreferenceStore } from '../../../stores/userPreference'
import { isOverdueTask, useTasksStore, type TasksNavView } from '../../../stores/tasks'

const route = useRoute()
const router = useRouter()
const tasksStore = useTasksStore()
const preference = useUserPreferenceStore()
const tab = ref<'list' | 'board'>('list')
const createOpen = ref(false)
const toast = useToast()

const detailOpen = computed({
  get: () => !!tasksStore.selectedId,
  set: (open: boolean) => {
    if (!open) {
      void closeDetail()
    }
  }
})

const createForm = reactive({
  title: '',
  dueTime: ''
})

const listNavViews: TasksNavView[] = ['mine', 'done', 'created']

const viewTitle = computed(() => {
  if (tasksStore.navView === 'following') {
    return '我关注的'
  }
  if (tasksStore.navView === 'activity') {
    return '动态'
  }
  if (tasksStore.navView === 'done') {
    return '已完成'
  }
  if (tasksStore.navView === 'created') {
    return '我创建的'
  }
  return '我负责的'
})

const listItems = computed(() => {
  if (tasksStore.navView === 'following') {
    return tasksStore.followingItems
  }
  return tasksStore.items
})

const showListTabs = computed(() => tasksStore.navView === 'mine')
const showCreateButton = computed(() =>
  tasksStore.navView === 'mine' || tasksStore.navView === 'created')
const showTaskActions = computed(() =>
  tasksStore.navView === 'mine' || tasksStore.navView === 'done' || tasksStore.navView === 'created')

function parseView(raw: unknown): TasksNavView {
  const v = typeof raw === 'string' ? raw : Array.isArray(raw) ? raw[0] : null
  if (v === 'following' || v === 'activity' || v === 'done' || v === 'created') {
    return v
  }
  return 'mine'
}

async function applyViewFromRoute() {
  const view = parseView(route.query.view)
  if (tasksStore.navView !== view) {
    await tasksStore.setNavView(view)
  }
}

async function switchView(view: TasksNavView) {
  const q = { ...route.query }
  if (view === 'mine') {
    delete q.view
  } else {
    q.view = view
  }
  await router.replace({ query: q })
  // Route watcher also applies view; avoid double fetch when already matching
  if (tasksStore.navView !== view) {
    await tasksStore.setNavView(view)
  }
}

async function applyTaskIdFromRoute() {
  const raw = route.query.taskId
  const taskId = typeof raw === 'string' ? raw : Array.isArray(raw) ? raw[0] : null
  const id = taskId?.trim() || null
  if (!id) {
    await tasksStore.selectTask(null)
    return
  }
  try {
    await tasksStore.selectTask(id)
  } catch {
    toast.add({ title: '无法打开该任务', color: 'error' })
    await closeDetail()
  }
}

async function openTask(id: string) {
  await router.replace({ query: { ...route.query, taskId: id } })
  try {
    await tasksStore.selectTask(id)
  } catch {
    toast.add({ title: '无法打开该任务', color: 'error' })
  }
}

async function openTaskFromActivity(taskId: string) {
  if (!listNavViews.includes(tasksStore.navView) && tasksStore.navView !== 'following') {
    await switchView('mine')
  }
  await openTask(taskId)
}

async function closeDetail() {
  const q = { ...route.query }
  delete q.taskId
  await router.replace({ query: q })
  await tasksStore.selectTask(null)
}

onMounted(async () => {
  void preference.fetchFromServer()
  await applyViewFromRoute()
  // First paint: applyViewFromRoute skips fetch when navView already matches query
  if (listNavViews.includes(tasksStore.navView) && tasksStore.items.length === 0 && !tasksStore.loading) {
    await tasksStore.fetchMyTasks({ pageNo: 1 })
  } else if (tasksStore.navView === 'following' && tasksStore.followingItems.length === 0 && !tasksStore.loading) {
    await tasksStore.fetchFollowingTasks({ pageNo: 1 })
  } else if (tasksStore.navView === 'activity' && tasksStore.activityFeed.length === 0 && !tasksStore.loading) {
    await tasksStore.fetchActivityFeed()
  }
  if (!listNavViews.includes(tasksStore.navView)) {
    void tasksStore.refreshOverdueBadge()
  }
  await applyTaskIdFromRoute()
})

watch(() => route.query.taskId, () => {
  void applyTaskIdFromRoute()
})

watch(() => route.query.view, () => {
  void applyViewFromRoute()
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

function formatActivityTime(iso: string) {
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

function overdueBadgeLabel(count: number, capped: boolean): string {
  if (capped) {
    return `${count}+`
  }
  return count > 99 ? '99+' : String(count)
}

const listTotal = computed(() => {
  if (tasksStore.navView === 'following') {
    return tasksStore.followingTotal
  }
  return tasksStore.total
})

const listPageNo = computed(() => {
  if (tasksStore.navView === 'following') {
    return tasksStore.followingPageNo
  }
  return tasksStore.pageNo
})

const showListPagination = computed(() =>
  tasksStore.navView !== 'activity' && listTotal.value > tasksStore.pageSize)

async function handleCreate() {
  const title = createForm.title.trim()
  if (!title) {
    toast.add({ title: '请输入任务标题', color: 'error' })
    return
  }
  try {
    if (tasksStore.navView !== 'mine' && tasksStore.navView !== 'created') {
      await switchView('mine')
    }
    const dueTime = createForm.dueTime ? new Date(createForm.dueTime).toISOString() : null
    const preferRemind = preference.task.defaultRemindBeforeMinutes
    const remindBeforeMinutes = dueTime && preferRemind >= 0 ? preferRemind : undefined
    const id = await tasksStore.addTask({
      title,
      dueTime,
      remindBeforeMinutes: remindBeforeMinutes ?? null
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

function navItemClass(view: TasksNavView) {
  return tasksStore.navView === view
    ? 'workspace-list-item w-full px-3 py-2 text-left text-sm font-medium'
    : 'workspace-list-item w-full px-3 py-2 text-left text-sm text-[var(--ws-text-muted)]'
}

function emptyTitle(): string {
  if (tasksStore.navView === 'following') {
    return '暂无关注的任务'
  }
  if (tasksStore.navView === 'done') {
    return '暂无已完成任务'
  }
  if (tasksStore.navView === 'created') {
    return '暂无我创建的任务'
  }
  return '暂无任务'
}

function emptyDescription(): string {
  if (tasksStore.navView === 'following') {
    return '在详情中点击「关注」后会出现在这里'
  }
  if (tasksStore.navView === 'done') {
    return '完成任务后会出现在这里'
  }
  if (tasksStore.navView === 'created') {
    return '你创建的任务会出现在这里'
  }
  return '点击右上角「新建」添加第一条任务'
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
        <button
          type="button"
          :class="navItemClass('mine')"
          :data-active="tasksStore.navView === 'mine' ? 'true' : undefined"
          @click="switchView('mine')"
        >
          <span class="flex items-center justify-between gap-2">
            <span>我负责的</span>
            <span
              v-if="tasksStore.overdueBadgeCount > 0"
              class="rounded-md bg-red-500/15 px-1.5 py-0.5 text-[10px] font-medium text-red-600 dark:text-red-400"
            >
              {{ overdueBadgeLabel(tasksStore.overdueBadgeCount, tasksStore.overdueBadgeCapped) }}
            </span>
          </span>
        </button>
        <button
          type="button"
          :class="navItemClass('done')"
          :data-active="tasksStore.navView === 'done' ? 'true' : undefined"
          @click="switchView('done')"
        >
          已完成
        </button>
        <button
          type="button"
          :class="navItemClass('created')"
          :data-active="tasksStore.navView === 'created' ? 'true' : undefined"
          @click="switchView('created')"
        >
          我创建的
        </button>
        <button
          type="button"
          :class="navItemClass('following')"
          :data-active="tasksStore.navView === 'following' ? 'true' : undefined"
          @click="switchView('following')"
        >
          我关注的
        </button>
        <button
          type="button"
          :class="navItemClass('activity')"
          :data-active="tasksStore.navView === 'activity' ? 'true' : undefined"
          @click="switchView('activity')"
        >
          动态
        </button>
      </div>
    </template>

    <div class="flex h-full min-h-0 flex-col">
      <header class="flex items-center gap-3 border-b border-[var(--ws-border-subtle)] px-5 py-3">
        <h1 class="flex-1 text-lg font-semibold">
          {{ viewTitle }}
        </h1>
        <UButton
          v-if="showCreateButton"
          color="primary"
          icon="i-lucide-plus"
          @click="createOpen = true"
        >
          新建
        </UButton>
      </header>

      <div
        v-if="showListTabs"
        class="flex gap-4 border-b border-[var(--ws-border-subtle)] px-5"
      >
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

      <div class="min-h-0 flex-1 overflow-y-auto p-5">
        <div v-if="tasksStore.navView === 'activity'">
          <div v-if="tasksStore.loading" class="flex justify-center py-12">
            <UIcon name="i-lucide-loader-circle" class="size-6 animate-spin text-[var(--ws-text-muted)]" />
          </div>
          <ul v-else-if="tasksStore.activityFeed.length" class="space-y-2">
            <li
              v-for="a in tasksStore.activityFeed"
              :key="a.id"
              role="button"
              tabindex="0"
              class="cursor-pointer rounded-lg border border-[var(--ws-border-subtle)] bg-[var(--ws-panel-bg)] px-4 py-3 hover:bg-[var(--ws-rail-hover)]/40"
              @click="openTaskFromActivity(a.taskId)"
              @keydown.enter.prevent="openTaskFromActivity(a.taskId)"
            >
              <div class="flex items-baseline justify-between gap-2 text-xs text-[var(--ws-text-muted)]">
                <span class="font-medium text-[var(--ws-text)]">{{ a.actorNickname }}</span>
                <span>{{ formatActivityTime(a.createTime) }}</span>
              </div>
              <p class="mt-1 text-sm">
                {{ a.summary }}
                <span class="text-[var(--ws-text-muted)]"> · {{ a.taskTitle }}</span>
              </p>
            </li>
          </ul>
          <UEmpty
            v-else
            icon="i-lucide-activity"
            title="暂无动态"
            description="关注或操作任务后，相关活动会出现在这里"
          />
        </div>

        <div v-else-if="tasksStore.navView === 'following' || !showListTabs || tab === 'list'">
          <div v-if="tasksStore.loading" class="flex justify-center py-12">
            <UIcon name="i-lucide-loader-circle" class="size-6 animate-spin text-[var(--ws-text-muted)]" />
          </div>
          <div v-else-if="listItems.length" class="mx-auto max-w-3xl space-y-2">
            <div
              v-for="task in listItems"
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
                v-if="showTaskActions"
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
                  <span
                    v-if="formatDue(task.dueTime)"
                    :class="isOverdueTask(task) ? 'font-medium text-red-600 dark:text-red-400' : ''"
                  >
                    {{ isOverdueTask(task) ? '已逾期' : '截止' }} {{ formatDue(task.dueTime) }}
                  </span>
                  <span v-if="subtaskHint(task)">子任务 {{ subtaskHint(task) }}</span>
                </p>
              </div>
              <UButton
                v-if="showTaskActions"
                color="neutral"
                variant="ghost"
                icon="i-lucide-trash-2"
                size="xs"
                aria-label="删除"
                @click.stop="handleDelete(task.id)"
              />
            </div>
            <div
              v-if="showListPagination"
              class="flex items-center justify-between gap-3 pt-3 text-sm text-[var(--ws-text-muted)]"
            >
              <span>共 {{ listTotal }} 条</span>
              <UPagination
                :page="listPageNo"
                :total="listTotal"
                :items-per-page="tasksStore.pageSize"
                @update:page="(p: number) => tasksStore.setListPage(p)"
              />
            </div>
          </div>
          <UEmpty
            v-else
            :icon="tasksStore.navView === 'following' ? 'i-lucide-eye' : 'i-lucide-list-todo'"
            :title="emptyTitle()"
            :description="emptyDescription()"
          />
        </div>
        <UEmpty v-else icon="i-lucide-kanban-square" title="看板视图" description="看板将在后续切片实现" />
      </div>
    </div>

    <USlideover
      v-model:open="detailOpen"
      side="right"
      :close="false"
      :ui="{ content: 'w-full max-w-md sm:max-w-lg p-0 gap-0' }"
    >
      <template #content>
        <TaskDetailPanel
          :task="tasksStore.selectedDetail"
          :subtasks="tasksStore.selectedSubtasks"
          :loading="tasksStore.detailLoading"
          @close="detailOpen = false"
        />
      </template>
    </USlideover>

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
