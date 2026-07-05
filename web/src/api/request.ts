export interface ApiResult<T> {
  code: number
  msg: string
  data: T
}

export class ApiError extends Error {
  readonly code: number

  constructor(code: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

const TOKEN_KEY = 'relayflow:admin:access-token'

function resolveUrl(path: string): string {
  const base = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') ?? ''
  return `${base}${path}`
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (!headers.has('Content-Type') && init.body) {
    headers.set('Content-Type', 'application/json')
  }

  const token = localStorage.getItem(TOKEN_KEY)
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(resolveUrl(path), {
    ...init,
    headers
  })

  let payload: ApiResult<T>
  try {
    payload = await response.json() as ApiResult<T>
  } catch {
    throw new ApiError(response.status, '服务响应格式错误')
  }

  if (payload.code !== 0) {
    throw new ApiError(payload.code, payload.msg || '请求失败')
  }

  return payload.data
}
