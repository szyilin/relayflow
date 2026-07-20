import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { TaskItem } from '../../api/app/task'
import { groupMoveTask } from '../../api/app/task'
import {
  createListField as createListFieldApi,
  createListFieldOption,
  deleteListField as deleteListFieldApi,
  deleteListFieldOption,
  listListFields,
  putListFieldValue,
  updateListField,
  updateListFieldOption,
  type ListCustomField,
  type ListCustomFieldOption
} from '../../api/app/taskListField'
import type { TaskGroupBucket } from './groupByLocal'
import { EMPTY_GROUP_KEY } from './groupByLocal'
import {
  parseCustomFieldId,
  partitionByCustomField,
  toCustomFieldKey,
  valueStorageKey
} from './customFieldsLocal'

export {
  isCustomFieldKey,
  parseCustomFieldId,
  toCustomFieldKey
} from './customFieldsLocal'
export type { ListCustomField, ListCustomFieldOption }

interface ListFieldCache {
  fields: ListCustomField[]
  /** optionId or null when explicitly cleared; missing key = empty */
  values: Record<string, string | null>
  loaded: boolean
}

function emptyCache(): ListFieldCache {
  return { fields: [], values: {}, loaded: false }
}

export const useCustomFieldsStore = defineStore('customFields', () => {
  const byListId = ref<Record<string, ListFieldCache>>({})
  const activeListId = ref<string | null>(null)
  const loading = ref(false)

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
  }

  function applyList(listId: string, result: Awaited<ReturnType<typeof listListFields>>) {
    const values: Record<string, string | null> = {}
    for (const v of result.values) {
      values[valueStorageKey(listId, v.itemId, v.fieldId)] = v.optionId
    }
    byListId.value = {
      ...byListId.value,
      [listId]: {
        fields: result.fields,
        values,
        loaded: true
      }
    }
  }

  async function fetchList(listId: string | null | undefined, force = false) {
    if (!listId) {
      return
    }
    if (loading.value) {
      return
    }
    const cache = ensureCache(listId)
    if (cache.loaded && !force) {
      return
    }
    loading.value = true
    try {
      applyList(listId, await listListFields(listId))
    } finally {
      loading.value = false
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

  function patchValueLocal(listId: string, itemId: string, fieldId: string, optionId: string | null) {
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

  async function setValue(listId: string, itemId: string, fieldId: string, optionId: string | null) {
    const prev = getValue(listId, itemId, fieldId)
    patchValueLocal(listId, itemId, fieldId, optionId)
    try {
      await putListFieldValue({ listId, itemId, fieldId, optionId })
    } catch (e) {
      patchValueLocal(listId, itemId, fieldId, prev)
      throw e
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

  async function createField(name: string, optionLabels: string[]): Promise<ListCustomField> {
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
    const field = await createListFieldApi({
      listId,
      name: trimmed,
      options: labels.map(label => ({ label }))
    })
    const cache = ensureCache(listId)
    byListId.value = {
      ...byListId.value,
      [listId]: {
        ...cache,
        loaded: true,
        fields: [...cache.fields.filter(f => f.id !== field.id), field]
      }
    }
    return field
  }

  async function renameField(fieldId: string, name: string) {
    const listId = activeListId.value
    if (!listId) {
      throw new Error('TASK_LIST_FIELD_FORBIDDEN')
    }
    const trimmed = name.trim()
    if (!trimmed) {
      throw new Error('TASK_LIST_FIELD_NAME_EMPTY')
    }
    await updateListField({ id: fieldId, name: trimmed })
    const cache = ensureCache(listId)
    byListId.value = {
      ...byListId.value,
      [listId]: {
        ...cache,
        fields: cache.fields.map(f => (f.id === fieldId ? { ...f, name: trimmed } : f))
      }
    }
  }

  async function deleteField(fieldId: string) {
    const listId = activeListId.value
    if (!listId) {
      throw new Error('TASK_LIST_FIELD_FORBIDDEN')
    }
    await deleteListFieldApi(fieldId)
    const cache = ensureCache(listId)
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

  async function addOption(fieldId: string, label: string) {
    const listId = activeListId.value
    if (!listId) {
      throw new Error('TASK_LIST_FIELD_FORBIDDEN')
    }
    const trimmed = label.trim()
    if (!trimmed) {
      throw new Error('TASK_LIST_FIELD_OPTION_EMPTY')
    }
    const option = await createListFieldOption({ fieldId, label: trimmed })
    const cache = ensureCache(listId)
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

  async function renameOption(fieldId: string, optionId: string, label: string) {
    const listId = activeListId.value
    if (!listId) {
      throw new Error('TASK_LIST_FIELD_FORBIDDEN')
    }
    const trimmed = label.trim()
    if (!trimmed) {
      throw new Error('TASK_LIST_FIELD_OPTION_EMPTY')
    }
    await updateListFieldOption({ id: optionId, label: trimmed })
    const cache = ensureCache(listId)
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

  async function deleteOption(fieldId: string, optionId: string) {
    const listId = activeListId.value
    if (!listId) {
      throw new Error('TASK_LIST_FIELD_FORBIDDEN')
    }
    const cache = ensureCache(listId)
    const field = cache.fields.find(f => f.id === fieldId)
    if (field && field.options.length <= 2) {
      throw new Error('TASK_LIST_FIELD_OPTIONS_MIN')
    }
    await deleteListFieldOption(optionId)
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

  async function moveTask(taskId: string, fieldKey: string, targetKey: string) {
    const listId = activeListId.value
    const field = fieldByGroupKey(fieldKey)
    if (!listId || !field) {
      throw new Error('TASK_LIST_FIELD_NOT_FOUND')
    }
    const prev = getValue(listId, taskId, field.id)
    let nextOptionId: string | null = null
    let apiValue: string | null = null
    if (targetKey !== EMPTY_GROUP_KEY) {
      const opt = field.options.find(o => o.valueKey === targetKey)
      if (!opt) {
        throw new Error('TASK_LIST_FIELD_OPTION_NOT_FOUND')
      }
      nextOptionId = opt.id
      apiValue = opt.valueKey
    }
    patchValueLocal(listId, taskId, field.id, nextOptionId)
    try {
      await groupMoveTask({
        id: taskId,
        fieldKey,
        value: apiValue,
        listId
      })
    } catch (e) {
      patchValueLocal(listId, taskId, field.id, prev)
      throw e
    }
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
    loading,
    setActiveList,
    fetchList,
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
