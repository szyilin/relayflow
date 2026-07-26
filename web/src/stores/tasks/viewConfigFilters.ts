import type { TaskItem, TaskItemStatus } from '../../api/app/task'
import type { TaskViewContextType, TaskViewFilterClause, TaskViewFilterField, TaskViewFilterOp } from './viewConfigLocal'

/** Sentinel for "current user" in person-field filter values. */
export const FILTER_ME = '__ME__'

export const FILTER_FIELDS: { field: TaskViewFilterField, label: string }[] = [
  { field: 'status', label: '状态' },
  { field: 'assigneeId', label: '负责人' },
  { field: 'creatorId', label: '创建人' },
  { field: 'assignerId', label: '分配人' },
  { field: 'dueTime', label: '截止时间' },
  { field: 'createTime', label: '创建时间' },
  { field: 'title', label: '标题' }
]

const OPS_BY_FIELD: Record<TaskViewFilterField, { op: TaskViewFilterOp, label: string }[]> = {
  status: [
    { op: 'IN', label: '包含' },
    { op: 'EQ', label: '等于' }
  ],
  assigneeId: [
    { op: 'CONTAINS', label: '包含' },
    { op: 'NOT_CONTAINS', label: '不包含' },
    { op: 'EQ', label: '等于' },
    { op: 'NE', label: '不等于' },
    { op: 'IS_EMPTY', label: '为空' },
    { op: 'IS_NOT_EMPTY', label: '不为空' }
  ],
  creatorId: [
    { op: 'CONTAINS', label: '包含' },
    { op: 'NOT_CONTAINS', label: '不包含' },
    { op: 'EQ', label: '等于' },
    { op: 'NE', label: '不等于' },
    { op: 'IS_EMPTY', label: '为空' },
    { op: 'IS_NOT_EMPTY', label: '不为空' }
  ],
  assignerId: [
    { op: 'CONTAINS', label: '包含' },
    { op: 'NOT_CONTAINS', label: '不包含' },
    { op: 'EQ', label: '等于' },
    { op: 'NE', label: '不等于' },
    { op: 'IS_EMPTY', label: '为空' },
    { op: 'IS_NOT_EMPTY', label: '不为空' }
  ],
  dueTime: [
    { op: 'BEFORE', label: '早于' },
    { op: 'AFTER', label: '晚于' },
    { op: 'ON_OR_BEFORE', label: '不晚于' },
    { op: 'ON_OR_AFTER', label: '不早于' },
    { op: 'IS_EMPTY', label: '为空' },
    { op: 'IS_NOT_EMPTY', label: '不为空' }
  ],
  createTime: [
    { op: 'BEFORE', label: '早于' },
    { op: 'AFTER', label: '晚于' },
    { op: 'ON_OR_BEFORE', label: '不晚于' },
    { op: 'ON_OR_AFTER', label: '不早于' },
    { op: 'IS_EMPTY', label: '为空' },
    { op: 'IS_NOT_EMPTY', label: '不为空' }
  ],
  title: [
    { op: 'CONTAINS', label: '包含' },
    { op: 'NOT_CONTAINS', label: '不包含' },
    { op: 'EQ', label: '等于' }
  ]
}

export function opsForField(field: TaskViewFilterField) {
  return OPS_BY_FIELD[field] ?? []
}

export function defaultOpForField(field: TaskViewFilterField): TaskViewFilterOp {
  return opsForField(field)[0]?.op ?? 'EQ'
}

export function opNeedsValue(op: TaskViewFilterOp): boolean {
  return op !== 'IS_EMPTY' && op !== 'IS_NOT_EMPTY'
}

export function seedFiltersForContext(contextType: TaskViewContextType): TaskViewFilterClause[] {
  switch (contextType) {
    case 'MINE':
      return [
        { field: 'assigneeId', op: 'CONTAINS', values: [FILTER_ME] },
        { field: 'status', op: 'IN', values: ['TODO'] }
      ]
    case 'CREATED':
      return [
        { field: 'creatorId', op: 'EQ', values: [FILTER_ME] }
      ]
    case 'ASSIGNED_BY_ME':
      return [
        { field: 'assignerId', op: 'EQ', values: [FILTER_ME] },
        { field: 'assigneeId', op: 'NOT_CONTAINS', values: [FILTER_ME] }
      ]
    case 'COMPLETED':
      return [
        { field: 'status', op: 'IN', values: ['DONE'] }
      ]
    default:
      return []
  }
}

export function normalizeFilterClause(raw: unknown): TaskViewFilterClause | null {
  if (!raw || typeof raw !== 'object') {
    return null
  }
  const row = raw as Record<string, unknown>
  const field = String(row.field ?? '') as TaskViewFilterField
  const op = String(row.op ?? '') as TaskViewFilterOp
  if (!FILTER_FIELDS.some(f => f.field === field)) {
    return null
  }
  if (!opsForField(field).some(o => o.op === op)) {
    return null
  }
  const values = Array.isArray(row.values)
    ? row.values.map(v => String(v))
    : []
  return { field, op, values }
}

