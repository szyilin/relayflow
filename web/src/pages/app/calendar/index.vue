<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  addDays,
  addMonths,
  addWeeks,
  eachDayOfInterval,
  endOfMonth,
  endOfWeek,
  format,
  formatISO,
  isBefore,
  isSameDay,
  isSameMonth,
  parseISO,
  startOfDay,
  startOfMonth,
  startOfWeek
} from 'date-fns'
import { zhCN } from 'date-fns/locale'
import CalendarEventEditor from '../../../components/workspace/CalendarEventEditor.vue'
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'
import type { CalendarEvent, CalendarItem } from '../../../api/app/calendar'
import { eventKey } from '../../../api/app/calendar'
import type { TaskItem } from '../../../api/app/task'
import { useAuthStore } from '../../../stores/auth'
import { useCalendarStore } from '../../../stores/calendar'
import { useContactsStore } from '../../../stores/contacts'
import { useTasksStore } from '../../../stores/tasks'
import { useUserPreferenceStore } from '../../../stores/userPreference'

type ViewMode = 'day' | 'week' | 'month'

const HOUR_START = 0
const HOUR_END = 24
const HOUR_HEIGHT = 48
const DRAG_THRESHOLD = 4
const RESIZE_HANDLE_HEIGHT = 6
/** Fixed amber for task projections — distinct from calendar event colors. */
const TASK_LAYER_COLOR = '#D97706'
const TASK_BLOCK_MINUTES = 30

const route = useRoute()
const router = useRouter()
const calendarStore = useCalendarStore()
const tasksStore = useTasksStore()
const preference = useUserPreferenceStore()
const contacts = useContactsStore()
const auth = useAuthStore()
const toast = useToast()

/** Session toggle; seeded from preference.showTaskLayer on enter. */
const taskLayerVisible = ref(true)

const viewMode = ref<ViewMode>('week')
const anchorDate = ref(startOfDay(new Date()))
const nowTick = ref(new Date())
let nowTimer: ReturnType<typeof setInterval> | undefined

const eventEditorOpen = ref(false)
const eventEditorMode = ref<'create' | 'edit'>('create')
const editingEvent = ref<CalendarEvent | null>(null)
const createInitialStart = ref<Date | undefined>()
const createInitialEnd = ref<Date | undefined>()
const createInitialAllDay = ref(false)

const createCalendarOpen = ref(false)
const createCalendarForm = reactive({
  name: '',
  color: calendarStore.DEFAULT_COLORS[1]!,
  description: ''
})
const createCalendarSubmitting = ref(false)

const shareModalOpen = ref(false)
const shareCalendar = ref<CalendarItem | null>(null)
const shareMemberKeyword = ref('')
const shareSubmitting = ref(false)

interface ActiveDrag {
  event: CalendarEvent
  day: Date
  columnEl: HTMLElement
  mode: 'move' | 'resize'
  pointerId: number
  startClientY: number
  startClientX: number
  /** Cursor minutes minus event-start minutes at grab (move mode). */
  grabOffsetMinutes: number
  originStart: Date
  originEnd: Date
  moved: boolean
  previewStart: Date
  previewEnd: Date
}

const activeDrag = ref<ActiveDrag | null>(null)
const suppressClick = ref(false)

const weekStartsOn = computed(() =>
  (preference.calendar.weekStartsOn === 1 ? 1 : 0) as 0 | 1)

const rangeStart = computed(() => {
  if (viewMode.value === 'day') {
    return startOfDay(anchorDate.value)
  }
  if (viewMode.value === 'week') {
    return startOfWeek(anchorDate.value, { weekStartsOn: weekStartsOn.value })
  }
  return startOfWeek(startOfMonth(anchorDate.value), { weekStartsOn: weekStartsOn.value })
})

const rangeEnd = computed(() => {
  if (viewMode.value === 'day') {
    return addDays(startOfDay(anchorDate.value), 1)
  }
  if (viewMode.value === 'week') {
    return addDays(startOfWeek(anchorDate.value, { weekStartsOn: weekStartsOn.value }), 7)
  }
  return addDays(
    endOfWeek(endOfMonth(anchorDate.value), { weekStartsOn: weekStartsOn.value }),
    1
  )
})

const daysInView = computed(() =>
  eachDayOfInterval({
    start: rangeStart.value,
    end: addDays(rangeEnd.value, -1)
  }))

const hours = computed(() =>
  Array.from({ length: HOUR_END - HOUR_START }, (_, i) => HOUR_START + i))

const headerTitle = computed(() => {
  if (viewMode.value === 'day') {
    return format(anchorDate.value, 'yyyy年M月d日 EEEE', { locale: zhCN })
  }
  if (viewMode.value === 'week') {
    const start = rangeStart.value
    const end = addDays(rangeEnd.value, -1)
    if (start.getMonth() === end.getMonth()) {
      return `${format(start, 'yyyy年M月d日', { locale: zhCN })} – ${format(end, 'd日', { locale: zhCN })}`
    }
    return `${format(start, 'M月d日', { locale: zhCN })} – ${format(end, 'M月d日', { locale: zhCN })}`
  }
  return format(anchorDate.value, 'yyyy年M月', { locale: zhCN })
})

const miniMonthDays = computed(() => {
  const monthStart = startOfMonth(anchorDate.value)
  const start = startOfWeek(monthStart, { weekStartsOn: weekStartsOn.value })
  const end = endOfWeek(endOfMonth(monthStart), { weekStartsOn: weekStartsOn.value })
  return eachDayOfInterval({ start, end })
})

const weekDayLabels = computed(() => {
  const base = startOfWeek(new Date(), { weekStartsOn: weekStartsOn.value })
  return eachDayOfInterval({ start: base, end: addDays(base, 6) })
    .map(d => format(d, 'EEEEE', { locale: zhCN }))
})

