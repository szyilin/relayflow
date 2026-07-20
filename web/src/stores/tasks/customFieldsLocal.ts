import type { TaskItem } from '../../api/app/task'
import {
  EMPTY_GROUP_KEY,
  EMPTY_GROUP_LABEL,
  type TaskGroupBucket
} from './groupByLocal'
import type { ListCustomField, ListCustomFieldOption } from '../../api/app/taskListField'

export type { ListCustomField, ListCustomFieldOption }

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
