import type { TaskItemStatus } from '../../api/app/task'
import type { TasksNavView } from './helpers'

export type TaskViewContextType =
  | 'MINE'
  | 'FOLLOWING'
  | 'ALL'
  | 'CREATED'
  | 'ASSIGNED_BY_ME'
  | 'COMPLETED'
  | 'LIST'

export type TaskViewDisplayMode = 'LIST' | 'BOARD'

export type TaskViewSortKey = 'createTime' | 'dueTime' | 'title' | 'status' | 'updateTime'

export type TaskViewGroupBy =
  | null
  | { mode: 'FIELD', fieldKey: 'status' | 'dueTime' | 'assigneeId' }
  | { mode: 'PERSONAL_CUSTOM' }
  | { mode: 'LIST_GROUP' }

export type TaskViewSort =
  | 'MANUAL'
  | { key: TaskViewSortKey, order: 'ASC' | 'DESC' }

export interface TaskViewFilterClause {
  field: 'status'
  op: 'IN'
  values: TaskItemStatus[]
}

export interface TaskViewConfig {
  displayMode: TaskViewDisplayMode
  groupBy: TaskViewGroupBy
  sort: TaskViewSort
  filters: TaskViewFilterClause[]
  visibleFieldKeys: string[]
}

export interface TaskViewContextKey {
  contextType: TaskViewContextType
  contextId?: string | null
}

/** Temporary until workspace-task-view-config-api / integrate. */
export const USE_LOCAL_VIEW_CONFIG = true

const STORAGE_PREFIX = 'relayflow-task-view-config-v1'

export const DEFAULT_VISIBLE_FIELDS = ['dueTime', 'assignee', 'status'] as const

export function defaultViewConfig(contextType: TaskViewContextType): TaskViewConfig {
  if (contextType === 'COMPLETED') {
    return {
      displayMode: 'LIST',
      groupBy: null,
      sort: { key: 'createTime', order: 'DESC' },
      filters: [],
      visibleFieldKeys: [...DEFAULT_VISIBLE_FIELDS]
    }
  }
  if (contextType === 'LIST') {
    return {
      displayMode: 'LIST',
      groupBy: { mode: 'FIELD', fieldKey: 'status' },
      sort: 'MANUAL',
      filters: [],
      visibleFieldKeys: [...DEFAULT_VISIBLE_FIELDS]
    }
  }
  return {
    displayMode: 'LIST',
    groupBy: null,
    sort: { key: 'createTime', order: 'DESC' },
    filters: [],
    visibleFieldKeys: [...DEFAULT_VISIBLE_FIELDS]
  }
}

export function navViewToContextType(nav: TasksNavView): TaskViewContextType | null {
  switch (nav) {
    case 'mine':
      return 'MINE'
    case 'following':
      return 'FOLLOWING'
    case 'all':
      return 'ALL'
    case 'created':
      return 'CREATED'
    case 'assigned_by_me':
      return 'ASSIGNED_BY_ME'
    case 'done':
      return 'COMPLETED'
    default:
      return null
  }
}

export function contextStorageKey(ctx: TaskViewContextKey): string {
  if (ctx.contextType === 'LIST') {
    return `LIST:${ctx.contextId ?? ''}`
  }
  return ctx.contextType
}

function storageBucketKey(tenantId: string, userId: string): string {
  return `${STORAGE_PREFIX}:${tenantId}:${userId}`
}

export function loadLocalViewConfigMap(tenantId: string, userId: string): Record<string, TaskViewConfig> {
  if (!USE_LOCAL_VIEW_CONFIG || !tenantId || !userId) {
    return {}
  }
  try {
    const raw = localStorage.getItem(storageBucketKey(tenantId, userId))
    if (!raw) {
      return {}
    }
    const parsed = JSON.parse(raw) as Record<string, TaskViewConfig>
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

export function saveLocalViewConfigMap(
  tenantId: string,
  userId: string,
  map: Record<string, TaskViewConfig>
) {
  if (!USE_LOCAL_VIEW_CONFIG || !tenantId || !userId) {
    return
  }
  try {
    localStorage.setItem(storageBucketKey(tenantId, userId), JSON.stringify(map))
  } catch {
    // ignore quota
  }
}

export function resolveViewConfig(
  map: Record<string, TaskViewConfig>,
  ctx: TaskViewContextKey
): TaskViewConfig {
  const key = contextStorageKey(ctx)
  const stored = map[key]
  if (stored) {
    return {
      ...defaultViewConfig(ctx.contextType),
      ...stored,
      filters: stored.filters ?? [],
      visibleFieldKeys: stored.visibleFieldKeys?.length
        ? stored.visibleFieldKeys
        : [...DEFAULT_VISIBLE_FIELDS]
    }
  }
  return defaultViewConfig(ctx.contextType)
}
