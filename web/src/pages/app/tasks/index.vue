<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchMembers, type MemberSearchItem } from '../../../api/app/member-search'
import type { TaskListRole } from '../../../api/app/taskList'
import TaskBoardView from '../../../components/workspace/TaskBoardView.vue'
import TaskCustomFieldsModal from '../../../components/workspace/TaskCustomFieldsModal.vue'
import TaskDetailPanel from '../../../components/workspace/TaskDetailPanel.vue'
import TaskViewToolbar from '../../../components/workspace/TaskViewToolbar.vue'
import WsDateTimeInput from '../../../components/workspace/WsDateTimeInput.vue'
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'
import { useUserPreferenceStore } from '../../../stores/userPreference'
import { useAuthStore } from '../../../stores/auth'
import { useContactsStore } from '../../../stores/contacts'
import { isOverdueTask, useTasksStore, type TasksNavView } from '../../../stores/tasks'
import {
  partitionByGroupBy
} from '../../../stores/tasks/groupByLocal'
import { resolveAssigneeIds } from '../../../stores/tasks/assigneeLocal'
import { useMineGroupsStore } from '../../../stores/tasks/mineGroupsStore'
import { useListGroupsStore } from '../../../stores/tasks/listGroupsStore'
import {
  isCustomFieldKey,
  useCustomFieldsStore
} from '../../../stores/tasks/customFieldsStore'
import {
  navViewToContextType
} from '../../../stores/tasks/viewConfigLocal'
import { useTaskViewConfigStore } from '../../../stores/tasks/viewConfigStore'

const route = useRoute()
const router = useRouter()
const tasksStore = useTasksStore()
const viewConfigStore = useTaskViewConfigStore()
const mineGroupsStore = useMineGroupsStore()
const listGroupsStore = useListGroupsStore()
const customFieldsStore = useCustomFieldsStore()
const preference = useUserPreferenceStore()
const auth = useAuthStore()
const contacts = useContactsStore()
const tab = ref<'list' | 'board'>('list')
const createOpen = ref(false)
const createListOpen = ref(false)
const createMineGroupOpen = ref(false)
const createMineGroupName = ref('')
const createListGroupOpen = ref(false)
const createListGroupName = ref('')
const customFieldsOpen = ref(false)
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

const PANEL_COLLAPSE_KEY = 'relayflow:tasks:panel-collapse'
const quickAccessExpanded = ref(true)
const listsExpanded = ref(true)
const archivedExpanded = ref(false)
const archiveConfirmOpen = ref(false)

function loadPanelCollapse() {
  try {
    const raw = localStorage.getItem(PANEL_COLLAPSE_KEY)
    if (!raw) {
      return
    }
    const parsed = JSON.parse(raw) as {
      quickAccess?: boolean
      lists?: boolean
      archived?: boolean
    }
    if (typeof parsed.quickAccess === 'boolean') {
      quickAccessExpanded.value = parsed.quickAccess
    }
    if (typeof parsed.lists === 'boolean') {
      listsExpanded.value = parsed.lists
    }
    if (typeof parsed.archived === 'boolean') {
      archivedExpanded.value = parsed.archived
    }
  } catch {
    // ignore
  }
}

function persistPanelCollapse() {
  try {
    localStorage.setItem(PANEL_COLLAPSE_KEY, JSON.stringify({
      quickAccess: quickAccessExpanded.value,
      lists: listsExpanded.value,
      archived: archivedExpanded.value
    }))
  } catch {
    // ignore
  }
}

function toggleQuickAccess() {
  quickAccessExpanded.value = !quickAccessExpanded.value
  persistPanelCollapse()
}

function toggleLists() {
  listsExpanded.value = !listsExpanded.value
  persistPanelCollapse()
}

function toggleArchived() {
  archivedExpanded.value = !archivedExpanded.value
  persistPanelCollapse()
}

loadPanelCollapse()

const activeListArchived = computed(() => tasksStore.activeList?.archived === true)

/** Views that load into tasksStore.items via fetchMyTasks */
const itemPageViews: TasksNavView[] = ['mine', 'all', 'created', 'assigned_by_me', 'done']

const inListContext = computed(() => !!tasksStore.activeListId)

const viewTitle = computed(() => {
  if (inListContext.value) {
    return tasksStore.activeList?.name ?? '清单'
  }
  switch (tasksStore.navView) {
    case 'following':
      return '我关注的'
    case 'activity':
      return '动态'
    case 'all':
      return '全部任务'
    case 'done':
      return '已完成'
    case 'created':
      return '我创建的'
    case 'assigned_by_me':
      return '我分配的'
    default:
      return '我负责的'
  }
})

const displayItems = computed(() => {
  let raw
  if (inListContext.value) {
    raw = tasksStore.listItems
  } else if (tasksStore.navView === 'following') {
    raw = tasksStore.followingItems
  } else {
    raw = tasksStore.items
  }
  return viewConfigStore.applyClientTransforms(raw)
})

const showViewToolbar = computed(
  () => !inListContext.value
    ? tasksStore.navView !== 'activity'
    : true
)

