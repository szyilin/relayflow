import { del, get, post, put } from '../request'

export type DataScope = 'ALL' | 'DEPT' | 'DEPT_AND_CHILD' | 'SELF' | 'CUSTOM'
export type RoleType = 'SYSTEM' | 'CUSTOM'

export interface RolePageItem {
  id: number
  parentId: number
  name: string
  code: string
  roleType: RoleType
  dataScope: DataScope
  canDelegate?: number
  sort?: number
  status?: number
  remark?: string
  createTime?: string
}

export interface RoleDetail extends RolePageItem {
  permissionIds: number[]
  deptIds: number[]
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
  parentId: number
  name: string
  code: string
  dataScope: DataScope
  canDelegate?: number
  sort?: number
  status?: number
  remark?: string
  permissionIds?: number[]
  deptIds?: number[]
}

export interface RoleUpdatePayload extends RoleSavePayload {
  id: number
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
  return get<RolePageResult>('/admin-api/system/role/page', {
    params: {
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      keyword: query.keyword?.trim() || undefined
    }
  })
}

export function getRole(id: number): Promise<RoleDetail> {
  return get<RoleDetail>('/admin-api/system/role/get', {
    params: { id }
  })
}

export function createRole(data: RoleSavePayload): Promise<number> {
  return post<number>('/admin-api/system/role/create', data)
}

export function updateRole(data: RoleUpdatePayload): Promise<boolean> {
  return put<boolean>('/admin-api/system/role/update', data)
}

export function deleteRole(id: number): Promise<boolean> {
  return del<boolean>('/admin-api/system/role/delete', {
    params: { id }
  })
}

export function getPermissionList(): Promise<PermissionNode[]> {
  return get<PermissionNode[]>('/admin-api/system/permission/list')
}
