import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { addDays, addMinutes, endOfDay, isBefore, parseISO, startOfDay } from 'date-fns'
import {
  createCalendar as apiCreateCalendar,
  createEvent as apiCreateEvent,
  createShare as apiCreateShare,
  deleteCalendar as apiDeleteCalendar,
  deleteEvent as apiDeleteEvent,
  deleteShare as apiDeleteShare,
  eventKey,
  getEvent as apiGetEvent,
  listCalendars as apiListCalendars,
  listEvents as apiListEvents,
  listShares as apiListShares,
  respondEvent as apiRespondEvent,
  rescheduleEvent as apiRescheduleEvent,
  updateCalendar as apiUpdateCalendar,
  updateEvent as apiUpdateEvent,
  type CalendarEvent,
  type CalendarEventCreatePayload,
  type CalendarEventDeleteOptions,
  type CalendarEventReschedulePayload,
  type CalendarEventUpdatePayload,
  type CalendarItem,
  type CalendarShare
} from '../api/app/calendar'
import { ApiError } from '../api/request'

export { eventKey }

const VISIBLE_KEY = 'relayflow-calendar-visible-ids-v1'
const DEFAULT_COLORS = ['#3B82F6', '#10B981', '#EC4899', '#F59E0B', '#8B5CF6', '#06B6D4']

function overlaps(event: CalendarEvent, from: Date, to: Date): boolean {
  const start = parseISO(event.startTime)
  const end = parseISO(event.endTime)
  return isBefore(start, to) && isBefore(from, end)
}

function readVisibleIds(): string[] | null {
  try {
    const raw = localStorage.getItem(VISIBLE_KEY)
    if (!raw) {
      return null
    }
    const parsed = JSON.parse(raw) as string[]
    return Array.isArray(parsed) ? parsed.map(String) : null
  } catch {
    return null
  }
}

function toError(error: unknown, fallback: string): Error {
  if (error instanceof ApiError) {
    return new Error(error.message || fallback)
  }
  if (error instanceof Error) {
    return error
  }
  return new Error(fallback)
}

function sameInstant(a?: string | null, b?: string | null): boolean {
  if (a == null || b == null) {
    return a == null && b == null
  }
  if (a === b) {
    return true
  }
  const ta = Date.parse(a)
  const tb = Date.parse(b)
  return Number.isFinite(ta) && Number.isFinite(tb) && ta === tb
}

function findEventIndex(events: CalendarEvent[], id: string, instanceStart?: string): number {
  return events.findIndex((event) => {
    if (event.id !== id) {
      return false
    }
    if (instanceStart) {
      return sameInstant(event.instanceStart ?? event.startTime, instanceStart)
    }
    return !event.instanceStart || sameInstant(event.instanceStart, event.startTime)
  })
}

