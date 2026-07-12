import { post } from '../request'
import type { AuthLoginResp } from '../admin/auth'

export interface AuthRegisterReq {
  mobile: string
  password: string
  nickname: string
  tenantName: string
}

export interface AuthRegisterTenantSummary {
  tenantId: number
  tenantName: string
  owner: boolean
}

export interface AuthRegisterResp extends AuthLoginResp {
  tenants?: AuthRegisterTenantSummary[]
}

export function register(data: AuthRegisterReq): Promise<AuthRegisterResp> {
  return post<AuthRegisterResp>('/app-api/system/auth/register', data)
}
