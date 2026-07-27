import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  DEFAULT_INVITE_SETTINGS,
  getInviteSettings,
  updateInviteSettings,
  type InviteSettings
} from '../api/admin/inviteSettings'
import { ApiError } from '../api/request'

function cloneSettings(source: InviteSettings): InviteSettings {
  return {
    inviteMode: source.inviteMode,
    nonAdminScope: source.nonAdminScope,
    nonAdminDeptIds: [...(source.nonAdminDeptIds ?? [])],
    nonAdminExpireDays: source.nonAdminExpireDays,
    nonAdminRequireApproval: source.nonAdminRequireApproval
  }
}

/** Tenant invite settings — admin-api is the only source of truth. */
export const useInviteSettingsStore = defineStore('inviteSettings', () => {
  const settings = ref<InviteSettings>(cloneSettings(DEFAULT_INVITE_SETTINGS))
  const loading = ref(false)
  const saving = ref(false)
  const lastError = ref<string | null>(null)
  const hydrated = ref(false)

  async function load() {
    loading.value = true
    lastError.value = null
    try {
      const data = await getInviteSettings()
      settings.value = cloneSettings(data)
      hydrated.value = true
    } catch (error) {
      lastError.value = error instanceof ApiError
        ? error.message
        : '加载邀请设置失败，请确认后端服务已启动'
      throw error
    } finally {
      loading.value = false
    }
  }

  async function save(next: InviteSettings) {
    saving.value = true
    lastError.value = null
    try {
      if (next.inviteMode === 'ALL_MEMBERS'
        && next.nonAdminScope === 'DESIGNATED_DEPTS'
        && next.nonAdminDeptIds.length === 0) {
        throw new Error('请至少指定一个可邀请部门')
      }
      const data = await updateInviteSettings(cloneSettings(next))
      settings.value = cloneSettings(data)
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
    settings.value = cloneSettings(DEFAULT_INVITE_SETTINGS)
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
