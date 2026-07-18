import { del, get, post, put } from '../request'

export interface MineGroup {
  id: string
  name: string
  rank: number
  isDefault: boolean
}

export interface MineGroupMembership {
  taskId: string
  groupId: string
  rank: number
}

export interface MineGroupListResult {
  groups: MineGroup[]
  memberships: MineGroupMembership[]
}

function strId(v: string | number | null | undefined): string {
  return v == null || v === '' ? '' : String(v)
}

export async function listMineGroups(): Promise<MineGroupListResult> {
  const data = await get<{
    groups?: Array<{
      id?: string | number
      name?: string
      rank?: number
      isDefault?: boolean
    }>
    memberships?: Array<{
      taskId?: string | number
      groupId?: string | number
      rank?: number
    }>
  }>('/app-api/task/mine-group/list')

  return {
    groups: (data.groups ?? []).map(g => ({
      id: strId(g.id),
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

export async function createMineGroup(name: string): Promise<MineGroup> {
  const data = await post<{
    id?: string | number
    name?: string
    rank?: number
    isDefault?: boolean
  }>('/app-api/task/mine-group/create', { name })
  return {
    id: strId(data.id),
    name: data.name ?? name,
    rank: data.rank ?? 0,
    isDefault: !!data.isDefault
  }
}

export async function updateMineGroup(payload: {
  id: string
  name?: string
  rank?: number
}): Promise<void> {
  await put<boolean>('/app-api/task/mine-group/update', {
    id: payload.id,
    name: payload.name,
    rank: payload.rank
  })
}

export async function deleteMineGroup(id: string): Promise<void> {
  await del<boolean>('/app-api/task/mine-group/delete', { params: { id } })
}

export async function moveMineGroupTask(payload: {
  taskId: string
  groupId: string
  beforeId?: string | null
}): Promise<void> {
  await put<boolean>('/app-api/task/mine-group/move', {
    taskId: payload.taskId,
    groupId: payload.groupId,
    beforeId: payload.beforeId ?? undefined
  })
}
