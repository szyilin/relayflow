import { del, get, post, put } from '../request'

export type CalendarType = 'PRIMARY' | 'OWNED'
export type CalendarEventStatus = 'CONFIRMED' | 'CANCELLED'
export type CalendarAttendeeRole = 'ORGANIZER' | 'ATTENDEE'
export type CalendarAttendeeResponse = 'NEEDS_ACTION' | 'ACCEPTED' | 'DECLINED'

export interface CalendarItem {
  id: string
  name: string
  color: string
  description?: string | null
  type: CalendarType
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
}

export interface CalendarEventUpdatePayload extends CalendarEventCreatePayload {
  id: string
}

function asId(value: string | number | undefined): string {
  return String(value ?? '')
}

function normalizeCalendar(item: CalendarItem & { id?: string | number }): CalendarItem {
  return {
    id: asId(item.id),
    name: item.name,
    color: item.color,
    description: item.description ?? null,
    type: item.type
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
  startTime?: unknown
  endTime?: unknown
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
    invitedOnly: Boolean(item.invitedOnly)
  }
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

export async function deleteEvent(id: string): Promise<boolean> {
  return del<boolean>('/app-api/calendar/event/delete', { params: { id } })
}

export async function respondEvent(id: string, response: 'ACCEPTED' | 'DECLINED'): Promise<boolean> {
  return put<boolean>('/app-api/calendar/event/respond', { id, response })
}
