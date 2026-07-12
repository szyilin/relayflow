import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  createTask,
  deleteTask,
  getTaskPage,
  toggleTaskDone,
  type TaskItem
} from '../api/app/task'

export const useTasksStore = defineStore('tasks', () => {
  const items = ref<TaskItem[]>([])
  const total = ref(0)
  const loading = ref(false)
  const saving = ref(false)

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
    todoItems,
    doneItems,
    fetchMyTasks,
    addTask,
    setTaskDone,
    removeTask
  }
})
