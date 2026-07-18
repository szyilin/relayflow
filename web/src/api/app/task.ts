import { del, get, post, put } from '../request'

export type TaskItemStatus = 'TODO' | 'IN_PROGRESS' | 'DONE'

export interface TaskItem {
  id: string
  title: string
  status: TaskItemStatus
  startTime?: string | null
  dueTime?: string | null
  remindBeforeMinutes?: number | null
  description?: string | null
  parentId?: string | null
  listId?: string | null
  /** Multi-list memberships; empty = 不属于任何清单. */
  listIds?: string[]
  /** Primary assignee projection (first of assigneeIds); compat with group-by / legacy. */
  assigneeId?: string | null
  /** Multi-assignee set; empty = 无负责人. */
  assigneeIds?: string[]
  creatorId?: string | null
  /** 分配人；quick-views / assigner 切片目标字段 */
  assignerId?: string | null
  createTime: string
  boardRank?: number | null
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
  item: TaskItem & {
    id?: string | number
    parentId?: string | number | null
    listId?: string | number | null
    listIds?: Array<string | number> | null
    assigneeId?: string | number | null
    assigneeIds?: Array<string | number> | null
    creatorId?: string | number | null
    assignerId?: string | number | null
  }
): TaskItem {
  const fromListIds = (item.listIds ?? [])
    .map(id => (id != null && id !== '' ? String(id) : ''))
    .filter(Boolean)
  const primaryList = item.listId != null && item.listId !== '' ? String(item.listId) : null
  const listIds = fromListIds.length > 0
    ? Array.from(new Set(fromListIds))
    : primaryList
      ? [primaryList]
      : []
  const fromList = (item.assigneeIds ?? [])
    .map(id => (id != null && id !== '' ? String(id) : ''))
    .filter(Boolean)
  const primary = item.assigneeId != null && item.assigneeId !== '' ? String(item.assigneeId) : null
  const assigneeIds = fromList.length > 0
    ? Array.from(new Set(fromList))
    : primary
      ? [primary]
      : []
  return {
    id: String(item.id),
    title: item.title,
    status: item.status as TaskItemStatus,
    startTime: item.startTime ?? null,
    dueTime: item.dueTime ?? null,
    remindBeforeMinutes: item.remindBeforeMinutes ?? null,
    description: item.description ?? null,
    parentId: item.parentId != null && item.parentId !== '' ? String(item.parentId) : null,
    listId: listIds[0] ?? null,
    listIds,
    assigneeId: assigneeIds[0] ?? null,
    assigneeIds,
    creatorId: item.creatorId != null ? String(item.creatorId) : null,
    assignerId: item.assignerId != null ? String(item.assignerId) : null,
    createTime: item.createTime,
    boardRank: item.boardRank ?? null,
    subtaskDoneCount: item.subtaskDoneCount ?? 0,
    subtaskTotal: item.subtaskTotal ?? 0
  }
}

/** ALL / ASSIGNED_BY_ME：契约草案；-api 就绪前 store 走临时逻辑 */
export type TaskPageScope = 'ASSIGNEE' | 'CREATOR' | 'ALL' | 'ASSIGNED_BY_ME'

export async function getTaskPage(params?: {
  pageNo?: number
  pageSize?: number
  status?: TaskItemStatus
  scope?: TaskPageScope
  listId?: string
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
  remindBeforeMinutes?: number | null
  listId?: string | null
}): Promise<string> {
  const id = await post<number | string>('/app-api/task/item/create', payload)
  return String(id)
}

export async function toggleTaskDone(id: string, done: boolean): Promise<boolean> {
  return put<boolean>('/app-api/task/item/toggle-done', { id, done })
}

/** Board drag: set status + optional boardRank (list root tasks only; prefer groupMoveTask). */
export async function boardMoveTask(payload: {
  id: string
  status: TaskItemStatus
  boardRank?: number | null
}): Promise<boolean> {
  return put<boolean>('/app-api/task/item/board-move', payload)
}

/** Field group-by drag: persist status / dueTime / assigneeId bucket. */
export async function groupMoveTask(payload: {
  id: string
  fieldKey: 'status' | 'dueTime' | 'assigneeId'
  value: string | null
  beforeId?: string | null
}): Promise<boolean> {
  return put<boolean>('/app-api/task/item/group-move', payload)
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

/** Full replace of assignee set (multi-assignee). */
export async function replaceTaskAssignees(payload: {
  id: string
  assigneeIds: string[]
}): Promise<boolean> {
  return put<boolean>('/app-api/task/item/assignees', {
    id: payload.id,
    assigneeIds: payload.assigneeIds
  })
}

/** Full replace of list memberships (multi-list). */
export async function replaceTaskListMemberships(payload: {
  id: string
  listIds: string[]
}): Promise<boolean> {
  return put<boolean>('/app-api/task/item/list-memberships', {
    id: payload.id,
    listIds: payload.listIds
  })
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
