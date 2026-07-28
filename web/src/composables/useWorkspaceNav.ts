import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute } from 'vue-router'
import { PRODUCT_REGISTRY } from '../products/registry'
import { useImStore } from '../stores/im'

export interface WorkspaceRailItem {
  id: string
  label: string
  icon: string
  to: string
  badge?: string
}

function formatUnreadBadge(count: number): string | undefined {
  if (count <= 0) {
    return undefined
  }
  return count > 99 ? '99+' : String(count)
}

/** Non-product rail entries that stay outside the product registry. */
const EXTRA_RAIL: Omit<WorkspaceRailItem, 'badge'>[] = [
  {
    id: 'tasks',
    label: '任务',
    icon: 'i-lucide-check-square',
    to: '/app/tasks'
  },
  {
    id: 'contacts',
    label: '通讯录',
    icon: 'i-lucide-users',
    to: '/app/contacts'
  }
]

export function useWorkspaceNav() {
  const route = useRoute()
  const imStore = useImStore()
  const { totalUnreadCount } = storeToRefs(imStore)

  const railItems = computed<WorkspaceRailItem[]>(() => {
    const fromProducts: WorkspaceRailItem[] = PRODUCT_REGISTRY
      .filter(p => p.workspacePath)
      .map(p => ({
        id: p.workspaceRailId ?? p.id,
        label: p.id === 'im' ? '消息' : p.label,
        icon: p.icon,
        to: p.workspacePath!,
        badge: p.id === 'im' ? formatUnreadBadge(totalUnreadCount.value) : undefined
      }))

    // Preserve rail order: messages, calendar, meeting, tasks, docs, contacts
    const byId = new Map([...fromProducts, ...EXTRA_RAIL].map(item => [item.id, item]))
    const order = ['messages', 'calendar', 'meeting', 'tasks', 'docs', 'contacts'] as const
    return order.flatMap((id) => {
      const item = byId.get(id)
      return item ? [item] : []
    })
  })

  const activeRailId = computed(() => {
    for (const item of railItems.value) {
      if (route.path.startsWith(item.to)) {
        return item.id
      }
    }
    return 'messages'
  })

  return {
    railItems,
    activeRailId
  }
}
