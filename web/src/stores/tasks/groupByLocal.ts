import type { TaskItem, TaskItemStatus } from '../../api/app/task'
import { resolveAssigneeIds } from './assigneeLocal'
import type { TaskViewGroupBy } from './viewConfigLocal'
import { BOARD_COLUMN_LABELS, isBoardStatus } from './boardLocal'

export const EMPTY_GROUP_KEY = '__empty__'
export const EMPTY_GROUP_LABEL = '无分组'
export const ALL_GROUP_KEY = '__all__'
export const ALL_GROUP_LABEL = '全部'

export interface TaskGroupBucket {
  key: string
  label: string
  items: TaskItem[]
}

export interface PartitionGroupOptions {
  /** Resolve assignee column title (nickname). Falls back to userId. */
  assigneeLabel?: (userId: string) => string
}

const STATUS_ORDER: TaskItemStatus[] = ['TODO', 'DONE']

export function partitionByGroupBy(
  items: TaskItem[],
  groupBy: TaskViewGroupBy,
  options?: PartitionGroupOptions
): TaskGroupBucket[] {
  if (!groupBy) {
    return [{ key: ALL_GROUP_KEY, label: ALL_GROUP_LABEL, items: [...items] }]
  }
  if (groupBy.mode === 'LIST_GROUP') {
    // Prefer useListGroupsStore.partition in the page; this is a safe fallback.
    return [{
      key: ALL_GROUP_KEY,
      label: '清单分组',
      items: [...items]
    }]
  }
  // PERSONAL_CUSTOM: prefer useMineGroupsStore.partition in the page; this is a safe fallback.
  if (groupBy.mode === 'PERSONAL_CUSTOM') {
    return [{
      key: ALL_GROUP_KEY,
      label: '自定义分组',
      items: [...items]
    }]
  }

  const fieldKey = groupBy.fieldKey
  // Custom fields: prefer useCustomFieldsStore.partition in the page.
  if (fieldKey.startsWith('custom:')) {
    return [{
      key: ALL_GROUP_KEY,
      label: '自定义字段',
      items: [...items]
    }]
  }

  const map = new Map<string, TaskItem[]>()

  for (const item of items) {
    if (fieldKey === 'assigneeId') {
      // Multi-assignee: fan-out into each assignee's column (same task ref).
      const ids = resolveAssigneeIds(item)
      if (!ids.length) {
        pushToBucket(map, EMPTY_GROUP_KEY, item)
      } else {
        for (const id of ids) {
          pushToBucket(map, id, item)
        }
      }
      continue
    }
    pushToBucket(map, bucketKeyForItem(item, fieldKey), item)
  }

  if (fieldKey === 'status') {
    const buckets: TaskGroupBucket[] = []
    for (const status of STATUS_ORDER) {
      buckets.push({
        key: status,
        label: BOARD_COLUMN_LABELS[status],
        items: map.get(status) ?? []
      })
      map.delete(status)
    }
    // unexpected statuses
    for (const [key, list] of map) {
      if (key === EMPTY_GROUP_KEY) {
        continue
      }
      buckets.push({ key, label: key, items: list })
    }
    if (map.has(EMPTY_GROUP_KEY)) {
      buckets.push({
        key: EMPTY_GROUP_KEY,
        label: EMPTY_GROUP_LABEL,
        items: map.get(EMPTY_GROUP_KEY) ?? []
      })
    }
    return buckets
  }

  const keys = Array.from(map.keys()).sort((a, b) => {
    if (a === EMPTY_GROUP_KEY) {
      return 1
    }
    if (b === EMPTY_GROUP_KEY) {
      return -1
    }
    const labelA = labelForBucket(fieldKey, a, options)
    const labelB = labelForBucket(fieldKey, b, options)
    return labelA.localeCompare(labelB, 'zh-CN') || a.localeCompare(b)
  })

  return keys.map(key => ({
    key,
    label: key === EMPTY_GROUP_KEY ? EMPTY_GROUP_LABEL : labelForBucket(fieldKey, key, options),
    items: map.get(key) ?? []
  }))
}

function pushToBucket(map: Map<string, TaskItem[]>, key: string, item: TaskItem) {
  const list = map.get(key) ?? []
  list.push(item)
  map.set(key, list)
}

function bucketKeyForItem(
  item: TaskItem,
  fieldKey: 'status' | 'dueTime' | 'assigneeId' | string
): string {
  if (fieldKey.startsWith('custom:')) {
    return EMPTY_GROUP_KEY
  }
  if (fieldKey === 'status') {
    return item.status || EMPTY_GROUP_KEY
  }
  if (fieldKey === 'assigneeId') {
    // Prefer resolveAssigneeIds + fan-out in partitionByGroupBy; single-key fallback for tests.
    const ids = resolveAssigneeIds(item)
    return ids[0] ?? EMPTY_GROUP_KEY
  }
  // dueTime
  if (!item.dueTime) {
    return EMPTY_GROUP_KEY
  }
  const d = new Date(item.dueTime)
  if (Number.isNaN(d.getTime())) {
    return EMPTY_GROUP_KEY
  }
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function labelForBucket(
  fieldKey: string,
  key: string,
  options?: PartitionGroupOptions
): string {
  if (fieldKey === 'dueTime') {
    return key
  }
  if (fieldKey === 'assigneeId') {
    return options?.assigneeLabel?.(key)?.trim() || key
  }
  return key
}

/** Apply a drag target to a task clone (local mock). */
export function applyGroupTargetToTask(
  task: TaskItem,
  fieldKey: 'status' | 'dueTime' | 'assigneeId' | string,
  targetKey: string
): TaskItem {
  if (fieldKey.startsWith('custom:')) {
    return task
  }
  if (fieldKey === 'status') {
    if (targetKey === EMPTY_GROUP_KEY || !isBoardStatus(targetKey)) {
      return task
    }
    return { ...task, status: targetKey }
  }
  if (fieldKey === 'assigneeId') {
    const nextId = targetKey === EMPTY_GROUP_KEY ? null : targetKey
    return {
      ...task,
      assigneeId: nextId,
      assigneeIds: nextId ? [nextId] : []
    }
  }
  // dueTime
  if (targetKey === EMPTY_GROUP_KEY) {
    return { ...task, dueTime: null }
  }
  // keep time-of-day if possible; default noon local
  const existing = task.dueTime ? new Date(task.dueTime) : null
  const [y, m, d] = targetKey.split('-').map(Number)
  if (!y || !m || !d) {
    return task
  }
  const next = existing && !Number.isNaN(existing.getTime())
    ? new Date(existing)
    : new Date(y, m - 1, d, 12, 0, 0)
  next.setFullYear(y, m - 1, d)
  return { ...task, dueTime: next.toISOString() }
}
