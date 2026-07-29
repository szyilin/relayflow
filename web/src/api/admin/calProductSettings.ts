import { del, get, post, put } from '../request'

export interface CalAllStaffScopeItem {
  scopeType: 'ALL' | 'USER' | 'DEPT'
  scopeId?: string | number | null
  scopeLabel?: string | null
}

export interface CalAllStaffCalendar {
  id: string
  name: string
  color: string
  description?: string | null
  ownerUserId?: string
  scopes: CalAllStaffScopeItem[]
}

export interface CalProductSettings {
  allowMemberCreateOwnedCalendar: boolean
  allStaffCalendars: CalAllStaffCalendar[]
}

export function defaultCalProductSettings(): CalProductSettings {
  return {
    allowMemberCreateOwnedCalendar: true,
    allStaffCalendars: []
  }
}

export function cloneCalProductSettings(src: CalProductSettings): CalProductSettings {
  return {
    allowMemberCreateOwnedCalendar: src.allowMemberCreateOwnedCalendar,
    allStaffCalendars: src.allStaffCalendars.map(c => ({
      ...c,
      scopes: c.scopes.map(s => ({ ...s }))
    }))
  }
}

function asId(value: string | number | undefined | null): string {
  return String(value ?? '')
}

function normalizeScope(item: CalAllStaffScopeItem): CalAllStaffScopeItem {
  return {
    scopeType: item.scopeType,
    scopeId: item.scopeId != null && item.scopeType !== 'ALL' ? asId(item.scopeId) : null,
    scopeLabel: item.scopeLabel ?? null
  }
}

function normalizeAllStaff(item: CalAllStaffCalendar & { id?: string | number, ownerUserId?: string | number }): CalAllStaffCalendar {
  return {
    id: asId(item.id),
    name: item.name,
    color: item.color,
    description: item.description ?? null,
    ownerUserId: item.ownerUserId != null ? asId(item.ownerUserId) : undefined,
    scopes: (item.scopes ?? []).map(normalizeScope)
  }
}

function normalizeSettings(data: CalProductSettings | null | undefined): CalProductSettings {
  const base = defaultCalProductSettings()
  if (!data) {
    return base
  }
  return {
    allowMemberCreateOwnedCalendar: data.allowMemberCreateOwnedCalendar ?? true,
    allStaffCalendars: (data.allStaffCalendars ?? []).map(normalizeAllStaff)
  }
}

export async function getCalProductSettings(): Promise<CalProductSettings> {
  const data = await get<CalProductSettings>('/admin-api/calendar/product-settings')
  return normalizeSettings(data)
}

export async function updateCalProductSettings(payload: {
  allowMemberCreateOwnedCalendar: boolean
}): Promise<CalProductSettings> {
  const data = await put<CalProductSettings>('/admin-api/calendar/product-settings', payload)
  return normalizeSettings(data)
}

export async function createAllStaffCalendar(payload: {
  name: string
  color: string
  description?: string | null
  scopes: CalAllStaffScopeItem[]
}): Promise<string> {
  const id = await post<number | string>('/admin-api/calendar/product-settings/all-staff', {
    name: payload.name,
    color: payload.color,
    description: payload.description ?? null,
    scopes: payload.scopes.map(s => ({
      scopeType: s.scopeType,
      scopeId: s.scopeType === 'ALL' ? null : Number(s.scopeId)
    }))
  })
  return asId(id)
}

export async function updateAllStaffCalendar(payload: {
  id: string
  name: string
  color: string
  description?: string | null
  scopes: CalAllStaffScopeItem[]
}): Promise<boolean> {
  return put<boolean>('/admin-api/calendar/product-settings/all-staff', {
    id: payload.id,
    name: payload.name,
    color: payload.color,
    description: payload.description ?? null,
    scopes: payload.scopes.map(s => ({
      scopeType: s.scopeType,
      scopeId: s.scopeType === 'ALL' ? null : Number(s.scopeId)
    }))
  })
}

export async function deleteAllStaffCalendar(id: string): Promise<boolean> {
  return del<boolean>('/admin-api/calendar/product-settings/all-staff', { params: { id } })
}
