import type { ComputedRef, Ref } from 'vue'
import { computed } from 'vue'
import { addDays, format, isSameDay, parseISO, startOfDay } from 'date-fns'
import type { CalendarEvent } from '../../api/app/calendar'
import { eventKey } from '../../api/app/calendar'
import type { CalendarViewMode } from './constants'
import { HOUR_END, HOUR_HEIGHT, HOUR_START } from './constants'

export interface CalendarDragPreview {
  event: CalendarEvent
  previewStart: Date
  previewEnd: Date
}

/**
 * Day/week timed grid layout helpers (event clipping, all-day rows, now line).
 */
export function useCalendarTimedGrid(options: {
  viewMode: Ref<CalendarViewMode>
  daysInView: ComputedRef<Date[]>
  eventsInView: ComputedRef<CalendarEvent[]>
  nowTick: Ref<Date>
  activeDrag: Ref<CalendarDragPreview | null>
}) {
  const { viewMode, daysInView, eventsInView, nowTick, activeDrag } = options

  function eventTimes(event: CalendarEvent): { start: Date, end: Date } {
    const drag = activeDrag.value
    if (drag && event.id === drag.event.id) {
      const sameOccurrence = !drag.event.recurring
        || eventKey(event) === eventKey(drag.event)
        || (event.instanceStart ?? event.startTime) === (drag.event.instanceStart ?? drag.event.startTime)
      if (sameOccurrence) {
        return { start: drag.previewStart, end: drag.previewEnd }
      }
    }
    return { start: parseISO(event.startTime), end: parseISO(event.endTime) }
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

  const hours = computed(() =>
    Array.from({ length: HOUR_END - HOUR_START }, (_, i) => HOUR_START + i))

  return {
    eventTimes,
    isDraggedEvent,
    timedLayout,
    allDayEventsForDay,
    timedEventsForDay,
    monthEventsForDay,
    nowLineTop,
    nowLineVisibleDayIndex,
    nowLineByDay,
    nowLineLabel,
    nowLineTopPx,
    hours
  }
}
