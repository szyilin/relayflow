import { get, put } from '../request'

/** Preset member limits for chat groups (custom allowed separately). */
export const GROUP_SIZE_PRESETS = [5, 10, 30, 50] as const

export type GroupSizeSubjectType = 'DEPT' | 'USER'
export type DeptGroupLeavePolicy = 'ALLOW' | 'DENY'
export type MemberExportPolicy = 'ALLOW_OWNER_ADMIN' | 'DENY_ALL' | 'ALLOW_DESIGNATED'
export type RecallEditWindowHours = 2 | 24 | 168

export interface GroupSizeUserRule {
  /** Client-only key for list rendering before save */
  key?: string
  subjectType: GroupSizeSubjectType
  subjectId: string
  subjectLabel: string
  memberLimit: number
}

export interface GroupSizeGroupRule {
  key?: string
  conversationId: string
  groupName: string
  memberLimit: number
}

export interface ImProductSettings {
  groupMemberLimitDefault: number
  userRules: GroupSizeUserRule[]
  groupRules: GroupSizeGroupRule[]
  allowPublicGroup: boolean
  deptGroupLeavePolicy: DeptGroupLeavePolicy
  memberExportPolicy: MemberExportPolicy
  memberExportDesignatedUserIds: string[]
  recallEnabled: boolean
  recallWithinHours: RecallEditWindowHours
  editEnabled: boolean
  editWithinHours: RecallEditWindowHours
}

export const DEFAULT_IM_PRODUCT_SETTINGS: ImProductSettings = {
  groupMemberLimitDefault: 50,
  userRules: [],
  groupRules: [],
  allowPublicGroup: true,
  deptGroupLeavePolicy: 'ALLOW',
  memberExportPolicy: 'DENY_ALL',
  memberExportDesignatedUserIds: [],
  recallEnabled: true,
  recallWithinHours: 24,
  editEnabled: true,
  editWithinHours: 24
}

export const RECALL_EDIT_WINDOW_OPTIONS: Array<{ label: string, value: RecallEditWindowHours }> = [
  { label: '2 小时内', value: 2 },
  { label: '24 小时内', value: 24 },
  { label: '7 天内', value: 168 }
]

export interface ImGroupSearchItem {
  conversationId: string
  name: string
  memberCount: number
}

export function cloneImProductSettings(source: ImProductSettings): ImProductSettings {
  return {
    groupMemberLimitDefault: source.groupMemberLimitDefault,
    userRules: source.userRules.map(rule => ({ ...rule })),
    groupRules: source.groupRules.map(rule => ({ ...rule })),
    allowPublicGroup: source.allowPublicGroup,
    deptGroupLeavePolicy: source.deptGroupLeavePolicy,
    memberExportPolicy: source.memberExportPolicy,
    memberExportDesignatedUserIds: [...source.memberExportDesignatedUserIds],
    recallEnabled: source.recallEnabled,
    recallWithinHours: source.recallWithinHours,
    editEnabled: source.editEnabled,
    editWithinHours: source.editWithinHours
  }
}

export function getImProductSettings(): Promise<ImProductSettings> {
  return get<ImProductSettings>('/admin-api/im/product-settings')
}

export function updateImProductSettings(payload: ImProductSettings): Promise<ImProductSettings> {
  return put<ImProductSettings>('/admin-api/im/product-settings', payload)
}

export function searchTenantGroups(keyword: string): Promise<ImGroupSearchItem[]> {
  return get<ImGroupSearchItem[]>('/admin-api/im/product-settings/groups', {
    params: { keyword: keyword || undefined }
  })
}
