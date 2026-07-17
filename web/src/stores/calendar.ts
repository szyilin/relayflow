import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { addDays, addMinutes, endOfDay, formatISO, isBefore, parseISO, startOfDay } from 'date-fns'
import type {
  CalendarAttendee,
  CalendarEvent,
  CalendarEventCreatePayload,
  CalendarEventUpdatePayload,
  CalendarItem
} from '../api/app/calendar'
import { useAuthStore } from './auth'

const VISIBLE_KEY = 'relayflow-calendar-visible-ids-v1'
const DEFAULT_COLORS = ['#3B82F6', '#10B981', '#EC4899', '#F59E0B', '#8B5CF6', '#06B6D4']

function snowflake(): string {
  return String(Date.now() * 1000 + Math.floor(Math.random() * 1000))
}

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

export const useCalendarStore = defineStore('calendar', () => {
  const calendars = ref<CalendarItem[]>([])
  const events = ref<CalendarEvent[]>([])
  const visibleCalendarIds = ref<string[]>([])
  const loading = ref(false)
  const bootstrapped = ref(false)

  const ownedCalendars = computed(() => calendars.value)
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

  function persistVisible() {
    localStorage.setItem(VISIBLE_KEY, JSON.stringify(visibleCalendarIds.value))
  }

  function ensureSeed() {
    if (bootstrapped.value) {
      return
    }
    const auth = useAuthStore()
    const userId = String(auth.userId ?? '1')
    const primaryId = 'cal-primary'
    calendars.value = [{
      id: primaryId,
      name: '我的日历',
      color: DEFAULT_COLORS[0]!,
      description: null,
      type: 'PRIMARY'
    }, {
      id: 'cal-work',
      name: '工作',
      color: DEFAULT_COLORS[2]!,
      description: '工作相关',
      type: 'OWNED'
    }]
    const start = addMinutes(startOfDay(new Date()), 18 * 60 + 30)
    const end = addMinutes(start, 30)
    events.value = [{
      id: 'evt-demo',
      calendarId: 'cal-work',
      calendarColor: DEFAULT_COLORS[2]!,
      calendarName: '工作',
      title: '示例日程',
      description: '',
      startTime: formatISO(start),
      endTime: formatISO(end),
      allDay: false,
      organizerId: userId,
      remindBeforeMinutes: 5,
      allDayRemindTime: null,
      status: 'CONFIRMED',
      viewerRole: 'ORGANIZER',
      attendees: [{
        userId,
        role: 'ORGANIZER',
        response: 'ACCEPTED',
        nickname: auth.user?.nickname || '我'
      }],
      invitedOnly: false
    }]
    const saved = readVisibleIds()
    visibleCalendarIds.value = saved?.length
      ? saved.filter(id => calendars.value.some(c => c.id === id))
      : calendars.value.map(c => c.id)
    bootstrapped.value = true
  }

  async function fetchCalendars() {
    loading.value = true
    try {
      ensureSeed()
    } finally {
      loading.value = false
    }
  }

  async function fetchEvents(_range: { from: string, to: string }) {
    ensureSeed()
    // temp: all events already in memory; range filter applied by callers via listInRange
  }

  function listInRange(from: Date, to: Date): CalendarEvent[] {
    ensureSeed()
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
    ensureSeed()
    const id = snowflake()
    calendars.value.push({
      id,
      name: payload.name.trim(),
      color: payload.color,
      description: payload.description ?? null,
      type: 'OWNED'
    })
    visibleCalendarIds.value = [...visibleCalendarIds.value, id]
    persistVisible()
    return id
  }

  async function updateCalendar(payload: { id: string, name: string, color: string, description?: string | null }) {
    const cal = calendars.value.find(c => c.id === payload.id)
    if (!cal) {
      throw new Error('日历不存在')
    }
    cal.name = payload.name.trim()
    cal.color = payload.color
    cal.description = payload.description ?? null
    for (const event of events.value) {
      if (event.calendarId === cal.id) {
        event.calendarName = cal.name
        event.calendarColor = cal.color
      }
    }
    return true
  }

  async function removeCalendar(id: string) {
    const cal = calendars.value.find(c => c.id === id)
    if (!cal) {
      throw new Error('日历不存在')
    }
    if (cal.type === 'PRIMARY') {
      throw new Error('主日历不可删除')
    }
    if (events.value.some(e => e.calendarId === id && e.status !== 'CANCELLED')) {
      throw new Error('日历下仍有日程，无法删除')
    }
    calendars.value = calendars.value.filter(c => c.id !== id)
    visibleCalendarIds.value = visibleCalendarIds.value.filter(v => v !== id)
    persistVisible()
    return true
  }

  function resolveCalendar(calendarId: string): CalendarItem {
    const cal = calendars.value.find(c => c.id === calendarId)
    if (!cal) {
      throw new Error('日历不存在')
    }
    return cal
  }

  async function createEvent(payload: CalendarEventCreatePayload) {
    ensureSeed()
    const auth = useAuthStore()
    const userId = String(auth.userId ?? '1')
    const cal = resolveCalendar(payload.calendarId)
    if (isBefore(parseISO(payload.endTime), parseISO(payload.startTime))
      || parseISO(payload.endTime).getTime() === parseISO(payload.startTime).getTime()) {
      throw new Error('日程时间无效')
    }
    const id = snowflake()
    const attendees: CalendarAttendee[] = [{
      userId,
      role: 'ORGANIZER',
      response: 'ACCEPTED',
      nickname: auth.user?.nickname || '我'
    }]
    for (const uid of payload.attendeeUserIds ?? []) {
      if (String(uid) === userId) {
        continue
      }
      attendees.push({
        userId: String(uid),
        role: 'ATTENDEE',
        response: 'NEEDS_ACTION',
        nickname: `用户 ${uid}`
      })
    }
    events.value.push({
      id,
      calendarId: cal.id,
      calendarColor: cal.color,
      calendarName: cal.name,
      title: payload.title.trim() || '(无主题)',
      description: payload.description ?? '',
      startTime: payload.startTime,
      endTime: payload.endTime,
      allDay: payload.allDay,
      organizerId: userId,
      remindBeforeMinutes: payload.remindBeforeMinutes ?? null,
      allDayRemindTime: payload.allDayRemindTime ?? null,
      status: 'CONFIRMED',
      viewerRole: 'ORGANIZER',
      attendees,
      invitedOnly: false
    })
    return id
  }

  async function updateEvent(payload: CalendarEventUpdatePayload) {
    const event = events.value.find(e => e.id === payload.id)
    if (!event) {
      throw new Error('日程不存在')
    }
    if (event.viewerRole !== 'ORGANIZER') {
      throw new Error('无权操作该日程')
    }
    const cal = resolveCalendar(payload.calendarId)
    const auth = useAuthStore()
    const userId = String(auth.userId ?? '1')
    event.calendarId = cal.id
    event.calendarColor = cal.color
    event.calendarName = cal.name
    event.title = payload.title.trim() || '(无主题)'
    event.description = payload.description ?? ''
    event.startTime = payload.startTime
    event.endTime = payload.endTime
    event.allDay = payload.allDay
    event.remindBeforeMinutes = payload.remindBeforeMinutes ?? null
    event.allDayRemindTime = payload.allDayRemindTime ?? null
    const attendees: CalendarAttendee[] = [{
      userId,
      role: 'ORGANIZER',
      response: 'ACCEPTED',
      nickname: auth.user?.nickname || '我'
    }]
    for (const uid of payload.attendeeUserIds ?? []) {
      if (String(uid) === userId) {
        continue
      }
      const prev = event.attendees.find(a => a.userId === String(uid))
      attendees.push({
        userId: String(uid),
        role: 'ATTENDEE',
        response: prev?.response ?? 'NEEDS_ACTION',
        nickname: prev?.nickname ?? `用户 ${uid}`
      })
    }
    event.attendees = attendees
    return true
  }

  async function removeEvent(id: string) {
    const event = events.value.find(e => e.id === id)
    if (!event) {
      throw new Error('日程不存在')
    }
    if (event.viewerRole !== 'ORGANIZER') {
      throw new Error('无权操作该日程')
    }
    event.status = 'CANCELLED'
    return true
  }

  async function respondEvent(id: string, response: 'ACCEPTED' | 'DECLINED') {
    const event = events.value.find(e => e.id === id)
    if (!event) {
      throw new Error('日程不存在')
    }
    const auth = useAuthStore()
    const userId = String(auth.userId ?? '1')
    const attendee = event.attendees.find(a => a.userId === userId && a.role === 'ATTENDEE')
    if (!attendee) {
      throw new Error('无权操作该日程')
    }
    attendee.response = response
    return true
  }

  function getEventById(id: string): CalendarEvent | undefined {
    ensureSeed()
    return events.value.find(e => e.id === id && e.status !== 'CANCELLED')
  }

  /** Helpers for UI defaults */
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
    visibleCalendarIds,
    loading,
    ownedCalendars,
    primaryCalendar,
    visibleEvents,
    fetchCalendars,
    fetchEvents,
    listInRange,
    toggleCalendarVisible,
    createCalendar,
    updateCalendar,
    removeCalendar,
    createEvent,
    updateEvent,
    removeEvent,
    respondEvent,
    getEventById,
    defaultTimedRange,
    defaultAllDayRange,
    endOfDay,
    DEFAULT_COLORS
  }
})
