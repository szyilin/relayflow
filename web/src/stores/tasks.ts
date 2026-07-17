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
/** -web interim collab overlay until collab-api. Integrate: remove. */
const COLLAB_LOCAL_KEY = 'relayflow-task-collab-local-v1'

interface CollabLocalBundle {
  followedTaskIds: string[]
  followingSnapshots: Record<string, TaskItem>
  followers: Record<string, TaskFollower[]>
  comments: Record<string, TaskComment[]>
  activities: Record<string, TaskActivity[]>
  feed: TaskActivity[]
  assigneeOverrides: Record<string, string>
}

function emptyCollab(): CollabLocalBundle {
  return {
    followedTaskIds: [],
    followingSnapshots: {},
    followers: {},
    comments: {},
    activities: {},
    feed: [],
    assigneeOverrides: {}
  }
}

function readCollabLocal(): CollabLocalBundle {
  try {
    const raw = localStorage.getItem(COLLAB_LOCAL_KEY)
    if (!raw) {
      return emptyCollab()
    }
    const parsed = JSON.parse(raw) as Partial<CollabLocalBundle>
    return {
      ...emptyCollab(),
      ...parsed,
      followedTaskIds: parsed.followedTaskIds ?? [],
      followingSnapshots: parsed.followingSnapshots ?? {},
      followers: parsed.followers ?? {},
      comments: parsed.comments ?? {},
      activities: parsed.activities ?? {},
      feed: parsed.feed ?? [],
      assigneeOverrides: parsed.assigneeOverrides ?? {}
    }
  } catch {
    return emptyCollab()
  }
}

function writeCollabLocal(bundle: CollabLocalBundle) {
  localStorage.setItem(COLLAB_LOCAL_KEY, JSON.stringify(bundle))
}

function recomputeProgress(parent: TaskItem, children: TaskItem[]): TaskItem {
  const total = children.length
  const done = children.filter(c => c.status === 'DONE').length
  return {
    ...parent,
    subtaskTotal: total,
    subtaskDoneCount: done
  }
}

function clearLegacyDetailLocal() {
  try {
    localStorage.removeItem(LEGACY_DETAIL_LOCAL_KEY)
  } catch {
    // ignore
  }
}

function avatarText(nickname: string): string {
  return nickname.trim().slice(0, 1) || '?'
}

