import { post } from '../request'

export interface AuthLoginReq {
  username: string
  password: string
}

export interface AuthLoginResp {
  accessToken: string
  tenantId: number
}

export function login(data: AuthLoginReq): Promise<AuthLoginResp> {
  return post<AuthLoginResp>('/admin-api/system/auth/login', data)
}
