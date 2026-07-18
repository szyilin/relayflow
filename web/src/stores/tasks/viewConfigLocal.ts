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

export const DEFAULT_VISIBLE_FIELDS = ['dueTime', 'assignee', 'status'] as const

const LEGACY_STORAGE_PREFIX = 'relayflow-task-view-config-v1'

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

/** One-shot cleanup of pre-integrate local draft configs. */
export function clearLegacyViewConfigLocalStorage() {
  try {
    const keys: string[] = []
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key?.startsWith(LEGACY_STORAGE_PREFIX)) {
        keys.push(key)
      }
    }
    for (const key of keys) {
      localStorage.removeItem(key)
    }
  } catch {
    // ignore
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
