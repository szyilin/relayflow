import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { register as registerApi, type AuthRegisterReq } from '../api/app/auth-register'
import {
  getMyTenantList,
  isStaleSessionApiError,
  parseTenantSelectionPayload,
  switchTenant as switchTenantApi,
  TENANT_SELECTION_REQUIRED_CODE,
  type TenantSummary
} from '../api/app/tenant'
import { getPermissionInfo, login as loginApi, logout as logoutApi } from '../api/admin/auth'
import { ApiError } from '../api/request'
import { idsEqual, normalizeId } from '../utils/id'
import { isValidMobile, normalizeMobile } from '../utils/mobile'
import {
  clearSessionStorage,
  getSessionItem,
  setSessionItem
} from '../utils/session-storage'

export interface AuthUser {
  username: string
  nickname: string
  avatar?: string
}

function readStoredUser(): AuthUser | null {
  const raw = getSessionItem('user')
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as AuthUser
  } catch {
    return null
  }
}

function readStoredTenants(): TenantSummary[] {
  const raw = getSessionItem('tenants')
  if (!raw) {
    return []
  }

  try {
    const parsed = JSON.parse(raw) as TenantSummary[]
    if (!Array.isArray(parsed)) {
      return []
    }
    return parsed.map(item => ({
      ...item,
      tenantId: String(item.tenantId)
    }))
  } catch {
    return []
  }
}

function persistSession(accessToken: string, tenant: string, authUser: AuthUser) {
  setSessionItem('accessToken', accessToken)
  setSessionItem('tenantId', tenant)
  setSessionItem('user', JSON.stringify(authUser))
}

function persistTenants(items: TenantSummary[]) {
  setSessionItem('tenants', JSON.stringify(items))
}

function persistUser(authUser: AuthUser) {
  setSessionItem('user', JSON.stringify(authUser))
}

export type LoginResult =
  | { ok: true }
  | { ok: false, message: string }
  | {
    ok: false
    needTenantSelection: true
    tenants: TenantSummary[]
    credentials: { mobile: string, password: string }
  }

export type RegisterResult =
  | { ok: true, invitedTenantNames: string[] }
  | { ok: false, message: string }
