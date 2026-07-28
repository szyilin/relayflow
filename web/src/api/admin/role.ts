import { del, get, post, put } from '../request'

export type RoleId = string
export type RoleType = 'SYSTEM' | 'CUSTOM'

export interface RoleAdminSimple {
  userId: string
  nickname?: string
  username?: string
  avatar?: string | null
  mobile?: string | null
  deptName?: string | null
}

export interface RolePageItem {
  id: RoleId
  parentId: RoleId | number
  name: string
  code?: string | null
  roleType: RoleType
  canDelegate?: number
  sort?: number
  status?: number
  remark?: string
  createTime?: string
  permissionIds?: number[]
  permissionSummary?: string
  admins?: RoleAdminSimple[]
}

export interface RoleDetail extends RolePageItem {
  permissionIds: number[]
}

export interface RolePageResult {
  list: RolePageItem[]
  total: number
}

export interface RolePageQuery {
  pageNo: number
  pageSize: number
  keyword?: string
}

export interface RoleSavePayload {
  parentId?: RoleId | number | null
  name: string
  canDelegate?: number
  sort?: number
  status?: number
  remark?: string
  permissionIds?: number[]
}

export interface RoleUpdatePayload extends RoleSavePayload {
  id: RoleId
}

export interface PermissionNode {
  id: number
  parentId: number
  name: string
  code: string
  type?: number
  sort?: number
  children?: PermissionNode[]
}

export function getRolePage(query: RolePageQuery): Promise<RolePageResult> {
  return get<{ list: RolePageItem[], total: number | string }>('/admin-api/system/role/page', {
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

export function getRole(id: RoleId): Promise<RoleDetail> {
  return get<RoleDetail>('/admin-api/system/role/get', {
    params: { id }
  })
}

export function createRole(data: RoleSavePayload): Promise<RoleId> {
  return post<RoleId | number>('/admin-api/system/role/create', data).then(id => String(id))
}

export function updateRole(data: RoleUpdatePayload): Promise<boolean> {
  return put<boolean>('/admin-api/system/role/update', data)
}

export function deleteRole(id: RoleId): Promise<boolean> {
  return del<boolean>('/admin-api/system/role/delete', {
    params: { id }
  })
}

export function getRoleMembers(roleId: RoleId): Promise<RoleAdminSimple[]> {
  return get<RoleAdminSimple[]>('/admin-api/system/role/members', {
    params: { roleId }
  })
}

export function addRoleMembers(roleId: RoleId, userIds: string[]): Promise<boolean> {
  return post<boolean>('/admin-api/system/role/add-members', { roleId, userIds })
}

export function removeRoleMember(roleId: RoleId, userId: string): Promise<boolean> {
  return post<boolean>('/admin-api/system/role/remove-member', { roleId, userId })
}

export function getPermissionList(): Promise<PermissionNode[]> {
  return get<PermissionNode[]>('/admin-api/system/permission/list')
}