const eventsInView = computed(() =>
  calendarStore.listInRange(rangeStart.value, rangeEnd.value))

const dimPast = computed(() => preference.calendar.dimPastEvents)

const projectedTasks = computed(() =>
  taskLayerVisible.value ? tasksStore.dueRangeItems : [])

function isPast(event: CalendarEvent): boolean {
  return isBefore(parseISO(event.endTime), nowTick.value)
}

function eventStyle(event: CalendarEvent): Record<string, string> {
  const color = event.calendarColor || '#3B82F6'
  const opacity = dimPast.value && isPast(event) ? '0.45' : '1'
  return {
    backgroundColor: `${color}22`,
    borderLeft: `3px solid ${color}`,
    color,
    opacity
  }
}

function taskProjectionStyle(): Record<string, string> {
  return {
    backgroundColor: `${TASK_LAYER_COLOR}18`,
    borderLeft: `3px dashed ${TASK_LAYER_COLOR}`,
    color: TASK_LAYER_COLOR
  }
}

function openProjectedTask(task: TaskItem) {
  void router.push({ path: '/app/tasks', query: { taskId: task.id } })
}

function eventTimes(event: CalendarEvent): { start: Date, end: Date } {
  const drag = activeDrag.value
  if (drag && event.id === drag.event.id) {
    // Non-recurring: one row per id. Recurring: keep matching the dragged occurrence.
    const sameOccurrence = !drag.event.recurring
      || eventKey(event) === eventKey(drag.event)
      || (event.instanceStart ?? event.startTime) === (drag.event.instanceStart ?? drag.event.startTime)
    if (sameOccurrence) {
      return { start: drag.previewStart, end: drag.previewEnd }
    }
  }
  return { start: parseISO(event.startTime), end: parseISO(event.endTime) }
}

function canDragEvent(event: CalendarEvent): boolean {
  return viewMode.value !== 'month'
    && event.viewerRole === 'ORGANIZER'
    && !event.allDay
}

function isDraggedEvent(event: CalendarEvent): boolean {
  const drag = activeDrag.value
  if (!drag || event.id !== drag.event.id) {
    return false
  }
  if (!drag.event.recurring) {
    return true
  }
  return eventKey(event) === eventKey(drag.event)
    || (event.instanceStart ?? event.startTime) === (drag.event.instanceStart ?? drag.event.startTime)
}

function rawMinutesFromClientY(clientY: number, columnEl: HTMLElement): number {
  const rect = columnEl.getBoundingClientRect()
  const relY = clientY - rect.top
  return (relY / HOUR_HEIGHT) * 60 + HOUR_START * 60
}

function snapMinutes(minutes: number): number {
  const max = (HOUR_END - HOUR_START) * 60
  return Math.round(Math.max(0, Math.min(max, minutes)) / 15) * 15
}

function minutesFromClientY(clientY: number, columnEl: HTMLElement): number {
  return snapMinutes(rawMinutesFromClientY(clientY, columnEl))
}

function setDragUiLock(locked: boolean) {
  const root = document.documentElement
  if (locked) {
    root.classList.add('select-none')
    root.style.cursor = 'grabbing'
  } else {
    root.classList.remove('select-none')
    root.style.cursor = ''
  }
}

function dateWithMinutes(day: Date, minutes: number): Date {
  const d = startOfDay(day)
  d.setMinutes(minutes)
  return d
}

function resolveColumnEl(clientX: number): HTMLElement | null {
  const columns = document.querySelectorAll('[data-cal-day-column]')
  for (const col of columns) {
    const rect = col.getBoundingClientRect()
    if (clientX >= rect.left && clientX <= rect.right) {
      return col as HTMLElement
    }
  }
  return null
}

function resolveDayIndex(clientX: number): number {
  const columns = document.querySelectorAll('[data-cal-day-column]')
  for (let i = 0; i < columns.length; i++) {
    const rect = columns[i]!.getBoundingClientRect()
    if (clientX >= rect.left && clientX <= rect.right) {
      return i
    }
  }
  return -1
}

function cleanupDragListeners() {
  document.removeEventListener('pointermove', onDocumentPointerMove)
  document.removeEventListener('pointerup', onDocumentPointerUp)
  document.removeEventListener('selectstart', preventSelectDuringDrag)
  setDragUiLock(false)
}

function preventSelectDuringDrag(e: Event) {
  e.preventDefault()
}

function onDocumentPointerMove(e: PointerEvent) {
  const drag = activeDrag.value
  if (!drag || e.pointerId !== drag.pointerId) {
    return
  }
  e.preventDefault()
  const dx = Math.abs(e.clientX - drag.startClientX)
  const dy = Math.abs(e.clientY - drag.startClientY)
  if (!drag.moved && dx < DRAG_THRESHOLD && dy < DRAG_THRESHOLD) {
    return
  }
  if (!drag.moved) {
    drag.moved = true
    setDragUiLock(true)
  }

  const column = resolveColumnEl(e.clientX) ?? drag.columnEl
  const dayIndex = resolveDayIndex(e.clientX)
  const targetDay = dayIndex >= 0 ? daysInView.value[dayIndex]! : drag.day

  if (drag.mode === 'resize') {
    const endMinutes = minutesFromClientY(e.clientY, column)
    const startMinutes = drag.previewStart.getHours() * 60 + drag.previewStart.getMinutes()
    drag.previewEnd = dateWithMinutes(targetDay, Math.max(startMinutes + 15, endMinutes))
  } else {
    const duration = drag.originEnd.getTime() - drag.originStart.getTime()
    // Preserve grab point inside the block (avoid jumping top to cursor).
    const startMinutes = snapMinutes(rawMinutesFromClientY(e.clientY, column) - drag.grabOffsetMinutes)
    drag.previewStart = dateWithMinutes(targetDay, startMinutes)
    drag.previewEnd = new Date(drag.previewStart.getTime() + duration)
    drag.day = targetDay
  }
}

