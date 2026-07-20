import type { TaskItem, TaskItemStatus } from '../../api/app/task'
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

const STATUS_ORDER: TaskItemStatus[] = ['TODO', 'IN_PROGRESS', 'DONE']

export function partitionByGroupBy(
  items: TaskItem[],
  groupBy: TaskViewGroupBy
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
    const key = bucketKeyForItem(item, fieldKey)
    const list = map.get(key) ?? []
    list.push(item)
    map.set(key, list)
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
    return a.localeCompare(b)
  })

  return keys.map(key => ({
    key,
    label: key === EMPTY_GROUP_KEY ? EMPTY_GROUP_LABEL : labelForBucket(fieldKey, key),
    items: map.get(key) ?? []
  }))
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
    return item.assigneeId ? String(item.assigneeId) : EMPTY_GROUP_KEY
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

function labelForBucket(fieldKey: string, key: string): string {
  if (fieldKey === 'dueTime') {
    return key
  }
  if (fieldKey === 'assigneeId') {
    return `负责人 ${key}`
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
