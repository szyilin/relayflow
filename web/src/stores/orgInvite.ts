import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { useInviteSettingsStore } from './inviteSettings'

function randomToken() {
  return `t_${Math.random().toString(36).slice(2, 10)}${Date.now().toString(36)}`
}

function randomCode() {
  return Math.random().toString(36).slice(2, 8).toUpperCase()
}

export interface MobileInviteDraft {
  id: string
  mobile: string
  createdAt: string
  status: 'pending' | 'consumed'
}

/**
 * Client-side org invite drafts for `-web`.
 * Replaced by /app-api/system/org-invite/* on integrate.
 */
export const useOrgInviteStore = defineStore('orgInvite', () => {
  const sharedToken = ref(randomToken())
  const inviteCode = ref(randomCode())
  const revokedTokens = ref<string[]>([])
  const revokedCodes = ref<string[]>([])
  const mobileDrafts = ref<MobileInviteDraft[]>([])
  const lastError = ref<string | null>(null)

  const expireDays = computed(() => {
    const settings = useInviteSettingsStore().settings
    return settings.nonAdminExpireDays || 3
  })

  const expiresAt = computed(() => {
    const d = new Date()
    d.setDate(d.getDate() + expireDays.value)
    return d
  })

  const expiresLabel = computed(() => {
    const d = expiresAt.value
    return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
  })

  function inviteLink(origin?: string): string {
    const base = origin
      ?? (typeof window !== 'undefined' ? window.location.origin : '')
    return `${base}/app/invite/join?t=${encodeURIComponent(sharedToken.value)}`
  }

  function resetSharedChannel() {
    revokedTokens.value = [...revokedTokens.value, sharedToken.value]
    revokedCodes.value = [...revokedCodes.value, inviteCode.value]
    sharedToken.value = randomToken()
    inviteCode.value = randomCode()
  }

  function addMobileInvite(mobile: string) {
    lastError.value = null
    const trimmed = mobile.trim()
    if (!/^\d{11}$/.test(trimmed)) {
      lastError.value = '请输入 11 位手机号'
      throw new Error(lastError.value)
    }
    mobileDrafts.value = [
      {
        id: `m_${Date.now()}`,
        mobile: trimmed,
        createdAt: new Date().toISOString(),
        status: 'pending'
      },
      ...mobileDrafts.value
    ]
  }

  function validateToken(token: string | null | undefined): { ok: boolean, message: string } {
    if (!token) {
      return { ok: false, message: '缺少邀请凭证' }
    }
    if (revokedTokens.value.includes(token)) {
      return { ok: false, message: '邀请已重置或失效' }
    }
    if (token !== sharedToken.value) {
      return { ok: false, message: '邀请无效或不存在' }
    }
    if (useInviteSettingsStore().settings.inviteMode === 'OFF') {
      return { ok: false, message: '当前企业已关闭邀请' }
    }
    return { ok: true, message: '邀请有效（草稿）' }
  }

  function validateCode(code: string | null | undefined): { ok: boolean, message: string } {
    const normalized = (code ?? '').trim().toUpperCase()
    if (!normalized) {
      return { ok: false, message: '请输入邀请码' }
    }
    if (revokedCodes.value.includes(normalized)) {
      return { ok: false, message: '邀请码已重置或失效' }
    }
    if (normalized !== inviteCode.value) {
      return { ok: false, message: '邀请码无效' }
    }
    if (useInviteSettingsStore().settings.inviteMode === 'OFF') {
      return { ok: false, message: '当前企业已关闭邀请' }
    }
    return { ok: true, message: '邀请码有效（草稿）' }
  }

  function consumeDraft(kind: 'token' | 'code', value: string): { ok: boolean, message: string } {
    const result = kind === 'token' ? validateToken(value) : validateCode(value)
    if (!result.ok) {
      return result
    }
    return {
      ok: true,
      message: '已接受邀请（前端草稿，未写入成员关系；对接 API 后生效）'
    }
  }

  function resetForTenantSwitch() {
    sharedToken.value = randomToken()
    inviteCode.value = randomCode()
    revokedTokens.value = []
    revokedCodes.value = []
    mobileDrafts.value = []
    lastError.value = null
  }

  return {
    sharedToken,
    inviteCode,
    mobileDrafts,
    lastError,
    expireDays,
    expiresAt,
    expiresLabel,
    inviteLink,
    resetSharedChannel,
    addMobileInvite,
    validateToken,
    validateCode,
    consumeDraft,
    resetForTenantSwitch
  }
})
