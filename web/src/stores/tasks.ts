import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  createTask,
  deleteTask,
  getTaskPage,
  toggleTaskDone,
  type TaskItem
} from '../api/app/task'
import { ApiError } from '../api/request'
import {
  mockCreateTask,
  mockDeleteTask,
  mockTaskPage,
  mockToggleTaskDone
} from '../mocks/tasks'

function shouldUseMock(error: unknown): boolean {
  if (error instanceof ApiError) {
    return error.code === 0 || error.code >= 500 || error.code === 404
  }
  return true
}

export const useTasksStore = defineStore('tasks', () => {
  const items = ref<TaskItem[]>([])
  const total = ref(0)
  const loading = ref(false)
  const saving = ref(false)
  const usingMock = ref(false)

  const todoItems = computed(() => items.value.filter(item => item.status === 'TODO'))
  const doneItems = computed(() => items.value.filter(item => item.status === 'DONE'))

  async function fetchMyTasks() {
    loading.value = true
    try {
      const data = await getTaskPage({ pageNo: 1, pageSize: 100 })
      items.value = data.list
      total.value = data.total
      usingMock.value = false
    } catch (error) {
      if (shouldUseMock(error)) {
        const mock = mockTaskPage()
        items.value = mock.list
        total.value = mock.total
        usingMock.value = true
        return
      }
      items.value = []
      total.value = 0
      throw error
    } finally {
      loading.value = false
    }
  }

  async function addTask(payload: { title: string, dueTime?: string | null }) {
    saving.value = true
    try {
      if (usingMock.value) {
        mockCreateTask(payload)
        await fetchMyTasks()
        return
      }
      await createTask(payload)
      await fetchMyTasks()
    } catch (error) {
      if (!usingMock.value && shouldUseMock(error)) {
        usingMock.value = true
        await addTask(payload)
        return
      }
      throw error
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
      if (usingMock.value) {
        mockToggleTaskDone(id, done)
        return
      }
      await toggleTaskDone(id, done)
    } catch (error) {
      if (item && previous) {
        item.status = previous
      }
      if (!usingMock.value && shouldUseMock(error)) {
        usingMock.value = true
        await setTaskDone(id, done)
        return
      }
      throw error
    }
  }

  async function removeTask(id: string) {
    saving.value = true
    try {
      if (usingMock.value) {
        mockDeleteTask(id)
        await fetchMyTasks()
        return
      }
      await deleteTask(id)
      await fetchMyTasks()
    } catch (error) {
      if (!usingMock.value && shouldUseMock(error)) {
        usingMock.value = true
        await removeTask(id)
        return
      }
      throw error
    } finally {
      saving.value = false
    }
  }

  return {
    items,
    total,
    loading,
    saving,
    usingMock,
    todoItems,
    doneItems,
    fetchMyTasks,
    addTask,
    setTaskDone,
    removeTask
  }
})
