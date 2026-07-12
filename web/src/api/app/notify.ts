import { get, post } from '../request'

export interface NotifyItem {
  id: string
  tenantId: string
  type: string
  title: string
  body: string
  payload?: Record<string, unknown>
  read: boolean
  createTime: string
}

export interface NotifyPageResult {
  list: NotifyItem[]
  total: number
}

export interface NotifyUnreadCount {
  unreadCount: number
}

function normalizeNotifyItem(
  item: NotifyItem & { id?: string | number, tenantId?: string | number, read?: boolean }
): NotifyItem {
  return {
    id: String(item.id),
    tenantId: String(item.tenantId),
    type: item.type,
    title: item.title,
    body: item.body,
    payload: item.payload,
    read: Boolean(item.read),
    createTime: item.createTime
  }
}

export async function getNotifyUnreadCount(): Promise<NotifyUnreadCount> {
  return get<NotifyUnreadCount>('/app-api/infra/notify/unread-count')
}

export async function getNotifyPage(pageNo = 1, pageSize = 20): Promise<NotifyPageResult> {
  const data = await get<NotifyPageResult>('/app-api/infra/notify/page', {
    params: { pageNo, pageSize }
  })
  return {
    list: (data.list ?? []).map(item => normalizeNotifyItem(item as NotifyItem & { id?: string | number })),
    total: data.total ?? 0
  }
}

export async function markNotifyRead(ids: string[]): Promise<boolean> {
  return post<boolean>('/app-api/infra/notify/read', { ids })
}
