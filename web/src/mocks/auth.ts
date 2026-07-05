export interface MockUser {
  id: string
  username: string
  nickname: string
}

export interface MockLoginResult {
  ok: true
  token: string
  user: MockUser
}

export interface MockLoginError {
  ok: false
  message: string
}

export type MockLoginResponse = MockLoginResult | MockLoginError

export function mockLogin(username: string, password: string): MockLoginResponse {
  if (!username.trim() || !password.trim()) {
    return { ok: false, message: '请输入用户名和密码' }
  }

  const normalized = username.trim()
  const user: MockUser = {
    id: '1',
    username: normalized,
    nickname: normalized === 'admin' ? '管理员' : normalized
  }

  return {
    ok: true,
    token: `mock-token-${Date.now()}`,
    user
  }
}

export function mockLogout(): void {
  // no-op; store clears storage
}
