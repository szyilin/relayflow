import { del, get, post, put } from '../request'

export type DeptId = string

export interface DeptItem {
  id: DeptId
  parentId: DeptId
  name: string
  sort?: number
  leaderUserId?: DeptId | null
  status?: number
  createTime?: string
}

export interface DeptCreatePayload {
  parentId: DeptId
  name: string
  sort?: number
  leaderUserId?: DeptId | null
  status?: number
}

export interface DeptUpdatePayload extends DeptCreatePayload {
  id: DeptId
}

export function getDeptList(): Promise<DeptItem[]> {
  return get<DeptItem[]>('/admin-api/system/dept/list')
}

export function getDept(id: DeptId): Promise<DeptItem> {
  return get<DeptItem>('/admin-api/system/dept/get', {
    params: { id }
  })
}

export function createDept(data: DeptCreatePayload): Promise<DeptId> {
  return post<DeptId>('/admin-api/system/dept/create', data)
}

export function updateDept(data: DeptUpdatePayload): Promise<boolean> {
  return put<boolean>('/admin-api/system/dept/update', data)
}

export function deleteDept(id: DeptId): Promise<boolean> {
  return del<boolean>('/admin-api/system/dept/delete', {
    params: { id }
  })
}