async function onDocumentPointerUp(e: PointerEvent) {
  const drag = activeDrag.value
  if (!drag || e.pointerId !== drag.pointerId) {
    return
  }
  cleanupDragListeners()

  if (drag.moved) {
    suppressClick.value = true
    // Keep preview until store refresh finishes so the block does not snap back.
    try {
      await calendarStore.rescheduleEvent({
        id: drag.event.id,
        startTime: formatISO(drag.previewStart),
        endTime: formatISO(drag.previewEnd),
        editScope: drag.event.recurring ? 'THIS' : undefined,
        instanceStart: drag.event.recurring
          ? (drag.event.instanceStart ?? drag.event.startTime)
          : undefined
      })
    } catch (error) {
      toast.add({
        title: error instanceof Error ? error.message : '改期失败',
        color: 'error'
      })
    }
  }
  activeDrag.value = null
}

function onEventPointerDown(
  e: PointerEvent,
  event: CalendarEvent,
  day: Date,
  mode: 'move' | 'resize'
) {
  if (!canDragEvent(event)) {
    return
  }
  // Prevent native text selection / image drag while grabbing the block.
  e.preventDefault()
  e.stopPropagation()
  const column = (e.currentTarget as HTMLElement).closest('[data-cal-day-column]') as HTMLElement | null
  if (!column) {
    return
  }
  const originStart = parseISO(event.startTime)
  const originEnd = parseISO(event.endTime)
  const originStartMinutes = originStart.getHours() * 60 + originStart.getMinutes()
  const grabOffsetMinutes = mode === 'move'
    ? rawMinutesFromClientY(e.clientY, column) - originStartMinutes
    : 0
  activeDrag.value = {
    event,
    day,
    columnEl: column,
    mode,
    pointerId: e.pointerId,
    startClientY: e.clientY,
    startClientX: e.clientX,
    grabOffsetMinutes,
    originStart,
    originEnd,
    moved: false,
    previewStart: originStart,
    previewEnd: originEnd
  }
  document.addEventListener('pointermove', onDocumentPointerMove, { passive: false })
  document.addEventListener('pointerup', onDocumentPointerUp)
  document.addEventListener('selectstart', preventSelectDuringDrag)
}

function timedLayout(event: CalendarEvent, day: Date): { top: string, height: string } | null {
  if (event.allDay) {
    return null
  }
  const { start, end } = eventTimes(event)
  const dayStart = startOfDay(day)
  const dayEnd = addDays(dayStart, 1)
  const clippedStart = start < dayStart ? dayStart : start
  const clippedEnd = end > dayEnd ? dayEnd : end
  if (!(clippedEnd > clippedStart)) {
    return null
  }
  const startMinutes = (clippedStart.getHours() - HOUR_START) * 60 + clippedStart.getMinutes()
  const endMinutes = (clippedEnd.getHours() - HOUR_START) * 60 + clippedEnd.getMinutes()
    + (clippedEnd.getTime() === dayEnd.getTime() ? (HOUR_END - HOUR_START) * 60 : 0)
  const top = (startMinutes / 60) * HOUR_HEIGHT
  const height = Math.max(((endMinutes - startMinutes) / 60) * HOUR_HEIGHT, 18)
  return { top: `${top}px`, height: `${height}px` }
}

function allDayEventsForDay(day: Date): CalendarEvent[] {
  return eventsInView.value.filter((event) => {
    if (!event.allDay) {
      return false
    }
    const start = parseISO(event.startTime)
    const end = parseISO(event.endTime)
    return start < addDays(day, 1) && end > day
  })
}

function timedEventsForDay(day: Date): CalendarEvent[] {
  return eventsInView.value.filter((event) => {
    if (event.allDay) {
      return false
    }
    // Use eventTimes so in-drag preview can land on another day column.
    const { start, end } = eventTimes(event)
    return start < addDays(day, 1) && end > day
  })
}

function monthEventsForDay(day: Date): CalendarEvent[] {
  return eventsInView.value.filter((event) => {
    const start = parseISO(event.startTime)
    const end = parseISO(event.endTime)
    return start < addDays(day, 1) && end > day
  }).slice(0, 3)
}

function tasksForDay(day: Date): TaskItem[] {
  const dayStart = startOfDay(day)
  const dayEnd = addDays(dayStart, 1)
  return projectedTasks.value.filter((task) => {
    if (!task.dueTime) {
      return false
    }
    const due = parseISO(task.dueTime)
    return due >= dayStart && due < dayEnd
  })
}

function monthTasksForDay(day: Date): TaskItem[] {
  return tasksForDay(day).slice(0, 3)
}

function timedTaskLayout(task: TaskItem, day: Date): { top: string, height: string } | null {
  if (!task.dueTime) {
    return null
  }
  const due = parseISO(task.dueTime)
  if (!isSameDay(due, day)) {
    return null
  }
  const startMinutes = (due.getHours() - HOUR_START) * 60 + due.getMinutes()
  const top = (startMinutes / 60) * HOUR_HEIGHT
  const height = Math.max((TASK_BLOCK_MINUTES / 60) * HOUR_HEIGHT, 18)
  return { top: `${top}px`, height: `${height}px` }
}

