import { get, post } from '../request'

export interface TenantSummary {
  tenantId: number
  tenantName: string
  owner?: boolean
}

export interface TenantSwitchReq {
  tenantId: number
}

export interface TenantSwitchResp {
  accessToken: string
  tenantId: number
}

/** 与后端 ErrorCodeConstants.TENANT_SELECTION_REQUIRED 对齐（tenant-switch-v2-api） */
export const TENANT_SELECTION_REQUIRED_CODE = 1_001_003_001

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
      tenantId: Number(item.tenantId),
      tenantName: String(item.tenantName),
      owner: 'owner' in item ? Boolean(item.owner) : undefined
    }))
    .filter(item => Number.isFinite(item.tenantId) && item.tenantName)
}
