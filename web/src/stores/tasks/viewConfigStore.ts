import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { TaskItem } from '../../api/app/task'
import {
  getTaskViewConfig,
  saveTaskViewConfig
} from '../../api/app/taskViewConfig'
import {
  type TaskViewConfig,
  type TaskViewContextKey,
  clearLegacyViewConfigLocalStorage,
  contextStorageKey,
  defaultViewConfig,
  resolveViewConfig
} from './viewConfigLocal'

export const useTaskViewConfigStore = defineStore('taskViewConfig', () => {
  const map = ref<Record<string, TaskViewConfig>>({})
  const activeContext = ref<TaskViewContextKey>({ contextType: 'MINE' })
  const loading = ref(false)
  const saving = ref(false)
  let legacyCleared = false

  const activeConfig = computed(() =>
    resolveViewConfig(map.value, activeContext.value))

  function ensureLegacyCleared() {
    if (legacyCleared) {
      return
    }
    clearLegacyViewConfigLocalStorage()
    legacyCleared = true
  }

  async function setActiveContext(ctx: TaskViewContextKey) {
    ensureLegacyCleared()
    activeContext.value = {
      contextType: ctx.contextType,
      contextId: ctx.contextType === 'LIST' ? ctx.contextId ?? null : null
    }
    const key = contextStorageKey(activeContext.value)
    loading.value = true
    try {
      const config = await getTaskViewConfig({
        contextType: activeContext.value.contextType,
        contextId: activeContext.value.contextId
      })
      map.value = { ...map.value, [key]: config }
    } catch {
      if (!map.value[key]) {
        map.value = {
          ...map.value,
          [key]: defaultViewConfig(activeContext.value.contextType)
        }
      }
    } finally {
      loading.value = false
    }
  }

  async function persistActive(config: TaskViewConfig) {
    saving.value = true
    try {
      await saveTaskViewConfig({
        contextType: activeContext.value.contextType,
        contextId: activeContext.value.contextId,
        config
      })
    } finally {
      saving.value = false
    }
  }

  async function patchActiveConfig(patch: Partial<TaskViewConfig>) {
    const key = contextStorageKey(activeContext.value)
    const next: TaskViewConfig = {
      ...resolveViewConfig(map.value, activeContext.value),
      ...patch
    }
    map.value = { ...map.value, [key]: next }
    try {
      await persistActive(next)
    } catch {
      // keep optimistic UI; next context reload will reconcile
    }
  }

  async function resetActiveToDefault() {
    const key = contextStorageKey(activeContext.value)
    const next = defaultViewConfig(activeContext.value.contextType)
    map.value = { ...map.value, [key]: next }
    try {
      await persistActive(next)
    } catch {
      // keep optimistic UI
    }
  }

  function resetForTenantSwitch() {
    map.value = {}
    activeContext.value = { contextType: 'MINE' }
    legacyCleared = false
  }

  /** @deprecated no-op; kept for call sites that ran sync ensure before fetch */
  function ensureLoaded() {
    ensureLegacyCleared()
  }

  function applyClientTransforms(items: TaskItem[]): TaskItem[] {
    const cfg = activeConfig.value
    let next = [...items]

    const statusFilter = cfg.filters.find(f => f.field === 'status' && f.op === 'IN')
    if (statusFilter?.values?.length) {
      const allowed = new Set(statusFilter.values)
      next = next.filter(item => allowed.has(item.status))
    }

    if (cfg.sort !== 'MANUAL') {
      const { key, order } = cfg.sort
      const dir = order === 'ASC' ? 1 : -1
      next.sort((a, b) => {
        const av = sortValue(a, key)
        const bv = sortValue(b, key)
        if (av < bv) {
          return -1 * dir
        }
        if (av > bv) {
          return 1 * dir
        }
        return 0
      })
    }
    return next
  }

  function sortValue(item: TaskItem, key: string): string | number {
    switch (key) {
      case 'dueTime':
        return item.dueTime ? new Date(item.dueTime).getTime() : Number.POSITIVE_INFINITY
      case 'title':
        return (item.title || '').toLowerCase()
      case 'status':
        return item.status
      case 'updateTime':
        return item.createTime ? new Date(item.createTime).getTime() : 0
      case 'createTime':
      default:
        return item.createTime ? new Date(item.createTime).getTime() : 0
    }
  }

  function isFieldVisible(fieldKey: string): boolean {
    return activeConfig.value.visibleFieldKeys.includes(fieldKey)
  }

  return {
    activeContext,
    activeConfig,
    loading,
    saving,
    setActiveContext,
    patchActiveConfig,
    resetActiveToDefault,
    resetForTenantSwitch,
    applyClientTransforms,
    isFieldVisible,
    ensureLoaded
  }
})

export type {
  TaskViewConfig,
  TaskViewContextType,
  TaskViewContextKey,
  TaskViewGroupBy,
  TaskViewSort,
  TaskViewSortKey,
  TaskViewDisplayMode
} from './viewConfigLocal'
