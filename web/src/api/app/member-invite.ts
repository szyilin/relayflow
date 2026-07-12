import { get, post } from '../request'
import type { AuthLoginResp } from '../admin/auth'

export interface MemberInvitePreview {
  tenantId: number
  tenantName: string
  nickname: string
}

export interface MemberInviteAcceptReq {
  mobile: string
  password: string
}

/**
 * @deprecated V2 请使用 `/app/register` 与 `auth-register` API；保留供 `enabled=false` 兼容参考
 */
export function previewMemberInvite(mobile: string): Promise<MemberInvitePreview> {
  return get<MemberInvitePreview>('/app-api/system/member-invite/preview', { mobile })
}

/** @deprecated 见 {@link previewMemberInvite} */
export function acceptMemberInvite(data: MemberInviteAcceptReq): Promise<AuthLoginResp> {
  return post<AuthLoginResp>('/app-api/system/member-invite/accept', data)
}
