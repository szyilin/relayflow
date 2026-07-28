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
    label: '企业信息',
    icon: 'i-lucide-building',
    to: '/admin/system/enterprise',
    permission: 'system:enterprise:query',
    onSelect: closeSidebar
  }, {
    label: '用户管理',
    icon: 'i-lucide-users',
    to: '/admin/system/user',
    permission: 'system:user:list',
    onSelect: closeSidebar
  }, {
    label: '管理员角色',
    icon: 'i-lucide-shield',
    to: '/admin/system/role',
    permission: 'system:role:list',
    onSelect: closeSidebar
  }, {
    label: '部门管理',
    icon: 'i-lucide-building-2',
    to: '/admin/system/dept',
    permission: 'system:dept:list',
    onSelect: closeSidebar
  }]

  const productLinksRaw: AdminNavItem[] = [{
    label: '产品设置',
    icon: 'i-lucide-boxes',
    to: '/admin/products',
    permission: 'im:product-settings:query',
    onSelect: closeSidebar
  }]

  const infraLinksRaw: AdminNavItem[] = [{
    label: '存储设置',
    icon: 'i-lucide-hard-drive',
    to: '/admin/infra/storage',
    permission: 'infra:storage:query',
    onSelect: closeSidebar
  }, {
    label: '模型设置',
    icon: 'i-lucide-sparkles',
    to: '/admin/infra/llm',
    permission: 'agent:llm:query',
    onSelect: closeSidebar
  }, {
    label: '文件管理',
    icon: 'i-lucide-folder-open',
    to: '/admin/infra/file',
    permission: 'infra:file:list',
    onSelect: closeSidebar
  }]

  const systemLinks = computed(() => filterNavItems(systemLinksRaw, hasPermission))
  const productLinks = computed(() => filterNavItems(productLinksRaw, hasPermission))
  const infraLinks = computed(() => filterNavItems(infraLinksRaw, hasPermission))

  return {
    open,
    primaryLinks,
    systemLinks,
    productLinks,
    infraLinks,
    closeSidebar
  }
}
