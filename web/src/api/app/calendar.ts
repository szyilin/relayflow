import { del, get, post, put } from '../request'

export type CalendarType = 'PRIMARY' | 'OWNED' | 'SHARED'
export type CalendarPermission = 'READ'
export type CalendarShareDirection = 'OUTGOING' | 'INCOMING'
export type CalendarEventStatus = 'CONFIRMED' | 'CANCELLED'
export type CalendarAttendeeRole = 'ORGANIZER' | 'ATTENDEE'
export type CalendarAttendeeResponse = 'NEEDS_ACTION' | 'ACCEPTED' | 'DECLINED'
export type CalendarEditScope = 'THIS' | 'ALL'

export interface CalendarItem {
  id: string
  name: string
  color: string
  description?: string | null
  type: CalendarType
  ownerUserId?: string
  permission?: CalendarPermission
}

export interface CalendarShare {
  id: string
  calendarId: string
  calendarName: string
  calendarColor: string
  granteeUserId: string
  granteeNickname?: string
  ownerUserId: string
  ownerNickname?: string
  permission: CalendarPermission
  direction: CalendarShareDirection
}

export interface CalendarAttendee {
  userId: string
  role: CalendarAttendeeRole
  response: CalendarAttendeeResponse
  nickname?: string
}

export interface CalendarEvent {
  id: string
  calendarId: string
  calendarColor: string
  calendarName: string
  title: string
  description?: string | null
  startTime: string
  endTime: string
  allDay: boolean
  organizerId: string
  remindBeforeMinutes?: number | null
  allDayRemindTime?: string | null
  status: CalendarEventStatus
  viewerRole: CalendarAttendeeRole
  attendees: CalendarAttendee[]
  invitedOnly: boolean
  rrule?: string | null
  masterEventId?: string
  instanceStart?: string
  isException?: boolean
  recurring?: boolean
}

export interface CalendarEventCreatePayload {
  calendarId: string
  title: string
  description?: string | null
  startTime: string
  endTime: string
  allDay: boolean
  remindBeforeMinutes?: number | null
  allDayRemindTime?: string | null
  attendeeUserIds?: string[]
  rrule?: string | null
}

export interface CalendarEventUpdatePayload extends CalendarEventCreatePayload {
  id: string
  editScope?: CalendarEditScope
  instanceStart?: string
}

export interface CalendarEventDeleteOptions {
  editScope?: CalendarEditScope
  instanceStart?: string
}

export interface CalendarEventReschedulePayload {
  id: string
  startTime: string
  endTime: string
  editScope?: CalendarEditScope
  instanceStart?: string
}

function asId(value: string | number | undefined): string {
  return String(value ?? '')
}

function normalizeCalendar(item: CalendarItem & { id?: string | number, ownerUserId?: string | number }): CalendarItem {
  return {
    id: asId(item.id),
    name: item.name,
    color: item.color,
    description: item.description ?? null,
    type: item.type,
    ownerUserId: item.ownerUserId != null ? asId(item.ownerUserId) : undefined,
    permission: item.permission
  }
}

function normalizeShare(item: CalendarShare & {
  id?: string | number
  calendarId?: string | number
  granteeUserId?: string | number
  ownerUserId?: string | number
}): CalendarShare {
  return {
    id: asId(item.id),
    calendarId: asId(item.calendarId),
    calendarName: item.calendarName,
    calendarColor: item.calendarColor,
    granteeUserId: asId(item.granteeUserId),
    granteeNickname: item.granteeNickname,
    ownerUserId: asId(item.ownerUserId),
    ownerNickname: item.ownerNickname,
    permission: item.permission ?? 'READ',
    direction: item.direction
  }
}

function asIso(value: unknown): string {
  if (typeof value === 'string') {
    return value
  }
  if (value instanceof Date) {
    return value.toISOString()
  }
  return String(value ?? '')
}