export function normalizeFilters(raw: unknown): TaskViewFilterClause[] {
  if (!Array.isArray(raw)) {
    return []
  }
  return raw.map(normalizeFilterClause).filter((c): c is TaskViewFilterClause => !!c)
}

function resolvePersonId(value: string, meUserId: string | null | undefined): string {
  if (value === FILTER_ME) {
    return meUserId ?? ''
  }
  return value
}

function assigneeIdsOf(item: TaskItem): string[] {
  if (item.assigneeIds?.length) {
    return item.assigneeIds.map(String)
  }
  return item.assigneeId ? [String(item.assigneeId)] : []
}

function personValues(item: TaskItem, field: TaskViewFilterField): string[] {
  switch (field) {
    case 'assigneeId':
      return assigneeIdsOf(item)
    case 'creatorId':
      return item.creatorId ? [String(item.creatorId)] : []
    case 'assignerId':
      return item.assignerId ? [String(item.assignerId)] : []
    default:
      return []
  }
}

function dayStartMs(isoOrDate: string): number | null {
  if (!isoOrDate) {
    return null
  }
  const d = new Date(isoOrDate.length <= 10 ? `${isoOrDate}T00:00:00` : isoOrDate)
  const t = d.getTime()
  return Number.isFinite(t) ? t : null
}

function matchPerson(
  ids: string[],
  op: TaskViewFilterOp,
  values: string[],
  meUserId: string | null | undefined
): boolean {
  const targets = values.map(v => resolvePersonId(v, meUserId)).filter(Boolean)
  switch (op) {
    case 'IS_EMPTY':
      return ids.length === 0
    case 'IS_NOT_EMPTY':
      return ids.length > 0
    case 'CONTAINS':
      return targets.some(t => ids.includes(t))
    case 'NOT_CONTAINS':
      return !targets.some(t => ids.includes(t))
    case 'EQ':
      return targets.length > 0 && ids.length === 1 && ids[0] === targets[0]
    case 'NE':
      return targets.length > 0 && !(ids.length === 1 && ids[0] === targets[0])
    default:
      return true
  }
}

function matchStatus(item: TaskItem, op: TaskViewFilterOp, values: string[]): boolean {
  const allowed = values as TaskItemStatus[]
  if (op === 'EQ') {
    return allowed.length > 0 && item.status === allowed[0]
  }
  if (op === 'IN') {
    return allowed.length === 0 || allowed.includes(item.status)
  }
  return true
}

function matchTime(
  raw: string | null | undefined,
  op: TaskViewFilterOp,
  values: string[]
): boolean {
  const empty = !raw
  if (op === 'IS_EMPTY') {
    return empty
  }
  if (op === 'IS_NOT_EMPTY') {
    return !empty
  }
  if (empty) {
    return false
  }
  const itemMs = dayStartMs(raw)
  const boundMs = dayStartMs(values[0] ?? '')
  if (itemMs == null || boundMs == null) {
    return false
  }
  switch (op) {
    case 'BEFORE':
      return itemMs < boundMs
    case 'AFTER':
      return itemMs > boundMs
    case 'ON_OR_BEFORE':
      return itemMs <= boundMs
    case 'ON_OR_AFTER':
      return itemMs >= boundMs
    default:
      return true
  }
}

function matchTitle(title: string, op: TaskViewFilterOp, values: string[]): boolean {
  const needle = (values[0] ?? '').toLowerCase()
  const hay = (title || '').toLowerCase()
  switch (op) {
    case 'CONTAINS':
      return !needle || hay.includes(needle)
    case 'NOT_CONTAINS':
      return !needle || !hay.includes(needle)
    case 'EQ':
      return hay === needle
    default:
      return true
  }
}

export function matchFilterClause(
  item: TaskItem,
  clause: TaskViewFilterClause,
  meUserId: string | null | undefined
): boolean {
  if (opNeedsValue(clause.op) && !(clause.values?.length)) {
    // Incomplete clause: do not exclude rows
    return true
  }
  switch (clause.field) {
    case 'status':
      return matchStatus(item, clause.op, clause.values)
    case 'assigneeId':
    case 'creatorId':
    case 'assignerId':
      return matchPerson(personValues(item, clause.field), clause.op, clause.values, meUserId)
    case 'dueTime':
      return matchTime(item.dueTime, clause.op, clause.values)
    case 'createTime':
      return matchTime(item.createTime, clause.op, clause.values)
    case 'title':
      return matchTitle(item.title, clause.op, clause.values)
    default:
      return true
  }
}

export function applyFilterClauses(
  items: TaskItem[],
  filters: TaskViewFilterClause[],
  meUserId: string | null | undefined
): TaskItem[] {
  if (!filters.length) {
    return items
  }
  return items.filter(item =>
    filters.every(clause => matchFilterClause(item, clause, meUserId)))
}