const canUsePersonalCustomGroup = computed(
  () => !inListContext.value && tasksStore.navView === 'mine'
)

const isPersonalCustomActive = computed(() => {
  const groupBy = viewConfigStore.activeConfig.groupBy
  return canUsePersonalCustomGroup.value && groupBy?.mode === 'PERSONAL_CUSTOM'
})

/** D7: list-local groups when not field-grouped. */
const isListGroupActive = computed(() => {
  if (!inListContext.value) {
    return false
  }
  const groupBy = viewConfigStore.activeConfig.groupBy
  return groupBy === null || groupBy.mode === 'LIST_GROUP'
})

const isCustomFieldGroupActive = computed(() => {
  if (!inListContext.value) {
    return false
  }
  const groupBy = viewConfigStore.activeConfig.groupBy
  return groupBy?.mode === 'FIELD' && isCustomFieldKey(groupBy.fieldKey)
})

const customFieldGroupItems = computed(() =>
  inListContext.value ? customFieldsStore.groupByMenuItems() : []
)

const displayBuckets = computed(() => {
  const groupBy = viewConfigStore.activeConfig.groupBy
  if (groupBy?.mode === 'PERSONAL_CUSTOM' && canUsePersonalCustomGroup.value) {
    return mineGroupsStore.partition(displayItems.value)
  }
  if (isListGroupActive.value) {
    return listGroupsStore.partition(displayItems.value)
  }
  if (isCustomFieldGroupActive.value && groupBy?.mode === 'FIELD') {
    return customFieldsStore.partition(displayItems.value, groupBy.fieldKey)
  }
  return partitionByGroupBy(displayItems.value, groupBy, {
    assigneeLabel: assigneeBucketLabel
  })
})

function assigneeBucketLabel(userId: string): string {
  if (String(auth.userId) === userId) {
    return auth.user?.nickname || auth.user?.username || '我'
  }
  const member = contacts.members.find(m => m.id === userId)
  return member?.nickname || userId
}

async function ensureContactsForAssigneeGroup() {
  const groupBy = viewConfigStore.activeConfig.groupBy
  if (groupBy?.mode !== 'FIELD' || groupBy.fieldKey !== 'assigneeId') {
    return
  }
  if (contacts.members.length) {
    return
  }
  try {
    await contacts.fetchDepts()
    const rootId = contacts.rootDeptId()
    if (rootId) {
      await contacts.fetchMembers({ deptId: rootId })
    }
  } catch {
    // Labels fall back to userId; grouping keys remain correct.
  }
}

const showGroupHeaders = computed(() =>
  viewConfigStore.activeConfig.groupBy !== null || isListGroupActive.value
)

const showListTabs = computed(() => false)
const showCreateButton = computed(() => {
  if (inListContext.value) {
    return tasksStore.listCanMutateTasks
  }
  return tasksStore.navView === 'mine'
    || tasksStore.navView === 'created'
    || tasksStore.navView === 'all'
})
const showTaskActions = computed(() => {
  if (inListContext.value) {
    return tasksStore.listCanMutateTasks
  }
  return tasksStore.navView === 'mine'
    || tasksStore.navView === 'done'
    || tasksStore.navView === 'created'
    || tasksStore.navView === 'all'
    || tasksStore.navView === 'assigned_by_me'
})

const canDragGrouped = computed(() => {
  const groupBy = viewConfigStore.activeConfig.groupBy
  if (isListGroupActive.value) {
    return tasksStore.listCanMutateTasks
  }
  if (!groupBy) {
    return false
  }
  if (groupBy.mode === 'PERSONAL_CUSTOM') {
    return canUsePersonalCustomGroup.value && showTaskActions.value
  }
  if (groupBy.mode !== 'FIELD') {
    return false
  }
  if (isCustomFieldKey(groupBy.fieldKey)) {
    return inListContext.value && tasksStore.listCanMutateTasks
  }
  if (inListContext.value) {
    return tasksStore.listCanMutateTasks
  }
  return showTaskActions.value
})

const deletableMineGroupKeys = computed(() =>
  isPersonalCustomActive.value
    ? mineGroupsStore.sortedGroups.filter(g => !g.isDefault).map(g => g.id)
    : []
)

const deletableListGroupKeys = computed(() =>
  isListGroupActive.value
    ? listGroupsStore.sortedGroups.filter(g => !g.isDefault).map(g => g.id)
    : []
)

const deletableBucketKeys = computed(() =>
  isPersonalCustomActive.value
    ? deletableMineGroupKeys.value
    : isListGroupActive.value
      ? deletableListGroupKeys.value
      : []
)

