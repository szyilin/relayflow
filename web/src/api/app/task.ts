import { del, get, post, put } from '../request'

export type TaskItemStatus = 'TODO' | 'DONE'

export interface TaskItem {
  id: string
  title: string
  status: TaskItemStatus
  dueTime?: string | null
  createTime: string
}

export interface TaskPageResult {
  list: TaskItem[]
  total: number
}

function normalizeTaskItem(
  item: TaskItem & { id?: string | number, status?: string }
): TaskItem {
  return {
    id: String(item.id),
    title: item.title,
    status: item.status as TaskItemStatus,
    dueTime: item.dueTime ?? null,
    createTime: item.createTime
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

export async function createTask(payload: { title: string, dueTime?: string | null }): Promise<string> {
  const id = await post<number | string>('/app-api/task/item/create', payload)
  return String(id)
}

export async function toggleTaskDone(id: string, done: boolean): Promise<boolean> {
  return put<boolean>('/app-api/task/item/toggle-done', { id, done })
}

export async function updateTask(payload: {
  id: string
  title?: string
  dueTime?: string | null
}): Promise<boolean> {
  return put<boolean>('/app-api/task/item/update', payload)
}

export async function deleteTask(id: string): Promise<boolean> {
  return del<boolean>('/app-api/task/item/delete', { params: { id } })
}