export const useCalendarStore = defineStore('calendar', () => {
  const calendars = ref<CalendarItem[]>([])
  const events = ref<CalendarEvent[]>([])
  const shares = ref<CalendarShare[]>([])
  const visibleCalendarIds = ref<string[]>([])
  const loading = ref(false)
  const lastRange = ref<{ from: string, to: string } | null>(null)

  const ownedCalendars = computed(() =>
    calendars.value.filter(c => c.type === 'PRIMARY' || c.type === 'OWNED'))
  const sharedCalendars = computed(() =>
    calendars.value.filter(c => c.type === 'SHARED'))
  const primaryCalendar = computed(() => calendars.value.find(c => c.type === 'PRIMARY') ?? null)

  const visibleEvents = computed(() => {
    const visible = new Set(visibleCalendarIds.value)
    return events.value.filter((event) => {
      if (event.status === 'CANCELLED') {
        return false
      }
      if (event.invitedOnly) {
        return true
      }
      return visible.has(event.calendarId)
    })
  })

  function resetForTenantSwitch() {
    calendars.value = []
    events.value = []
    shares.value = []
    visibleCalendarIds.value = []
    loading.value = false
    lastRange.value = null
  }

  function persistVisible() {
    localStorage.setItem(VISIBLE_KEY, JSON.stringify(visibleCalendarIds.value))
  }

  function syncVisibleIds() {
    const saved = readVisibleIds()
    const ids = calendars.value.map(c => c.id)
    if (saved?.length) {
      const filtered = saved.filter(id => ids.includes(id))
      visibleCalendarIds.value = filtered.length ? filtered : ids
    } else {
      visibleCalendarIds.value = ids
    }
    persistVisible()
  }

  async function fetchCalendars() {
    loading.value = true
    try {
      calendars.value = await apiListCalendars()
      syncVisibleIds()
    } catch (error) {
      throw toError(error, '加载日历失败')
    } finally {
      loading.value = false
    }
  }

  async function fetchShares() {
    try {
      shares.value = await apiListShares()
    } catch (error) {
      throw toError(error, '加载共享失败')
    }
  }

  async function createShare(payload: { calendarId: string, granteeUserId: string }) {
    try {
      const id = await apiCreateShare(payload)
      await fetchShares()
      await fetchCalendars()
      return id
    } catch (error) {
      throw toError(error, '共享日历失败')
    }
  }

  async function removeShare(id: string) {
    try {
      await apiDeleteShare(id)
      await fetchShares()
      await fetchCalendars()
      return true
    } catch (error) {
      throw toError(error, '取消共享失败')
    }
  }

  function outgoingSharesForCalendar(calendarId: string): CalendarShare[] {
    return shares.value.filter(s => s.direction === 'OUTGOING' && s.calendarId === calendarId)
  }

  async function fetchEvents(range: { from: string, to: string }) {
    lastRange.value = range
    try {
      events.value = await apiListEvents({
        from: range.from,
        to: range.to
      })
    } catch (error) {
      throw toError(error, '加载日程失败')
    }
  }

  async function refreshLastRange() {
    if (lastRange.value) {
      await fetchEvents(lastRange.value)
    }
  }

  function listInRange(from: Date, to: Date): CalendarEvent[] {
    return visibleEvents.value.filter(event => overlaps(event, from, to))
  }

  function toggleCalendarVisible(id: string, visible: boolean) {
    const set = new Set(visibleCalendarIds.value)
    if (visible) {
      set.add(id)
    } else {
      set.delete(id)
    }
    visibleCalendarIds.value = [...set]
    persistVisible()
  }

  async function createCalendar(payload: { name: string, color: string, description?: string | null }) {
    try {
      const id = await apiCreateCalendar(payload)
      await fetchCalendars()
      if (!visibleCalendarIds.value.includes(id)) {
        visibleCalendarIds.value = [...visibleCalendarIds.value, id]
        persistVisible()
      }
      return id
    } catch (error) {
      throw toError(error, '创建日历失败')
    }
  }

  async function updateCalendar(payload: { id: string, name: string, color: string, description?: string | null }) {
    try {
      await apiUpdateCalendar(payload)
      await fetchCalendars()
      await refreshLastRange()
      return true
    } catch (error) {
      throw toError(error, '更新日历失败')
    }
  }

  async function removeCalendar(id: string) {
    try {
      await apiDeleteCalendar(id)
      await fetchCalendars()
      visibleCalendarIds.value = visibleCalendarIds.value.filter(v => v !== id)
      persistVisible()
      await refreshLastRange()
      return true
    } catch (error) {
      throw toError(error, '删除日历失败')
    }
  }

  async function createEvent(payload: CalendarEventCreatePayload) {
    try {
      const id = await apiCreateEvent(payload)
      await refreshLastRange()
      return id
    } catch (error) {
      throw toError(error, '创建日程失败')
    }
  }

  async function updateEvent(payload: CalendarEventUpdatePayload) {
    try {
      await apiUpdateEvent(payload)
      await refreshLastRange()
      return true
    } catch (error) {
      throw toError(error, '更新日程失败')
    }
  }

  async function removeEvent(id: string, opts?: CalendarEventDeleteOptions) {
    try {
      await apiDeleteEvent(id, opts)
      await refreshLastRange()
      return true
    } catch (error) {
      throw toError(error, '删除日程失败')
    }
  }

  async function rescheduleEvent(payload: CalendarEventReschedulePayload) {
    let idx = findEventIndex(events.value, payload.id, payload.instanceStart)
    if (idx < 0) {
      idx = events.value.findIndex(event => event.id === payload.id)
    }
    const previous = idx >= 0 ? { ...events.value[idx]! } : null
    // Keep recurring instance identity (original occurrence); otherwise sync instanceStart with start.
    const nextInstanceStart = payload.instanceStart ?? payload.startTime
    if (idx >= 0) {
      const next = {
        ...events.value[idx]!,
        startTime: payload.startTime,
        endTime: payload.endTime,
        instanceStart: nextInstanceStart
      }
      events.value = events.value.map((event, i) => (i === idx ? next : event))
    }
    try {
      await apiRescheduleEvent(payload)
      // Server list is source of truth (esp. RRULE exceptions / expanded instances).
      await refreshLastRange()
      return true
    } catch (error) {
      if (previous && idx >= 0) {
        events.value = events.value.map((event, i) => (i === idx ? previous : event))
      } else {
        await refreshLastRange()
      }
      throw toError(error, '改期失败')
    }
  }

  async function respondEvent(id: string, response: 'ACCEPTED' | 'DECLINED') {
    try {
      await apiRespondEvent(id, response)
      await refreshLastRange()
      return true
    } catch (error) {
      throw toError(error, '响应日程失败')
    }
  }

  function getEventById(id: string, instanceStart?: string): CalendarEvent | undefined {
    const idx = findEventIndex(events.value.filter(e => e.status !== 'CANCELLED'), id, instanceStart)
    return idx >= 0 ? events.value[idx] : undefined
  }

  async function fetchEventById(id: string): Promise<CalendarEvent | null> {
    const cached = getEventById(id)
    if (cached) {
      return cached
    }
    try {
      const event = await apiGetEvent(id)
      const idx = findEventIndex(events.value, event.id, event.instanceStart)
      if (idx >= 0) {
        events.value[idx] = event
      } else {
        events.value = [...events.value, event]
      }
      return event
    } catch (error) {
      if (error instanceof ApiError) {
        return null
      }
      throw toError(error, '加载日程失败')
    }
  }

  function defaultTimedRange(anchor: Date, durationMinutes: number) {
    const start = new Date(anchor)
    start.setSeconds(0, 0)
    const minutes = start.getMinutes()
    start.setMinutes(minutes < 30 ? 0 : 30)
    const end = addMinutes(start, durationMinutes)
    return { start, end }
  }

  function defaultAllDayRange(day: Date) {
    const start = startOfDay(day)
    const end = addDays(start, 1)
    return { start, end }
  }

  return {
    calendars,
    events,
    shares,
    visibleCalendarIds,
    loading,
    ownedCalendars,
    sharedCalendars,
    primaryCalendar,
    visibleEvents,
    resetForTenantSwitch,
    fetchCalendars,
    fetchShares,
    createShare,
    removeShare,
    outgoingSharesForCalendar,
    fetchEvents,
    listInRange,
    toggleCalendarVisible,
    createCalendar,
    updateCalendar,
    removeCalendar,
    createEvent,
    updateEvent,
    removeEvent,
    rescheduleEvent,
    respondEvent,
    getEventById,
    fetchEventById,
    defaultTimedRange,
    defaultAllDayRange,
    endOfDay,
    DEFAULT_COLORS
  }
})
