import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  createSubtask,
  createTask,
  deleteTask,
  getTaskById,
  getTaskDueRange,
  getTaskPage,
  getTaskSubtasks,
  toggleTaskDone,
  updateTask,
  type TaskDetailUpdatePayload,
  type TaskItem
} from '../api/app/task'

const DUE_RANGE_LIMIT = 200
/** Cleared once after detail-api integrate; was -web interim overlay. */
const LEGACY_DETAIL_LOCAL_KEY = 'relayflow-task-detail-local-v1'

function recomputeProgress(parent: TaskItem, children: TaskItem[]): TaskItem {
  const total = children.length
  const done = children.filter(c => c.status === 'DONE').length
  return {
    ...parent,
    subtaskTotal: total,
    subtaskDoneCount: done
  }
}

function clearLegacyDetailLocal() {
  try {
    localStorage.removeItem(LEGACY_DETAIL_LOCAL_KEY)
  } catch {
    // ignore
  }
}

export const useTasksStore = defineStore('tasks', () => {
  const items = ref<TaskItem[]>([])
  const total = ref(0)
  const loading = ref(false)
  const saving = ref(false)
  const detailLoading = ref(false)

  const selectedId = ref<string | null>(null)
  const selectedDetail = ref<TaskItem | null>(null)
  const selectedSubtasks = ref<TaskItem[]>([])

  /** Calendar projection cache for visible range. */
  const dueRangeItems = ref<TaskItem[]>([])
  const dueRangeLoading = ref(false)

  const todoItems = computed(() => items.value.filter(item => item.status === 'TODO'))
  const doneItems = computed(() => items.value.filter(item => item.status === 'DONE'))

  async function fetchMyTasks() {
    loading.value = true
    clearLegacyDetailLocal()
    try {
      const data = await getTaskPage({ pageNo: 1, pageSize: 100 })
      items.value = data.list
      total.value = data.total
    } finally {
      loading.value = false
    }
  }

  async function fetchDueRange(fromIso: string, toIso: string) {
    dueRangeLoading.value = true
    try {
      dueRangeItems.value = await getTaskDueRange({
        from: fromIso,
        to: toIso,
        limit: DUE_RANGE_LIMIT
      })
    } finally {
      dueRangeLoading.value = false
    }
  }

  function clearDueRange() {
    dueRangeItems.value = []
  }

  async function selectTask(id: string | null) {
    selectedId.value = id
    if (!id) {
      selectedDetail.value = null
      selectedSubtasks.value = []
      return
    }
    detailLoading.value = true
    try {
      const [detail, subtasks] = await Promise.all([
        getTaskById(id),
        getTaskSubtasks(id)
      ])
      selectedDetail.value = recomputeProgress(detail, subtasks)
      selectedSubtasks.value = subtasks

      const idx = items.value.findIndex(t => t.id === id)
      if (idx >= 0) {
        items.value[idx] = recomputeProgress(detail, subtasks)
      }
    } catch (error) {
      selectedDetail.value = null
      selectedSubtasks.value = []
      throw error
    } finally {
      detailLoading.value = false
    }
  }

  async function addTask(payload: { title: string, dueTime?: string | null, startTime?: string | null }) {
    saving.value = true
    try {
      const id = await createTask(payload)
      await fetchMyTasks()
      await selectTask(id)
      return id
    } finally {
      saving.value = false
    }
  }

  async function setTaskDone(id: string, done: boolean) {
    const snapshotItems = items.value.map(item => ({ ...item }))
    const snapshotDetail = selectedDetail.value ? { ...selectedDetail.value } : null
    const snapshotSubtasks = selectedSubtasks.value.map(item => ({ ...item }))

    const patchStatus = (list: TaskItem[]) => {
      const row = list.find(item => item.id === id)
      if (row) {
        row.status = done ? 'DONE' : 'TODO'
      }
    }

    patchStatus(items.value)
    if (selectedDetail.value?.id === id) {
      selectedDetail.value = { ...selectedDetail.value, status: done ? 'DONE' : 'TODO' }
    }
    patchStatus(selectedSubtasks.value)
    if (selectedId.value && selectedDetail.value && selectedSubtasks.value.some(c => c.id === id)) {
      selectedDetail.value = recomputeProgress(selectedDetail.value, selectedSubtasks.value)
      const idx = items.value.findIndex(t => t.id === selectedId.value)
      if (idx >= 0) {
        items.value[idx] = recomputeProgress(items.value[idx]!, selectedSubtasks.value)
      }
    }

    try {
      await toggleTaskDone(id, done)
    } catch (error) {
      items.value = snapshotItems
      selectedDetail.value = snapshotDetail
      selectedSubtasks.value = snapshotSubtasks
      throw error
    }
  }

  async function removeTask(id: string) {
    saving.value = true
    try {
      await deleteTask(id)
      if (selectedId.value === id) {
        await selectTask(null)
      } else if (selectedId.value) {
        await selectTask(selectedId.value)
      }
      await fetchMyTasks()
    } finally {
      saving.value = false
    }
  }

  async function saveDetail(payload: TaskDetailUpdatePayload) {
    saving.value = true
    try {
      await updateTask(payload)
      await fetchMyTasks()
      if (selectedId.value === payload.id) {
        await selectTask(payload.id)
      }
    } finally {
      saving.value = false
    }
  }

  async function addSubtask(parentId: string, title: string) {
    saving.value = true
    try {
      const id = await createSubtask({ parentId, title })
      await selectTask(parentId)
      await fetchMyTasks()
      return id
    } finally {
      saving.value = false
    }
  }

  return {
    items,
    total,
    loading,
    saving,
    detailLoading,
    selectedId,
    selectedDetail,
    selectedSubtasks,
    dueRangeItems,
    dueRangeLoading,
    todoItems,
    doneItems,
    fetchMyTasks,
    fetchDueRange,
    clearDueRange,
    selectTask,
    addTask,
    setTaskDone,
    removeTask,
    saveDetail,
    addSubtask
  }
})
