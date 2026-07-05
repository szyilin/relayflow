import { get, post } from '../request'

export interface AuthLoginReq {
  username: string
  password: string
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
}

export function login(data: AuthLoginReq): Promise<AuthLoginResp> {
  return post<AuthLoginResp>('/admin-api/system/auth/login', data)
}

export function getPermissionInfo(): Promise<AuthPermissionInfoResp> {
  return get<AuthPermissionInfoResp>('/admin-api/system/auth/get-permission-info')
}
