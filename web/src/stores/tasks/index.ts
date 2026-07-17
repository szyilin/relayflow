import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  assignTask,
  createSubtask,
  createTask,
  createTaskComment,
  deleteTask,
  followTask,
  getFollowingTaskPage,
  getTaskActivities,
  getTaskActivityFeed,
  getTaskById,
  getTaskComments,
  getTaskDueRange,
  getTaskFollowers,
  getTaskPage,
  getTaskSubtasks,
  toggleTaskDone,
  unfollowTask,
  updateTask,
  type TaskActivity,
  type TaskComment,
  type TaskDetailUpdatePayload,
  type TaskFollower,
  type TaskItem
} from '../../api/app/task'
import {
  archiveTaskList as apiArchiveTaskList,
  createTaskList as apiCreateTaskList,
  getMyTaskLists,
  getTaskListMembers,
  inviteTaskListMember as apiInviteTaskListMember,
  removeTaskListMember as apiRemoveTaskListMember,
  type TaskList,
  type TaskListMember,
  type TaskListRole
} from '../../api/app/taskList'
import { DEFAULT_LIST_PAGE_SIZE } from '../../constants/pagination'
import { useAuthStore } from '../auth'
import {
  clearLegacyTaskLocal,
  isOverdueTask,
  recomputeTaskProgress,
  type TasksNavView
} from './helpers'
import {
  canEditListMeta,
  canMutateListTasks,
  nextLocalId,
  seedLocalLists,
  USE_LOCAL_TASK_LISTS
} from './listLocal'

export type { TasksNavView } from './helpers'

const DUE_RANGE_LIMIT = 200
/** Overdue badge via due-range [epoch, now); cap matches API limit honesty. */
const OVERDUE_RANGE_LIMIT = 200