function nowLineTop(): string | null {
  const n = nowTick.value
  if (viewMode.value === 'month') {
    return null
  }
  const inView = daysInView.value.some(d => isSameDay(d, n))
  if (!inView) {
    return null
  }
  const minutes = (n.getHours() - HOUR_START) * 60 + n.getMinutes()
  if (minutes < 0 || minutes > (HOUR_END - HOUR_START) * 60) {
    return null
  }
  return `${(minutes / 60) * HOUR_HEIGHT}px`
}

const nowLineVisibleDayIndex = computed(() => {
  const n = nowTick.value
  return daysInView.value.findIndex(d => isSameDay(d, n))
})

/** 飞书式：过期日淡线 · 今日圆点 + 实线 · 未来日实线 */
const nowLineByDay = computed(() => {
  const top = nowLineTop()
  const todayIdx = nowLineVisibleDayIndex.value
  if (!top || todayIdx < 0) {
    return daysInView.value.map(() => null as 'past' | 'today' | 'future' | null)
  }
  return daysInView.value.map((_, dayIndex) => {
    if (dayIndex < todayIdx) {
      return 'past' as const
    }
    if (dayIndex === todayIdx) {
      return 'today' as const
    }
    return 'future' as const
  })
})

const nowLineLabel = computed(() => format(nowTick.value, 'HH:mm'))

const nowLineTopPx = computed(() => nowLineTop())

async function refreshRange() {
  await calendarStore.fetchCalendars()
  await calendarStore.fetchShares()
  await calendarStore.fetchEvents({
    from: formatISO(rangeStart.value),
    to: formatISO(rangeEnd.value)
  })
  if (taskLayerVisible.value) {
    await tasksStore.fetchDueRange(
      formatISO(rangeStart.value),
      formatISO(rangeEnd.value)
    )
  } else {
    tasksStore.clearDueRange()
  }
}

function goToday() {
  anchorDate.value = startOfDay(new Date())
}

function goPrev() {
  if (viewMode.value === 'day') {
    anchorDate.value = addDays(anchorDate.value, -1)
  } else if (viewMode.value === 'week') {
    anchorDate.value = addWeeks(anchorDate.value, -1)
  } else {
    anchorDate.value = addMonths(anchorDate.value, -1)
  }
}

function goNext() {
  if (viewMode.value === 'day') {
    anchorDate.value = addDays(anchorDate.value, 1)
  } else if (viewMode.value === 'week') {
    anchorDate.value = addWeeks(anchorDate.value, 1)
  } else {
    anchorDate.value = addMonths(anchorDate.value, 1)
  }
}

function selectMiniDay(day: Date) {
  anchorDate.value = startOfDay(day)
  if (viewMode.value === 'month') {
    viewMode.value = 'day'
  }
}

function openCreate(day?: Date, hour?: number) {
  const base = day ? startOfDay(day) : anchorDate.value
  if (hour != null) {
    const start = new Date(base)
    start.setHours(hour, 0, 0, 0)
    const { end } = calendarStore.defaultTimedRange(
      start,
      preference.calendar.defaultEventDurationMinutes
    )
    createInitialStart.value = start
    createInitialEnd.value = end
    createInitialAllDay.value = false
  } else if (viewMode.value === 'month') {
    createInitialStart.value = base
    createInitialEnd.value = undefined
    createInitialAllDay.value = true
  } else {
    const { start, end } = calendarStore.defaultTimedRange(
      new Date(),
      preference.calendar.defaultEventDurationMinutes
    )
    createInitialStart.value = start
    createInitialEnd.value = end
    createInitialAllDay.value = false
  }
  editingEvent.value = null
  eventEditorMode.value = 'create'
  eventEditorOpen.value = true
}

function openEvent(event: CalendarEvent) {
  editingEvent.value = event
  eventEditorMode.value = 'edit'
  eventEditorOpen.value = true
  void router.replace({
    query: {
      ...route.query,
      eventId: event.id
    }
  })
}

function handleEventClick(event: CalendarEvent) {
  if (suppressClick.value) {
    suppressClick.value = false
    return
  }
  openEvent(event)
}

const outgoingShares = computed(() =>
  shareCalendar.value
    ? calendarStore.outgoingSharesForCalendar(shareCalendar.value.id)
    : [])

const shareCandidateMembers = computed(() => {
  const selfId = String(auth.userId ?? '')
  const granted = new Set(outgoingShares.value.map(s => s.granteeUserId))
  const q = shareMemberKeyword.value.trim().toLowerCase()
  return contacts.members.filter((member) => {
    if (member.id === selfId || granted.has(member.id)) {
      return false
    }
    if (!q) {
      return true
    }
    return member.nickname.toLowerCase().includes(q)
      || member.username.toLowerCase().includes(q)
  })
})

async function openShareModal(cal: CalendarItem) {
  shareCalendar.value = cal
  shareModalOpen.value = true
  shareMemberKeyword.value = ''
  try {
    await calendarStore.fetchShares()
    await contacts.fetchDepts()
    const rootId = contacts.rootDeptId()
    if (rootId) {
      await contacts.fetchMembers({ deptId: rootId })
    }
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '加载共享信息失败',
      color: 'error'
    })
  }
}

async function addShareGrantee(userId: string) {
  if (!shareCalendar.value) {
    return
  }
  shareSubmitting.value = true
  try {
    await calendarStore.createShare({
      calendarId: shareCalendar.value.id,
      granteeUserId: userId
    })
    toast.add({ title: '已共享', color: 'success' })
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '共享失败',
      color: 'error'
    })
  } finally {
    shareSubmitting.value = false
  }
}

async function removeShareGrantee(shareId: string) {
  shareSubmitting.value = true
  try {
    await calendarStore.removeShare(shareId)
    toast.add({ title: '已取消共享', color: 'success' })
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '取消共享失败',
      color: 'error'
    })
  } finally {
    shareSubmitting.value = false
  }
}

