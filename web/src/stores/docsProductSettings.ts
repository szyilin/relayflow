import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  DEFAULT_DOCS_PRODUCT_SETTINGS,
  cloneDocsProductSettings,
  getDocsProductSettings,
  updateDocsProductSettings,
  type DocsProductSettings
} from '../api/admin/docsProductSettings'
import { ApiError } from '../api/request'

/** Tenant docs product settings — admin-api is the source of truth. */
export const useDocsProductSettingsStore = defineStore('docsProductSettings', () => {
  const settings = ref<DocsProductSettings>(cloneDocsProductSettings(DEFAULT_DOCS_PRODUCT_SETTINGS))
  const loading = ref(false)
  const saving = ref(false)
  const lastError = ref<string | null>(null)
  const hydrated = ref(false)

  async function load() {
    loading.value = true
    lastError.value = null
    try {
      const data = await getDocsProductSettings()
      settings.value = cloneDocsProductSettings(data)
      hydrated.value = true
    } catch (error) {
      lastError.value = error instanceof ApiError
        ? error.message
        : '加载产品设置失败，请确认后端服务已启动'
      throw error
    } finally {
      loading.value = false
    }
  }

  async function save(next: DocsProductSettings) {
    saving.value = true
    lastError.value = null
    try {
      const data = await updateDocsProductSettings(cloneDocsProductSettings(next))
      settings.value = cloneDocsProductSettings(data)
      hydrated.value = true
    } catch (error) {
      lastError.value = error instanceof ApiError || error instanceof Error
        ? error.message
        : '保存失败'
      throw error
    } finally {
      saving.value = false
    }
  }

  function resetForTenantSwitch() {
    settings.value = cloneDocsProductSettings(DEFAULT_DOCS_PRODUCT_SETTINGS)
    hydrated.value = false
    lastError.value = null
  }

  return {
    settings,
    loading,
    saving,
    lastError,
    hydrated,
    load,
    save,
    resetForTenantSwitch
  }
})
