import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { register as registerApi, type AuthRegisterReq } from '../api/app/auth-register'
import { getPermissionInfo, login as loginApi, logout as logoutApi } from '../api/admin/auth'
import { ApiError } from '../api/request'

export interface AuthUser {
  username: string
  nickname: string
}

const TOKEN_KEY = 'relayflow:admin:access-token'
const TENANT_KEY = 'relayflow:admin:tenant-id'
const USER_KEY = 'relayflow:admin:user'

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

function persistSession(accessToken: string, tenant: number, authUser: AuthUser) {
  localStorage.setItem(TOKEN_KEY, accessToken)
  localStorage.setItem(TENANT_KEY, String(tenant))
  localStorage.setItem(USER_KEY, JSON.stringify(authUser))
}

function persistUser(authUser: AuthUser) {
  localStorage.setItem(USER_KEY, JSON.stringify(authUser))
}

export type LoginResult =
  | { ok: true }
  | { ok: false, message: string }

export type RegisterResult = LoginResult

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const storedTenantId = localStorage.getItem(TENANT_KEY)
  const tenantId = ref<number | null>(storedTenantId ? Number(storedTenantId) : null)
  const user = ref<AuthUser | null>(readStoredUser())
  const userId = ref<number | null>(null)
  const permissions = ref<string[]>([])
  const isAdmin = ref(false)
  const permissionInfoLoaded = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value))

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

  async function login(username: string, password: string): Promise<LoginResult> {
    if (!username.trim() || !password.trim()) {
      return { ok: false, message: '请输入用户名和密码' }
    }

    const trimmedUsername = username.trim()

    try {
      const data = await loginApi({
        username: trimmedUsername,
        password
      })

      await establishSession(data.accessToken, data.tenantId, trimmedUsername)

      return { ok: true }
    } catch (error) {
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
      await establishSession(data.accessToken, data.tenantId, mobile, nickname)
      return { ok: true }
    } catch (error) {
      const message = error instanceof ApiError
        ? error.message
        : '注册失败，请确认后端服务已启动后重试'
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
    userId.value = null
    permissions.value = []
    isAdmin.value = false
    permissionInfoLoaded.value = false
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(TENANT_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return {
    token,
    tenantId,
    user,
    userId,
    permissions,
    isAdmin,
    permissionInfoLoaded,
    isAuthenticated,
    login,
    register,
    establishSession,
    logout,
    fetchPermissionInfo
  }
})
