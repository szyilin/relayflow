import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  DEFAULT_INVITE_SETTINGS,
  type InviteSettings
} from '../api/admin/inviteSettings'

function cloneSettings(source: InviteSettings): InviteSettings {
  return {
    inviteMode: source.inviteMode,
    nonAdminScope: source.nonAdminScope,
    nonAdminDeptIds: [...source.nonAdminDeptIds],
    nonAdminExpireDays: source.nonAdminExpireDays,
    nonAdminRequireApproval: source.nonAdminRequireApproval
  }
}

/**
 * Tenant invite settings. `-web` keeps session memory only;
 * `-api` / integrate will replace load/save with admin-api.
 */
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
      // Keep local defaults until API is ready
      if (!hydrated.value) {
        settings.value = cloneSettings(DEFAULT_INVITE_SETTINGS)
        hydrated.value = true
      }
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
      settings.value = cloneSettings(next)
      hydrated.value = true
    } catch (error) {
      lastError.value = error instanceof Error ? error.message : '保存失败'
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
