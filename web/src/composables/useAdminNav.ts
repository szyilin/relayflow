import { computed, ref } from 'vue'
import type { NavigationMenuItem } from '@nuxt/ui'
import { usePermission } from './usePermission'

export type AdminNavItem = NavigationMenuItem & {
  permission?: string
}

function filterNavItems(items: AdminNavItem[], hasPermission: (code: string) => boolean): NavigationMenuItem[] {
  return items.filter(item => !item.permission || hasPermission(item.permission))
}

export function useAdminNav() {
  const { hasPermission } = usePermission()
  const open = ref(false)

  const closeSidebar = () => {
    open.value = false
  }

  const primaryLinks: NavigationMenuItem[] = [{
    label: '概览',
    icon: 'i-lucide-layout-dashboard',
    to: '/admin',
    exact: true,
    onSelect: closeSidebar
  }]

  const systemLinksRaw: AdminNavItem[] = [{
    label: '用户管理',
    icon: 'i-lucide-users',
    to: '/admin/system/user',
    permission: 'system:user:list',
    onSelect: closeSidebar
  }, {
    label: '部门管理',
    icon: 'i-lucide-building-2',
    to: '/admin/system/dept',
    permission: 'system:dept:list',
    onSelect: closeSidebar
  }]

  const infraLinksRaw: AdminNavItem[] = [{
    label: '文件管理',
    icon: 'i-lucide-folder-open',
    to: '/admin/infra/file',
    onSelect: closeSidebar
  }]

  const devLinks: NavigationMenuItem[] = [{
    label: '设计预览',
    icon: 'i-lucide-palette',
    to: '/admin/design-preview',
    onSelect: closeSidebar
  }]

  const systemLinks = computed(() => filterNavItems(systemLinksRaw, hasPermission))
  const infraLinks = computed(() => filterNavItems(infraLinksRaw, hasPermission))

  return {
    open,
    primaryLinks,
    systemLinks,
    infraLinks,
    devLinks,
    closeSidebar
  }
}
