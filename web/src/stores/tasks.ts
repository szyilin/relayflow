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
/** -web interim: detail/subtask overlay until detail-api lands. Integrate: remove. */
const DETAIL_LOCAL_KEY = 'relayflow-task-detail-local-v1'

interface LocalDetailBundle {
  extras: Record<string, Partial<TaskItem>>
  subtasks: Record<string, TaskItem[]>
}

function readLocalBundle(): LocalDetailBundle {
  try {
    const raw = localStorage.getItem(DETAIL_LOCAL_KEY)
    if (!raw) {
      return { extras: {}, subtasks: {} }
    }
    const parsed = JSON.parse(raw) as LocalDetailBundle
    return {
      extras: parsed.extras ?? {},
      subtasks: parsed.subtasks ?? {}
    }
  } catch {
    return { extras: {}, subtasks: {} }
  }
}

function writeLocalBundle(bundle: LocalDetailBundle) {
  localStorage.setItem(DETAIL_LOCAL_KEY, JSON.stringify(bundle))
}

function mergeDetail(base: TaskItem, extra?: Partial<TaskItem>): TaskItem {
  if (!extra) {
    return { ...base }
  }
  return {
    ...base,
    ...extra,
    id: base.id,
    createTime: base.createTime || extra.createTime || new Date().toISOString()
  }
}

