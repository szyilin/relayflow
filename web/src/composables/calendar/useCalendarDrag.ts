import type { ComputedRef, Ref } from 'vue'
import { ref } from 'vue'
import { formatISO, parseISO, startOfDay } from 'date-fns'
import type { CalendarEvent } from '../../api/app/calendar'
import type { CalendarViewMode } from './constants'
import { DRAG_THRESHOLD, HOUR_END, HOUR_HEIGHT, HOUR_START } from './constants'
import type { CalendarDragPreview } from './useCalendarTimedGrid'

export interface ActiveDrag extends CalendarDragPreview {
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
}

export function useCalendarDrag(options: {
  viewMode: Ref<CalendarViewMode>
  daysInView: ComputedRef<Date[]>
  rescheduleEvent: (payload: {
    id: string
    startTime: string
    endTime: string
    editScope?: 'THIS' | 'ALL'
    instanceStart?: string
  }) => Promise<unknown>
  onError: (message: string) => void
}) {
  const activeDrag = ref<ActiveDrag | null>(null)
  const suppressClick = ref(false)

  function canDragEvent(event: CalendarEvent): boolean {
    return options.viewMode.value !== 'month'
      && event.viewerRole === 'ORGANIZER'
      && !event.allDay
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
    const targetDay = dayIndex >= 0 ? options.daysInView.value[dayIndex]! : drag.day

    if (drag.mode === 'resize') {
      const endMinutes = minutesFromClientY(e.clientY, column)
      const startMinutes = drag.previewStart.getHours() * 60 + drag.previewStart.getMinutes()
      drag.previewEnd = dateWithMinutes(targetDay, Math.max(startMinutes + 15, endMinutes))
    } else {
      const duration = drag.originEnd.getTime() - drag.originStart.getTime()
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
      try {
        await options.rescheduleEvent({
          id: drag.event.id,
          startTime: formatISO(drag.previewStart),
          endTime: formatISO(drag.previewEnd),
          editScope: drag.event.recurring ? 'THIS' : undefined,
          instanceStart: drag.event.recurring
            ? (drag.event.instanceStart ?? drag.event.startTime)
            : undefined
        })
      } catch (error) {
        options.onError(error instanceof Error ? error.message : '改期失败')
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

  return {
    activeDrag,
    suppressClick,
    canDragEvent,
    onEventPointerDown,
    cleanupDragListeners
  }
}
