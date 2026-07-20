import { del, get, post, put } from '../request'

export interface ListCustomFieldOption {
  id: string
  valueKey: string
  label: string
  rank: number
  color?: string | null
}

export interface ListCustomField {
  id: string
  listId: string
  name: string
  fieldKey: string
  fieldType: 'SINGLE_SELECT'
  rank: number
  options: ListCustomFieldOption[]
}

export interface ListCustomFieldValue {
  itemId: string
  fieldId: string
  optionId: string | null
}

export interface ListCustomFieldListResult {
  fields: ListCustomField[]
  values: ListCustomFieldValue[]
}

function strId(v: string | number | null | undefined): string {
  return v == null || v === '' ? '' : String(v)
}

function mapOption(o: {
  id?: string | number
  valueKey?: string
  label?: string
  rank?: number
  color?: string | null
}): ListCustomFieldOption {
  return {
    id: strId(o.id),
    valueKey: o.valueKey ?? '',
    label: o.label ?? '',
    rank: o.rank ?? 0,
    color: o.color
  }
}

function mapField(f: {
  id?: string | number
  listId?: string | number
  name?: string
  fieldKey?: string
  fieldType?: string
  rank?: number
  options?: Array<Parameters<typeof mapOption>[0]>
}, fallbackListId: string): ListCustomField {
  return {
    id: strId(f.id),
    listId: strId(f.listId) || fallbackListId,
    name: f.name ?? '',
    fieldKey: f.fieldKey ?? (f.id != null ? `custom:${f.id}` : ''),
    fieldType: 'SINGLE_SELECT',
    rank: f.rank ?? 0,
    options: (f.options ?? []).map(mapOption).filter(o => o.id)
  }
}

export async function listListFields(listId: string): Promise<ListCustomFieldListResult> {
  const data = await get<{
    fields?: Array<Parameters<typeof mapField>[0]>
    values?: Array<{
      itemId?: string | number
      fieldId?: string | number
      optionId?: string | number | null
    }>
  }>('/app-api/task/list-field/list', { params: { listId } })

  return {
    fields: (data.fields ?? [])
      .map(f => mapField(f, listId))
      .filter(f => f.id),
    values: (data.values ?? []).map(v => ({
      itemId: strId(v.itemId),
      fieldId: strId(v.fieldId),
      optionId: v.optionId == null ? null : strId(v.optionId)
    })).filter(v => v.itemId && v.fieldId)
  }
}

export async function createListField(payload: {
  listId: string
  name: string
  options: Array<{ label: string, valueKey?: string }>
}): Promise<ListCustomField> {
  const data = await post<Parameters<typeof mapField>[0]>('/app-api/task/list-field/create', {
    listId: payload.listId,
    name: payload.name,
    fieldType: 'SINGLE_SELECT',
    options: payload.options
  })
  return mapField(data, payload.listId)
}

export async function updateListField(payload: {
  id: string
  name?: string
  rank?: number
}): Promise<void> {
  await put<boolean>('/app-api/task/list-field/update', payload)
}

export async function deleteListField(id: string): Promise<void> {
  await del<boolean>('/app-api/task/list-field/delete', { params: { id } })
}

export async function createListFieldOption(payload: {
  fieldId: string
  label: string
  valueKey?: string
  rank?: number
}): Promise<ListCustomFieldOption> {
  const data = await post<Parameters<typeof mapOption>[0]>(
    '/app-api/task/list-field/option/create',
    payload
  )
  return mapOption(data)
}

export async function updateListFieldOption(payload: {
  id: string
  label?: string
  rank?: number
  color?: string
}): Promise<void> {
  await put<boolean>('/app-api/task/list-field/option/update', payload)
}

export async function deleteListFieldOption(id: string): Promise<void> {
  await del<boolean>('/app-api/task/list-field/option/delete', { params: { id } })
}

export async function putListFieldValue(payload: {
  listId: string
  itemId: string
  fieldId: string
  optionId: string | null
}): Promise<void> {
  await put<boolean>('/app-api/task/list-field/value', {
    listId: payload.listId,
    itemId: payload.itemId,
    fieldId: payload.fieldId,
    optionId: payload.optionId
  })
}
