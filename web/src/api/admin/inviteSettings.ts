/** Admin invite settings — types aligned with org-invite contract. */

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

/**
 * Reserved for org-invite-settings-api.
 * `-web` store keeps an in-memory copy and does not call these yet.
 */
export async function getInviteSettings(): Promise<InviteSettings> {
  throw new Error('invite-settings API not ready; use store local settings')
}

export async function updateInviteSettings(
  _payload: InviteSettings
): Promise<InviteSettings> {
  throw new Error('invite-settings API not ready; use store local settings')
}
