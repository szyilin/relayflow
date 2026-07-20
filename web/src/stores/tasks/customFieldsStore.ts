import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { TaskItem } from '../../api/app/task'
import type { TaskGroupBucket } from './groupByLocal'
import { EMPTY_GROUP_KEY } from './groupByLocal'
import {
  USE_LOCAL_CUSTOM_FIELD,
  createSeedField,
  nextLocalId,
  parseCustomFieldId,
  partitionByCustomField,
  toCustomFieldKey,
  valueStorageKey,
  type ListCustomField,
  type ListCustomFieldOption
} from './customFieldsLocal'

export {
  USE_LOCAL_CUSTOM_FIELD,
  isCustomFieldKey,
  parseCustomFieldId,
  toCustomFieldKey
} from './customFieldsLocal'
export type { ListCustomField, ListCustomFieldOption }

interface ListFieldCache {
  fields: ListCustomField[]
  /** optionId or null when explicitly cleared; missing key = empty */
  values: Record<string, string | null>
  seeded: boolean
}

function emptyCache(): ListFieldCache {
  return { fields: [], values: {}, seeded: false }
}

export const useCustomFieldsStore = defineStore('customFields', () => {
  const byListId = ref<Record<string, ListFieldCache>>({})
  const activeListId = ref<string | null>(null)

  function ensureCache(listId: string): ListFieldCache {
    if (!byListId.value[listId]) {
      byListId.value = {
        ...byListId.value,
        [listId]: emptyCache()
      }
    }
    return byListId.value[listId]!
  }

  function setActiveList(listId: string | null) {
    activeListId.value = listId
    if (listId && USE_LOCAL_CUSTOM_FIELD) {
      ensureSeed(listId)
    }
  }

  function ensureSeed(listId: string) {
    const cache = ensureCache(listId)
    if (cache.seeded) {
      return
    }
    byListId.value = {
      ...byListId.value,
      [listId]: {
        fields: [createSeedField(listId)],
        values: { ...cache.values },
        seeded: true
      }
    }
  }

  const activeCache = computed(() => {
    const id = activeListId.value
    if (!id) {
      return emptyCache()
    }
    return ensureCache(id)
  })

  const fields = computed(() =>
    [...activeCache.value.fields].sort((a, b) => a.rank - b.rank || a.id.localeCompare(b.id))
  )

  function fieldById(fieldId: string): ListCustomField | undefined {
    return activeCache.value.fields.find(f => f.id === fieldId)
  }

  function fieldByGroupKey(fieldKey: string): ListCustomField | undefined {
    const id = parseCustomFieldId(fieldKey)
    return id ? fieldById(id) : undefined
  }

  function getValue(listId: string, itemId: string, fieldId: string): string | null {
    const cache = ensureCache(listId)
    const key = valueStorageKey(listId, itemId, fieldId)
    return cache.values[key] ?? null
  }

  function setValue(listId: string, itemId: string, fieldId: string, optionId: string | null) {
    const cache = ensureCache(listId)
    const key = valueStorageKey(listId, itemId, fieldId)
    byListId.value = {
      ...byListId.value,
      [listId]: {
        ...cache,
        values: { ...cache.values, [key]: optionId }
      }
    }
  }

  function partition(items: TaskItem[], fieldKey: string): TaskGroupBucket[] {
    const listId = activeListId.value
    const field = fieldByGroupKey(fieldKey)
    if (!listId || !field) {
      return [{ key: EMPTY_GROUP_KEY, label: '无分组', items: [...items] }]
    }
    return partitionByCustomField(items, field, itemId => getValue(listId, itemId, field.id))
  }

  function createField(name: string, optionLabels: string[]): ListCustomField {
    const listId = activeListId.value
    if (!listId) {
      throw new Error('TASK_LIST_FIELD_FORBIDDEN')
    }
    const trimmed = name.trim()
    if (!trimmed) {
      throw new Error('TASK_LIST_FIELD_NAME_EMPTY')
    }
    const labels = optionLabels.map(l => l.trim()).filter(Boolean)
    if (labels.length < 2) {
      throw new Error('TASK_LIST_FIELD_OPTIONS_MIN')
    }
    const cache = ensureCache(listId)
    const options: ListCustomFieldOption[] = labels.map((label, rank) => ({
      id: nextLocalId('opt'),
      valueKey: `opt_${rank}_${nextLocalId('vk')}`,
      label,
      rank
    }))
    const field: ListCustomField = {
      id: nextLocalId('field'),
      listId,
      name: trimmed,
      fieldType: 'SINGLE_SELECT',
      rank: cache.fields.length,
      options
    }
    byListId.value = {
      ...byListId.value,
      [listId]: {
        ...cache,
        seeded: true,
        fields: [...cache.fields, field]
      }
    }
    return field
  }

  function renameField(fieldId: string, name: string) {
    const listId = activeListId.value
    if (!listId) {
      throw new Error('TASK_LIST_FIELD_FORBIDDEN')
    }
    const trimmed = name.trim()
    if (!trimmed) {
      throw new Error('TASK_LIST_FIELD_NAME_EMPTY')
    }
    const cache = ensureCache(listId)
    if (!cache.fields.some(f => f.id === fieldId)) {
      throw new Error('TASK_LIST_FIELD_NOT_FOUND')
    }
    byListId.value = {
      ...byListId.value,
      [listId]: {
        ...cache,
        fields: cache.fields.map(f => (f.id === fieldId ? { ...f, name: trimmed } : f))
      }
    }
  }

  function deleteField(fieldId: string) {
    const listId = activeListId.value
    if (!listId) {
      throw new Error('TASK_LIST_FIELD_FORBIDDEN')
    }
    const cache = ensureCache(listId)
    if (!cache.fields.some(f => f.id === fieldId)) {
      throw new Error('TASK_LIST_FIELD_NOT_FOUND')
    }
    const prefix = `${listId}:`
    const suffix = `:${fieldId}`
    const nextValues = { ...cache.values }
    for (const key of Object.keys(nextValues)) {
      if (key.startsWith(prefix) && key.endsWith(suffix)) {
        delete nextValues[key]
      }
    }
    byListId.value = {
      ...byListId.value,
      [listId]: {
        ...cache,
        fields: cache.fields.filter(f => f.id !== fieldId),
        values: nextValues
      }
    }
  }

  function addOption(fieldId: string, label: string) {
    const listId = activeListId.value
    if (!listId) {
      throw new Error('TASK_LIST_FIELD_FORBIDDEN')
    }
    const trimmed = label.trim()
    if (!trimmed) {
      throw new Error('TASK_LIST_FIELD_OPTION_EMPTY')
    }
    const cache = ensureCache(listId)
    const field = cache.fields.find(f => f.id === fieldId)
    if (!field) {
      throw new Error('TASK_LIST_FIELD_NOT_FOUND')
    }
    const option: ListCustomFieldOption = {
      id: nextLocalId('opt'),
      valueKey: `opt_${field.options.length}_${nextLocalId('vk')}`,
      label: trimmed,
      rank: field.options.length
    }
    byListId.value = {
      ...byListId.value,
      [listId]: {
        ...cache,
        fields: cache.fields.map(f =>
          f.id === fieldId ? { ...f, options: [...f.options, option] } : f
        )
      }
    }
    return option
  }

  function renameOption(fieldId: string, optionId: string, label: string) {
    const listId = activeListId.value
    if (!listId) {
      throw new Error('TASK_LIST_FIELD_FORBIDDEN')
    }
    const trimmed = label.trim()
    if (!trimmed) {
      throw new Error('TASK_LIST_FIELD_OPTION_EMPTY')
    }
    const cache = ensureCache(listId)
    const field = cache.fields.find(f => f.id === fieldId)
    if (!field?.options.some(o => o.id === optionId)) {
      throw new Error('TASK_LIST_FIELD_OPTION_NOT_FOUND')
    }
    byListId.value = {
      ...byListId.value,
      [listId]: {
        ...cache,
        fields: cache.fields.map(f =>
          f.id !== fieldId
            ? f
            : {
                ...f,
                options: f.options.map(o =>
                  o.id === optionId ? { ...o, label: trimmed } : o
                )
              }
        )
      }
    }
  }

  function deleteOption(fieldId: string, optionId: string) {
    const listId = activeListId.value
    if (!listId) {
      throw new Error('TASK_LIST_FIELD_FORBIDDEN')
    }
    const cache = ensureCache(listId)
    const field = cache.fields.find(f => f.id === fieldId)
    if (!field) {
      throw new Error('TASK_LIST_FIELD_NOT_FOUND')
    }
    if (field.options.length <= 2) {
      throw new Error('TASK_LIST_FIELD_OPTIONS_MIN')
    }
    if (!field.options.some(o => o.id === optionId)) {
      throw new Error('TASK_LIST_FIELD_OPTION_NOT_FOUND')
    }
    const nextValues = { ...cache.values }
    for (const [key, val] of Object.entries(nextValues)) {
      if (val === optionId) {
        nextValues[key] = null
      }
    }
    byListId.value = {
      ...byListId.value,
      [listId]: {
        ...cache,
        values: nextValues,
        fields: cache.fields.map(f =>
          f.id !== fieldId
            ? f
            : {
                ...f,
                options: f.options
                  .filter(o => o.id !== optionId)
                  .map((o, rank) => ({ ...o, rank }))
              }
        )
      }
    }
  }

  function moveTask(taskId: string, fieldKey: string, targetKey: string) {
    const listId = activeListId.value
    const field = fieldByGroupKey(fieldKey)
    if (!listId || !field) {
      throw new Error('TASK_LIST_FIELD_NOT_FOUND')
    }
    if (targetKey === EMPTY_GROUP_KEY) {
      setValue(listId, taskId, field.id, null)
      return
    }
    const opt = field.options.find(o => o.valueKey === targetKey)
    if (!opt) {
      throw new Error('TASK_LIST_FIELD_OPTION_NOT_FOUND')
    }
    setValue(listId, taskId, field.id, opt.id)
  }

  function groupByMenuItems(): { fieldKey: string, label: string }[] {
    return fields.value.map(f => ({
      fieldKey: toCustomFieldKey(f.id),
      label: f.name
    }))
  }

  function resetForTenantSwitch() {
    byListId.value = {}
    activeListId.value = null
  }

  return {
    activeListId,
    fields,
    setActiveList,
    ensureSeed,
    fieldById,
    fieldByGroupKey,
    getValue,
    setValue,
    partition,
    createField,
    renameField,
    deleteField,
    addOption,
    renameOption,
    deleteOption,
    moveTask,
    groupByMenuItems,
    resetForTenantSwitch
  }
})
