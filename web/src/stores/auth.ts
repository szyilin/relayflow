import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { login as loginApi } from '../api/admin/auth'
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

export type LoginResult =
  | { ok: true }
  | { ok: false, message: string }

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const storedTenantId = localStorage.getItem(TENANT_KEY)
const tenantId = ref<number | null>(storedTenantId ? Number(storedTenantId) : null)
  const user = ref<AuthUser | null>(readStoredUser())

  const isAuthenticated = computed(() => Boolean(token.value))

  async function login(username: string, password: string): Promise<LoginResult> {
    if (!username.trim() || !password.trim()) {
      return { ok: false, message: '请输入用户名和密码' }
    }

    try {
      const data = await loginApi({
        username: username.trim(),
        password
      })

      const authUser: AuthUser = {
        username: username.trim(),
        nickname: username.trim()
      }

      token.value = data.accessToken
      tenantId.value = data.tenantId
      user.value = authUser
      localStorage.setItem(TOKEN_KEY, data.accessToken)
      localStorage.setItem(TENANT_KEY, String(data.tenantId))
      localStorage.setItem(USER_KEY, JSON.stringify(authUser))

      return { ok: true }
    } catch (error) {
      const message = error instanceof ApiError
        ? error.message
        : '登录失败，请稍后重试'

      return { ok: false, message }
    }
  }

  function logout() {
    token.value = null
    tenantId.value = null
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(TENANT_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return {
    token,
    tenantId,
    user,
    isAuthenticated,
    login,
    logout
  }
})
