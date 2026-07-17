<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchMembers, type MemberSearchItem } from '../../../api/app/member-search'
import type { TaskListRole } from '../../../api/app/taskList'
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
const createListOpen = ref(false)
const membersOpen = ref(false)
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

const createListForm = reactive({
  name: '',
  description: ''
})

const inviteForm = reactive({
  keyword: '',
  userId: '',
  nickname: '',
  role: 'EDITOR' as Exclude<TaskListRole, 'OWNER'>
})
const inviteHits = ref<MemberSearchItem[]>([])
const inviteSearching = ref(false)

const listNavViews: TasksNavView[] = ['mine', 'done', 'created']

const inListContext = computed(() => !!tasksStore.activeListId)

const viewTitle = computed(() => {
  if (inListContext.value) {
    return tasksStore.activeList?.name ?? '清单'
  }
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

const displayItems = computed(() => {
  if (inListContext.value) {
    return tasksStore.listItems
  }
  if (tasksStore.navView === 'following') {
    return tasksStore.followingItems
  }
  return tasksStore.items
})

const showListTabs = computed(() => inListContext.value || tasksStore.navView === 'mine')
const showCreateButton = computed(() => {
  if (inListContext.value) {
    return tasksStore.listCanMutateTasks
  }
  return tasksStore.navView === 'mine' || tasksStore.navView === 'created'
})
const showTaskActions = computed(() => {
  if (inListContext.value) {
    return tasksStore.listCanMutateTasks
  }
  return tasksStore.navView === 'mine' || tasksStore.navView === 'done' || tasksStore.navView === 'created'
})

function parseView(raw: unknown): TasksNavView {
  const v = typeof raw === 'string' ? raw : Array.isArray(raw) ? raw[0] : null
  if (v === 'following' || v === 'activity' || v === 'done' || v === 'created') {
    return v
  }
  return 'mine'
}

function parseListId(raw: unknown): string | null {
  const v = typeof raw === 'string' ? raw : Array.isArray(raw) ? raw[0] : null
  const id = v?.trim()
  return id || null
}

async function applyListIdFromRoute() {
  const listId = parseListId(route.query.listId)
  if (!listId) {
    if (tasksStore.activeListId) {
      await tasksStore.setNavView(parseView(route.query.view))
    }
    return
  }
  if (tasksStore.activeListId === listId) {
    return
  }
  try {
    await tasksStore.selectList(listId)
    tab.value = 'list'
  } catch {
    toast.add({ title: '无法打开该清单', color: 'error' })
    const q = { ...route.query }
    delete q.listId
    await router.replace({ query: q })
  }
}

async function applyViewFromRoute() {
  if (parseListId(route.query.listId)) {
    return
  }
  const view = parseView(route.query.view)
  if (tasksStore.navView !== view || tasksStore.activeListId) {
    await tasksStore.setNavView(view)
  }
}

async function switchView(view: TasksNavView) {
  const q = { ...route.query }
  delete q.listId
  if (view === 'mine') {
    delete q.view
  } else {
    q.view = view
  }
  await router.replace({ query: q })
  if (tasksStore.navView !== view || tasksStore.activeListId) {
    await tasksStore.setNavView(view)
  }
  tab.value = 'list'
}

async function openList(listId: string) {
  const q = { ...route.query }
  delete q.view
  q.listId = listId
  await router.replace({ query: q })
  try {
    await tasksStore.selectList(listId)
    tab.value = 'list'
  } catch {
    toast.add({ title: '无法打开该清单', color: 'error' })
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
  await tasksStore.fetchMyLists()
  const listId = parseListId(route.query.listId)
  if (listId) {
    await applyListIdFromRoute()
  } else {
    await applyViewFromRoute()
    if (listNavViews.includes(tasksStore.navView) && tasksStore.items.length === 0 && !tasksStore.loading) {
      await tasksStore.fetchMyTasks({ pageNo: 1 })
    } else if (tasksStore.navView === 'following' && tasksStore.followingItems.length === 0 && !tasksStore.loading) {
      await tasksStore.fetchFollowingTasks({ pageNo: 1 })
    } else if (tasksStore.navView === 'activity' && tasksStore.activityFeed.length === 0 && !tasksStore.loading) {
      await tasksStore.fetchActivityFeed()
    }
  }
  if (!listNavViews.includes(tasksStore.navView) && !inListContext.value) {
    void tasksStore.refreshOverdueBadge()
  }
  await applyTaskIdFromRoute()
})

watch(() => route.query.taskId, () => {
  void applyTaskIdFromRoute()
})

watch(() => route.query.view, () => {
  if (!parseListId(route.query.listId)) {
    void applyViewFromRoute()
  }
})

watch(() => route.query.listId, () => {
  void applyListIdFromRoute()
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
  if (inListContext.value) {
    return tasksStore.listTotal
  }
  if (tasksStore.navView === 'following') {
    return tasksStore.followingTotal
  }
  return tasksStore.total
})

const listPageNo = computed(() => {
  if (inListContext.value) {
    return tasksStore.listPageNo
  }
  if (tasksStore.navView === 'following') {
    return tasksStore.followingPageNo
  }
  return tasksStore.pageNo
})

const showListPagination = computed(() =>
  (inListContext.value || tasksStore.navView !== 'activity') && listTotal.value > tasksStore.pageSize)

async function handleCreate() {
  const title = createForm.title.trim()
  if (!title) {
    toast.add({ title: '请输入任务标题', color: 'error' })
    return
  }
  try {
    if (!inListContext.value && tasksStore.navView !== 'mine' && tasksStore.navView !== 'created') {
      await switchView('mine')
    }
    const dueTime = createForm.dueTime ? new Date(createForm.dueTime).toISOString() : null
    const preferRemind = preference.task.defaultRemindBeforeMinutes
    const remindBeforeMinutes = dueTime && preferRemind >= 0 ? preferRemind : undefined
    const id = await tasksStore.addTask({
      title,
      dueTime,
      remindBeforeMinutes: remindBeforeMinutes ?? null,
      listId: tasksStore.activeListId
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

async function handleCreateList() {
  const name = createListForm.name.trim()
  if (!name) {
    toast.add({ title: '请输入清单名称', color: 'error' })
    return
  }
  try {
    const id = await tasksStore.createList({
      name,
      description: createListForm.description.trim() || null
    })
    createListForm.name = ''
    createListForm.description = ''
    createListOpen.value = false
    toast.add({ title: '清单已创建', color: 'success' })
    await openList(id)
  } catch {
    toast.add({ title: '创建清单失败', color: 'error' })
  }
}

async function handleArchiveList() {
  const id = tasksStore.activeListId
  if (!id) {
    return
  }
  try {
    await tasksStore.archiveList(id)
    toast.add({ title: '清单已归档', color: 'success' })
    await switchView('mine')
  } catch {
    toast.add({ title: '归档失败', color: 'error' })
  }
}

async function searchInviteCandidates() {
  const kw = inviteForm.keyword.trim()
  if (!kw) {
    inviteHits.value = []
    return
  }
  inviteSearching.value = true
  try {
    inviteHits.value = await searchMembers(kw, 8)
  } catch {
    inviteHits.value = []
  } finally {
    inviteSearching.value = false
  }
}

function pickInviteMember(m: MemberSearchItem) {
  inviteForm.userId = m.id
  inviteForm.nickname = m.title
  inviteForm.keyword = m.title
  inviteHits.value = []
}

async function handleInviteMember() {
  const nickname = inviteForm.nickname.trim() || inviteForm.keyword.trim()
  const userId = inviteForm.userId.trim() || (nickname ? `local-invite-${Date.now()}` : '')
  if (!userId || !nickname) {
    toast.add({ title: '请选择或填写成员', color: 'error' })
    return
  }
  try {
    await tasksStore.inviteListMember({
      userId,
      nickname,
      role: inviteForm.role
    })
    inviteForm.keyword = ''
    inviteForm.userId = ''
    inviteForm.nickname = ''
    inviteForm.role = 'EDITOR'
    toast.add({ title: '已邀请', color: 'success' })
  } catch {
    toast.add({ title: '邀请失败', color: 'error' })
  }
}

async function handleRemoveMember(userId: string) {
  try {
    await tasksStore.removeListMember(userId)
    toast.add({ title: '已移除', color: 'success' })
  } catch {
    toast.add({ title: '移除失败', color: 'error' })
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
  const active = !inListContext.value && tasksStore.navView === view
  return active
    ? 'workspace-list-item w-full px-3 py-2 text-left text-sm font-medium'
    : 'workspace-list-item w-full px-3 py-2 text-left text-sm text-[var(--ws-text-muted)]'
}

function listNavClass(listId: string) {
  return tasksStore.activeListId === listId
    ? 'workspace-list-item w-full px-3 py-2 text-left text-sm font-medium'
    : 'workspace-list-item w-full px-3 py-2 text-left text-sm text-[var(--ws-text-muted)]'
}

function roleLabel(role: TaskListRole) {
  if (role === 'OWNER') {
    return '所有者'
  }
  if (role === 'EDITOR') {
    return '编辑'
  }
  return '只读'
}

function emptyTitle(): string {
  if (inListContext.value) {
    return '清单暂无任务'
  }
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
  if (inListContext.value) {
    return tasksStore.listCanMutateTasks ? '点击右上角「新建」添加任务到本清单' : '等待编辑者添加任务'
  }
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
          :data-active="!inListContext && tasksStore.navView === 'mine' ? 'true' : undefined"
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
          :data-active="!inListContext && tasksStore.navView === 'done' ? 'true' : undefined"
          @click="switchView('done')"
        >
          已完成
        </button>
        <button
          type="button"
          :class="navItemClass('created')"
          :data-active="!inListContext && tasksStore.navView === 'created' ? 'true' : undefined"
          @click="switchView('created')"
        >
          我创建的
        </button>
        <button
          type="button"
          :class="navItemClass('following')"
          :data-active="!inListContext && tasksStore.navView === 'following' ? 'true' : undefined"
          @click="switchView('following')"
        >
          我关注的
        </button>
        <button
          type="button"
          :class="navItemClass('activity')"
          :data-active="!inListContext && tasksStore.navView === 'activity' ? 'true' : undefined"
          @click="switchView('activity')"
        >
          动态
        </button>

        <div class="mt-3 border-t border-[var(--ws-border-subtle)] pt-3">
          <div class="mb-1 flex items-center justify-between px-3">
            <span class="text-xs font-medium text-[var(--ws-text-muted)]">清单</span>
            <UButton
              color="neutral"
              variant="ghost"
              icon="i-lucide-plus"
              size="xs"
              aria-label="新建清单"
              @click="createListOpen = true"
            />
          </div>
          <button
            v-for="list in tasksStore.myLists"
            :key="list.id"
            type="button"
            :class="listNavClass(list.id)"
            :data-active="tasksStore.activeListId === list.id ? 'true' : undefined"
            @click="openList(list.id)"
          >
            {{ list.name }}
          </button>
          <p
            v-if="!tasksStore.myLists.length"
            class="px-3 py-2 text-xs text-[var(--ws-text-muted)]"
          >
            暂无清单，点击 + 创建
          </p>
        </div>
      </div>
    </template>

    <div class="flex h-full min-h-0 flex-col">
      <header class="flex items-center gap-3 border-b border-[var(--ws-border-subtle)] px-5 py-3">
        <h1 class="min-w-0 flex-1 truncate text-lg font-semibold">
          {{ viewTitle }}
        </h1>
        <UButton
          v-if="inListContext"
          color="neutral"
          variant="soft"
          icon="i-lucide-users"
          @click="membersOpen = true"
        >
          成员
        </UButton>
        <UButton
          v-if="inListContext && tasksStore.listCanEditMeta"
          color="neutral"
          variant="ghost"
          icon="i-lucide-archive"
          @click="handleArchiveList"
        >
          归档
        </UButton>
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
        <div v-if="!inListContext && tasksStore.navView === 'activity'">
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

        <div v-else-if="tab === 'list' || (!inListContext && tasksStore.navView === 'following')">
          <div v-if="tasksStore.loading" class="flex justify-center py-12">
            <UIcon name="i-lucide-loader-circle" class="size-6 animate-spin text-[var(--ws-text-muted)]" />
          </div>
          <div v-else-if="displayItems.length" class="mx-auto max-w-3xl space-y-2">
            <div
              v-for="task in displayItems"
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
            :icon="!inListContext && tasksStore.navView === 'following' ? 'i-lucide-eye' : 'i-lucide-list-todo'"
            :title="emptyTitle()"
            :description="emptyDescription()"
          />
        </div>
        <UEmpty
          v-else
          icon="i-lucide-kanban-square"
          title="看板视图"
          :description="inListContext ? '看板将在后续切片实现' : '请打开清单后使用看板'"
        />
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

    <UModal v-model:open="createListOpen" title="新建清单">
      <template #body>
        <form class="space-y-4" @submit.prevent="handleCreateList">
          <UFormField label="名称" required>
            <UInput v-model="createListForm.name" placeholder="例如：产品发布" autofocus />
          </UFormField>
          <UFormField label="描述">
            <UTextarea v-model="createListForm.description" :rows="2" placeholder="可选" />
          </UFormField>
          <div class="flex justify-end gap-2">
            <UButton color="neutral" variant="soft" @click="createListOpen = false">
              取消
            </UButton>
            <UButton type="submit" color="primary" :loading="tasksStore.saving">
              创建
            </UButton>
          </div>
        </form>
      </template>
    </UModal>

    <UModal v-model:open="membersOpen" title="清单成员">
      <template #body>
        <div class="space-y-4">
          <ul class="max-h-48 space-y-2 overflow-y-auto">
            <li
              v-for="m in tasksStore.listMembers"
              :key="m.userId"
              class="flex items-center gap-3 rounded-lg border border-[var(--ws-border-subtle)] px-3 py-2"
            >
              <span class="flex size-8 items-center justify-center rounded-full bg-primary/10 text-sm font-medium">
                {{ m.avatarText }}
              </span>
              <div class="min-w-0 flex-1">
                <p class="truncate text-sm font-medium">
                  {{ m.nickname }}
                </p>
                <p class="text-xs text-[var(--ws-text-muted)]">
                  {{ roleLabel(m.role) }}
                </p>
              </div>
              <UButton
                v-if="tasksStore.listCanEditMeta && m.role !== 'OWNER'"
                color="neutral"
                variant="ghost"
                size="xs"
                icon="i-lucide-user-minus"
                aria-label="移除"
                @click="handleRemoveMember(m.userId)"
              />
            </li>
          </ul>

          <div
            v-if="tasksStore.listCanEditMeta"
            class="space-y-3 border-t border-[var(--ws-border-subtle)] pt-3"
          >
            <p class="text-sm font-medium">
              邀请成员
            </p>
            <UFormField label="搜索同事">
              <div class="flex gap-2">
                <UInput
                  v-model="inviteForm.keyword"
                  class="flex-1"
                  placeholder="姓名关键字"
                  @keyup.enter="searchInviteCandidates"
                />
                <UButton
                  color="neutral"
                  variant="soft"
                  :loading="inviteSearching"
                  @click="searchInviteCandidates"
                >
                  搜索
                </UButton>
              </div>
            </UFormField>
            <ul v-if="inviteHits.length" class="space-y-1">
              <li
                v-for="hit in inviteHits"
                :key="hit.id"
                role="button"
                tabindex="0"
                class="cursor-pointer rounded-md px-2 py-1.5 text-sm hover:bg-[var(--ws-rail-hover)]/40"
                @click="pickInviteMember(hit)"
                @keydown.enter.prevent="pickInviteMember(hit)"
              >
                {{ hit.title }}
                <span v-if="hit.subtitle" class="text-[var(--ws-text-muted)]"> · {{ hit.subtitle }}</span>
              </li>
            </ul>
            <UFormField label="角色">
              <USelect
                v-model="inviteForm.role"
                :items="[
                  { label: '编辑', value: 'EDITOR' },
                  { label: '只读', value: 'VIEWER' }
                ]"
              />
            </UFormField>
            <UButton
              color="primary"
              :loading="tasksStore.saving"
              :disabled="!inviteForm.userId && !inviteForm.keyword.trim()"
              @click="handleInviteMember"
            >
              邀请
            </UButton>
            <p class="text-xs text-[var(--ws-text-muted)]">
              可搜索同事后邀请；无结果时直接输入姓名并邀请（本地预览）。
            </p>
          </div>
        </div>
      </template>
    </UModal>
  </WorkspaceShell>
</template>
