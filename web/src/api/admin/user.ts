import { get, post, put } from '../request'

export type UserId = string

export interface UserPageItem {
  id: UserId
  username: string
  nickname: string
  dept?: string
  status?: number
  createTime?: string
}

export interface UserPageResult {
  list: UserPageItem[]
  total: number
}

export interface UserPageQuery {
  pageNo: number
  pageSize: number
  keyword?: string
}

export interface UserDetail {
  id: UserId
  username: string
  nickname: string
  mobile?: string | null
  email?: string | null
  status: number
  deptId?: UserId | null
  roleIds: UserId[]
  createTime?: string
}

export interface UserCreatePayload {
  username: string
  password: string
  nickname?: string
  mobile?: string
  email?: string
  deptId?: UserId | null
  roleIds?: UserId[]
}

export interface UserUpdatePayload {
  id: UserId
  nickname?: string
  mobile?: string
  email?: string
}

export interface UserUpdateStatusPayload {
  id: UserId
  status: number
}

export interface UserUpdateDeptPayload {
  id: UserId
  deptId?: UserId | null
}

export interface UserUpdateRolePayload {
  id: UserId
  roleIds?: UserId[]
}

export function getUserPage(query: UserPageQuery): Promise<UserPageResult> {
  return get<{ list: UserPageItem[], total: number | string }>('/admin-api/system/user/page', {
    params: {
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      keyword: query.keyword?.trim() || undefined
    }
  }).then(data => ({
    list: data.list,
    total: Number(data.total)
  }))
}

export function getUser(id: UserId): Promise<UserDetail> {
  return get<UserDetail>('/admin-api/system/user/get', {
    params: { id }
  })
}

export function createUser(data: UserCreatePayload): Promise<UserId> {
  return post<UserId>('/admin-api/system/user/create', data)
}

export function updateUser(data: UserUpdatePayload): Promise<boolean> {
  return put<boolean>('/admin-api/system/user/update', data)
}

export function updateUserStatus(data: UserUpdateStatusPayload): Promise<boolean> {
  return put<boolean>('/admin-api/system/user/update-status', data)
}

export function updateUserDept(data: UserUpdateDeptPayload): Promise<boolean> {
  return put<boolean>('/admin-api/system/user/update-dept', data)
}

export function updateUserRole(data: UserUpdateRolePayload): Promise<boolean> {
  return put<boolean>('/admin-api/system/user/update-role', data)
}
