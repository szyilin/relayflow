import type { NotifyType } from '../api/app/notify'

export type NotifyFilterKey = 'all' | 'MEMBER_INVITE' | 'TASK_DUE'

export const NOTIFY_FILTER_OPTIONS: { key: NotifyFilterKey, label: string }[] = [
  { key: 'all', label: '全部' },
  { key: 'MEMBER_INVITE', label: '邀请' },
  { key: 'TASK_DUE', label: '任务' }
]

export function notifyTypeIcon(type: NotifyType): string {
  switch (type) {
    case 'MEMBER_INVITE':
      return 'i-lucide-building-2'
    case 'TASK_DUE':
    case 'TASK_ASSIGNED':
      return 'i-lucide-check-square'
    case 'IM_MENTION':
      return 'i-lucide-at-sign'
    case 'APPROVAL_PENDING':
      return 'i-lucide-file-check'
    default:
      return 'i-lucide-bell'
  }
}

export function resolveNotifyRoute(payload?: Record<string, unknown>): string | null {
  const route = payload?.route
  if (typeof route !== 'string') {
    return null
  }
  const trimmed = route.trim()
  if (!trimmed.startsWith('/app/') || trimmed.startsWith('//')) {
    return null
  }
  return trimmed
}
