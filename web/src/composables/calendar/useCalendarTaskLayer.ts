import type { ComputedRef, Ref } from 'vue'
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { addDays, isSameDay, parseISO, startOfDay } from 'date-fns'
import type { TaskItem } from '../../api/app/task'
import { HOUR_HEIGHT, HOUR_START, TASK_BLOCK_MINUTES, TASK_LAYER_COLOR } from './constants'

/**
 * Task due-time projections on the calendar grid (not cal_event).
 */
export function useCalendarTaskLayer(options: {
  taskLayerVisible: Ref<boolean>
  dueRangeItems: Ref<TaskItem[]> | ComputedRef<TaskItem[]>
}) {
  const router = useRouter()

  const projectedTasks = computed(() =>
    options.taskLayerVisible.value ? options.dueRangeItems.value : [])

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

  return {
    projectedTasks,
    taskProjectionStyle,
    openProjectedTask,
    tasksForDay,
    monthTasksForDay,
    timedTaskLayout
  }
}
