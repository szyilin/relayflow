import { get } from '../request'

export interface TenantResp {
  id: number
  code: string
  name: string
  status: number
  createTime: string
}

export function getDefaultTenant(): Promise<TenantResp> {
  return get<TenantResp>('/admin-api/system/tenant/default')
}
