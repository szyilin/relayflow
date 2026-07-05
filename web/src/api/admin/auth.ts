import { request } from '../request'

export interface AuthLoginReq {
  username: string
  password: string
}

export interface AuthLoginResp {
  accessToken: string
  tenantId: number
}

export function login(data: AuthLoginReq): Promise<AuthLoginResp> {
  return request<AuthLoginResp>('/admin-api/system/auth/login', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}
