import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  createTask,
  deleteTask,
  getTaskDueRange,
  getTaskPage,
  toggleTaskDone,
  type TaskItem
} from '../api/app/task'

const DUE_RANGE_LIMIT = 200

function dueTimeInRange(dueTime: string | null | undefined, from: Date, to: Date): boolean {
  if (!dueTime) {
    return false
  }
  const due = new Date(dueTime)
  if (Number.isNaN(due.getTime())) {
    return false
  }
  return due >= from && due < to
}

export const useTasksStore = defineStore('tasks', () => {
  const items = ref<TaskItem[]>([])
  const total = ref(0)
  const loading = ref(false)
  const saving = ref(false)

  /** Calendar projection cache for visible range. */
  const dueRangeItems = ref<TaskItem[]>([])
  const dueRangeLoading = ref(false)

  const todoItems = computed(() => items.value.filter(item => item.status === 'TODO'))
  const doneItems = computed(() => items.value.filter(item => item.status === 'DONE'))

  async function fetchMyTasks() {
    loading.value = true
    try {
      const data = await getTaskPage({ pageNo: 1, pageSize: 100 })
      items.value = data.list
      total.value = data.total
    } finally {
      loading.value = false
    }
  }

  /**
   * Load TODO tasks with dueTime in [from, to) for calendar projection.
   * Prefers GET /due-range; falls back to page + client filter until -api lands.
   */
  async function fetchDueRange(fromIso: string, toIso: string) {
    dueRangeLoading.value = true
    const from = new Date(fromIso)
    const to = new Date(toIso)
    try {
      try {
        dueRangeItems.value = await getTaskDueRange({
          from: fromIso,
          to: toIso,
          limit: DUE_RANGE_LIMIT
        })
      } catch {
        // -web interim: due-range not ready — filter existing page API
        const data = await getTaskPage({ pageNo: 1, pageSize: DUE_RANGE_LIMIT, status: 'TODO' })
        dueRangeItems.value = data.list
          .filter(item => item.status === 'TODO' && dueTimeInRange(item.dueTime, from, to))
          .slice(0, DUE_RANGE_LIMIT)
      }
    } finally {
      dueRangeLoading.value = false
    }
  }

  function clearDueRange() {
    dueRangeItems.value = []
  }

  async function addTask(payload: { title: string, dueTime?: string | null }) {
    saving.value = true
    try {
      await createTask(payload)
      await fetchMyTasks()
    } finally {
      saving.value = false
    }
  }

  async function setTaskDone(id: string, done: boolean) {
    const previous = items.value.find(item => item.id === id)?.status
    const item = items.value.find(row => row.id === id)
    if (item) {
      item.status = done ? 'DONE' : 'TODO'
    }
    try {
      await toggleTaskDone(id, done)
    } catch (error) {
      if (item && previous) {
        item.status = previous
      }
      throw error
    }
  }

  async function removeTask(id: string) {
    saving.value = true
    try {
      await deleteTask(id)
      await fetchMyTasks()
    } finally {
      saving.value = false
    }
  }

  return {
    items,
    total,
    loading,
    saving,
    dueRangeItems,
    dueRangeLoading,
    todoItems,
    doneItems,
    fetchMyTasks,
    fetchDueRange,
    clearDueRange,
    addTask,
    setTaskDone,
    removeTask
  }
})
