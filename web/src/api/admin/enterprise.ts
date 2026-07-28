/** Admin enterprise profile & addresses — draft contract for -api. */

import { del, get, post, put } from '../request'

export type EnterpriseAddressType = 'JOB' | 'INTERVIEW' | 'STORE'

export type EnterpriseAddressStatus = 'ENABLED' | 'DISABLED'

export interface EnterpriseProfile {
  code: string
  name: string
  industry: string | null
  scale: string | null
  /** Read-only display; V1 fixed to mainland China. */
  dataResidency: string
  contactName: string | null
  contactEmail: string | null
  contactMobile: string | null
  contactLandline: string | null
}

export type EnterpriseProfileUpdate = Omit<EnterpriseProfile, 'code' | 'dataResidency'>

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

export function updateEnterpriseProfile(payload: EnterpriseProfileUpdate): Promise<EnterpriseProfile> {
  return put<EnterpriseProfile>('/admin-api/system/enterprise/profile', payload)
}

export function listEnterpriseAddresses(
  query?: EnterpriseAddressListQuery
): Promise<EnterpriseAddress[]> {
  return get<EnterpriseAddress[]>('/admin-api/system/enterprise/address/list', {
    params: query as Record<string, unknown> | undefined
  })
}

export function createEnterpriseAddress(
  payload: EnterpriseAddressCreate
): Promise<EnterpriseAddress> {
  return post<EnterpriseAddress>('/admin-api/system/enterprise/address/create', payload)
}

export function updateEnterpriseAddress(
  id: string,
  payload: EnterpriseAddressUpdate
): Promise<EnterpriseAddress> {
  return put<EnterpriseAddress>(`/admin-api/system/enterprise/address/update?id=${encodeURIComponent(id)}`, payload)
}

export function updateEnterpriseAddressStatus(
  id: string,
  status: EnterpriseAddressStatus
): Promise<EnterpriseAddress> {
  return put<EnterpriseAddress>(
    `/admin-api/system/enterprise/address/status?id=${encodeURIComponent(id)}`,
    { status }
  )
}

export function deleteEnterpriseAddress(id: string): Promise<void> {
  return del<void>(`/admin-api/system/enterprise/address/delete?id=${encodeURIComponent(id)}`)
}
