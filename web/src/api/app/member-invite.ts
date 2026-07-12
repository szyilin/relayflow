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

export function previewMemberInvite(mobile: string): Promise<MemberInvitePreview> {
  return get<MemberInvitePreview>('/app-api/system/member-invite/preview', { mobile })
}

export function acceptMemberInvite(data: MemberInviteAcceptReq): Promise<AuthLoginResp> {
  return post<AuthLoginResp>('/app-api/system/member-invite/accept', data)
}
