import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getPermissionInfo, login as loginApi } from '../api/admin/auth'
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

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const storedTenantId = localStorage.getItem(TENANT_KEY)
  const tenantId = ref<number | null>(storedTenantId ? Number(storedTenantId) : null)
  const user = ref<AuthUser | null>(readStoredUser())
  const permissions = ref<string[]>([])

  const isAuthenticated = computed(() => Boolean(token.value))

  async function fetchPermissionInfo() {
    const data = await getPermissionInfo()
    permissions.value = data.permissions

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

      const authUser: AuthUser = {
        username: trimmedUsername,
        nickname: trimmedUsername
      }

      token.value = data.accessToken
      tenantId.value = data.tenantId
      user.value = authUser
      persistSession(data.accessToken, data.tenantId, authUser)

      await fetchPermissionInfo()

      return { ok: true }
    } catch (error) {
      const message = error instanceof ApiError
        ? error.message
        : '登录失败，请确认后端服务已启动后重试'

      return { ok: false, message }
    }
  }

  function logout() {
    token.value = null
    tenantId.value = null
    user.value = null
    permissions.value = []
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(TENANT_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return {
    token,
    tenantId,
    user,
    permissions,
    isAuthenticated,
    login,
    logout,
    fetchPermissionInfo
  }
})
