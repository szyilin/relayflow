import { del, get, post, put } from '../request'

export type TaskItemStatus = 'TODO' | 'DONE'

export interface TaskItem {
  id: string
  title: string
  status: TaskItemStatus
  startTime?: string | null
  dueTime?: string | null
  remindBeforeMinutes?: number | null
  description?: string | null
  parentId?: string | null
  assigneeId?: string | null
  creatorId?: string | null
  createTime: string
  subtaskDoneCount?: number
  subtaskTotal?: number
}

export interface TaskPageResult {
  list: TaskItem[]
  total: number
}

export type TaskDetailUpdatePayload = {
  id: string
  title?: string
  startTime?: string | null
  dueTime?: string | null
  remindBeforeMinutes?: number | null
  description?: string | null
}

function normalizeTaskItem(
  item: TaskItem & { id?: string | number, parentId?: string | number | null, assigneeId?: string | number | null, creatorId?: string | number | null }
): TaskItem {
  return {
    id: String(item.id),
    title: item.title,
    status: item.status as TaskItemStatus,
    startTime: item.startTime ?? null,
    dueTime: item.dueTime ?? null,
    remindBeforeMinutes: item.remindBeforeMinutes ?? null,
    description: item.description ?? null,
    parentId: item.parentId != null && item.parentId !== '' ? String(item.parentId) : null,
    assigneeId: item.assigneeId != null ? String(item.assigneeId) : null,
    creatorId: item.creatorId != null ? String(item.creatorId) : null,
    createTime: item.createTime,
    subtaskDoneCount: item.subtaskDoneCount ?? 0,
    subtaskTotal: item.subtaskTotal ?? 0
  }
}

export async function getTaskPage(params?: {
  pageNo?: number
  pageSize?: number
  status?: TaskItemStatus
}): Promise<TaskPageResult> {
  const data = await get<TaskPageResult>('/app-api/task/item/page', { params })
  return {
    list: (data.list ?? []).map(item => normalizeTaskItem(item as TaskItem & { id?: string | number })),
    total: data.total ?? 0
  }
}

export async function getTaskById(id: string): Promise<TaskItem> {
  const data = await get<TaskItem & { id?: string | number }>('/app-api/task/item/get', { params: { id } })
  return normalizeTaskItem(data)
}

export async function createTask(payload: {
  title: string
  dueTime?: string | null
  startTime?: string | null
}): Promise<string> {
  const id = await post<number | string>('/app-api/task/item/create', payload)
  return String(id)
}

export async function toggleTaskDone(id: string, done: boolean): Promise<boolean> {
  return put<boolean>('/app-api/task/item/toggle-done', { id, done })
}

export async function updateTask(payload: TaskDetailUpdatePayload): Promise<boolean> {
  return put<boolean>('/app-api/task/item/update', payload)
}

export async function deleteTask(id: string): Promise<boolean> {
  return del<boolean>('/app-api/task/item/delete', { params: { id } })
}

export async function getTaskSubtasks(parentId: string): Promise<TaskItem[]> {
  const data = await get<Array<TaskItem & { id?: string | number }>>(
    '/app-api/task/item/subtasks',
    { params: { parentId } }
  )
  return (data ?? []).map(item => normalizeTaskItem(item))
}

export async function createSubtask(payload: { parentId: string, title: string }): Promise<string> {
  const id = await post<number | string>('/app-api/task/item/subtask/create', payload)
  return String(id)
}

/** Calendar projection: TODO tasks with dueTime in [from, to). */
export async function getTaskDueRange(params: {
  from: string
  to: string
  limit?: number
}): Promise<TaskItem[]> {
  const data = await get<Array<TaskItem & { id?: string | number }>>(
    '/app-api/task/item/due-range',
    { params }
  )
  return (data ?? []).map(item => normalizeTaskItem(item))
}

// --- collab (P1) ---

export type TaskActivityType =
  | 'created'
  | 'field_changed'
  | 'subtask_created'
  | 'subtask_done'
  | 'follower_added'
  | 'follower_removed'
  | 'commented'
  | 'assigned'

export interface TaskFollower {
  userId: string
  nickname: string
  avatarText: string
  followTime: string
}

export interface TaskComment {
  id: string
  taskId: string
  authorId: string
  authorNickname: string
  content: string
  createTime: string
}

export interface TaskActivity {
  id: string
  taskId: string
  taskTitle: string
  actorId: string
  actorNickname: string
  type: TaskActivityType
  summary: string
  createTime: string
}

function normalizeFollower(row: TaskFollower & { userId?: string | number }): TaskFollower {
  return {
    userId: String(row.userId),
    nickname: row.nickname,
    avatarText: row.avatarText || (row.nickname?.slice(0, 1) ?? '?'),
    followTime: row.followTime
  }
}

function normalizeComment(row: TaskComment & { id?: string | number, taskId?: string | number, authorId?: string | number }): TaskComment {
  return {
    id: String(row.id),
    taskId: String(row.taskId),
    authorId: String(row.authorId),
    authorNickname: row.authorNickname,
    content: row.content,
    createTime: row.createTime
  }
}

function normalizeActivity(row: TaskActivity & { id?: string | number, taskId?: string | number, actorId?: string | number }): TaskActivity {
  return {
    id: String(row.id),
    taskId: String(row.taskId),
    taskTitle: row.taskTitle,
    actorId: String(row.actorId),
    actorNickname: row.actorNickname,
    type: row.type,
    summary: row.summary,
    createTime: row.createTime
  }
}

export async function followTask(taskId: string): Promise<boolean> {
  return post<boolean>('/app-api/task/item/follow', { taskId })
}

export async function unfollowTask(taskId: string): Promise<boolean> {
  return post<boolean>('/app-api/task/item/unfollow', { taskId })
}

export async function getTaskFollowers(taskId: string): Promise<TaskFollower[]> {
  const data = await get<Array<TaskFollower & { userId?: string | number }>>(
    '/app-api/task/item/followers',
    { params: { taskId } }
  )
  return (data ?? []).map(normalizeFollower)
}

export async function getFollowingTaskPage(params?: {
  pageNo?: number
  pageSize?: number
}): Promise<TaskPageResult> {
  const data = await get<TaskPageResult>('/app-api/task/item/following/page', { params })
  return {
    list: (data.list ?? []).map(item => normalizeTaskItem(item as TaskItem & { id?: string | number })),
    total: data.total ?? 0
  }
}

export async function assignTask(payload: { id: string, assigneeId: string }): Promise<boolean> {
  return put<boolean>('/app-api/task/item/assign', payload)
}

export async function getTaskComments(taskId: string): Promise<TaskComment[]> {
  const data = await get<Array<TaskComment & { id?: string | number }>>(
    '/app-api/task/item/comments',
    { params: { taskId } }
  )
  return (data ?? []).map(normalizeComment)
}

export async function createTaskComment(payload: { taskId: string, content: string }): Promise<string> {
  const id = await post<number | string>('/app-api/task/item/comment/create', payload)
  return String(id)
}

export async function getTaskActivities(taskId: string): Promise<TaskActivity[]> {
  const data = await get<Array<TaskActivity & { id?: string | number }>>(
    '/app-api/task/item/activities',
    { params: { taskId } }
  )
  return (data ?? []).map(normalizeActivity)
}

export async function getTaskActivityFeed(limit = 50): Promise<TaskActivity[]> {
  const data = await get<Array<TaskActivity & { id?: string | number }>>(
    '/app-api/task/activity/feed',
    { params: { limit } }
  )
  return (data ?? []).map(normalizeActivity)
}