function normalizeEvent(item: CalendarEvent & {
  id?: string | number
  calendarId?: string | number
  organizerId?: string | number
  masterEventId?: string | number
  startTime?: unknown
  endTime?: unknown
  instanceStart?: unknown
}): CalendarEvent {
  return {
    id: asId(item.id),
    calendarId: asId(item.calendarId),
    calendarColor: item.calendarColor,
    calendarName: item.calendarName,
    title: item.title,
    description: item.description ?? null,
    startTime: asIso(item.startTime),
    endTime: asIso(item.endTime),
    allDay: Boolean(item.allDay),
    organizerId: asId(item.organizerId),
    remindBeforeMinutes: item.remindBeforeMinutes ?? null,
    allDayRemindTime: item.allDayRemindTime ?? null,
    status: item.status,
    viewerRole: item.viewerRole,
    attendees: (item.attendees ?? []).map(a => ({
      userId: asId(a.userId),
      role: a.role,
      response: a.response,
      nickname: a.nickname
    })),
    invitedOnly: Boolean(item.invitedOnly),
    rrule: item.rrule ?? null,
    masterEventId: item.masterEventId != null ? asId(item.masterEventId) : undefined,
    instanceStart: item.instanceStart != null ? asIso(item.instanceStart) : undefined,
    isException: item.isException ?? undefined,
    recurring: item.recurring ?? undefined
  }
}

export function eventKey(event: Pick<CalendarEvent, 'id' | 'instanceStart' | 'startTime'>): string {
  return `${event.id}:${event.instanceStart ?? event.startTime}`
}

export async function listCalendars(): Promise<CalendarItem[]> {
  const data = await get<CalendarItem[]>('/app-api/calendar/calendar/list')
  return (data ?? []).map(item => normalizeCalendar(item))
}

export async function createCalendar(payload: {
  name: string
  color: string
  description?: string | null
}): Promise<string> {
  const id = await post<number | string>('/app-api/calendar/calendar/create', payload)
  return asId(id)
}

export async function updateCalendar(payload: {
  id: string
  name: string
  color: string
  description?: string | null
}): Promise<boolean> {
  return put<boolean>('/app-api/calendar/calendar/update', payload)
}

export async function deleteCalendar(id: string): Promise<boolean> {
  return del<boolean>('/app-api/calendar/calendar/delete', { params: { id } })
}

export async function listShares(): Promise<CalendarShare[]> {
  const data = await get<CalendarShare[]>('/app-api/calendar/share/list')
  return (data ?? []).map(item => normalizeShare(item))
}

export async function createShare(payload: {
  calendarId: string
  granteeUserId: string
  permission?: CalendarPermission
}): Promise<string> {
  const id = await post<number | string>('/app-api/calendar/share/create', {
    calendarId: payload.calendarId,
    granteeUserId: payload.granteeUserId,
    permission: payload.permission ?? 'READ'
  })
  return asId(id)
}

export async function deleteShare(id: string): Promise<boolean> {
  return del<boolean>('/app-api/calendar/share/delete', { params: { id } })
}

export async function listEvents(params: {
  from: string
  to: string
  calendarIds?: string[]
}): Promise<CalendarEvent[]> {
  const data = await get<CalendarEvent[]>('/app-api/calendar/event/list', {
    params: {
      from: params.from,
      to: params.to,
      calendarIds: params.calendarIds?.length ? params.calendarIds.join(',') : undefined
    }
  })
  return (data ?? []).map(item => normalizeEvent(item))
}

export async function getEvent(id: string): Promise<CalendarEvent> {
  const data = await get<CalendarEvent>('/app-api/calendar/event/get', { params: { id } })
  return normalizeEvent(data)
}

export async function createEvent(payload: CalendarEventCreatePayload): Promise<string> {
  const id = await post<number | string>('/app-api/calendar/event/create', payload)
  return asId(id)
}

export async function updateEvent(payload: CalendarEventUpdatePayload): Promise<boolean> {
  return put<boolean>('/app-api/calendar/event/update', payload)
}

export async function deleteEvent(id: string, opts?: CalendarEventDeleteOptions): Promise<boolean> {
  return del<boolean>('/app-api/calendar/event/delete', {
    params: {
      id,
      editScope: opts?.editScope,
      instanceStart: opts?.instanceStart
    }
  })
}

export async function rescheduleEvent(payload: CalendarEventReschedulePayload): Promise<boolean> {
  return put<boolean>('/app-api/calendar/event/reschedule', payload)
}

export async function respondEvent(id: string, response: 'ACCEPTED' | 'DECLINED'): Promise<boolean> {
  return put<boolean>('/app-api/calendar/event/respond', { id, response })
}
