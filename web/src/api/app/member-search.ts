import { get } from '../request'

export interface MemberSearchItem {
  id: string
  title: string
  subtitle?: string
  route: string
  entityType: string
  entityId: string
  deptId?: string
}

type RawMemberSearchItem = Omit<MemberSearchItem, 'id' | 'deptId'> & {
  id: number | string
  deptId?: number | string
}

export async function searchMembers(keyword: string, limit = 5): Promise<MemberSearchItem[]> {
  const list = await get<RawMemberSearchItem[]>('/app-api/system/member/search', {
    params: { keyword, limit }
  })
  return (list ?? []).map(item => ({
    ...item,
    id: String(item.id),
    deptId: item.deptId != null ? String(item.deptId) : undefined
  }))
}
