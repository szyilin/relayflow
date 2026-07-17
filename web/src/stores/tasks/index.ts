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
import { DEFAULT_LIST_PAGE_SIZE } from '../../constants/pagination'
import { useAuthStore } from '../auth'
import {
  clearLegacyTaskLocal,
  isOverdueTask,
  recomputeTaskProgress,
  type TasksNavView
} from './helpers'

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

  const todoItems = computed(() => items.value.filter(item => item.status === 'TODO'))
  const doneItems = computed(() => items.value.filter(item => item.status === 'DONE'))

  function currentUserId(): string {
    const auth = useAuthStore()
    return auth.userId ? String(auth.userId) : ''
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

  async function addTask(payload: {
    title: string
    dueTime?: string | null
    startTime?: string | null
    remindBeforeMinutes?: number | null
  }) {
    saving.value = true
    try {
      const id = await createTask(payload)
      if (navView.value !== 'mine' && navView.value !== 'created') {
        navView.value = 'mine'
      }
      await fetchMyTasks()
      await selectTask(id)
      return id
    } finally {
      saving.value = false
    }
  }

  async function setTaskDone(id: string, done: boolean) {
    const snapshotItems = items.value.map(item => ({ ...item }))
    const snapshotDetail = selectedDetail.value ? { ...selectedDetail.value } : null
    const snapshotSubtasks = selectedSubtasks.value.map(item => ({ ...item }))

    const patchStatus = (list: TaskItem[]) => {
      const row = list.find(item => item.id === id)
      if (row) {
        row.status = done ? 'DONE' : 'TODO'
      }
    }

    patchStatus(items.value)
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
      await toggleTaskDone(id, done)
      // mine/done 列表按状态过滤，完成后从当前列表移除
      if (navView.value === 'mine' || navView.value === 'done') {
        await fetchMyTasks()
      } else {
        void refreshOverdueBadge()
      }
      if (selectedId.value) {
        await loadCollabDetail(selectedId.value)
      }
    } catch (error) {
      items.value = snapshotItems
      selectedDetail.value = snapshotDetail
      selectedSubtasks.value = snapshotSubtasks
      throw error
    }
  }

  async function removeTask(id: string) {
    saving.value = true
    try {
      await deleteTask(id)
      if (selectedId.value === id) {
        await selectTask(null)
      } else if (selectedId.value) {
        await selectTask(selectedId.value)
      }
      await fetchMyTasks()
      if (navView.value === 'following') {
        await fetchFollowingTasks()
      } else if (navView.value === 'activity') {
        await fetchActivityFeed()
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
    assignTo
  }
})

export { isOverdueTask }
