import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute } from 'vue-router'
import type { NavigationMenuItem } from '@nuxt/ui'
import { useAuthStore } from '../stores/auth'

export interface WorkspaceRailItem {
  id: string
  label: string
  icon: string
  to: string
  badge?: string
}

export function useWorkspaceNav() {
  const route = useRoute()
  const authStore = useAuthStore()
  const { isAdmin } = storeToRefs(authStore)

  const railItems: WorkspaceRailItem[] = [{
    id: 'messages',
    label: '消息',
    icon: 'i-lucide-message-circle',
    to: '/app/messages',
    badge: '3'
  }, {
    id: 'tasks',
    label: '任务',
    icon: 'i-lucide-check-square',
    to: '/app/tasks'
  }, {
    id: 'docs',
    label: '云文档',
    icon: 'i-lucide-file-text',
    to: '/app/docs'
  }, {
    id: 'contacts',
    label: '通讯录',
    icon: 'i-lucide-users',
    to: '/app/contacts'
  }]

  const activeRailId = computed(() => {
    if (route.path.startsWith('/app/messages')) {
      return 'messages'
    }
    if (route.path.startsWith('/app/tasks')) {
      return 'tasks'
    }
    if (route.path.startsWith('/app/docs')) {
      return 'docs'
    }
    if (route.path.startsWith('/app/contacts')) {
      return 'contacts'
    }
    return 'messages'
  })

  const footerLinks = computed(() => {
    if (!isAdmin.value) {
      return [[]] satisfies NavigationMenuItem[][]
    }

    return [[{
      label: '管理后台',
      icon: 'i-lucide-shield',
      to: '/admin',
      target: '_self'
    }]] satisfies NavigationMenuItem[][]
  })

  return {
    railItems,
    activeRailId,
    footerLinks
  }
}
