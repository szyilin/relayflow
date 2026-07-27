/** Admin invite settings — types aligned with org-invite contract. */

import { get, put } from '../request'

export type InviteMode = 'OFF' | 'ADMIN_ONLY' | 'ALL_MEMBERS'

export type NonAdminInviteScope = 'INVITER_DEPT' | 'DESIGNATED_DEPTS'

export interface InviteSettings {
  inviteMode: InviteMode
  nonAdminScope: NonAdminInviteScope
  nonAdminDeptIds: string[]
  nonAdminExpireDays: number
  nonAdminRequireApproval: boolean
}

export const DEFAULT_INVITE_SETTINGS: InviteSettings = {
  inviteMode: 'ADMIN_ONLY',
  nonAdminScope: 'INVITER_DEPT',
  nonAdminDeptIds: [],
  nonAdminExpireDays: 3,
  nonAdminRequireApproval: false
}

export function getInviteSettings(): Promise<InviteSettings> {
  return get<InviteSettings>('/admin-api/system/invite-settings')
}

export function updateInviteSettings(
  payload: InviteSettings
): Promise<InviteSettings> {
  return put<InviteSettings>('/admin-api/system/invite-settings', payload)
}