function onEditorClosed() {
  const q = { ...route.query }
  delete q.eventId
  void router.replace({ query: q })
}

watch(eventEditorOpen, (open) => {
  if (!open) {
    onEditorClosed()
  }
})

async function submitCreateCalendar() {
  const name = createCalendarForm.name.trim()
  if (!name) {
    toast.add({ title: '请输入日历名称', color: 'error' })
    return
  }
  createCalendarSubmitting.value = true
  try {
    await calendarStore.createCalendar({
      name,
      color: createCalendarForm.color,
      description: createCalendarForm.description
    })
    createCalendarOpen.value = false
    createCalendarForm.name = ''
    createCalendarForm.description = ''
    toast.add({ title: '日历已创建', color: 'success' })
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '创建失败',
      color: 'error'
    })
  } finally {
    createCalendarSubmitting.value = false
  }
}

async function removeOwnedCalendar(id: string) {
  try {
    await calendarStore.removeCalendar(id)
    toast.add({ title: '日历已删除', color: 'success' })
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '删除失败',
      color: 'error'
    })
  }
}

function applyDeepLink() {
  const dateRaw = route.query.date
  const dateStr = typeof dateRaw === 'string' ? dateRaw : Array.isArray(dateRaw) ? dateRaw[0] : null
  if (dateStr) {
    const parsed = new Date(dateStr)
    if (!Number.isNaN(parsed.getTime())) {
      anchorDate.value = startOfDay(parsed)
    }
  }
}

async function openDeepLinkedEvent() {
  const eventRaw = route.query.eventId
  const eventId = typeof eventRaw === 'string' ? eventRaw : Array.isArray(eventRaw) ? eventRaw[0] : null
  if (!eventId) {
    return
  }
  const event = await calendarStore.fetchEventById(eventId)
  if (event) {
    openEvent(event)
  } else {
    toast.add({ title: '日程不存在或无权查看', color: 'error' })
  }
}

onMounted(async () => {
  preference.hydrateFromLocal()
  taskLayerVisible.value = preference.calendar.showTaskLayer
  void preference.fetchFromServer()
  applyDeepLink()
  try {
    await refreshRange()
    await openDeepLinkedEvent()
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '加载日历失败',
      color: 'error'
    })
  }
  await nextTick()
  const scroller = document.querySelector('[data-cal-scroll]')
  if (scroller) {
    scroller.scrollTop = 8 * HOUR_HEIGHT
  }
  nowTimer = setInterval(() => {
    nowTick.value = new Date()
  }, 30_000)
})

onUnmounted(() => {
  if (nowTimer) {
    clearInterval(nowTimer)
  }
  cleanupDragListeners()
})

watch([rangeStart, rangeEnd, viewMode], () => {
  void refreshRange().catch((error) => {
    toast.add({
      title: error instanceof Error ? error.message : '加载日程失败',
      color: 'error'
    })
  })
})

watch(taskLayerVisible, () => {
  void refreshRange().catch((error) => {
    toast.add({
      title: error instanceof Error ? error.message : '加载任务失败',
      color: 'error'
    })
  })
})

watch(() => route.query.eventId, () => {
  void openDeepLinkedEvent()
})

watch(() => route.query.date, () => {
  applyDeepLink()
})
</script>

<route lang="yaml">
meta:
  layout: workspace
</route>

