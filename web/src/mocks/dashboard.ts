export interface MockStatCard {
  label: string
  value: string
  icon: string
  change?: string
}

export const mockDashboardStats: MockStatCard[] = [{
  label: '用户总数',
  value: '128',
  icon: 'i-lucide-users',
  change: '+12 本月'
}, {
  label: '在线用户',
  value: '24',
  icon: 'i-lucide-activity',
  change: '实时'
}, {
  label: '存储用量',
  value: '42.6 GB',
  icon: 'i-lucide-hard-drive',
  change: '68% 配额'
}, {
  label: '消息总量',
  value: '15.2k',
  icon: 'i-lucide-message-square',
  change: '+320 今日'
}]

export interface MockQuickLink {
  label: string
  description: string
  icon: string
  to: string
}

export const mockQuickLinks: MockQuickLink[] = [{
  label: '用户管理',
  description: '查看与管理系统用户',
  icon: 'i-lucide-users',
  to: '/admin/system/user'
}, {
  label: '文件管理',
  description: '浏览已上传的文件',
  icon: 'i-lucide-folder-open',
  to: '/admin/infra/file'
}, {
  label: '部门管理',
  description: '组织架构与部门',
  icon: 'i-lucide-building-2',
  to: '/admin/system/dept'
}]
