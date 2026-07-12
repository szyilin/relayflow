import { get, post } from '../request'

import { idsEqual, normalizeId } from '../utils/id'

export interface TenantSummary {
  tenantId: string
  tenantName: string
  owner?: boolean
}

export interface TenantSwitchReq {
  tenantId: string
}

export interface TenantSwitchResp {
  accessToken: string
  tenantId: string
}

/** 与后端 ErrorCodeConstants.TENANT_SELECTION_REQUIRED 对齐 */
export const TENANT_SELECTION_REQUIRED_CODE = 1_001_003_003

export function getMyTenantList(): Promise<TenantSummary[]> {
  return get<TenantSummary[]>('/app-api/system/tenant/my-list')
}

export function switchTenant(data: TenantSwitchReq): Promise<TenantSwitchResp> {
  return post<TenantSwitchResp>('/app-api/system/tenant/switch', data)
}

export function parseTenantSelectionPayload(data: unknown): TenantSummary[] {
  if (!data || typeof data !== 'object' || !('tenants' in data)) {
    return []
  }
  const tenants = (data as { tenants?: unknown }).tenants
  if (!Array.isArray(tenants)) {
    return []
  }
  return tenants
    .filter((item): item is TenantSummary =>
      item != null
      && typeof item === 'object'
      && 'tenantId' in item
      && 'tenantName' in item)
    .map(item => ({
      tenantId: String(item.tenantId),
      tenantName: String(item.tenantName),
      owner: 'owner' in item ? Boolean(item.owner) : undefined
    }))
    .filter(item => item.tenantId && item.tenantName)
}
