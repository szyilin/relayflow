import { get } from '../request'

export interface MemberInvitePendingItem {
  tenantId: string
  tenantName: string
  invitedAt?: string
}

export interface MemberInvitePendingList {
  items: MemberInvitePendingItem[]
}

function normalizeItem(item: MemberInvitePendingItem & { tenantId?: string | number }): MemberInvitePendingItem {
  return {
    tenantId: String(item.tenantId),
    tenantName: item.tenantName,
    invitedAt: item.invitedAt
  }
}

export async function getMemberInvitePending(mobile: string): Promise<MemberInvitePendingList> {
  const data = await get<MemberInvitePendingList>('/app-api/system/member-invite/pending', {
    params: { mobile }
  })
  return {
    items: (data.items ?? []).map(item => normalizeItem(item as MemberInvitePendingItem & { tenantId?: string | number }))
  }
}
