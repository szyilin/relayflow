import { ref } from 'vue'
import type { NavigationMenuItem } from '@nuxt/ui'

export function useAdminNav() {
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

  const systemLinks: NavigationMenuItem[] = [{
    label: '用户管理',
    icon: 'i-lucide-users',
    to: '/admin/system/user',
    onSelect: closeSidebar
  }, {
    label: '部门管理',
    icon: 'i-lucide-building-2',
    to: '/admin/system/dept',
    onSelect: closeSidebar
  }]

  const infraLinks: NavigationMenuItem[] = [{
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

  return {
    open,
    primaryLinks,
    systemLinks,
    infraLinks,
    devLinks,
    closeSidebar
  }
}
