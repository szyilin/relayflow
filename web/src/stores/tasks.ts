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
} from '../api/app/task'
import { useAuthStore } from './auth'

const DUE_RANGE_LIMIT = 200
const LEGACY_DETAIL_LOCAL_KEY = 'relayflow-task-detail-local-v1'
const LEGACY_COLLAB_LOCAL_KEY = 'relayflow-task-collab-local-v1'

function recomputeProgress(parent: TaskItem, children: TaskItem[]): TaskItem {
  const total = children.length
  const done = children.filter(c => c.status === 'DONE').length
  return {
    ...parent,
    subtaskTotal: total,
    subtaskDoneCount: done
  }
}

function clearLegacyLocal() {
  try {
    localStorage.removeItem(LEGACY_DETAIL_LOCAL_KEY)
    localStorage.removeItem(LEGACY_COLLAB_LOCAL_KEY)
  } catch {
    // ignore
  }
}

export type TasksNavView = 'mine' | 'following' | 'activity'

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
  const followingItems = ref<TaskItem[]>([])
  const followingTotal = ref(0)
  const activityFeed = ref<TaskActivity[]>([])

  const dueRangeItems = ref<TaskItem[]>([])
  const dueRangeLoading = ref(false)

  const todoItems = computed(() => items.value.filter(item => item.status === 'TODO'))
  const doneItems = computed(() => items.value.filter(item => item.status === 'DONE'))

  function currentUserId(): string {
    const auth = useAuthStore()
    return auth.userId ? String(auth.userId) : ''
  }

  async function fetchMyTasks() {
    loading.value = true
    clearLegacyLocal()
    try {
      const data = await getTaskPage({ pageNo: 1, pageSize: 100 })
      items.value = data.list
      total.value = data.total
    } finally {
      loading.value = false
    }
  }

  async function fetchFollowingTasks() {
    loading.value = true
    clearLegacyLocal()
    try {
      const data = await getFollowingTaskPage({ pageNo: 1, pageSize: 100 })
      followingItems.value = data.list
      followingTotal.value = data.total
    } finally {
      loading.value = false
    }
  }

  async function fetchActivityFeed() {
    loading.value = true
    clearLegacyLocal()
    try {
      activityFeed.value = await getTaskActivityFeed(50)
    } finally {
      loading.value = false
    }
  }

  async function setNavView(view: TasksNavView) {
    navView.value = view
    if (view === 'mine') {
      await fetchMyTasks()
    } else if (view === 'following') {
      await fetchFollowingTasks()
    } else {
      await fetchActivityFeed()
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
      const merged = recomputeProgress(detail, subtasks)
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

  async function addTask(payload: { title: string, dueTime?: string | null, startTime?: string | null }) {
    saving.value = true
    try {
      const id = await createTask(payload)
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
      selectedDetail.value = recomputeProgress(selectedDetail.value, selectedSubtasks.value)
      const idx = items.value.findIndex(t => t.id === selectedId.value)
      if (idx >= 0) {
        items.value[idx] = recomputeProgress(items.value[idx]!, selectedSubtasks.value)
      }
    }

    try {
      await toggleTaskDone(id, done)
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
    followingItems,
    followingTotal,
    activityFeed,
    dueRangeItems,
    dueRangeLoading,
    todoItems,
    doneItems,
    fetchMyTasks,
    fetchFollowingTasks,
    fetchActivityFeed,
    setNavView,
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
