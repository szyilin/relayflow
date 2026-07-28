import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  DEFAULT_IM_PRODUCT_SETTINGS,
  cloneImProductSettings,
  getImProductSettings,
  updateImProductSettings,
  type ImProductSettings
} from '../api/admin/imProductSettings'
import { ApiError } from '../api/request'

/** Tenant IM product settings — admin-api is the source of truth. */
export const useImProductSettingsStore = defineStore('imProductSettings', () => {
  const settings = ref<ImProductSettings>(cloneImProductSettings(DEFAULT_IM_PRODUCT_SETTINGS))
  const loading = ref(false)
  const saving = ref(false)
  const lastError = ref<string | null>(null)
  const hydrated = ref(false)
  /** Kept for UI badge during transition; false once API responds. */
  const usingLocalDraft = ref(false)

  async function load() {
    loading.value = true
    lastError.value = null
    try {
      const data = await getImProductSettings()
      settings.value = cloneImProductSettings(data)
      usingLocalDraft.value = false
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

  async function save(next: ImProductSettings) {
    saving.value = true
    lastError.value = null
    try {
      if (next.groupMemberLimitDefault < 2) {
        throw new Error('群成员人数上限至少为 2')
      }
      for (const rule of next.userRules) {
        if (!rule.subjectId || rule.memberLimit < 2) {
          throw new Error('用户规则集不完整：请选择主体并设置有效上限')
        }
      }
      for (const rule of next.groupRules) {
        if (!rule.conversationId || rule.memberLimit < 2) {
          throw new Error('群组规则集不完整：请选择群并设置有效上限')
        }
      }
      if (next.memberExportPolicy === 'ALLOW_DESIGNATED'
        && next.memberExportDesignatedUserIds.length === 0) {
        throw new Error('请至少指定一名可导出成员')
      }

      const data = await updateImProductSettings(cloneImProductSettings(next))
      settings.value = cloneImProductSettings(data)
      usingLocalDraft.value = false
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
    settings.value = cloneImProductSettings(DEFAULT_IM_PRODUCT_SETTINGS)
    hydrated.value = false
    usingLocalDraft.value = false
    lastError.value = null
  }

  return {
    settings,
    loading,
    saving,
    lastError,
    hydrated,
    usingLocalDraft,
    load,
    save,
    resetForTenantSwitch
  }
})
