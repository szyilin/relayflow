/**
 * @deprecated 注册页不再按手机号公开查询待加入邀请，避免手机号枚举；注册成功后由 auth/register 返回租户列表。
 */
export interface MemberInvitePendingItem {
  tenantId: string
  tenantName: string
  invitedAt?: string
}

/** @deprecated 见 {@link MemberInvitePendingItem} */
export interface MemberInvitePendingList {
  items: MemberInvitePendingItem[]
}
