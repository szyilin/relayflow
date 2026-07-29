import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  cloneCalProductSettings,
  createAllStaffCalendar as apiCreateAllStaff,
  defaultCalProductSettings,
  deleteAllStaffCalendar as apiDeleteAllStaff,
  getCalProductSettings,
  updateAllStaffCalendar as apiUpdateAllStaff,
  updateCalProductSettings as apiUpdateSettings,
  type CalAllStaffCalendar,
  type CalAllStaffScopeItem,
  type CalProductSettings
} from '../api/admin/calProductSettings'
import { ApiError } from '../api/request'

function toError(error: unknown, fallback: string): Error {
  if (error instanceof ApiError) {
    return new Error(error.message || fallback)
  }
  if (error instanceof Error) {
    return error
  }
  return new Error(fallback)
}

export const useCalProductSettingsStore = defineStore('calProductSettings', () => {
  const settings = ref<CalProductSettings>(defaultCalProductSettings())
  const loaded = ref(false)
  const loading = ref(false)

  function resetForTenantSwitch() {
    settings.value = defaultCalProductSettings()
    loaded.value = false
    loading.value = false
  }

  async function load() {
    loading.value = true
    try {
      settings.value = await getCalProductSettings()
      loaded.value = true
      return settings.value
    } catch (error) {
      throw toError(error, '加载日历组织设置失败')
    } finally {
      loading.value = false
    }
  }

  async function saveOrgPolicy(allowMemberCreateOwnedCalendar: boolean) {
    try {
      settings.value = await apiUpdateSettings({ allowMemberCreateOwnedCalendar })
      return settings.value
    } catch (error) {
      throw toError(error, '保存组织策略失败')
    }
  }

  async function createAllStaff(payload: {
    name: string
    color: string
    description?: string | null
    scopes: CalAllStaffScopeItem[]
  }) {
    try {
      await apiCreateAllStaff(payload)
      await load()
    } catch (error) {
      throw toError(error, '创建全员日历失败')
    }
  }

  async function updateAllStaff(payload: {
    id: string
    name: string
    color: string
    description?: string | null
    scopes: CalAllStaffScopeItem[]
  }) {
    try {
      await apiUpdateAllStaff(payload)
      await load()
    } catch (error) {
      throw toError(error, '更新全员日历失败')
    }
  }

  async function removeAllStaff(id: string) {
    try {
      await apiDeleteAllStaff(id)
      await load()
    } catch (error) {
      throw toError(error, '删除全员日历失败')
    }
  }

  function snapshot(): CalProductSettings {
    return cloneCalProductSettings(settings.value)
  }

  return {
    settings,
    loaded,
    loading,
    resetForTenantSwitch,
    load,
    saveOrgPolicy,
    createAllStaff,
    updateAllStaff,
    removeAllStaff,
    snapshot
  }
})

export type { CalAllStaffCalendar, CalAllStaffScopeItem, CalProductSettings }
