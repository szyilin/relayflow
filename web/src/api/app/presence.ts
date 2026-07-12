import { get } from '../request'

export interface PresenceItem {
  userId: string
  online: boolean
}

export interface PresenceBatchResult {
  items: PresenceItem[]
}

type RawPresenceItem = Omit<PresenceItem, 'userId'> & {
  userId: number | string
}

function normalizePresenceItem(item: RawPresenceItem): PresenceItem {
  return {
    ...item,
    userId: String(item.userId)
  }
}

export function batchPresence(userIds: string[]): Promise<PresenceBatchResult> {
  if (userIds.length === 0) {
    return Promise.resolve({ items: [] })
  }
  return get<{ items: RawPresenceItem[] }>('/app-api/im/presence/batch', {
    params: { userIds: userIds.join(',') }
  }).then(result => ({
    items: result.items.map(normalizePresenceItem)
  }))
}
