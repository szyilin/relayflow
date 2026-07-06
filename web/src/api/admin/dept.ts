import { del, get, post, put } from '../request'

export interface DeptItem {
  id: number
  parentId: number
  name: string
  sort?: number
  leaderUserId?: number | null
  status?: number
  createTime?: string
}

export interface DeptCreatePayload {
  parentId: number
  name: string
  sort?: number
  leaderUserId?: number | null
  status?: number
}

export interface DeptUpdatePayload extends DeptCreatePayload {
  id: number
}

export function getDeptList(): Promise<DeptItem[]> {
  return get<DeptItem[]>('/admin-api/system/dept/list')
}

export function getDept(id: number): Promise<DeptItem> {
  return get<DeptItem>('/admin-api/system/dept/get', {
    params: { id }
  })
}

export function createDept(data: DeptCreatePayload): Promise<number> {
  return post<number>('/admin-api/system/dept/create', data)
}

export function updateDept(data: DeptUpdatePayload): Promise<boolean> {
  return put<boolean>('/admin-api/system/dept/update', data)
}

export function deleteDept(id: number): Promise<boolean> {
  return del<boolean>('/admin-api/system/dept/delete', {
    params: { id }
  })
}