export type SwitchTenantResult =
  | { ok: true }
  | { ok: false, message: string, forceLogin?: boolean }

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getSessionItem('accessToken'))
  const tenantId = ref<string | null>(normalizeId(getSessionItem('tenantId')))
  const user = ref<AuthUser | null>(readStoredUser())
  const tenants = ref<TenantSummary[]>(readStoredTenants())
  const userId = ref<string | null>(null)
  const permissions = ref<string[]>([])
  const isAdmin = ref(false)
  const permissionInfoLoaded = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value))

  const activeTenant = computed(() =>
    tenants.value.find(item => idsEqual(item.tenantId, tenantId.value)) ?? null)

  const activeTenantName = computed(() =>
    activeTenant.value?.tenantName ?? '当前企业')

  function setTenants(items: TenantSummary[]) {
    tenants.value = items
    persistTenants(items)
  }

  async function fetchMyTenants() {
    const items = await getMyTenantList()
    setTenants(items)
    return items
  }

  async function fetchPermissionInfo() {
    const data = await getPermissionInfo()
    permissions.value = data.permissions
    isAdmin.value = data.isAdmin
    userId.value = normalizeId(data.userId)
    permissionInfoLoaded.value = true

    const updatedUser: AuthUser = {
      username: data.username,
      nickname: data.nickname || data.username,
      avatar: data.avatar || undefined
    }
    user.value = updatedUser
    persistUser(updatedUser)
  }

  async function login(
    mobileInput: string,
    password: string,
    selectedTenantId?: string
  ): Promise<LoginResult> {
    const mobile = normalizeMobile(mobileInput.trim())
    const trimmedPassword = password.trim()

    if (!mobile || !trimmedPassword) {
      return { ok: false, message: '请输入手机号和密码' }
    }
    if (!isValidMobile(mobileInput)) {
      return { ok: false, message: '请输入 11 位手机号' }
    }

    try {
      const data = await loginApi({
        username: mobile,
        password: trimmedPassword,
        tenantId: selectedTenantId
      })

      await establishSession(data.accessToken, String(data.tenantId), mobile)
      await fetchMyTenants()
      await dockSyncAfterSession()

      return { ok: true }
    } catch (error) {
      if (error instanceof ApiError && error.code === TENANT_SELECTION_REQUIRED_CODE) {
        const selection = parseTenantSelectionPayload(error.data)
        if (selection.length > 0) {
          return {
            ok: false,
            needTenantSelection: true,
            tenants: selection,
            credentials: { mobile, password: trimmedPassword }
          }
        }
      }

      const message = error instanceof ApiError
        ? error.message
        : '登录失败，请确认后端服务已启动后重试'

      return { ok: false, message }
    }
  }

  async function register(payload: AuthRegisterReq): Promise<RegisterResult> {
    const mobile = normalizeMobile(payload.mobile.trim())
    const password = payload.password.trim()
    const nickname = payload.nickname.trim()
    const tenantName = payload.tenantName.trim()

    if (!mobile || !password || !nickname || !tenantName) {
      return { ok: false, message: '请完整填写注册信息' }
    }
    if (!isValidMobile(payload.mobile)) {
      return { ok: false, message: '请输入 11 位手机号' }
    }
    if (password.length < 6) {
      return { ok: false, message: '密码至少 6 位' }
    }

    try {
      const data = await registerApi({
        mobile,
        password,
        nickname,
        tenantName
      })
      if (data.tenants?.length) {
        setTenants(data.tenants.map(item => ({
          tenantId: String(item.tenantId),
          tenantName: item.tenantName,
          owner: item.owner
        })))
      }
      await establishSession(data.accessToken, String(data.tenantId), mobile, nickname)
      if (!data.tenants?.length) {
        await fetchMyTenants()
      }
      await dockSyncAfterSession()
      const invitedTenantNames = (data.tenants ?? [])
        .filter(item => !item.owner)
        .map(item => item.tenantName)
      return { ok: true, invitedTenantNames }
    } catch (error) {
      const message = error instanceof ApiError
        ? error.message
        : '注册失败，请确认后端服务已启动后重试'
      return { ok: false, message }
    }
  }

  async function switchTenant(targetTenantId: string): Promise<SwitchTenantResult> {
    if (idsEqual(targetTenantId, tenantId.value)) {
      return { ok: true }
    }

    try {
      const data = await switchTenantApi({ tenantId: targetTenantId })
      await establishSession(
        data.accessToken,
        String(data.tenantId),
        user.value?.username ?? '',
        user.value?.nickname
      )
      await fetchMyTenants()
      await dockSyncAfterSession()
      return { ok: true }
    } catch (error) {
      if (error instanceof ApiError && isStaleSessionApiError(error)) {
        return invalidateStaleAccountSession(targetTenantId, error.message)
      }
      const message = error instanceof ApiError
        ? error.message
        : '切换企业失败，请稍后重试'
      return { ok: false, message }
    }
  }

  /**
   * 清库 / 成员关系失效后：去掉该账号 Dock 残留；若无其他可用账号则清本地会话并要求重新登录。
   */
  async function invalidateStaleAccountSession(
    staleTenantId?: string,
    serverMessage?: string
  ): Promise<SwitchTenantResult> {
    const { useAccountDockStore } = await import('./accountDock')
    const dock = useAccountDockStore()
    const staleUserId = userId.value

    if (staleUserId != null && staleTenantId) {
      dock.removeEntry(`${staleUserId}:${staleTenantId}`)
    }

    try {
      await fetchPermissionInfo()
      const items = await fetchMyTenants()
      if (items.length === 0) {
        throw new Error('no tenants')
      }
      await dockSyncAfterSession()
      return {
        ok: false,
        message: serverMessage || '该企业已失效，已从列表移除'
      }
    } catch {
      if (staleUserId != null) {
        const remaining = dock.removeByUserId(staleUserId)
        if (remaining.length > 0) {
          try {
            await restoreDockEntry(remaining[0])
            return {
              ok: false,
              message: '该账号登录已失效，已切换到其他账号'
            }
          } catch {
            // fall through to full clear
          }
        }
      }

      dock.clearAll()
      clearLocalSession()
      return {
        ok: false,
        message: '登录已失效，请重新登录',
        forceLogin: true
      }
    }
  }

  async function establishSession(
    accessToken: string,
    tenant: string,
    username: string,
    nickname?: string
  ) {
    const authUser: AuthUser = {
      username,
      nickname: nickname?.trim() || username
    }

    token.value = accessToken
    tenantId.value = tenant
    user.value = authUser
    permissionInfoLoaded.value = false
    persistSession(accessToken, tenant, authUser)

    await fetchPermissionInfo()
    await dockSyncAfterSession()
  }

  async function logout(options?: { preserveOtherAccounts?: boolean }) {
    const { useAccountDockStore } = await import('./accountDock')
    const dock = useAccountDockStore()
    const currentUserId = userId.value

    try {
      await logoutApi()
    } catch {
      // 登出以清本地会话为准；服务端吊销失败不阻塞退出
    }

    if (options?.preserveOtherAccounts && currentUserId != null) {
      const remaining = dock.removeByUserId(currentUserId)
      if (remaining.length > 0) {
        const next = remaining[0]
        await restoreDockEntry(next)
        return
      }
    }

    dock.clearAll()
    clearLocalSession()
  }

  async function restoreDockEntry(entry: import('./accountDock').AccountDockEntry) {
    token.value = entry.token
    tenantId.value = entry.tenantId
    user.value = {
      username: entry.username,
      nickname: entry.nickname,
      avatar: entry.avatar
    }
    permissionInfoLoaded.value = false
    persistSession(entry.token, entry.tenantId, user.value!)
    await fetchPermissionInfo()
    await fetchMyTenants()
    await dockSyncAfterSession()
  }

  function clearLocalSession() {
    token.value = null
    tenantId.value = null
    user.value = null
    tenants.value = []
    userId.value = null
    permissions.value = []
    isAdmin.value = false
    permissionInfoLoaded.value = false
    clearSessionStorage()
  }

  async function dockSyncAfterSession() {
    const { useAccountDockStore } = await import('./accountDock')
    const dock = useAccountDockStore()
    dock.syncCurrentSession({ isAdmin: isAdmin.value })
    await dock.syncAllTenantsForCurrentAccount()
  }

  async function switchToDockEntry(entry: import('./accountDock').AccountDockEntry) {
    if (entry.userId === userId.value && idsEqual(entry.tenantId, tenantId.value)) {
      return { ok: true as const }
    }

    if (entry.userId === userId.value) {
      return switchTenant(entry.tenantId)
    }

    try {
      await restoreDockEntry(entry)
      return { ok: true as const }
    } catch (error) {
      const { useAccountDockStore } = await import('./accountDock')
      const dock = useAccountDockStore()
      dock.removeEntry(entry.key)

      if (error instanceof ApiError && isStaleSessionApiError(error)) {
        const remaining = dock.removeByUserId(entry.userId)
        if (remaining.length === 0 && !token.value) {
          clearLocalSession()
          return {
            ok: false as const,
            message: '登录已失效，请重新登录',
            forceLogin: true
          }
        }
        if (remaining.length === 0 && token.value) {
          // 切走的账号已废，当前会话仍在
          return {
            ok: false as const,
            message: '该账号登录已失效，已从列表移除'
          }
        }
      }

      if (!token.value) {
        clearLocalSession()
        return {
          ok: false as const,
          message: '登录已失效，请重新登录',
          forceLogin: true
        }
      }

      return { ok: false as const, message: '登录已过期，请重新登录' }
    }
  }

  return {
    token,
    tenantId,
    user,
    tenants,
    activeTenant,
    activeTenantName,
    userId,
    permissions,
    isAdmin,
    permissionInfoLoaded,
    isAuthenticated,
    login,
    register,
    establishSession,
    fetchMyTenants,
    switchTenant,
    logout,
    switchToDockEntry,
    dockSyncAfterSession,
    fetchPermissionInfo
  }
})