<template>
  <WorkspaceShell>
    <template #panel>
      <div class="flex h-full flex-col overflow-hidden">
        <div class="border-b border-[var(--ws-border-subtle)] px-3 py-3">
          <div class="mb-2 flex items-center justify-between">
            <h2 class="text-sm font-semibold">
              {{ format(anchorDate, 'yyyy年M月', { locale: zhCN }) }}
            </h2>
            <div class="flex gap-0.5">
              <UButton
                icon="i-lucide-chevron-left"
                size="xs"
                color="neutral"
                variant="ghost"
                @click="anchorDate = addMonths(anchorDate, -1)"
              />
              <UButton
                icon="i-lucide-chevron-right"
                size="xs"
                color="neutral"
                variant="ghost"
                @click="anchorDate = addMonths(anchorDate, 1)"
              />
            </div>
          </div>
          <div class="grid grid-cols-7 gap-0.5">
            <div
              v-for="(label, idx) in weekDayLabels"
              :key="`mini-label-${idx}`"
              class="flex aspect-square items-center justify-center text-[10px] text-[var(--ws-text-muted)]"
            >
              {{ label }}
            </div>
            <button
              v-for="day in miniMonthDays"
              :key="day.toISOString()"
              type="button"
              class="flex aspect-square w-full items-center justify-center rounded-full text-xs transition-colors"
              :class="[
                isSameDay(day, new Date()) ? 'bg-primary text-white' : '',
                isSameDay(day, anchorDate) && !isSameDay(day, new Date())
                  ? 'ring-1 ring-primary'
                  : '',
                isSameMonth(day, anchorDate)
                  ? 'text-[var(--ws-text)] hover:bg-[var(--ws-rail-hover)]'
                  : 'text-[var(--ws-text-muted)] opacity-50 hover:bg-[var(--ws-rail-hover)]'
              ]"
              @click="selectMiniDay(day)"
            >
              {{ format(day, 'd') }}
            </button>
          </div>
        </div>

        <div class="min-h-0 flex-1 overflow-y-auto p-3">
          <div class="mb-2 flex items-center justify-between">
            <p class="text-xs font-medium text-[var(--ws-text-muted)]">
              我管理的
            </p>
            <UButton
              size="xs"
              color="neutral"
              variant="ghost"
              icon="i-lucide-plus"
              @click="createCalendarOpen = true"
            >
              添加日历
            </UButton>
          </div>
          <ul class="space-y-1">
            <li
              v-for="cal in calendarStore.ownedCalendars"
              :key="cal.id"
              class="group flex items-center gap-2 rounded-lg px-1.5 py-1.5 hover:bg-[var(--ws-rail-hover)]"
            >
              <UCheckbox
                :model-value="calendarStore.visibleCalendarIds.includes(cal.id)"
                @update:model-value="(v: boolean | 'indeterminate') => calendarStore.toggleCalendarVisible(cal.id, v === true)"
              />
              <span
                class="size-2.5 shrink-0 rounded-full"
                :style="{ backgroundColor: cal.color }"
              />
              <span class="min-w-0 flex-1 truncate text-sm">{{ cal.name }}</span>
              <UButton
                icon="i-lucide-share-2"
                size="xs"
                color="neutral"
                variant="ghost"
                class="opacity-0 group-hover:opacity-100"
                title="共享"
                @click="openShareModal(cal)"
              />
              <UButton
                v-if="cal.type !== 'PRIMARY'"
                icon="i-lucide-trash-2"
                size="xs"
                color="neutral"
                variant="ghost"
                class="opacity-0 group-hover:opacity-100"
                @click="removeOwnedCalendar(cal.id)"
              />
            </li>
          </ul>

          <div v-if="calendarStore.sharedCalendars.length" class="mt-4">
            <p class="mb-2 text-xs font-medium text-[var(--ws-text-muted)]">
              共享给我的
            </p>
            <ul class="space-y-1">
              <li
                v-for="cal in calendarStore.sharedCalendars"
                :key="cal.id"
                class="flex items-center gap-2 rounded-lg px-1.5 py-1.5 hover:bg-[var(--ws-rail-hover)]"
              >
                <UCheckbox
                  :model-value="calendarStore.visibleCalendarIds.includes(cal.id)"
                  @update:model-value="(v: boolean | 'indeterminate') => calendarStore.toggleCalendarVisible(cal.id, v === true)"
                />
                <span
                  class="size-2.5 shrink-0 rounded-full"
                  :style="{ backgroundColor: cal.color }"
                />
                <span class="min-w-0 flex-1 truncate text-sm">{{ cal.name }}</span>
              </li>
            </ul>
          </div>

          <div class="mt-4">
            <p class="mb-2 text-xs font-medium text-[var(--ws-text-muted)]">
              虚拟图层
            </p>
            <ul class="space-y-1">
              <li class="flex items-center gap-2 rounded-lg px-1.5 py-1.5 hover:bg-[var(--ws-rail-hover)]">
                <UCheckbox
                  :model-value="taskLayerVisible"
                  @update:model-value="(v: boolean | 'indeterminate') => { taskLayerVisible = v === true }"
                />
                <span
                  class="size-2.5 shrink-0 rounded-sm border border-dashed"
                  :style="{ borderColor: TASK_LAYER_COLOR, backgroundColor: `${TASK_LAYER_COLOR}33` }"
                />
                <span class="min-w-0 flex-1 truncate text-sm">我的任务</span>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </template>

    <div class="flex h-full min-h-0 flex-col">
      <header class="flex shrink-0 flex-wrap items-center gap-2 border-b border-[var(--ws-border-subtle)] px-4 py-3">
        <UButton
          color="neutral"
          variant="soft"
          size="sm"
          @click="goToday"
        >
          今天
        </UButton>
        <div class="flex items-center gap-0.5">
          <UButton
            icon="i-lucide-chevron-left"
            size="sm"
            color="neutral"
            variant="ghost"
            @click="goPrev"
          />
          <UButton
            icon="i-lucide-chevron-right"
            size="sm"
            color="neutral"
            variant="ghost"
            @click="goNext"
          />
        </div>
        <h1 class="min-w-0 flex-1 truncate text-base font-semibold">
          {{ headerTitle }}
        </h1>
        <UTabs
          :model-value="viewMode"
          :items="[
            { label: '日', value: 'day' },
            { label: '周', value: 'week' },
            { label: '月', value: 'month' }
          ]"
          size="sm"
          class="w-auto"
          @update:model-value="(v: string | number) => { viewMode = String(v) as ViewMode }"
        />
        <UButton
          color="primary"
          size="sm"
          icon="i-lucide-plus"
          @click="openCreate()"
        >
          创建日程
        </UButton>
      </header>

      <!-- Month：表头与日期同一 grid，列宽天然对齐 -->
      <div
        v-if="viewMode === 'month'"
        class="min-h-0 flex-1 overflow-auto p-2"
      >
        <div
          class="grid h-full min-h-[28rem] grid-cols-7"
          style="grid-template-rows: auto; grid-auto-rows: minmax(4.5rem, 1fr)"
        >
          <div
            v-for="(label, idx) in weekDayLabels"
            :key="`month-label-${idx}`"
            class="flex items-center justify-center border-b border-[var(--ws-border-subtle)] py-2 text-xs text-[var(--ws-text-muted)]"
            :class="idx < 6 ? 'border-r border-[var(--ws-border-subtle)]' : ''"
          >
            {{ label }}
          </div>
          <button
            v-for="(day, dayIdx) in daysInView"
            :key="day.toISOString()"
            type="button"
            class="flex min-h-0 flex-col border-b border-[var(--ws-border-subtle)] p-1 text-left transition-colors hover:bg-[var(--ws-rail-hover)]/40"
            :class="(dayIdx % 7) < 6 ? 'border-r border-[var(--ws-border-subtle)]' : ''"
            @click="openCreate(day)"
          >
            <span
              class="mx-auto mb-1 flex size-6 shrink-0 items-center justify-center rounded-full text-xs"
              :class="[
                isSameDay(day, new Date()) ? 'bg-primary text-white' : '',
                !isSameMonth(day, anchorDate) ? 'text-[var(--ws-text-muted)] opacity-50' : ''
              ]"
            >
              {{ format(day, 'd') }}
            </span>
            <div class="min-h-0 flex-1 space-y-0.5 overflow-hidden">
              <div
                v-for="task in monthTasksForDay(day)"
                :key="`task-${task.id}`"
                class="truncate rounded px-1 py-0.5 text-[10px] leading-tight"
                :style="taskProjectionStyle()"
                @click.stop="openProjectedTask(task)"
              >
                ☐ {{ format(parseISO(task.dueTime!), 'HH:mm') }} {{ task.title }}
              </div>
              <div
                v-for="event in monthEventsForDay(day)"
                :key="eventKey(event)"
                class="truncate rounded px-1 py-0.5 text-[10px] leading-tight"
                :style="eventStyle(event)"
                @click.stop="handleEventClick(event)"
              >
                {{ event.allDay ? '' : format(parseISO(event.startTime), 'HH:mm') + ' ' }}{{ event.title }}
              </div>
            </div>
          </button>
        </div>
      </div>

      <!-- Day / Week：表头 sticky 在同一滚动容器内，避免滚动条占宽错位 -->
      <div
        v-else
        data-cal-scroll
        class="min-h-0 flex-1 overflow-auto"
      >
        <div
          class="sticky top-0 z-30 grid border-b border-[var(--ws-border-subtle)] bg-[var(--ws-main-bg)]"
          :style="{ gridTemplateColumns: `3.5rem repeat(${daysInView.length}, minmax(0, 1fr))` }"
        >
          <div class="border-r border-[var(--ws-border-subtle)]" />
          <div
            v-for="day in daysInView"
            :key="`head-${day.toISOString()}`"
            class="border-r border-[var(--ws-border-subtle)] px-1 py-2 text-center last:border-r-0"
          >
            <div class="text-xs text-[var(--ws-text-muted)]">
              {{ format(day, 'EEE', { locale: zhCN }) }}
            </div>
            <div
              class="mx-auto mt-0.5 flex size-7 items-center justify-center rounded-full text-sm font-medium"
              :class="isSameDay(day, new Date()) ? 'bg-primary text-white' : ''"
            >
              {{ format(day, 'd') }}
            </div>
            <div class="mt-1 min-h-6 space-y-0.5 px-0.5">
              <button
                v-for="event in allDayEventsForDay(day)"
                :key="eventKey(event)"
                type="button"
                class="block w-full truncate rounded px-1 py-0.5 text-left text-[10px]"
                :style="eventStyle(event)"
                @click="handleEventClick(event)"
              >
                {{ event.title }}
              </button>
            </div>
          </div>
        </div>

        <div
          class="relative grid"
          :style="{
            gridTemplateColumns: `3.5rem repeat(${daysInView.length}, minmax(0, 1fr))`,
            height: `${(HOUR_END - HOUR_START) * HOUR_HEIGHT}px`
          }"
        >
          <div class="relative border-r border-[var(--ws-border-subtle)]">
            <div
              v-for="hour in hours"
              :key="hour"
              class="absolute right-1 text-[10px] text-[var(--ws-text-muted)]"
              :style="{ top: `${(hour - HOUR_START) * HOUR_HEIGHT - 6}px` }"
            >
              {{ hour === 0 ? '' : `${String(hour).padStart(2, '0')}:00` }}
            </div>
            <div
              v-if="nowLineTopPx"
              class="pointer-events-none absolute right-1 z-20 -translate-y-1/2 text-[10px] tabular-nums text-[var(--ws-text-muted)]"
              :style="{ top: nowLineTopPx }"
            >
              {{ nowLineLabel }}
            </div>
          </div>

          <div
            v-for="(day, dayIndex) in daysInView"
            :key="day.toISOString()"
            data-cal-day-column
            class="relative border-r border-[var(--ws-border-subtle)] last:border-r-0"
          >
            <button
              v-for="hour in hours"
              :key="`${day.toISOString()}-${hour}`"
              type="button"
              class="absolute inset-x-0 border-t border-[var(--ws-border-subtle)]/70"
              :class="activeDrag ? 'pointer-events-none' : 'hover:bg-primary/5'"
              :style="{ top: `${(hour - HOUR_START) * HOUR_HEIGHT}px`, height: `${HOUR_HEIGHT}px` }"
              @click="openCreate(day, hour)"
            />
            <div
              v-for="event in timedEventsForDay(day)"
              :key="eventKey(event)"
              class="absolute inset-x-0.5 z-10 overflow-hidden rounded px-1 py-0.5 text-left text-[11px] leading-tight shadow-sm select-none"
              :class="[
                canDragEvent(event) ? 'cursor-grab active:cursor-grabbing' : '',
                isDraggedEvent(event) && activeDrag?.moved ? 'z-30 shadow-md ring-1 ring-primary/30' : '',
                activeDrag && !isDraggedEvent(event) ? 'pointer-events-none' : ''
              ]"
              :style="{ ...eventStyle(event), ...(timedLayout(event, day) ?? {}) }"
              @click.stop="handleEventClick(event)"
              @pointerdown="onEventPointerDown($event, event, day, 'move')"
            >
              <span class="pointer-events-none font-medium">{{ event.title }}</span>
              <span class="pointer-events-none mt-0.5 block opacity-80">
                {{ format(eventTimes(event).start, 'HH:mm') }}
                –
                {{ format(eventTimes(event).end, 'HH:mm') }}
              </span>
              <div
                v-if="canDragEvent(event)"
                class="absolute inset-x-0 bottom-0 cursor-ns-resize"
                :style="{ height: `${RESIZE_HANDLE_HEIGHT}px` }"
                @pointerdown.stop="onEventPointerDown($event, event, day, 'resize')"
              />
            </div>

            <button
              v-for="task in tasksForDay(day)"
              :key="`task-block-${task.id}`"
              type="button"
              class="absolute inset-x-0.5 z-10 overflow-hidden rounded px-1 py-0.5 text-left text-[11px] leading-tight shadow-sm"
              :style="{ ...taskProjectionStyle(), ...(timedTaskLayout(task, day) ?? {}) }"
              @click.stop="openProjectedTask(task)"
            >
              <span class="font-medium">☐ {{ task.title }}</span>
              <span class="mt-0.5 block opacity-80">
                截止 {{ format(parseISO(task.dueTime!), 'HH:mm') }}
              </span>
            </button>

            <!-- 当前时间线：过期淡灰 · 今日圆点+主色实线 · 未来主色实线 -->
            <div
              v-if="nowLineByDay[dayIndex] && nowLineTopPx"
              class="pointer-events-none absolute inset-x-0 z-20"
              :style="{ top: nowLineTopPx }"
            >
              <div
                class="absolute inset-x-0 top-0 -translate-y-1/2"
                :class="nowLineByDay[dayIndex] === 'past'
                  ? 'h-px bg-neutral-300/70 dark:bg-neutral-500/40'
                  : 'h-0.5 bg-primary'"
              />
              <span
                v-if="nowLineByDay[dayIndex] === 'today'"
                class="absolute left-0 top-0 size-2 -translate-x-1/2 -translate-y-1/2 rounded-full bg-primary"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <CalendarEventEditor
      v-model:open="eventEditorOpen"
      :mode="eventEditorMode"
      :event="editingEvent"
      :initial-start="createInitialStart"
      :initial-end="createInitialEnd"
      :initial-all-day="createInitialAllDay"
      @saved="refreshRange"
      @deleted="refreshRange"
    />

    <UModal
      v-model:open="createCalendarOpen"
      title="新建日历"
      description="创建你管理的日历图层"
    >
      <template #body>
        <div class="space-y-3">
          <UFormField label="名称">
            <UInput
              v-model="createCalendarForm.name"
              placeholder="请输入日历名称"
            />
          </UFormField>
          <UFormField label="颜色">
            <div class="flex flex-wrap gap-2">
              <button
                v-for="color in calendarStore.DEFAULT_COLORS"
                :key="color"
                type="button"
                class="size-7 rounded-full ring-offset-2"
                :class="createCalendarForm.color === color ? 'ring-2 ring-primary' : 'ring-1 ring-transparent'"
                :style="{ backgroundColor: color }"
                @click="createCalendarForm.color = color"
              />
            </div>
          </UFormField>
          <UFormField label="描述">
            <UTextarea
              v-model="createCalendarForm.description"
              :rows="2"
              placeholder="暂无描述"
            />
          </UFormField>
        </div>
      </template>
      <template #footer>
        <div class="flex justify-end gap-2">
          <UButton
            color="neutral"
            variant="ghost"
            @click="createCalendarOpen = false"
          >
            取消
          </UButton>
          <UButton
            color="primary"
            :loading="createCalendarSubmitting"
            @click="submitCreateCalendar"
          >
            创建
          </UButton>
        </div>
      </template>
    </UModal>

    <UModal
      v-model:open="shareModalOpen"
      :title="shareCalendar ? `共享：${shareCalendar.name}` : '共享日历'"
      description="添加成员只读访问此日历"
    >
      <template #body>
        <div class="space-y-4">
          <div>
            <p class="mb-2 text-sm font-medium">
              已共享给
            </p>
            <ul v-if="outgoingShares.length" class="space-y-1">
              <li
                v-for="share in outgoingShares"
                :key="share.id"
                class="flex items-center justify-between gap-2 rounded-lg bg-[var(--ws-input-bar-bg)] px-2 py-1.5 text-sm"
              >
                <span>{{ share.granteeNickname || share.granteeUserId }}</span>
                <UButton
                  icon="i-lucide-x"
                  size="xs"
                  color="neutral"
                  variant="ghost"
                  :loading="shareSubmitting"
                  @click="removeShareGrantee(share.id)"
                />
              </li>
            </ul>
            <p v-else class="text-sm text-[var(--ws-text-muted)]">
              尚未共享给任何人
            </p>
          </div>

          <UFormField label="添加成员">
            <UInput
              v-model="shareMemberKeyword"
              placeholder="搜索成员"
              icon="i-lucide-search"
              size="sm"
            />
            <div class="mt-2 max-h-40 space-y-1 overflow-y-auto">
              <button
                v-for="member in shareCandidateMembers"
                :key="member.id"
                type="button"
                class="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-sm hover:bg-[var(--ws-rail-hover)]"
                :disabled="shareSubmitting"
                @click="addShareGrantee(member.id)"
              >
                <span class="truncate">{{ member.nickname }}</span>
                <span class="ml-auto truncate text-xs text-[var(--ws-text-muted)]">{{ member.deptName }}</span>
              </button>
              <p
                v-if="shareCandidateMembers.length === 0"
                class="px-2 py-3 text-center text-xs text-[var(--ws-text-muted)]"
              >
                暂无可添加成员
              </p>
            </div>
          </UFormField>
        </div>
      </template>
      <template #footer>
        <div class="flex justify-end">
          <UButton
            color="neutral"
            variant="ghost"
            @click="shareModalOpen = false"
          >
            关闭
          </UButton>
        </div>
      </template>
    </UModal>
  </WorkspaceShell>
</template>
