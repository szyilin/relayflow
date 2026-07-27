/** App org-invite channels — types aligned with org-invite contract. */

import { get, post } from '../request'
import type { InviteMode } from '../admin/inviteSettings'

export type { InviteMode }

export interface OrgInviteCapability {
  canInvite: boolean
  inviteMode: InviteMode
}

export interface OrgInviteShared {
  token: string
  inviteCode: string
  /** ISO-8601 */
  expiresAt: string
}

export interface OrgInviteMobile {
  id: string
  mobile: string
  status: string
  createdAt: string
}

export interface OrgInviteConsumeResult {
  joined: boolean
  alreadyMember: boolean
  tenantId: string
}

export function getOrgInviteCapability(): Promise<OrgInviteCapability> {
  return get<OrgInviteCapability>('/app-api/system/org-invite/capability')
}

export function getSharedOrgInvite(): Promise<OrgInviteShared> {
  return get<OrgInviteShared>('/app-api/system/org-invite/shared')
}

export function resetSharedOrgInvite(): Promise<OrgInviteShared> {
  return post<OrgInviteShared>('/app-api/system/org-invite/shared/reset')
}

export function createMobileOrgInvite(mobile: string): Promise<OrgInviteMobile> {
  return post<OrgInviteMobile>('/app-api/system/org-invite/mobile', { mobile })
}

export function consumeOrgInvite(payload: {
  token?: string
  code?: string
}): Promise<OrgInviteConsumeResult> {
  return post<OrgInviteConsumeResult>('/app-api/system/org-invite/consume', payload)
}
