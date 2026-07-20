import type { TaskItem } from '../../api/app/task'
import {
  EMPTY_GROUP_KEY,
  EMPTY_GROUP_LABEL,
  type TaskGroupBucket
} from './groupByLocal'

/** Session-local until custom-field-api / integrate. */
export const USE_LOCAL_CUSTOM_FIELD = true

export type ListCustomFieldType = 'SINGLE_SELECT'

export interface ListCustomFieldOption {
  id: string
  valueKey: string
  label: string
  rank: number
}

export interface ListCustomField {
  id: string
  listId: string
  name: string
  fieldType: ListCustomFieldType
  rank: number
  options: ListCustomFieldOption[]
}

export type CustomFieldKey = `custom:${string}`

export function isCustomFieldKey(key: string): key is CustomFieldKey {
  return key.startsWith('custom:') && key.length > 'custom:'.length
}

export function toCustomFieldKey(fieldId: string): CustomFieldKey {
  return `custom:${fieldId}`
}

export function parseCustomFieldId(fieldKey: string): string | null {
  if (!isCustomFieldKey(fieldKey)) {
    return null
  }
  return fieldKey.slice('custom:'.length)
}

export function valueStorageKey(listId: string, itemId: string, fieldId: string): string {
  return `${listId}:${itemId}:${fieldId}`
}

let seq = 1
export function nextLocalId(prefix: string): string {
  seq += 1
  return `local-${prefix}-${Date.now()}-${seq}`
}

export function createSeedField(listId: string): ListCustomField {
  const fieldId = nextLocalId('field')
  const options: ListCustomFieldOption[] = [
    { id: nextLocalId('opt'), valueKey: 'high', label: '高', rank: 0 },
    { id: nextLocalId('opt'), valueKey: 'medium', label: '中', rank: 1 },
    { id: nextLocalId('opt'), valueKey: 'low', label: '低', rank: 2 }
  ]
  return {
    id: fieldId,
    listId,
    name: '优先级',
    fieldType: 'SINGLE_SELECT',
    rank: 0,
    options
  }
}

export function partitionByCustomField(
  items: TaskItem[],
  field: ListCustomField,
  valueOf: (itemId: string) => string | null
): TaskGroupBucket[] {
  const sorted = [...field.options].sort((a, b) => a.rank - b.rank || a.id.localeCompare(b.id))
  const buckets: TaskGroupBucket[] = sorted.map(opt => ({
    key: opt.valueKey,
    label: opt.label,
    items: []
  }))
  const empty: TaskItem[] = []
  const byKey = new Map(buckets.map(b => [b.key, b.items]))

  for (const item of items) {
    const optionId = valueOf(item.id)
    if (!optionId) {
      empty.push(item)
      continue
    }
    const opt = field.options.find(o => o.id === optionId)
    if (!opt) {
      empty.push(item)
      continue
    }
    const list = byKey.get(opt.valueKey)
    if (list) {
      list.push(item)
    } else {
      empty.push(item)
    }
  }

  buckets.push({
    key: EMPTY_GROUP_KEY,
    label: EMPTY_GROUP_LABEL,
    items: empty
  })
  return buckets
}
