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
import type { CalendarEvent } from '../../../api/app/calendar'
import { useCalendarStore } from '../../../stores/calendar'
import { useUserPreferenceStore } from '../../../stores/userPreference'

type ViewMode = 'day' | 'week' | 'month'

const HOUR_START = 0
const HOUR_END = 24
const HOUR_HEIGHT = 48

const route = useRoute()
const router = useRouter()
const calendarStore = useCalendarStore()
const preference = useUserPreferenceStore()
const toast = useToast()

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

function timedLayout(event: CalendarEvent, day: Date): { top: string, height: string } | null {
  if (event.allDay) {
    return null
  }
  const start = parseISO(event.startTime)
  const end = parseISO(event.endTime)
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
    const start = parseISO(event.startTime)
    const end = parseISO(event.endTime)
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
  return `${(minutes / 60) * HOUR_HEIGHT}px`
}

const nowLineVisibleDayIndex = computed(() => {
  const n = nowTick.value
  return daysInView.value.findIndex(d => isSameDay(d, n))
})

async function refreshRange() {
  await calendarStore.fetchCalendars()
  await calendarStore.fetchEvents({
    from: formatISO(rangeStart.value),
    to: formatISO(rangeEnd.value)
  })
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
})

watch([rangeStart, rangeEnd, viewMode], () => {
  void refreshRange().catch((error) => {
    toast.add({
      title: error instanceof Error ? error.message : '加载日程失败',
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
              v-for="cal in calendarStore.calendars"
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
                v-for="event in monthEventsForDay(day)"
                :key="event.id"
                class="truncate rounded px-1 py-0.5 text-[10px] leading-tight"
                :style="eventStyle(event)"
                @click.stop="openEvent(event)"
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
                :key="event.id"
                type="button"
                class="block w-full truncate rounded px-1 py-0.5 text-left text-[10px]"
                :style="eventStyle(event)"
                @click="openEvent(event)"
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
          </div>

          <div
            v-for="(day, dayIndex) in daysInView"
            :key="day.toISOString()"
            class="relative border-r border-[var(--ws-border-subtle)] last:border-r-0"
          >
            <button
              v-for="hour in hours"
              :key="`${day.toISOString()}-${hour}`"
              type="button"
              class="absolute inset-x-0 border-t border-[var(--ws-border-subtle)]/70 hover:bg-primary/5"
              :style="{ top: `${(hour - HOUR_START) * HOUR_HEIGHT}px`, height: `${HOUR_HEIGHT}px` }"
              @click="openCreate(day, hour)"
            />
            <button
              v-for="event in timedEventsForDay(day)"
              :key="event.id"
              type="button"
              class="absolute inset-x-0.5 z-10 overflow-hidden rounded px-1 py-0.5 text-left text-[11px] leading-tight shadow-sm"
              :style="{ ...eventStyle(event), ...timedLayout(event, day)! }"
              @click.stop="openEvent(event)"
            >
              <span class="font-medium">{{ event.title }}</span>
              <span class="mt-0.5 block opacity-80">
                {{ format(parseISO(event.startTime), 'HH:mm') }}
                –
                {{ format(parseISO(event.endTime), 'HH:mm') }}
              </span>
            </button>

            <div
              v-if="nowLineTop() && nowLineVisibleDayIndex === dayIndex"
              class="pointer-events-none absolute inset-x-0 z-20 flex items-center"
              :style="{ top: nowLineTop()! }"
            >
              <span class="size-2 shrink-0 rounded-full bg-red-500" />
              <span class="h-0.5 flex-1 bg-red-500" />
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
  </WorkspaceShell>
</template>