function parseView(raw: unknown): TasksNavView {
  const v = typeof raw === 'string' ? raw : Array.isArray(raw) ? raw[0] : null
  if (
    v === 'following'
    || v === 'activity'
    || v === 'done'
    || v === 'created'
    || v === 'all'
    || v === 'assigned_by_me'
  ) {
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
  if (view === 'all' || view === 'created' || view === 'assigned_by_me' || view === 'done') {
    if (!quickAccessExpanded.value) {
      quickAccessExpanded.value = true
      persistPanelCollapse()
    }
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
  const isArchived = tasksStore.archivedLists.some(l => l.id === listId)
  if (isArchived) {
    if (!archivedExpanded.value) {
      archivedExpanded.value = true
      persistPanelCollapse()
    }
  } else if (!listsExpanded.value) {
    listsExpanded.value = true
    persistPanelCollapse()
  }
  await router.replace({ query: q })
  try {
    await tasksStore.selectList(listId)
    tab.value = 'list'
  } catch {
    toast.add({ title: '无法打开该清单', color: 'error' })
  }
}

async function switchTab(next: 'list' | 'board') {
  tab.value = next
  viewConfigStore.patchActiveConfig({ displayMode: next === 'board' ? 'BOARD' : 'LIST' })
  if (next === 'board' && inListContext.value) {
    try {
      await tasksStore.fetchListBoard()
    } catch {
      toast.add({ title: '加载看板失败', color: 'error' })
    }
  }
}

async function syncViewConfigContext() {
  viewConfigStore.ensureLoaded()
  if (inListContext.value && tasksStore.activeListId) {
    await viewConfigStore.setActiveContext({
      contextType: 'LIST',
      contextId: tasksStore.activeListId
    })
  } else {
    const type = navViewToContextType(tasksStore.navView)
    if (type) {
      await viewConfigStore.setActiveContext({ contextType: type })
    }
  }
  const mode = viewConfigStore.activeConfig.displayMode
  tab.value = mode === 'BOARD' ? 'board' : 'list'
}

async function handleGroupMove(payload: {
  taskId: string
  bucketKey: string
  beforeId: string | null
}) {
  const groupBy = viewConfigStore.activeConfig.groupBy
  if (isListGroupActive.value) {
    if (!tasksStore.listCanMutateTasks) {
      return
    }
    try {
      await listGroupsStore.moveTask(payload.taskId, payload.bucketKey, payload.beforeId)
    } catch {
      toast.add({ title: '移动失败', color: 'error' })
    }
    return
  }
  if (!groupBy) {
    return
  }
  if (groupBy.mode === 'PERSONAL_CUSTOM') {
    if (!canUsePersonalCustomGroup.value) {
      return
    }
    try {
      await mineGroupsStore.moveTask(payload.taskId, payload.bucketKey, payload.beforeId)
    } catch {
      toast.add({ title: '移动失败', color: 'error' })
    }
    return
  }
  if (groupBy.mode !== 'FIELD') {
    return
  }
  if (isCustomFieldKey(groupBy.fieldKey)) {
    if (!tasksStore.listCanMutateTasks) {
      return
    }
    try {
      await customFieldsStore.moveTask(payload.taskId, groupBy.fieldKey, payload.bucketKey)
    } catch {
      toast.add({ title: '移动失败', color: 'error' })
    }
    return
  }
  try {
    await tasksStore.moveGroupedField({
      taskId: payload.taskId,
      fieldKey: groupBy.fieldKey,
      targetKey: payload.bucketKey,
      beforeId: payload.beforeId
    })
  } catch {
    toast.add({ title: '移动失败', color: 'error' })
  }
}

async function handleCreateMineGroup() {
  const name = createMineGroupName.value.trim()
  if (!name) {
    toast.add({ title: '请输入分组名称', color: 'error' })
    return
  }
  try {
    await mineGroupsStore.createGroup(name)
    createMineGroupName.value = ''
    createMineGroupOpen.value = false
    toast.add({ title: '分组已创建', color: 'success' })
  } catch {
    toast.add({ title: '创建分组失败', color: 'error' })
  }
}

async function handleDeleteMineGroup(groupId: string) {
  try {
    await mineGroupsStore.deleteGroup(groupId)
    toast.add({ title: '分组已删除，任务已回到默认组', color: 'success' })
  } catch {
    toast.add({ title: '删除分组失败', color: 'error' })
  }
}

async function handleCreateListGroup() {
  const name = createListGroupName.value.trim()
  if (!name) {
    toast.add({ title: '请输入分组名称', color: 'error' })
    return
  }
  try {
    await listGroupsStore.createGroup(name)
    createListGroupName.value = ''
    createListGroupOpen.value = false
    toast.add({ title: '分组已创建', color: 'success' })
  } catch {
    toast.add({ title: '创建分组失败', color: 'error' })
  }
}

async function handleDeleteListGroup(groupId: string) {
  try {
    await listGroupsStore.deleteGroup(groupId)
    toast.add({ title: '分组已删除，任务已回到默认组', color: 'success' })
  } catch {
    toast.add({ title: '删除分组失败', color: 'error' })
  }
}

function handleDeleteBucket(groupId: string) {
  if (isPersonalCustomActive.value) {
    void handleDeleteMineGroup(groupId)
  } else if (isListGroupActive.value) {
    void handleDeleteListGroup(groupId)
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
  if (!itemPageViews.includes(tasksStore.navView) && tasksStore.navView !== 'following') {
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
  await Promise.all([tasksStore.fetchMyLists(), tasksStore.fetchArchivedLists()])
  const listId = parseListId(route.query.listId)
  if (listId) {
    await applyListIdFromRoute()
  } else {
    await applyViewFromRoute()
    if (itemPageViews.includes(tasksStore.navView) && tasksStore.items.length === 0 && !tasksStore.loading) {
      await tasksStore.fetchMyTasks({ pageNo: 1 })
    } else if (tasksStore.navView === 'following' && tasksStore.followingItems.length === 0 && !tasksStore.loading) {
      await tasksStore.fetchFollowingTasks({ pageNo: 1 })
    } else if (tasksStore.navView === 'activity' && tasksStore.activityFeed.length === 0 && !tasksStore.loading) {
      await tasksStore.fetchActivityFeed()
    }
  }
  if (!itemPageViews.includes(tasksStore.navView) && !inListContext.value) {
    void tasksStore.refreshOverdueBadge()
  }
  await syncViewConfigContext()
  void ensureContactsForAssigneeGroup()
  if (tab.value === 'board' && inListContext.value) {
    void tasksStore.fetchListBoard().catch(() => {})
  }
  await applyTaskIdFromRoute()
})

watch(
  () => [tasksStore.navView, tasksStore.activeListId] as const,
  () => {
    void syncViewConfigContext()
    listGroupsStore.setActiveList(tasksStore.activeListId)
    customFieldsStore.setActiveList(tasksStore.activeListId)
    if (tasksStore.activeListId) {
      void customFieldsStore.fetchList(tasksStore.activeListId, true).catch(() => {
        toast.add({ title: '加载自定义字段失败', color: 'error' })
      })
    }
  },
  { immediate: true }
)

watch(
  () => viewConfigStore.activeConfig.groupBy,
  () => {
    void ensureContactsForAssigneeGroup()
  }
)

watch(isPersonalCustomActive, (active) => {
  if (active) {
    void mineGroupsStore.fetchList(true).catch(() => {
      toast.add({ title: '加载个人分组失败', color: 'error' })
    })
  }
})

watch(
  () => [isListGroupActive.value, tasksStore.activeListId] as const,
  ([active, listId]) => {
    if (active && listId) {
      void listGroupsStore.fetchList(listId, true).catch(() => {
        toast.add({ title: '加载清单分组失败', color: 'error' })
      })
    }
  },
  { immediate: true }
)

watch(
  () => viewConfigStore.activeConfig.displayMode,
  async (mode) => {
    const next = mode === 'BOARD' ? 'board' : 'list'
    if (tab.value !== next) {
      tab.value = next
    }
    if (next === 'board' && inListContext.value) {
      try {
        await tasksStore.fetchListBoard()
      } catch {
        // keep previous columns
      }
    }
  }
)

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

function assigneeFieldLabel(task: { assigneeId?: string | null, assigneeIds?: string[] }) {
  const ids = resolveAssigneeIds(task)
  if (!ids.length) {
    return ''
  }
  if (ids.length > 1) {
    return `${ids.length} 人`
  }
  return assigneeBucketLabel(ids[0]!)
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
      if (isPersonalCustomActive.value) {
        mineGroupsStore.ensureTaskInDefault(id)
        void mineGroupsStore.fetchList(true).catch(() => {})
      }
      if (isListGroupActive.value) {
        listGroupsStore.ensureTaskInDefault(id)
        void listGroupsStore.fetchList(tasksStore.activeListId, true).catch(() => {})
      }
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
  archiveConfirmOpen.value = true
}

async function confirmArchiveList() {
  const id = tasksStore.activeListId
  if (!id) {
    return
  }
  try {
    await tasksStore.archiveList(id)
    archiveConfirmOpen.value = false
    if (!archivedExpanded.value) {
      archivedExpanded.value = true
      persistPanelCollapse()
    }
    toast.add({
      title: '清单已归档',
      description: '可在侧栏「已归档」中查看并恢复',
      color: 'success'
    })
    await switchView('mine')
  } catch {
    toast.add({ title: '归档失败', color: 'error' })
  }
}

async function handleUnarchiveList(listId?: string) {
  const id = listId || tasksStore.activeListId
  if (!id) {
    return
  }
  try {
    await tasksStore.unarchiveList(id)
    if (!listsExpanded.value) {
      listsExpanded.value = true
      persistPanelCollapse()
    }
    toast.add({ title: '清单已恢复', color: 'success' })
    await openList(id)
  } catch {
    toast.add({ title: '恢复失败', color: 'error' })
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
  if (!inviteForm.userId.trim()) {
    toast.add({ title: '请先搜索并选择同事', color: 'error' })
    return
  }
  try {
    await tasksStore.inviteListMember({
      userId: inviteForm.userId.trim(),
      nickname: inviteForm.nickname.trim() || inviteForm.keyword.trim(),
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
  return [
    'workspace-list-item flex w-full items-center gap-2.5 px-3 py-2 text-left text-sm',
    active ? 'font-medium text-[var(--ws-text)]' : 'text-[var(--ws-text)]'
  ].join(' ')
}

function listNavClass(listId: string) {
  const active = tasksStore.activeListId === listId
  return [
    'workspace-list-item flex w-full items-center gap-2.5 px-3 py-2 text-left text-sm',
    active ? 'font-medium text-[var(--ws-text)]' : 'text-[var(--ws-text)]'
  ].join(' ')
}

const navIcons: Record<TasksNavView, string> = {
  mine: 'i-lucide-user',
  following: 'i-lucide-eye',
  activity: 'i-lucide-activity',
  all: 'i-lucide-layout-list',
  created: 'i-lucide-pen-line',
  assigned_by_me: 'i-lucide-user-round-cog',
  done: 'i-lucide-circle-check'
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
  if (tasksStore.navView === 'all') {
    return '暂无可见任务'
  }
  if (tasksStore.navView === 'assigned_by_me') {
    return '暂无我分配的任务'
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
  if (tasksStore.navView === 'all') {
    return '你负责、创建、关注或清单内的任务会出现在这里'
  }
  if (tasksStore.navView === 'assigned_by_me') {
    return '将任务负责人改为他人（自己不再是负责人）后会出现在这里'
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
        <h2 class="text-base font-semibold tracking-tight text-[var(--ws-text)]">
          任务
        </h2>
      </div>
      <div class="space-y-4 p-2">
        <!-- Primary shortcuts -->
        <div class="space-y-0.5">
          <button
            type="button"
            :class="navItemClass('mine')"
            :data-active="!inListContext && tasksStore.navView === 'mine' ? 'true' : undefined"
            @click="switchView('mine')"
          >
            <UIcon
              :name="navIcons.mine"
              class="size-4 shrink-0"
              :class="!inListContext && tasksStore.navView === 'mine' ? 'text-[var(--ui-primary)]' : 'text-[var(--ws-text-muted)]'"
            />
            <span class="min-w-0 flex-1 truncate">我负责的</span>
            <span
              v-if="tasksStore.overdueBadgeCount > 0"
              class="rounded-md bg-red-500/15 px-1.5 py-0.5 text-[10px] font-medium text-red-600 dark:text-red-400"
            >
              {{ overdueBadgeLabel(tasksStore.overdueBadgeCount, tasksStore.overdueBadgeCapped) }}
            </span>
          </button>
          <button
            type="button"
            :class="navItemClass('following')"
            :data-active="!inListContext && tasksStore.navView === 'following' ? 'true' : undefined"
            @click="switchView('following')"
          >
            <UIcon
              :name="navIcons.following"
              class="size-4 shrink-0"
              :class="!inListContext && tasksStore.navView === 'following' ? 'text-[var(--ui-primary)]' : 'text-[var(--ws-text-muted)]'"
            />
            <span class="min-w-0 flex-1 truncate">我关注的</span>
          </button>
          <button
            type="button"
            :class="navItemClass('activity')"
            :data-active="!inListContext && tasksStore.navView === 'activity' ? 'true' : undefined"
            @click="switchView('activity')"
          >
            <UIcon
              :name="navIcons.activity"
              class="size-4 shrink-0"
              :class="!inListContext && tasksStore.navView === 'activity' ? 'text-[var(--ui-primary)]' : 'text-[var(--ws-text-muted)]'"
            />
            <span class="min-w-0 flex-1 truncate">动态</span>
          </button>
        </div>

        <!-- Quick access (collapsible) -->
        <div class="space-y-0.5 border-t border-[var(--ws-border-subtle)] pt-3">
          <button
            type="button"
            class="mb-1 flex w-full items-center gap-1.5 rounded-md px-3 py-1.5 text-left text-[11px] font-semibold tracking-wide text-[var(--ws-text-muted)] hover:bg-[var(--ws-rail-hover)]"
            :aria-expanded="quickAccessExpanded"
            @click="toggleQuickAccess"
          >
            <UIcon
              :name="quickAccessExpanded ? 'i-lucide-chevron-down' : 'i-lucide-chevron-right'"
              class="size-3.5 shrink-0 opacity-80"
            />
            <span class="select-none">快速访问</span>
          </button>
          <template v-if="quickAccessExpanded">
            <button
              type="button"
              :class="navItemClass('all')"
              :data-active="!inListContext && tasksStore.navView === 'all' ? 'true' : undefined"
              @click="switchView('all')"
            >
              <UIcon
                :name="navIcons.all"
                class="size-4 shrink-0"
                :class="!inListContext && tasksStore.navView === 'all' ? 'text-[var(--ui-primary)]' : 'text-[var(--ws-text-muted)]'"
              />
              <span class="min-w-0 flex-1 truncate">全部任务</span>
            </button>
            <button
              type="button"
              :class="navItemClass('created')"
              :data-active="!inListContext && tasksStore.navView === 'created' ? 'true' : undefined"
              @click="switchView('created')"
            >
              <UIcon
                :name="navIcons.created"
                class="size-4 shrink-0"
                :class="!inListContext && tasksStore.navView === 'created' ? 'text-[var(--ui-primary)]' : 'text-[var(--ws-text-muted)]'"
              />
              <span class="min-w-0 flex-1 truncate">我创建的</span>
            </button>
            <button
              type="button"
              :class="navItemClass('assigned_by_me')"
              :data-active="!inListContext && tasksStore.navView === 'assigned_by_me' ? 'true' : undefined"
              @click="switchView('assigned_by_me')"
            >
              <UIcon
                :name="navIcons.assigned_by_me"
                class="size-4 shrink-0"
                :class="!inListContext && tasksStore.navView === 'assigned_by_me' ? 'text-[var(--ui-primary)]' : 'text-[var(--ws-text-muted)]'"
              />
              <span class="min-w-0 flex-1 truncate">我分配的</span>
            </button>
            <button
              type="button"
              :class="navItemClass('done')"
              :data-active="!inListContext && tasksStore.navView === 'done' ? 'true' : undefined"
              @click="switchView('done')"
            >
              <UIcon
                :name="navIcons.done"
                class="size-4 shrink-0"
                :class="!inListContext && tasksStore.navView === 'done' ? 'text-[var(--ui-primary)]' : 'text-[var(--ws-text-muted)]'"
              />
              <span class="min-w-0 flex-1 truncate">已完成</span>
            </button>
          </template>
        </div>

        <!-- Task lists (collapsible) -->
        <div class="space-y-0.5 border-t border-[var(--ws-border-subtle)] pt-3">
          <div class="mb-1 flex items-center gap-0.5">
            <button
              type="button"
              class="flex min-w-0 flex-1 items-center gap-1.5 rounded-md px-3 py-1.5 text-left text-[11px] font-semibold tracking-wide text-[var(--ws-text-muted)] hover:bg-[var(--ws-rail-hover)]"
              :aria-expanded="listsExpanded"
              @click="toggleLists"
            >
              <UIcon
                :name="listsExpanded ? 'i-lucide-chevron-down' : 'i-lucide-chevron-right'"
                class="size-3.5 shrink-0 opacity-80"
              />
              <span class="min-w-0 flex-1 truncate select-none">任务清单</span>
            </button>
            <UButton
              color="neutral"
              variant="ghost"
              icon="i-lucide-plus"
              size="xs"
              class="mr-1 shrink-0"
              aria-label="新建清单"
              @click.stop="createListOpen = true"
            />
          </div>
          <template v-if="listsExpanded">
            <button
              v-for="list in tasksStore.myLists"
              :key="list.id"
              type="button"
              :class="listNavClass(list.id)"
              :data-active="tasksStore.activeListId === list.id ? 'true' : undefined"
              @click="openList(list.id)"
            >
              <UIcon
                name="i-lucide-clipboard-list"
                class="size-4 shrink-0"
                :class="tasksStore.activeListId === list.id ? 'text-[var(--ui-primary)]' : 'text-[var(--ws-text-muted)]'"
              />
              <span class="min-w-0 flex-1 truncate">{{ list.name }}</span>
            </button>
            <p
              v-if="!tasksStore.myLists.length"
              class="px-3 py-2 pl-9 text-xs text-[var(--ws-text-muted)]"
            >
              暂无清单，点击 + 创建
            </p>
          </template>
        </div>

        <!-- Archived lists (collapsible) -->
        <div class="space-y-0.5 border-t border-[var(--ws-border-subtle)] pt-3">
          <button
            type="button"
            class="mb-1 flex w-full items-center gap-1.5 rounded-md px-3 py-1.5 text-left text-[11px] font-semibold tracking-wide text-[var(--ws-text-muted)] hover:bg-[var(--ws-rail-hover)]"
            :aria-expanded="archivedExpanded"
            @click="toggleArchived"
          >
            <UIcon
              :name="archivedExpanded ? 'i-lucide-chevron-down' : 'i-lucide-chevron-right'"
              class="size-3.5 shrink-0 opacity-80"
            />
            <span class="min-w-0 flex-1 truncate select-none">
              已归档
              <span v-if="tasksStore.archivedLists.length">
                （{{ tasksStore.archivedLists.length }}）
              </span>
            </span>
          </button>
          <template v-if="archivedExpanded">
            <div
              v-for="list in tasksStore.archivedLists"
              :key="list.id"
              class="flex items-center gap-1"
            >
              <button
                type="button"
                :class="listNavClass(list.id)"
                class="min-w-0 flex-1"
                :data-active="tasksStore.activeListId === list.id ? 'true' : undefined"
                @click="openList(list.id)"
              >
                <UIcon
                  name="i-lucide-archive"
                  class="size-4 shrink-0"
                  :class="tasksStore.activeListId === list.id ? 'text-[var(--ui-primary)]' : 'text-[var(--ws-text-muted)]'"
                />
                <span class="min-w-0 flex-1 truncate">{{ list.name }}</span>
              </button>
              <UButton
                v-if="list.myRole === 'OWNER'"
                color="neutral"
                variant="ghost"
                size="xs"
                class="mr-1 shrink-0"
                label="恢复"
                @click.stop="handleUnarchiveList(list.id)"
              />
            </div>
            <p
              v-if="!tasksStore.archivedLists.length"
              class="px-3 py-2 pl-9 text-xs text-[var(--ws-text-muted)]"
            >
              暂无已归档清单
            </p>
          </template>
        </div>
      </div>
    </template>

    <div class="flex h-full min-h-0 flex-col">
      <header class="flex items-center gap-3 border-b border-[var(--ws-border-subtle)] px-5 py-3">
        <h1 class="min-w-0 flex-1 truncate text-lg font-semibold">
          {{ viewTitle }}
        </h1>
        <UBadge
          v-if="activeListArchived"
          color="neutral"
          variant="subtle"
          label="已归档"
        />
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
          v-if="inListContext"
          color="neutral"
          variant="soft"
          icon="i-lucide-tags"
          @click="customFieldsOpen = true"
        >
          字段
        </UButton>
        <UButton
          v-if="inListContext && tasksStore.listCanEditMeta && !activeListArchived"
          color="neutral"
          variant="ghost"
          icon="i-lucide-archive"
          @click="handleArchiveList"
        >
          归档
        </UButton>
        <UButton
          v-if="inListContext && tasksStore.listCanEditMeta && activeListArchived"
          color="primary"
          variant="soft"
          icon="i-lucide-archive-restore"
          @click="handleUnarchiveList()"
        >
          恢复
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
        v-if="activeListArchived"
        class="flex items-center gap-2 border-b border-[var(--ws-border-subtle)] px-5 py-2 text-xs text-[var(--ws-text-muted)]"
      >
        <UIcon name="i-lucide-archive" class="size-3.5 shrink-0" />
        <span class="min-w-0 flex-1">此清单已归档，任务仍保留；恢复后可继续编辑。</span>
        <UButton
          v-if="tasksStore.listCanEditMeta"
          color="primary"
          variant="soft"
          size="xs"
          label="恢复清单"
          @click="handleUnarchiveList()"
        />
      </div>

      <TaskViewToolbar
        v-if="showViewToolbar"
        :show-board-mode="true"
        :can-use-personal-custom-group="canUsePersonalCustomGroup"
        :can-use-list-group="inListContext"
        :custom-field-group-items="customFieldGroupItems"
      />

      <div
        v-if="isPersonalCustomActive"
        class="flex items-center gap-2 border-b border-[var(--ws-border-subtle)] px-5 py-2"
      >
        <UButton
          color="neutral"
          variant="soft"
          size="xs"
          icon="i-lucide-folder-plus"
          @click="createMineGroupOpen = true"
        >
          新建分组
        </UButton>
        <span class="text-xs text-[var(--ws-text-muted)]">
          仅对本账号可见
        </span>
      </div>

      <div
        v-else-if="isListGroupActive && tasksStore.listCanMutateTasks"
        class="flex items-center gap-2 border-b border-[var(--ws-border-subtle)] px-5 py-2"
      >
        <UButton
          color="neutral"
          variant="soft"
          size="xs"
          icon="i-lucide-folder-plus"
          @click="createListGroupOpen = true"
        >
          新建分组
        </UButton>
        <span class="text-xs text-[var(--ws-text-muted)]">
          清单内分组；清单成员可见
        </span>
      </div>

      <div
        v-else-if="isCustomFieldGroupActive"
        class="flex items-center gap-2 border-b border-[var(--ws-border-subtle)] px-5 py-2"
      >
        <span class="text-xs text-[var(--ws-text-muted)]">
          按自定义字段分组；空值在「无分组」
        </span>
      </div>

      <div
        v-if="showListTabs"
        class="flex gap-4 border-b border-[var(--ws-border-subtle)] px-5"
      >
        <button
          type="button"
          class="border-b-2 py-2 text-sm"
          :class="tab === 'list' ? 'border-primary text-primary font-medium' : 'border-transparent text-[var(--ws-text-muted)]'"
          @click="switchTab('list')"
        >
          任务列表
        </button>
        <button
          type="button"
          class="border-b-2 py-2 text-sm"
          :class="tab === 'board' ? 'border-primary text-primary font-medium' : 'border-transparent text-[var(--ws-text-muted)]'"
          @click="switchTab('board')"
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

        <div v-else-if="tab === 'list'">
          <div v-if="tasksStore.loading" class="flex justify-center py-12">
            <UIcon name="i-lucide-loader-circle" class="size-6 animate-spin text-[var(--ws-text-muted)]" />
          </div>
          <div v-else-if="displayItems.length" class="mx-auto max-w-3xl space-y-4">
            <section
              v-for="bucket in displayBuckets"
              :key="bucket.key"
              class="space-y-2"
            >
              <header
                v-if="showGroupHeaders"
                class="flex items-center justify-between px-1 pt-1"
              >
                <h2 class="text-sm font-medium text-[var(--ws-text)]">
                  {{ bucket.label }}
                </h2>
                <div class="flex items-center gap-1">
                  <span class="text-xs text-[var(--ws-text-muted)]">
                    {{ bucket.items.length }}
                  </span>
                  <UButton
                    v-if="deletableBucketKeys.includes(bucket.key)"
                    color="neutral"
                    variant="ghost"
                    icon="i-lucide-trash-2"
                    size="xs"
                    aria-label="删除分组"
                    @click="handleDeleteBucket(bucket.key)"
                  />
                </div>
              </header>
              <div
                v-for="task in bucket.items"
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
                      v-if="viewConfigStore.isFieldVisible('dueTime') && formatDue(task.dueTime)"
                      :class="isOverdueTask(task) ? 'font-medium text-red-600 dark:text-red-400' : ''"
                    >
                      {{ isOverdueTask(task) ? '已逾期' : '截止' }} {{ formatDue(task.dueTime) }}
                    </span>
                    <span v-if="viewConfigStore.isFieldVisible('status')">
                      {{ task.status === 'DONE' ? '已完成' : '未完成' }}
                    </span>
                    <span v-if="viewConfigStore.isFieldVisible('assignee') && assigneeFieldLabel(task)">
                      负责人 {{ assigneeFieldLabel(task) }}
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
              <p
                v-if="showGroupHeaders && !bucket.items.length"
                class="px-1 py-2 text-xs text-[var(--ws-text-muted)]"
              >
                暂无任务
              </p>
            </section>
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
        <TaskBoardView
          v-else-if="tab === 'board' && (inListContext || showViewToolbar)"
          :buckets="displayBuckets"
          :can-drag="canDragGrouped"
          :deletable-bucket-keys="deletableBucketKeys"
          :selected-id="tasksStore.selectedId"
          :loading="tasksStore.loading"
          :show-toggle="showTaskActions"
          @open="openTask"
          @move="handleGroupMove"
          @delete-bucket="handleDeleteBucket"
          @toggle="({ taskId, done }) => handleToggle(taskId, done)"
        />
        <UEmpty
          v-else
          icon="i-lucide-kanban-square"
          title="看板视图"
          description="切换到列表或打开有任务的入口"
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
          :list-id="tasksStore.activeListId"
          :can-edit-custom-fields="tasksStore.listCanMutateTasks"
          @close="detailOpen = false"
        />
      </template>
    </USlideover>

    <TaskCustomFieldsModal
      v-model:open="customFieldsOpen"
      :can-edit="tasksStore.listCanMutateTasks"
    />

    <UModal v-model:open="createOpen" title="新建任务">
      <template #body>
        <form class="space-y-4" @submit.prevent="handleCreate">
          <UFormField label="标题" required>
            <UInput v-model="createForm.title" placeholder="任务标题" autofocus />
          </UFormField>
          <UFormField label="截止时间">
            <WsDateTimeInput v-model="createForm.dueTime" type="datetime-local" />
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

    <UModal v-model:open="archiveConfirmOpen" title="归档清单">
      <template #body>
        <div class="space-y-4">
          <p class="text-sm text-[var(--ws-text)]">
            归档后清单会从日常侧栏隐藏，可在「已归档」中查看并恢复。不会删除清单内的任务。
          </p>
          <div class="flex justify-end gap-2">
            <UButton color="neutral" variant="soft" @click="archiveConfirmOpen = false">
              取消
            </UButton>
            <UButton
              color="primary"
              :loading="tasksStore.saving"
              @click="confirmArchiveList"
            >
              确认归档
            </UButton>
          </div>
        </div>
      </template>
    </UModal>

    <UModal v-model:open="createMineGroupOpen" title="新建分组">
      <template #body>
        <form class="space-y-4" @submit.prevent="handleCreateMineGroup">
          <UFormField label="名称" required>
            <UInput
              v-model="createMineGroupName"
              placeholder="例如：本周"
              autofocus
            />
          </UFormField>
          <div class="flex justify-end gap-2">
            <UButton color="neutral" variant="soft" @click="createMineGroupOpen = false">
              取消
            </UButton>
            <UButton type="submit" color="primary">
              创建
            </UButton>
          </div>
        </form>
      </template>
    </UModal>

    <UModal v-model:open="createListGroupOpen" title="新建清单分组">
      <template #body>
        <form class="space-y-4" @submit.prevent="handleCreateListGroup">
          <UFormField label="名称" required>
            <UInput
              v-model="createListGroupName"
              placeholder="例如：本周冲刺"
              autofocus
            />
          </UFormField>
          <div class="flex justify-end gap-2">
            <UButton color="neutral" variant="soft" @click="createListGroupOpen = false">
              取消
            </UButton>
            <UButton type="submit" color="primary">
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
              :disabled="!inviteForm.userId"
              @click="handleInviteMember"
            >
              邀请
            </UButton>
            <p class="text-xs text-[var(--ws-text-muted)]">
              搜索并选择同事后邀请（须为租户有效成员）。
            </p>
          </div>
        </div>
      </template>
    </UModal>
  </WorkspaceShell>
</template>
