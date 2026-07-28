/** Admin enterprise profile & addresses — live admin-api. */

import { del, get, post, put } from '../request'

export type EnterpriseAddressType = 'JOB' | 'INTERVIEW' | 'STORE'

export type EnterpriseAddressStatus = 'ENABLED' | 'DISABLED'

export interface EnterpriseProfile {
  code: string
  name: string
  industry: string | null
  scale: string | null
  /** infra file id; null when unset */
  logoFileId: string | null
  contactName: string | null
  contactEmail: string | null
  contactMobile: string | null
  contactLandline: string | null
}

export type EnterpriseProfileUpdate = Omit<EnterpriseProfile, 'code'>

export interface EnterpriseOverview {
  code: string
  name: string
  logoFileId: string | null
  memberCount: number
  deptCount: number
  superAdminCount: number
  subAdminCount: number
}

export interface EnterpriseAddress {
  id: string
  types: EnterpriseAddressType[]
  regionId: string
  regionPath: string
  detail: string
  status: EnterpriseAddressStatus
  createTime: string
}

export interface EnterpriseAddressCreate {
  types: EnterpriseAddressType[]
  regionId: string
  regionPath: string
  detail: string
}

export type EnterpriseAddressUpdate = EnterpriseAddressCreate

export interface EnterpriseAddressListQuery {
  keyword?: string
  types?: EnterpriseAddressType[]
  status?: EnterpriseAddressStatus | 'ALL'
}

export function getEnterpriseProfile(): Promise<EnterpriseProfile> {
  return get<EnterpriseProfile>('/admin-api/system/enterprise/profile')
}

export function getEnterpriseOverview(): Promise<EnterpriseOverview> {
  return get<EnterpriseOverview>('/admin-api/system/enterprise/overview')
}

export function updateEnterpriseProfile(payload: EnterpriseProfileUpdate): Promise<EnterpriseProfile> {
  return put<EnterpriseProfile>('/admin-api/system/enterprise/profile', payload)
}

export function listEnterpriseAddresses(
  query?: EnterpriseAddressListQuery
): Promise<EnterpriseAddress[]> {
  const params: Record<string, string> = {}
  if (query?.keyword?.trim()) {
    params.keyword = query.keyword.trim()
  }
  if (query?.types?.length) {
    params.types = query.types.join(',')
  }
  if (query?.status && query.status !== 'ALL') {
    params.status = query.status
  }
  return get<EnterpriseAddress[]>('/admin-api/system/enterprise/address/list', {
    params
  }).then(rows => (rows ?? []).map(normalizeAddress))
}

function normalizeAddress(raw: EnterpriseAddress): EnterpriseAddress {
  return {
    ...raw,
    id: String(raw.id),
    regionId: String(raw.regionId),
    types: [...(raw.types ?? [])],
    detail: raw.detail ?? '',
    regionPath: raw.regionPath ?? '',
    status: raw.status,
    createTime: raw.createTime
  }
}

export function createEnterpriseAddress(
  payload: EnterpriseAddressCreate
): Promise<EnterpriseAddress> {
  return post<EnterpriseAddress>('/admin-api/system/enterprise/address/create', payload)
    .then(normalizeAddress)
}

export function updateEnterpriseAddress(
  id: string,
  payload: EnterpriseAddressUpdate
): Promise<EnterpriseAddress> {
  return put<EnterpriseAddress>(`/admin-api/system/enterprise/address/update?id=${encodeURIComponent(id)}`, payload)
    .then(normalizeAddress)
}

export function updateEnterpriseAddressStatus(
  id: string,
  status: EnterpriseAddressStatus
): Promise<EnterpriseAddress> {
  return put<EnterpriseAddress>(
    `/admin-api/system/enterprise/address/status?id=${encodeURIComponent(id)}`,
    { status }
  ).then(normalizeAddress)
}

export function deleteEnterpriseAddress(id: string): Promise<void> {
  return del<void>(`/admin-api/system/enterprise/address/delete?id=${encodeURIComponent(id)}`)
}
