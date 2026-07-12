import { get, post } from '../request'

export interface AuthLoginReq {
  username: string
  password: string
  tenantId?: number
}

export interface AuthLoginResp {
  accessToken: string
  tenantId: number
}

export interface AuthPermissionRole {
  id: number
  code: string
  name: string
}

export interface AuthPermissionInfoResp {
  userId: number
  username: string
  nickname: string
  roles: AuthPermissionRole[]
  permissions: string[]
  isAdmin: boolean
}

export function login(data: AuthLoginReq): Promise<AuthLoginResp> {
  return post<AuthLoginResp>('/admin-api/system/auth/login', data)
}

export function getPermissionInfo(): Promise<AuthPermissionInfoResp> {
  return get<AuthPermissionInfoResp>('/admin-api/system/auth/get-permission-info')
}

export function logout(): Promise<boolean> {
  return post<boolean>('/admin-api/system/auth/logout')
}
