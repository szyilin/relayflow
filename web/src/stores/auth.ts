import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { login as loginApi } from '../api/admin/auth'
import { ApiError, isApiUnavailable } from '../api/request'
import { mockLogin } from '../mocks/auth'

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

      return { ok: true }
    } catch (error) {
      if (isApiUnavailable(error)) {
        const result = mockLogin(trimmedUsername, password)
        if (!result.ok) {
          return result
        }

        const authUser: AuthUser = {
          username: result.user.username,
          nickname: result.user.nickname
        }

        token.value = result.token
        tenantId.value = 1
        user.value = authUser
        persistSession(result.token, 1, authUser)

        return { ok: true }
      }

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