function newLocalId(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`
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

  const collabLocal = ref<CollabLocalBundle>(emptyCollab())

  const todoItems = computed(() => items.value.filter(item => item.status === 'TODO'))
  const doneItems = computed(() => items.value.filter(item => item.status === 'DONE'))

  function hydrateCollab() {
    collabLocal.value = readCollabLocal()
  }

  function persistCollab() {
    writeCollabLocal(collabLocal.value)
  }

  function currentActor(): { userId: string, nickname: string } {
    const auth = useAuthStore()
    const userId = auth.userId ? String(auth.userId) : '0'
    const nickname = auth.user?.nickname || auth.user?.username || '我'
    return { userId, nickname }
  }

  function pushActivity(entry: Omit<TaskActivity, 'id' | 'createTime'> & { id?: string, createTime?: string }) {
    const activity: TaskActivity = {
      id: entry.id ?? newLocalId('act'),
      taskId: entry.taskId,
      taskTitle: entry.taskTitle,
      actorId: entry.actorId,
      actorNickname: entry.actorNickname,
      type: entry.type,
      summary: entry.summary,
      createTime: entry.createTime ?? new Date().toISOString()
    }
    const list = collabLocal.value.activities[entry.taskId] ?? []
    collabLocal.value = {
      ...collabLocal.value,
      activities: {
        ...collabLocal.value.activities,
        [entry.taskId]: [activity, ...list]
      },
      feed: [activity, ...collabLocal.value.feed].slice(0, 100)
    }
    persistCollab()
    if (selectedId.value === entry.taskId) {
      selectedActivities.value = collabLocal.value.activities[entry.taskId] ?? []
    }
    if (navView.value === 'activity') {
      activityFeed.value = [...collabLocal.value.feed]
    }
    return activity
  }

  function applyAssigneeOverride(task: TaskItem): TaskItem {
    const override = collabLocal.value.assigneeOverrides[task.id]
    if (!override) {
      return task
    }
    return { ...task, assigneeId: override }
  }

  async function fetchMyTasks() {
    loading.value = true
    clearLegacyDetailLocal()
    hydrateCollab()
    try {
      const data = await getTaskPage({ pageNo: 1, pageSize: 100 })
      items.value = data.list.map(applyAssigneeOverride)
      total.value = data.total
    } finally {
      loading.value = false
    }
  }

  async function fetchFollowingTasks() {
    loading.value = true
    hydrateCollab()
    try {
      try {
        const data = await getFollowingTaskPage({ pageNo: 1, pageSize: 100 })
        followingItems.value = data.list.map(applyAssigneeOverride)
        followingTotal.value = data.total
        return
      } catch {
        // -web: local following list
      }
      const ids = collabLocal.value.followedTaskIds
      const list: TaskItem[] = []
      for (const id of ids) {
        const snap = collabLocal.value.followingSnapshots[id]
        if (snap) {
          list.push(applyAssigneeOverride(snap))
          continue
        }
        try {
          list.push(applyAssigneeOverride(await getTaskById(id)))
        } catch {
          // skip missing
        }
      }
      followingItems.value = list
      followingTotal.value = list.length
    } finally {
      loading.value = false
    }
  }

  async function fetchActivityFeed() {
    loading.value = true
    hydrateCollab()
    try {
      try {
        activityFeed.value = await getTaskActivityFeed(50)
        return
      } catch {
        activityFeed.value = [...collabLocal.value.feed]
      }
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
    const actor = currentActor()
    hydrateCollab()

    let followers: TaskFollower[] = []
    try {
      followers = await getTaskFollowers(taskId)
    } catch {
      followers = [...(collabLocal.value.followers[taskId] ?? [])]
    }

    let comments: TaskComment[] = []
    try {
      comments = await getTaskComments(taskId)
    } catch {
      comments = [...(collabLocal.value.comments[taskId] ?? [])]
    }

    let activities: TaskActivity[] = []
    try {
      activities = await getTaskActivities(taskId)
    } catch {
      activities = [...(collabLocal.value.activities[taskId] ?? [])]
    }

    selectedFollowers.value = followers
    selectedComments.value = comments
    selectedActivities.value = activities
    iAmFollowing.value = followers.some(f => f.userId === actor.userId)
      || collabLocal.value.followedTaskIds.includes(taskId)
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
      const withAssignee = applyAssigneeOverride(recomputeProgress(detail, subtasks))
      selectedDetail.value = withAssignee
      selectedSubtasks.value = subtasks

      const idx = items.value.findIndex(t => t.id === id)
      if (idx >= 0) {
        items.value[idx] = withAssignee
      }
      const fIdx = followingItems.value.findIndex(t => t.id === id)
      if (fIdx >= 0) {
        followingItems.value[fIdx] = withAssignee
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
      const actor = currentActor()
      pushActivity({
        taskId: id,
        taskTitle: payload.title.trim(),
        actorId: actor.userId,
        actorNickname: actor.nickname,
        type: 'created',
        summary: '创建了任务'
      })
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
      if (done) {
        const actor = currentActor()
        pushActivity({
          taskId: selectedId.value,
          taskTitle: selectedDetail.value.title,
          actorId: actor.userId,
          actorNickname: actor.nickname,
          type: 'subtask_done',
          summary: '完成了子任务'
        })
      }
    }

    try {
      await toggleTaskDone(id, done)
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
      if (collabLocal.value.followedTaskIds.includes(id)) {
        const followingSnapshots = { ...collabLocal.value.followingSnapshots }
        const followers = { ...collabLocal.value.followers }
        const comments = { ...collabLocal.value.comments }
        const activities = { ...collabLocal.value.activities }
        const assigneeOverrides = { ...collabLocal.value.assigneeOverrides }
        delete followingSnapshots[id]
        delete followers[id]
        delete comments[id]
        delete activities[id]
        delete assigneeOverrides[id]
        collabLocal.value = {
          ...collabLocal.value,
          followedTaskIds: collabLocal.value.followedTaskIds.filter(x => x !== id),
          followingSnapshots,
          followers,
          comments,
          activities,
          assigneeOverrides,
          feed: collabLocal.value.feed.filter(a => a.taskId !== id)
        }
        persistCollab()
      }
      if (selectedId.value === id) {
        await selectTask(null)
      } else if (selectedId.value) {
        await selectTask(selectedId.value)
      }
      await fetchMyTasks()
      if (navView.value === 'following') {
        await fetchFollowingTasks()
      }
    } finally {
      saving.value = false
    }
  }

  async function saveDetail(payload: TaskDetailUpdatePayload) {
    saving.value = true
    try {
      await updateTask(payload)
      const actor = currentActor()
      const title = payload.title || selectedDetail.value?.title || '任务'
      pushActivity({
        taskId: payload.id,
        taskTitle: title,
        actorId: actor.userId,
        actorNickname: actor.nickname,
        type: 'field_changed',
        summary: '更新了任务详情'
      })
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
      const actor = currentActor()
      pushActivity({
        taskId: parentId,
        taskTitle: selectedDetail.value?.title || '任务',
        actorId: actor.userId,
        actorNickname: actor.nickname,
        type: 'subtask_created',
        summary: `添加了子任务「${title.trim()}」`
      })
      await selectTask(parentId)
      await fetchMyTasks()
      return id
    } finally {
      saving.value = false
    }
  }

  async function toggleFollow(taskId: string, follow: boolean) {
    saving.value = true
    const actor = currentActor()
    const task = selectedDetail.value?.id === taskId
      ? selectedDetail.value
      : items.value.find(t => t.id === taskId) || followingItems.value.find(t => t.id === taskId)
    try {
      try {
        if (follow) {
          await followTask(taskId)
        } else {
          await unfollowTask(taskId)
        }
      } catch {
        // -web local
      }

      let followers = [...(collabLocal.value.followers[taskId] ?? selectedFollowers.value)]
      if (follow) {
        if (!followers.some(f => f.userId === actor.userId)) {
          followers = [{
            userId: actor.userId,
            nickname: actor.nickname,
            avatarText: avatarText(actor.nickname),
            followTime: new Date().toISOString()
          }, ...followers]
        }
        const snap = task ?? await getTaskById(taskId).catch(() => null)
        collabLocal.value = {
          ...collabLocal.value,
          followedTaskIds: [...new Set([...collabLocal.value.followedTaskIds, taskId])],
          followingSnapshots: snap
            ? { ...collabLocal.value.followingSnapshots, [taskId]: snap }
            : collabLocal.value.followingSnapshots,
          followers: { ...collabLocal.value.followers, [taskId]: followers }
        }
        pushActivity({
          taskId,
          taskTitle: task?.title || snap?.title || '任务',
          actorId: actor.userId,
          actorNickname: actor.nickname,
          type: 'follower_added',
          summary: '关注了任务'
        })
      } else {
        followers = followers.filter(f => f.userId !== actor.userId)
        const followingSnapshots = { ...collabLocal.value.followingSnapshots }
        delete followingSnapshots[taskId]
        collabLocal.value = {
          ...collabLocal.value,
          followedTaskIds: collabLocal.value.followedTaskIds.filter(id => id !== taskId),
          followingSnapshots,
          followers: { ...collabLocal.value.followers, [taskId]: followers }
        }
        pushActivity({
          taskId,
          taskTitle: task?.title || '任务',
          actorId: actor.userId,
          actorNickname: actor.nickname,
          type: 'follower_removed',
          summary: '取消关注'
        })
      }
      persistCollab()
      selectedFollowers.value = followers
      iAmFollowing.value = follow
      if (navView.value === 'following') {
        await fetchFollowingTasks()
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
    const actor = currentActor()
    try {
      let id: string
      try {
        id = await createTaskComment({ taskId, content: trimmed })
      } catch {
        id = newLocalId('cmt')
      }
      const comment: TaskComment = {
        id,
        taskId,
        authorId: actor.userId,
        authorNickname: actor.nickname,
        content: trimmed,
        createTime: new Date().toISOString()
      }
      const prev = collabLocal.value.comments[taskId] ?? selectedComments.value
      collabLocal.value = {
        ...collabLocal.value,
        comments: {
          ...collabLocal.value.comments,
          [taskId]: [...prev, comment]
        }
      }
      persistCollab()
      selectedComments.value = [...(collabLocal.value.comments[taskId] ?? [])]
      pushActivity({
        taskId,
        taskTitle: selectedDetail.value?.title || '任务',
        actorId: actor.userId,
        actorNickname: actor.nickname,
        type: 'commented',
        summary: '添加了评论'
      })
      return id
    } finally {
      saving.value = false
    }
  }

  async function assignTo(taskId: string, assigneeId: string, assigneeNickname: string) {
    saving.value = true
    const actor = currentActor()
    try {
      try {
        await assignTask({ id: taskId, assigneeId })
      } catch {
        // -web: local override until assign API
      }
      collabLocal.value = {
        ...collabLocal.value,
        assigneeOverrides: {
          ...collabLocal.value.assigneeOverrides,
          [taskId]: assigneeId
        }
      }
      persistCollab()
      if (selectedDetail.value?.id === taskId) {
        selectedDetail.value = { ...selectedDetail.value, assigneeId }
      }
      const idx = items.value.findIndex(t => t.id === taskId)
      if (idx >= 0) {
        items.value[idx] = { ...items.value[idx]!, assigneeId }
      }
      pushActivity({
        taskId,
        taskTitle: selectedDetail.value?.title || '任务',
        actorId: actor.userId,
        actorNickname: actor.nickname,
        type: 'assigned',
        summary: `指派给 ${assigneeNickname}`
      })
      await fetchMyTasks()
      if (selectedId.value === taskId) {
        await selectTask(taskId)
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
    hydrateCollab,
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