function recomputeProgress(parent: TaskItem, children: TaskItem[]): TaskItem {
  const total = children.length
  const done = children.filter(c => c.status === 'DONE').length
  return {
    ...parent,
    subtaskTotal: total,
    subtaskDoneCount: done
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

  const localExtras = ref<Record<string, Partial<TaskItem>>>({})
  const localSubtasks = ref<Record<string, TaskItem[]>>({})

  /** Calendar projection cache for visible range. */
  const dueRangeItems = ref<TaskItem[]>([])
  const dueRangeLoading = ref(false)

  const todoItems = computed(() => items.value.filter(item => item.status === 'TODO'))
  const doneItems = computed(() => items.value.filter(item => item.status === 'DONE'))

  function hydrateLocal() {
    const bundle = readLocalBundle()
    localExtras.value = bundle.extras
    localSubtasks.value = bundle.subtasks
  }

  function persistLocal() {
    writeLocalBundle({
      extras: localExtras.value,
      subtasks: localSubtasks.value
    })
  }

  function applyLocalToList(list: TaskItem[]): TaskItem[] {
    return list.map((item) => {
      const merged = mergeDetail(item, localExtras.value[item.id])
      const children = localSubtasks.value[item.id]
      if (children) {
        return recomputeProgress(merged, children)
      }
      return merged
    })
  }

  async function fetchMyTasks() {
    loading.value = true
    hydrateLocal()
    try {
      const data = await getTaskPage({ pageNo: 1, pageSize: 100 })
      items.value = applyLocalToList(data.list)
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
      let detail: TaskItem | null = null
      try {
        detail = await getTaskById(id)
      } catch {
        const fromList = items.value.find(t => t.id === id)
        detail = fromList ? mergeDetail(fromList, localExtras.value[id]) : null
        if (!detail && localExtras.value[id]) {
          detail = mergeDetail({
            id,
            title: localExtras.value[id]!.title || '任务',
            status: 'TODO',
            createTime: new Date().toISOString()
          }, localExtras.value[id])
        }
      }
      if (!detail) {
        selectedDetail.value = null
        selectedSubtasks.value = []
        return
      }
      detail = mergeDetail(detail, localExtras.value[id])

      let subtasks: TaskItem[] = []
      try {
        subtasks = await getTaskSubtasks(id)
      } catch {
        subtasks = []
      }
      if (localSubtasks.value[id]?.length) {
        // -web: keep local subtasks when API missing or empty
        if (subtasks.length === 0) {
          subtasks = [...localSubtasks.value[id]!]
        }
      }

      selectedDetail.value = recomputeProgress(detail, subtasks)
      selectedSubtasks.value = subtasks

      const idx = items.value.findIndex(t => t.id === id)
      if (idx >= 0) {
        items.value[idx] = recomputeProgress(
          mergeDetail(items.value[idx]!, localExtras.value[id]),
          subtasks
        )
      }
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
    const patchStatus = (list: TaskItem[]) => {
      const row = list.find(item => item.id === id)
      if (row) {
        row.status = done ? 'DONE' : 'TODO'
      }
    }
    const previousRoot = items.value.find(item => item.id === id)?.status
    const previousDetail = selectedDetail.value?.id === id ? selectedDetail.value.status : null
    const previousSub = selectedSubtasks.value.find(s => s.id === id)?.status

    patchStatus(items.value)
    if (selectedDetail.value?.id === id) {
      selectedDetail.value = { ...selectedDetail.value, status: done ? 'DONE' : 'TODO' }
    }
    patchStatus(selectedSubtasks.value)
    if (selectedId.value) {
      const children = selectedSubtasks.value
      if (selectedDetail.value && children.some(c => c.id === id)) {
        selectedDetail.value = recomputeProgress(selectedDetail.value, children)
        const idx = items.value.findIndex(t => t.id === selectedId.value)
        if (idx >= 0) {
          items.value[idx] = recomputeProgress(items.value[idx]!, children)
        }
        localSubtasks.value = {
          ...localSubtasks.value,
          [selectedId.value]: children.map(c => ({ ...c }))
        }
        persistLocal()
      }
    }

    try {
      await toggleTaskDone(id, done)
    } catch (error) {
      if (previousRoot != null) {
        const row = items.value.find(item => item.id === id)
        if (row) {
          row.status = previousRoot
        }
      }
      if (previousDetail != null && selectedDetail.value?.id === id) {
        selectedDetail.value = { ...selectedDetail.value, status: previousDetail }
      }
      if (previousSub != null) {
        const row = selectedSubtasks.value.find(s => s.id === id)
        if (row) {
          row.status = previousSub
        }
      }
      throw error
    }
  }

  async function removeTask(id: string) {
    saving.value = true
    try {
      await deleteTask(id)
      if (localExtras.value[id]) {
        const next = { ...localExtras.value }
        delete next[id]
        localExtras.value = next
      }
      if (localSubtasks.value[id]) {
        const next = { ...localSubtasks.value }
        delete next[id]
        localSubtasks.value = next
      }
      // remove from parent local subtasks
      for (const [parentId, list] of Object.entries(localSubtasks.value)) {
        if (list.some(s => s.id === id)) {
          localSubtasks.value = {
            ...localSubtasks.value,
            [parentId]: list.filter(s => s.id !== id)
          }
        }
      }
      persistLocal()
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
      try {
        await updateTask(payload)
      } catch {
        // -web: classic update may reject unknown fields; continue with local overlay
      }
      // Keep detail overlay until detail-api integrate (new fields + refresh resilience)
      localExtras.value = {
        ...localExtras.value,
        [payload.id]: {
          ...(localExtras.value[payload.id] ?? {}),
          title: payload.title,
          startTime: payload.startTime,
          dueTime: payload.dueTime,
          remindBeforeMinutes: payload.remindBeforeMinutes,
          description: payload.description
        }
      }
      persistLocal()
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
      let id: string
      try {
        id = await createSubtask({ parentId, title })
      } catch {
        id = `local-${Date.now()}`
        const child: TaskItem = {
          id,
          title: title.trim(),
          status: 'TODO',
          parentId,
          createTime: new Date().toISOString(),
          dueTime: null,
          startTime: null,
          description: null,
          remindBeforeMinutes: null
        }
        const prev = localSubtasks.value[parentId] ?? []
        localSubtasks.value = {
          ...localSubtasks.value,
          [parentId]: [...prev, child]
        }
        persistLocal()
      }
      await selectTask(parentId)
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
    hydrateLocal,
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
