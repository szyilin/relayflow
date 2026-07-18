import { del, get, post, put } from '../request'

export interface ListGroup {
  id: string
  listId: string
  name: string
  rank: number
  isDefault: boolean
}

export interface ListGroupMembership {
  taskId: string
  groupId: string
  rank: number
}

export interface ListGroupListResult {
  groups: ListGroup[]
  memberships: ListGroupMembership[]
}

function strId(v: string | number | null | undefined): string {
  return v == null || v === '' ? '' : String(v)
}

export async function listListGroups(listId: string): Promise<ListGroupListResult> {
  const data = await get<{
    groups?: Array<{
      id?: string | number
      listId?: string | number
      name?: string
      rank?: number
      isDefault?: boolean
    }>
    memberships?: Array<{
      taskId?: string | number
      groupId?: string | number
      rank?: number
    }>
  }>('/app-api/task/list-group/list', { params: { listId } })

  return {
    groups: (data.groups ?? []).map(g => ({
      id: strId(g.id),
      listId: strId(g.listId) || listId,
      name: g.name ?? '',
      rank: g.rank ?? 0,
      isDefault: !!g.isDefault
    })).filter(g => g.id),
    memberships: (data.memberships ?? []).map(m => ({
      taskId: strId(m.taskId),
      groupId: strId(m.groupId),
      rank: m.rank ?? 0
    })).filter(m => m.taskId && m.groupId)
  }
}

export async function createListGroup(listId: string, name: string): Promise<ListGroup> {
  const data = await post<{
    id?: string | number
    listId?: string | number
    name?: string
    rank?: number
    isDefault?: boolean
  }>('/app-api/task/list-group/create', { listId, name })
  return {
    id: strId(data.id),
    listId: strId(data.listId) || listId,
    name: data.name ?? name,
    rank: data.rank ?? 0,
    isDefault: !!data.isDefault
  }
}

export async function updateListGroup(payload: {
  id: string
  name?: string
  rank?: number
}): Promise<void> {
  await put<boolean>('/app-api/task/list-group/update', {
    id: payload.id,
    name: payload.name,
    rank: payload.rank
  })
}

export async function deleteListGroup(id: string): Promise<void> {
  await del<boolean>('/app-api/task/list-group/delete', { params: { id } })
}

export async function moveListGroupTask(payload: {
  listId: string
  taskId: string
  groupId: string
  beforeId?: string | null
}): Promise<void> {
  await put<boolean>('/app-api/task/list-group/move', {
    listId: payload.listId,
    taskId: payload.taskId,
    groupId: payload.groupId,
    beforeId: payload.beforeId ?? undefined
  })
}