export const useTasksStore = defineStore('tasks', () => {
  const items = ref<TaskItem[]>([])
  const total = ref(0)
  const loading = ref(false)
  const saving = ref(false)
  const detailLoading = ref(false)

  const selectedId = ref<string | null>(null)
  const selectedDetail = ref<TaskItem | null>(null)
  const selectedSubtasks = ref<TaskItem[]>([])
  const selectedFollowers = ref<TaskFollower[]>([])
  const selectedComments = ref<TaskComment[]>([])
  const selectedActivities = ref<TaskActivity[]>([])
  const iAmFollowing = ref(false)

  const navView = ref<TasksNavView>('mine')
  const pageNo = ref(1)
  const pageSize = ref(DEFAULT_LIST_PAGE_SIZE)
  const followingItems = ref<TaskItem[]>([])
  const followingTotal = ref(0)
  const followingPageNo = ref(1)
  const activityFeed = ref<TaskActivity[]>([])
  const overdueBadgeCount = ref(0)
  /** True when overdue count may be truncated by OVERDUE_RANGE_LIMIT. */
  const overdueBadgeCapped = ref(false)

  const dueRangeItems = ref<TaskItem[]>([])
  const dueRangeLoading = ref(false)

  /** Active task list context; null = personal nav views. */
  const activeListId = ref<string | null>(null)
  const myLists = ref<TaskList[]>([])
  const listMembers = ref<TaskListMember[]>([])
  const listItems = ref<TaskItem[]>([])
  const listTotal = ref(0)
  const listPageNo = ref(1)
  /** Local-only maps while USE_LOCAL_TASK_LISTS (integrate: delete). */
  const localMembersByList = ref<Record<string, TaskListMember[]>>({})
  const localTasksByList = ref<Record<string, TaskItem[]>>({})
  const localSeeded = ref(false)

  const todoItems = computed(() => items.value.filter(item => item.status === 'TODO'))
  const doneItems = computed(() => items.value.filter(item => item.status === 'DONE'))

  const activeList = computed(() =>
    activeListId.value ? myLists.value.find(l => l.id === activeListId.value) ?? null : null)

  const activeListRole = computed(() => activeList.value?.myRole ?? null)

  const listCanEditMeta = computed(() => canEditListMeta(activeListRole.value))
  const listCanMutateTasks = computed(() => canMutateListTasks(activeListRole.value))

  function currentUserId(): string {
    const auth = useAuthStore()
    return auth.userId ? String(auth.userId) : ''
  }

  function currentNickname(): string {
    const auth = useAuthStore()
    return auth.user?.nickname?.trim() || auth.user?.username?.trim() || '我'
  }

  function ensureLocalSeed() {
    if (!USE_LOCAL_TASK_LISTS || localSeeded.value) {
      return
    }
    const seeded = seedLocalLists(currentUserId(), currentNickname())
    myLists.value = seeded.lists
    localMembersByList.value = seeded.membersByList
    localTasksByList.value = seeded.tasksByList
    localSeeded.value = true
  }

  function resetForTenantSwitch() {
    items.value = []
    total.value = 0
    pageNo.value = 1
    selectedId.value = null
    selectedDetail.value = null
    selectedSubtasks.value = []
    selectedFollowers.value = []
    selectedComments.value = []
    selectedActivities.value = []
    iAmFollowing.value = false
    navView.value = 'mine'
    followingItems.value = []
    followingTotal.value = 0
    followingPageNo.value = 1
    activityFeed.value = []
    overdueBadgeCount.value = 0
    overdueBadgeCapped.value = false
    dueRangeItems.value = []
    loading.value = false
    saving.value = false
    detailLoading.value = false
    dueRangeLoading.value = false
    activeListId.value = null
    myLists.value = []
    listMembers.value = []
    listItems.value = []
    listTotal.value = 0
    listPageNo.value = 1
    localMembersByList.value = {}
    localTasksByList.value = {}
    localSeeded.value = false
  }

  /**
   * Count assignee TODO with dueTime in [epoch, now) via due-range (not first list page).
   * If result length hits the limit, badge is capped (UI may show "+").
   */
  async function refreshOverdueBadge() {
    try {
      const now = new Date().toISOString()
      const list = await getTaskDueRange({
        from: '1970-01-01T00:00:00.000Z',
        to: now,
        limit: OVERDUE_RANGE_LIMIT
      })
      overdueBadgeCount.value = list.length
      overdueBadgeCapped.value = list.length >= OVERDUE_RANGE_LIMIT
    } catch {
      // keep last count
    }
  }

  async function fetchMyTasks(options?: { pageNo?: number }) {
    loading.value = true
    clearLegacyTaskLocal()
    if (options?.pageNo !== undefined) {
      pageNo.value = options.pageNo
    }
    try {
      const view = navView.value
      if (view === 'done') {
        const data = await getTaskPage({
          pageNo: pageNo.value,
          pageSize: pageSize.value,
          status: 'DONE',
          scope: 'ASSIGNEE'
        })
        items.value = data.list
        total.value = data.total
      } else if (view === 'created') {
        const data = await getTaskPage({
          pageNo: pageNo.value,
          pageSize: pageSize.value,
          scope: 'CREATOR'
        })
        items.value = data.list
        total.value = data.total
      } else {
        const data = await getTaskPage({
          pageNo: pageNo.value,
          pageSize: pageSize.value,
          status: 'TODO',
          scope: 'ASSIGNEE'
        })
        items.value = data.list
        total.value = data.total
      }
      void refreshOverdueBadge()
    } finally {
      loading.value = false
    }
  }

  async function fetchFollowingTasks(options?: { pageNo?: number }) {
    loading.value = true
    clearLegacyTaskLocal()
    if (options?.pageNo !== undefined) {
      followingPageNo.value = options.pageNo
    }
    try {
      const data = await getFollowingTaskPage({
        pageNo: followingPageNo.value,
        pageSize: pageSize.value
      })
      followingItems.value = data.list
      followingTotal.value = data.total
      void refreshOverdueBadge()
    } finally {
      loading.value = false
    }
  }

  async function fetchActivityFeed() {
    loading.value = true
    clearLegacyTaskLocal()
    try {
      activityFeed.value = await getTaskActivityFeed(50)
      void refreshOverdueBadge()
    } finally {
      loading.value = false
    }
  }

  async function setNavView(view: TasksNavView) {
    activeListId.value = null
    navView.value = view
    pageNo.value = 1
    followingPageNo.value = 1
    if (view === 'mine' || view === 'done' || view === 'created') {
      await fetchMyTasks({ pageNo: 1 })
    } else if (view === 'following') {
      await fetchFollowingTasks({ pageNo: 1 })
    } else {
      await fetchActivityFeed()
    }
  }

  async function setListPage(nextPage: number) {
    if (activeListId.value) {
      await fetchListTasks({ pageNo: nextPage })
      return
    }
    if (navView.value === 'following') {
      await fetchFollowingTasks({ pageNo: nextPage })
      return
    }
    if (navView.value === 'mine' || navView.value === 'done' || navView.value === 'created') {
      await fetchMyTasks({ pageNo: nextPage })
    }
  }

  async function fetchDueRange(fromIso: string, toIso: string) {
    dueRangeLoading.value = true
    try {
      dueRangeItems.value = await getTaskDueRange({
        from: fromIso,
        to: toIso,
        limit: DUE_RANGE_LIMIT
      })
    } finally {
      dueRangeLoading.value = false
    }
  }

  function clearDueRange() {
    dueRangeItems.value = []
  }

  async function loadCollabDetail(taskId: string) {
    const [followers, comments, activities] = await Promise.all([
      getTaskFollowers(taskId),
      getTaskComments(taskId),
      getTaskActivities(taskId)
    ])
    selectedFollowers.value = followers
    selectedComments.value = comments
    selectedActivities.value = activities
    const uid = currentUserId()
    iAmFollowing.value = uid !== '' && followers.some(f => f.userId === uid)
  }

  function findLocalListTask(id: string): TaskItem | null {
    const fromPage = listItems.value.find(t => t.id === id)
    if (fromPage) {
      return fromPage
    }
    for (const tasks of Object.values(localTasksByList.value)) {
      const hit = tasks.find(t => t.id === id)
      if (hit) {
        return hit
      }
    }
    return null
  }

  function isLocalTaskId(id: string): boolean {
    return USE_LOCAL_TASK_LISTS && (id.startsWith('local-') || !!findLocalListTask(id))
  }

  async function selectTask(id: string | null) {
    selectedId.value = id
    if (!id) {
      selectedDetail.value = null
      selectedSubtasks.value = []
      selectedFollowers.value = []
      selectedComments.value = []
      selectedActivities.value = []
      iAmFollowing.value = false
      return
    }
    detailLoading.value = true
    try {
      if (isLocalTaskId(id)) {
        const local = findLocalListTask(id)
        if (!local) {
          throw new Error('TASK_NOT_FOUND')
        }
        selectedDetail.value = { ...local }
        selectedSubtasks.value = []
        selectedFollowers.value = []
        selectedComments.value = []
        selectedActivities.value = []
        iAmFollowing.value = false
        return
      }

      const [detail, subtasks] = await Promise.all([
        getTaskById(id),
        getTaskSubtasks(id)
      ])
      const merged = recomputeTaskProgress(detail, subtasks)
      selectedDetail.value = merged
      selectedSubtasks.value = subtasks

      const idx = items.value.findIndex(t => t.id === id)
      if (idx >= 0) {
        items.value[idx] = merged
      }
      const fIdx = followingItems.value.findIndex(t => t.id === id)
      if (fIdx >= 0) {
        followingItems.value[fIdx] = merged
      }
      const lIdx = listItems.value.findIndex(t => t.id === id)
      if (lIdx >= 0) {
        listItems.value[lIdx] = merged
      }

      await loadCollabDetail(id)
    } catch (error) {
      selectedDetail.value = null
      selectedSubtasks.value = []
      selectedFollowers.value = []
      selectedComments.value = []
      selectedActivities.value = []
      iAmFollowing.value = false
      throw error
    } finally {
      detailLoading.value = false
    }
  }

  async function fetchMyLists() {
    if (USE_LOCAL_TASK_LISTS) {
      ensureLocalSeed()
      return
    }
    myLists.value = await getMyTaskLists()
  }

  async function fetchListMembers(listId: string) {
    if (USE_LOCAL_TASK_LISTS) {
      ensureLocalSeed()
      listMembers.value = [...(localMembersByList.value[listId] ?? [])]
      return
    }
    listMembers.value = await getTaskListMembers(listId)
  }

  async function fetchListTasks(opts?: { pageNo?: number }) {
    const listId = activeListId.value
    if (!listId) {
      return
    }
    const nextPage = opts?.pageNo ?? listPageNo.value
    loading.value = true
    try {
      if (USE_LOCAL_TASK_LISTS) {
        ensureLocalSeed()
        const all = [...(localTasksByList.value[listId] ?? [])]
        listTotal.value = all.length
        listPageNo.value = nextPage
        const start = (nextPage - 1) * pageSize.value
        listItems.value = all.slice(start, start + pageSize.value)
        return
      }
      const page = await getTaskPage({
        pageNo: nextPage,
        pageSize: pageSize.value,
        listId
      })
      listItems.value = page.list
      listTotal.value = page.total
      listPageNo.value = nextPage
    } finally {
      loading.value = false
    }
  }

  async function selectList(listId: string) {
    activeListId.value = listId
    listPageNo.value = 1
    await fetchMyLists()
    if (!myLists.value.some(l => l.id === listId)) {
      activeListId.value = null
      throw new Error('TASK_LIST_FORBIDDEN')
    }
    await Promise.all([fetchListTasks({ pageNo: 1 }), fetchListMembers(listId)])
  }

  async function createList(payload: { name: string, description?: string | null }) {
    const name = payload.name.trim()
    if (!name) {
      throw new Error('TASK_LIST_NAME_EMPTY')
    }
    saving.value = true
    try {
      if (USE_LOCAL_TASK_LISTS) {
        ensureLocalSeed()
        const id = nextLocalId('local-list')
        const now = new Date().toISOString()
        const uid = currentUserId() || '1'
        const nick = currentNickname()
        const row: TaskList = {
          id,
          name,
          description: payload.description ?? null,
          ownerId: uid,
          archived: false,
          myRole: 'OWNER',
          createTime: now
        }
        myLists.value = [...myLists.value, row]
        localMembersByList.value = {
          ...localMembersByList.value,
          [id]: [{
            userId: uid,
            nickname: nick,
            avatarText: nick.slice(0, 1),
            role: 'OWNER',
            joinTime: now
          }]
        }
        localTasksByList.value = { ...localTasksByList.value, [id]: [] }
        return id
      }
      const id = await apiCreateTaskList(payload)
      await fetchMyLists()
      return id
    } finally {
      saving.value = false
    }
  }

  async function archiveList(listId: string) {
    if (!canEditListMeta(activeList.value?.myRole ?? myLists.value.find(l => l.id === listId)?.myRole)) {
      throw new Error('TASK_LIST_FORBIDDEN')
    }
    saving.value = true
    try {
      if (USE_LOCAL_TASK_LISTS) {
        myLists.value = myLists.value.filter(l => l.id !== listId)
        if (activeListId.value === listId) {
          activeListId.value = null
          listItems.value = []
          listMembers.value = []
          listTotal.value = 0
        }
        return
      }
      await apiArchiveTaskList(listId)
      await fetchMyLists()
      if (activeListId.value === listId) {
        activeListId.value = null
        listItems.value = []
        listMembers.value = []
      }
    } finally {
      saving.value = false
    }
  }

  async function inviteListMember(payload: {
    userId: string
    nickname: string
    role: Exclude<TaskListRole, 'OWNER'>
  }) {
    const listId = activeListId.value
    if (!listId || !canEditListMeta(activeListRole.value)) {
      throw new Error('TASK_LIST_FORBIDDEN')
    }
    saving.value = true
    try {
      if (USE_LOCAL_TASK_LISTS) {
        const now = new Date().toISOString()
        const members = [...(localMembersByList.value[listId] ?? [])]
        const idx = members.findIndex(m => m.userId === payload.userId)
        const row: TaskListMember = {
          userId: payload.userId,
          nickname: payload.nickname,
          avatarText: payload.nickname.slice(0, 1) || '?',
          role: payload.role,
          joinTime: now
        }
        if (idx >= 0) {
          members[idx] = { ...members[idx]!, role: payload.role }
        } else {
          members.push(row)
        }
        localMembersByList.value = { ...localMembersByList.value, [listId]: members }
        listMembers.value = [...members]
        return
      }
      await apiInviteTaskListMember({
        listId,
        userId: payload.userId,
        role: payload.role
      })
      await fetchListMembers(listId)
    } finally {
      saving.value = false
    }
  }

  async function removeListMember(userId: string) {
    const listId = activeListId.value
    if (!listId || !canEditListMeta(activeListRole.value)) {
      throw new Error('TASK_LIST_FORBIDDEN')
    }
    saving.value = true
    try {
      if (USE_LOCAL_TASK_LISTS) {
        const members = (localMembersByList.value[listId] ?? []).filter(m => m.userId !== userId)
        if (!members.some(m => m.role === 'OWNER')) {
          throw new Error('TASK_LIST_OWNER_REQUIRED')
        }
        localMembersByList.value = { ...localMembersByList.value, [listId]: members }
        listMembers.value = [...members]
        return
      }
      await apiRemoveTaskListMember({ listId, userId })
      await fetchListMembers(listId)
    } finally {
      saving.value = false
    }
  }

  async function addTask(payload: {
    title: string
    dueTime?: string | null
    startTime?: string | null
    remindBeforeMinutes?: number | null
    listId?: string | null
  }) {
    saving.value = true
    try {
      const listId = payload.listId ?? activeListId.value
      if (listId && USE_LOCAL_TASK_LISTS) {
        if (!canMutateListTasks(activeListRole.value)) {
          throw new Error('TASK_LIST_FORBIDDEN')
        }
        ensureLocalSeed()
        const id = nextLocalId('local-task')
        const now = new Date().toISOString()
        const uid = currentUserId() || '1'
        const row: TaskItem = {
          id,
          title: payload.title.trim(),
          status: 'TODO',
          listId,
          dueTime: payload.dueTime ?? null,
          startTime: payload.startTime ?? null,
          remindBeforeMinutes: payload.remindBeforeMinutes ?? null,
          description: null,
          parentId: null,
          assigneeId: uid,
          creatorId: uid,
          createTime: now,
          subtaskDoneCount: 0,
          subtaskTotal: 0
        }
        const existing = [...(localTasksByList.value[listId] ?? [])]
        localTasksByList.value = { ...localTasksByList.value, [listId]: [row, ...existing] }
        await fetchListTasks({ pageNo: 1 })
        await selectTask(id)
        return id
      }

      const id = await createTask({
        title: payload.title,
        dueTime: payload.dueTime,
        startTime: payload.startTime,
        remindBeforeMinutes: payload.remindBeforeMinutes,
        listId: listId ?? null
      })
      if (listId) {
        await fetchListTasks()
      } else {
        if (navView.value !== 'mine' && navView.value !== 'created') {
          navView.value = 'mine'
        }
        await fetchMyTasks()
      }
      await selectTask(id)
      return id
    } finally {
      saving.value = false
    }
  }

  async function setTaskDone(id: string, done: boolean) {
    const snapshotItems = items.value.map(item => ({ ...item }))
    const snapshotListItems = listItems.value.map(item => ({ ...item }))
    const snapshotDetail = selectedDetail.value ? { ...selectedDetail.value } : null
    const snapshotSubtasks = selectedSubtasks.value.map(item => ({ ...item }))

    const patchStatus = (list: TaskItem[]) => {
      const row = list.find(item => item.id === id)
      if (row) {
        row.status = done ? 'DONE' : 'TODO'
      }
    }

    patchStatus(items.value)
    patchStatus(listItems.value)
    if (selectedDetail.value?.id === id) {
      selectedDetail.value = { ...selectedDetail.value, status: done ? 'DONE' : 'TODO' }
    }
    patchStatus(selectedSubtasks.value)
    if (selectedId.value && selectedDetail.value && selectedSubtasks.value.some(c => c.id === id)) {
      selectedDetail.value = recomputeTaskProgress(selectedDetail.value, selectedSubtasks.value)
      const idx = items.value.findIndex(t => t.id === selectedId.value)
      if (idx >= 0) {
        items.value[idx] = recomputeTaskProgress(items.value[idx]!, selectedSubtasks.value)
      }
    }

    try {
      if (isLocalTaskId(id) && activeListId.value) {
        const lid = activeListId.value
        const all = [...(localTasksByList.value[lid] ?? [])]
        const row = all.find(t => t.id === id)
        if (row) {
          row.status = done ? 'DONE' : 'TODO'
        }
        localTasksByList.value = { ...localTasksByList.value, [lid]: all }
        await fetchListTasks()
        return
      }
      await toggleTaskDone(id, done)
      if (activeListId.value) {
        await fetchListTasks()
      } else if (navView.value === 'mine' || navView.value === 'done') {
        await fetchMyTasks()
      } else {
        void refreshOverdueBadge()
      }
      if (selectedId.value && !isLocalTaskId(selectedId.value)) {
        await loadCollabDetail(selectedId.value)
      }
    } catch (error) {
      items.value = snapshotItems
      listItems.value = snapshotListItems
      selectedDetail.value = snapshotDetail
      selectedSubtasks.value = snapshotSubtasks
      throw error
    }
  }

  async function removeTask(id: string) {
    saving.value = true
    try {
      if (isLocalTaskId(id) && activeListId.value) {
        const lid = activeListId.value
        localTasksByList.value = {
          ...localTasksByList.value,
          [lid]: (localTasksByList.value[lid] ?? []).filter(t => t.id !== id)
        }
        if (selectedId.value === id) {
          await selectTask(null)
        }
        await fetchListTasks()
        return
      }
      await deleteTask(id)
      if (selectedId.value === id) {
        await selectTask(null)
      } else if (selectedId.value) {
        await selectTask(selectedId.value)
      }
      if (activeListId.value) {
        await fetchListTasks()
      } else {
        await fetchMyTasks()
        if (navView.value === 'following') {
          await fetchFollowingTasks()
        } else if (navView.value === 'activity') {
          await fetchActivityFeed()
        }
      }
    } finally {
      saving.value = false
    }
  }

  async function saveDetail(payload: TaskDetailUpdatePayload) {
    saving.value = true
    try {
      await updateTask(payload)
      await fetchMyTasks()
      if (selectedId.value === payload.id) {
        await selectTask(payload.id)
      }
    } finally {
      saving.value = false
    }
  }

  async function addSubtask(parentId: string, title: string) {
    saving.value = true
    try {
      const id = await createSubtask({ parentId, title })
      await selectTask(parentId)
      await fetchMyTasks()
      return id
    } finally {
      saving.value = false
    }
  }

  async function toggleFollow(taskId: string, follow: boolean) {
    saving.value = true
    try {
      if (follow) {
        await followTask(taskId)
      } else {
        await unfollowTask(taskId)
      }
      await loadCollabDetail(taskId)
      if (navView.value === 'following') {
        await fetchFollowingTasks()
      } else if (navView.value === 'activity') {
        await fetchActivityFeed()
      }
    } finally {
      saving.value = false
    }
  }

  async function addComment(taskId: string, content: string) {
    const trimmed = content.trim()
    if (!trimmed) {
      throw new Error('TASK_COMMENT_EMPTY')
    }
    saving.value = true
    try {
      const id = await createTaskComment({ taskId, content: trimmed })
      await loadCollabDetail(taskId)
      if (navView.value === 'activity') {
        await fetchActivityFeed()
      }
      return id
    } finally {
      saving.value = false
    }
  }

  async function assignTo(taskId: string, assigneeId: string, _assigneeNickname: string) {
    saving.value = true
    try {
      await assignTask({ id: taskId, assigneeId })
      await fetchMyTasks()
      if (selectedId.value === taskId) {
        await selectTask(taskId)
      }
      if (navView.value === 'following') {
        await fetchFollowingTasks()
      } else if (navView.value === 'activity') {
        await fetchActivityFeed()
      }
    } finally {
      saving.value = false
    }
  }

  return {
    items,
    total,
    loading,
    saving,
    detailLoading,
    selectedId,
    selectedDetail,
    selectedSubtasks,
    selectedFollowers,
    selectedComments,
    selectedActivities,
    iAmFollowing,
    navView,
    pageNo,
    pageSize,
    followingItems,
    followingTotal,
    followingPageNo,
    activityFeed,
    overdueBadgeCount,
    overdueBadgeCapped,
    dueRangeItems,
    dueRangeLoading,
    todoItems,
    doneItems,
    activeListId,
    myLists,
    listMembers,
    listItems,
    listTotal,
    listPageNo,
    activeList,
    activeListRole,
    listCanEditMeta,
    listCanMutateTasks,
    resetForTenantSwitch,
    fetchMyTasks,
    fetchFollowingTasks,
    fetchActivityFeed,
    setNavView,
    setListPage,
    refreshOverdueBadge,
    fetchDueRange,
    clearDueRange,
    selectTask,
    addTask,
    setTaskDone,
    removeTask,
    saveDetail,
    addSubtask,
    toggleFollow,
    addComment,
    assignTo,
    fetchMyLists,
    selectList,
    createList,
    archiveList,
    fetchListMembers,
    inviteListMember,
    removeListMember,
    fetchListTasks
  }
})

export { isOverdueTask }
