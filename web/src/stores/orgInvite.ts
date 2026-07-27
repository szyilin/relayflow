import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  consumeOrgInvite,
  createMobileOrgInvite,
  getOrgInviteCapability,
  getSharedOrgInvite,
  resetSharedOrgInvite,
  type InviteMode,
  type OrgInviteConsumeResult,
  type OrgInviteMobile
} from '../api/app/orgInvite'
import { ApiError } from '../api/request'

function formatExpiresLabel(iso: string | null): string {
  if (!iso) {
    return '—'
  }
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) {
    return iso
  }
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

function errorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError || error instanceof Error) {
    return error.message
  }
  return fallback
}

/** Tenant org invite channels — app-api is the only source of truth. */
export const useOrgInviteStore = defineStore('orgInvite', () => {
  const canInvite = ref(false)
  const inviteMode = ref<InviteMode | null>(null)
  const sharedToken = ref('')
  const inviteCode = ref('')
  const expiresAt = ref<string | null>(null)
  const mobileInvites = ref<OrgInviteMobile[]>([])
  const loadingCapability = ref(false)
  const loadingShared = ref(false)
  const saving = ref(false)
  const lastError = ref<string | null>(null)
  const capabilityLoaded = ref(false)

  const expiresLabel = computed(() => formatExpiresLabel(expiresAt.value))

  function applyShared(data: { token: string, inviteCode: string, expiresAt: string }) {
    sharedToken.value = data.token
    inviteCode.value = data.inviteCode
    expiresAt.value = data.expiresAt
  }

  function inviteLink(origin?: string): string {
    const base = origin
      ?? (typeof window !== 'undefined' ? window.location.origin : '')
    if (!sharedToken.value) {
      return `${base}/app/invite/join`
    }
    return `${base}/app/invite/join?t=${encodeURIComponent(sharedToken.value)}`
  }

  async function loadCapability() {
    loadingCapability.value = true
    lastError.value = null
    try {
      const data = await getOrgInviteCapability()
      canInvite.value = Boolean(data.canInvite)
      inviteMode.value = data.inviteMode
      capabilityLoaded.value = true
    } catch (error) {
      lastError.value = errorMessage(error, '加载邀请权限失败')
      throw error
    } finally {
      loadingCapability.value = false
    }
  }

  async function ensureShared() {
    loadingShared.value = true
    lastError.value = null
    try {
      const data = await getSharedOrgInvite()
      applyShared(data)
    } catch (error) {
      lastError.value = errorMessage(error, '加载邀请凭证失败')
      throw error
    } finally {
      loadingShared.value = false
    }
  }

  async function resetShared() {
    saving.value = true
    lastError.value = null
    try {
      const data = await resetSharedOrgInvite()
      applyShared(data)
    } catch (error) {
      lastError.value = errorMessage(error, '重置邀请失败')
      throw error
    } finally {
      saving.value = false
    }
  }

  async function inviteMobile(mobile: string) {
    saving.value = true
    lastError.value = null
    try {
      const trimmed = mobile.trim()
      if (!/^\d{11}$/.test(trimmed)) {
        throw new Error('请输入 11 位手机号')
      }
      const data = await createMobileOrgInvite(trimmed)
      mobileInvites.value = [data, ...mobileInvites.value]
      return data
    } catch (error) {
      lastError.value = errorMessage(error, '发送手机号邀请失败')
      throw error
    } finally {
      saving.value = false
    }
  }

  async function consume(payload: {
    token?: string
    code?: string
  }): Promise<OrgInviteConsumeResult> {
    saving.value = true
    lastError.value = null
    try {
      const token = payload.token?.trim()
      const code = payload.code?.trim()
      if (!token && !code) {
        throw new Error('请提供邀请链接或邀请码')
      }
      return await consumeOrgInvite(token ? { token } : { code: code!.toUpperCase() })
    } catch (error) {
      lastError.value = errorMessage(error, '加入企业失败')
      throw error
    } finally {
      saving.value = false
    }
  }

  function resetForTenantSwitch() {
    canInvite.value = false
    inviteMode.value = null
    sharedToken.value = ''
    inviteCode.value = ''
    expiresAt.value = null
    mobileInvites.value = []
    lastError.value = null
    capabilityLoaded.value = false
  }

  return {
    canInvite,
    inviteMode,
    sharedToken,
    inviteCode,
    expiresAt,
    expiresLabel,
    mobileInvites,
    loadingCapability,
    loadingShared,
    saving,
    lastError,
    capabilityLoaded,
    inviteLink,
    loadCapability,
    ensureShared,
    resetShared,
    inviteMobile,
    consume,
    resetForTenantSwitch
  }
})
