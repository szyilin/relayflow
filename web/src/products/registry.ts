/**
 * Shared product metadata — single source for workspace rail icons and admin product settings.
 * Change an icon here only; do not hardcode product icons in nav pages.
 */
export type ProductAdminStatus = 'enabled' | 'placeholder'

export interface ProductDefinition {
  id: string
  label: string
  /** Nuxt UI / Lucide icon name, e.g. i-lucide-message-circle */
  icon: string
  workspacePath?: string
  /** Workspace rail id when different from product id (e.g. messages vs im) */
  workspaceRailId?: string
  adminSettingsPath: string
  adminStatus: ProductAdminStatus
  /** Short blurb on the products catalog card */
  description: string
}

export const PRODUCT_REGISTRY: ProductDefinition[] = [
  {
    id: 'im',
    label: '即时消息',
    icon: 'i-lucide-message-circle',
    workspacePath: '/app/messages',
    workspaceRailId: 'messages',
    adminSettingsPath: '/admin/products/im/group-size',
    adminStatus: 'enabled',
    description: '群规模、群权限与消息权限等组织策略'
  },
  {
    id: 'docs',
    label: '云文档',
    icon: 'i-lucide-file-text',
    workspacePath: '/app/docs',
    workspaceRailId: 'docs',
    adminSettingsPath: '/admin/products/docs',
    adminStatus: 'placeholder',
    description: '组织级云文档设置（即将开放）'
  },
  {
    id: 'calendar',
    label: '日历',
    icon: 'i-lucide-calendar',
    workspacePath: '/app/calendar',
    workspaceRailId: 'calendar',
    adminSettingsPath: '/admin/products/calendar',
    adminStatus: 'placeholder',
    description: '组织级日历设置（即将开放）'
  }
]

/** Products shown in admin「产品设置」catalog (ordered). */
export const ADMIN_PRODUCT_CATALOG = PRODUCT_REGISTRY

export function getProductById(id: string): ProductDefinition | undefined {
  return PRODUCT_REGISTRY.find(p => p.id === id)
}

export function getProductByWorkspaceRailId(railId: string): ProductDefinition | undefined {
  return PRODUCT_REGISTRY.find(p => (p.workspaceRailId ?? p.id) === railId)
}
