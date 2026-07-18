import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import type { TaskItem } from '../../api/app/task'
import { useAuthStore } from '../auth'
import {
  type TaskViewConfig,
  type TaskViewContextKey,
  contextStorageKey,
  defaultViewConfig,
  loadLocalViewConfigMap,
  resolveViewConfig,
  saveLocalViewConfigMap
} from './viewConfigLocal'

export const useTaskViewConfigStore = defineStore('taskViewConfig', () => {
  const map = ref<Record<string, TaskViewConfig>>({})
  const activeContext = ref<TaskViewContextKey>({ contextType: 'MINE' })
  const loaded = ref(false)

  const activeConfig = computed(() =>
    resolveViewConfig(map.value, activeContext.value))

  function ensureLoaded() {
    if (loaded.value) {
      return
    }
    const auth = useAuthStore()
    const tenantId = auth.tenantId ? String(auth.tenantId) : ''
    const userId = auth.userId ? String(auth.userId) : ''
    map.value = loadLocalViewConfigMap(tenantId, userId)
    loaded.value = true
  }

  function persist() {
    const auth = useAuthStore()
    const tenantId = auth.tenantId ? String(auth.tenantId) : ''
    const userId = auth.userId ? String(auth.userId) : ''
    saveLocalViewConfigMap(tenantId, userId, map.value)
  }

  function setActiveContext(ctx: TaskViewContextKey) {
    ensureLoaded()
    activeContext.value = {
      contextType: ctx.contextType,
      contextId: ctx.contextType === 'LIST' ? ctx.contextId ?? null : null
    }
  }

  function patchActiveConfig(patch: Partial<TaskViewConfig>) {
    ensureLoaded()
    const key = contextStorageKey(activeContext.value)
    const next: TaskViewConfig = {
      ...resolveViewConfig(map.value, activeContext.value),
      ...patch
    }
    map.value = { ...map.value, [key]: next }
    persist()
  }

  function resetActiveToDefault() {
    ensureLoaded()
    const key = contextStorageKey(activeContext.value)
    const next = defaultViewConfig(activeContext.value.contextType)
    map.value = { ...map.value, [key]: next }
    persist()
  }

  function resetForTenantSwitch() {
    map.value = {}
    loaded.value = false
    activeContext.value = { contextType: 'MINE' }
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

  watch(
    () => {
      const auth = useAuthStore()
      return `${auth.tenantId ?? ''}:${auth.userId ?? ''}`
    },
    () => {
      loaded.value = false
      ensureLoaded()
    }
  )

  return {
    activeContext,
    activeConfig,
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
