import { get, post, put } from '../request'

export type TaskListRole = 'OWNER' | 'EDITOR' | 'VIEWER'

export interface TaskList {
  id: string
  name: string
  description?: string | null
  ownerId: string
  archived: boolean
  myRole: TaskListRole
  createTime: string
}

export interface TaskListMember {
  userId: string
  nickname: string
  avatarText: string
  role: TaskListRole
  joinTime: string
}

function normalizeList(row: TaskList & { id?: string | number, ownerId?: string | number }): TaskList {
  return {
    id: String(row.id),
    name: row.name,
    description: row.description ?? null,
    ownerId: String(row.ownerId),
    archived: !!row.archived,
    myRole: row.myRole,
    createTime: row.createTime
  }
}

function normalizeMember(row: TaskListMember & { userId?: string | number }): TaskListMember {
  return {
    userId: String(row.userId),
    nickname: row.nickname,
    avatarText: row.avatarText || (row.nickname?.slice(0, 1) ?? '?'),
    role: row.role,
    joinTime: row.joinTime
  }
}

export async function getMyTaskLists(): Promise<TaskList[]> {
  const data = await get<Array<TaskList & { id?: string | number }>>('/app-api/task/list/mine')
  return (data ?? []).map(normalizeList)
}

export async function getTaskList(id: string): Promise<TaskList> {
  const data = await get<TaskList & { id?: string | number }>('/app-api/task/list/get', { params: { id } })
  return normalizeList(data)
}

export async function createTaskList(payload: {
  name: string
  description?: string | null
}): Promise<string> {
  const id = await post<number | string>('/app-api/task/list/create', payload)
  return String(id)
}

export async function updateTaskList(payload: {
  id: string
  name?: string
  description?: string | null
}): Promise<boolean> {
  return put<boolean>('/app-api/task/list/update', payload)
}

export async function archiveTaskList(id: string): Promise<boolean> {
  return post<boolean>('/app-api/task/list/archive', { id })
}

export async function getTaskListMembers(listId: string): Promise<TaskListMember[]> {
  const data = await get<Array<TaskListMember & { userId?: string | number }>>(
    '/app-api/task/list/member/list',
    { params: { listId } }
  )
  return (data ?? []).map(normalizeMember)
}

export async function inviteTaskListMember(payload: {
  listId: string
  userId: string
  role: Exclude<TaskListRole, 'OWNER'>
}): Promise<boolean> {
  return post<boolean>('/app-api/task/list/member/invite', payload)
}

export async function updateTaskListMemberRole(payload: {
  listId: string
  userId: string
  role: TaskListRole
}): Promise<boolean> {
  return put<boolean>('/app-api/task/list/member/update-role', payload)
}

export async function removeTaskListMember(payload: {
  listId: string
  userId: string
}): Promise<boolean> {
  return post<boolean>('/app-api/task/list/member/remove', payload)
}
