import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { register as registerApi, type AuthRegisterReq } from '../api/app/auth-register'
import {
  getMyTenantList,
  parseTenantSelectionPayload,
  switchTenant as switchTenantApi,
  TENANT_SELECTION_REQUIRED_CODE,
  type TenantSummary
} from '../api/app/tenant'
import { getPermissionInfo, login as loginApi, logout as logoutApi } from '../api/admin/auth'
import { ApiError } from '../api/request'

export interface AuthUser {
  username: string
  nickname: string
}

const TOKEN_KEY = 'relayflow:admin:access-token'
const TENANT_KEY = 'relayflow:admin:tenant-id'
const USER_KEY = 'relayflow:admin:user'
const TENANTS_KEY = 'relayflow:admin:tenants'

function readStoredUser(): AuthUser | null {
  const raw = localStorage.getItem(USER_KEY)
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
  const raw = localStorage.getItem(TENANTS_KEY)
  if (!raw) {
    return []
  }

  try {
    const parsed = JSON.parse(raw) as TenantSummary[]
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function persistSession(accessToken: string, tenant: number, authUser: AuthUser) {
  localStorage.setItem(TOKEN_KEY, accessToken)
  localStorage.setItem(TENANT_KEY, String(tenant))
  localStorage.setItem(USER_KEY, JSON.stringify(authUser))
}

function persistTenants(items: TenantSummary[]) {
  localStorage.setItem(TENANTS_KEY, JSON.stringify(items))
}

function persistUser(authUser: AuthUser) {
  localStorage.setItem(USER_KEY, JSON.stringify(authUser))
}

export type LoginResult =
  | { ok: true }
  | { ok: false, message: string }
  | {
    ok: false
    needTenantSelection: true
    tenants: TenantSummary[]
    credentials: { username: string, password: string }
  }

export type RegisterResult = Exclude<LoginResult, { needTenantSelection: true }>
export type SwitchTenantResult = { ok: true } | { ok: false, message: string }

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const storedTenantId = localStorage.getItem(TENANT_KEY)
  const tenantId = ref<number | null>(storedTenantId ? Number(storedTenantId) : null)
  const user = ref<AuthUser | null>(readStoredUser())
  const tenants = ref<TenantSummary[]>(readStoredTenants())
  const userId = ref<number | null>(null)
  const permissions = ref<string[]>([])
  const isAdmin = ref(false)
  const permissionInfoLoaded = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value))

  const activeTenant = computed(() =>
    tenants.value.find(item => item.tenantId === tenantId.value) ?? null)

  const activeTenantName = computed(() =>
    activeTenant.value?.tenantName ?? (tenantId.value != null ? `企业 #${tenantId.value}` : '当前企业'))

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
    userId.value = data.userId
    permissionInfoLoaded.value = true

    if (user.value) {
      const updatedUser: AuthUser = {
        username: data.username,
        nickname: data.nickname || data.username
      }
      user.value = updatedUser
      persistUser(updatedUser)
    }
  }

  async function login(
    username: string,
    password: string,
    selectedTenantId?: number
  ): Promise<LoginResult> {
    if (!username.trim() || !password.trim()) {
      return { ok: false, message: '请输入用户名和密码' }
    }

    const trimmedUsername = username.trim()

    try {
      const data = await loginApi({
        username: trimmedUsername,
        password,
        tenantId: selectedTenantId
      })

      await establishSession(data.accessToken, data.tenantId, trimmedUsername)
      await fetchMyTenants()

      return { ok: true }
    } catch (error) {
      if (error instanceof ApiError && error.code === TENANT_SELECTION_REQUIRED_CODE) {
        const selection = parseTenantSelectionPayload(error.data)
        if (selection.length > 0) {
          return {
            ok: false,
            needTenantSelection: true,
            tenants: selection,
            credentials: { username: trimmedUsername, password }
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
    const mobile = payload.mobile.trim()
    const password = payload.password.trim()
    const nickname = payload.nickname.trim()
    const tenantName = payload.tenantName.trim()

    if (!mobile || !password || !nickname || !tenantName) {
      return { ok: false, message: '请完整填写注册信息' }
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
        setTenants(data.tenants)
      }
      await establishSession(data.accessToken, data.tenantId, mobile, nickname)
      if (!data.tenants?.length) {
        await fetchMyTenants()
      }
      return { ok: true }
    } catch (error) {
      const message = error instanceof ApiError
        ? error.message
        : '注册失败，请确认后端服务已启动后重试'
      return { ok: false, message }
    }
  }

  async function switchTenant(targetTenantId: number): Promise<SwitchTenantResult> {
    if (targetTenantId === tenantId.value) {
      return { ok: true }
    }

    try {
      const data = await switchTenantApi({ tenantId: targetTenantId })
      await establishSession(
        data.accessToken,
        data.tenantId,
        user.value?.username ?? '',
        user.value?.nickname
      )
      await fetchMyTenants()
      return { ok: true }
    } catch (error) {
      const message = error instanceof ApiError
        ? error.message
        : '切换企业失败，请稍后重试'
      return { ok: false, message }
    }
  }

  async function establishSession(
    accessToken: string,
    tenant: number,
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
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 登出以清本地会话为准；服务端吊销失败不阻塞退出
    }
    token.value = null
    tenantId.value = null
    user.value = null
    tenants.value = []
    userId.value = null
    permissions.value = []
    isAdmin.value = false
    permissionInfoLoaded.value = false
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(TENANT_KEY)
    localStorage.removeItem(USER_KEY)
    localStorage.removeItem(TENANTS_KEY)
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
    fetchPermissionInfo
  }
})
