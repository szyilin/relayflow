import axios, { type AxiosRequestConfig } from 'axios'
import { clearSessionStorage, getAccessToken } from '../utils/session-storage'

export interface ApiResult<T> {
  code: number
  msg: string
  data: T
}

export class ApiError extends Error {
  readonly code: number
  readonly data?: unknown

  constructor(code: number, message: string, data?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.data = data
  }
}

const LOGIN_PATH = '/app/login'
const FORBIDDEN_PATH = '/app/forbidden'

const AUTH_PATH_MARKERS = [
  '/auth/login',
  '/auth/register',
  '/auth/logout'
]

let handlingUnauthorized = false
let handlingForbidden = false

function requestUrl(config?: AxiosRequestConfig): string {
  return `${config?.baseURL ?? ''}${config?.url ?? ''}`
}

function isAuthEndpoint(config?: AxiosRequestConfig): boolean {
  const url = requestUrl(config)
  return AUTH_PATH_MARKERS.some(marker => url.includes(marker))
}

function currentPath(): string {
  return window.location.pathname
}

function redirectToLogin(): void {
  if (handlingUnauthorized) {
    return
  }
  if (currentPath() === LOGIN_PATH || currentPath() === '/app/register') {
    return
  }
  handlingUnauthorized = true
  clearSessionStorage()
  const redirect = encodeURIComponent(`${window.location.pathname}${window.location.search}`)
  window.location.assign(`${LOGIN_PATH}?redirect=${redirect}`)
}

function redirectToForbidden(): void {
  if (handlingForbidden) {
    return
  }
  if (currentPath() === FORBIDDEN_PATH || currentPath() === LOGIN_PATH) {
    return
  }
  handlingForbidden = true
  window.location.assign(FORBIDDEN_PATH)
}

function handleAuthFailure(error: ApiError, config?: AxiosRequestConfig): void {
  if (isAuthEndpoint(config)) {
    return
  }
  if (error.code === 401) {
    redirectToLogin()
    return
  }
  if (error.code === 403) {
    redirectToForbidden()
  }
}

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') ?? '',
  headers: {
    'Content-Type': 'application/json'
  }
})

client.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token && config.headers && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function parseApiResult<T>(payload: unknown): ApiResult<T> {
  if (!payload || typeof payload !== 'object' || !('code' in payload)) {
    throw new ApiError(0, '服务响应格式错误')
  }

  return payload as ApiResult<T>
}

export async function request<T>(path: string, config: AxiosRequestConfig = {}): Promise<T> {
  try {
    const response = await client.request<ApiResult<T>>({
      url: path,
      ...config
    })

    const payload = parseApiResult<T>(response.data)
    if (payload.code !== 0) {
      const error = new ApiError(payload.code, payload.msg || '请求失败', payload.data)
      handleAuthFailure(error, { ...config, url: path })
      throw error
    }

    return payload.data
  } catch (error) {
    if (error instanceof ApiError) {
      throw error
    }

    if (axios.isAxiosError(error) && error.response?.data) {
      try {
        const payload = parseApiResult<T>(error.response.data)
        const apiError = new ApiError(payload.code, payload.msg || '请求失败', payload.data)
        handleAuthFailure(apiError, error.config)
        throw apiError
      } catch (inner) {
        if (inner instanceof ApiError) {
          throw inner
        }
      }
    }

    if (axios.isAxiosError(error)) {
      const status = error.response?.status ?? 0
      const apiError = new ApiError(status, status === 401
        ? '登录已失效，请重新登录'
        : status === 403
          ? '没有权限执行此操作'
          : '服务响应格式错误')
      handleAuthFailure(apiError, error.config)
      throw apiError
    }

    throw error
  }
}

export async function get<T>(path: string, config: Omit<AxiosRequestConfig, 'method' | 'url'> = {}): Promise<T> {
  return request<T>(path, { ...config, method: 'GET' })
}

export async function post<T>(path: string, data?: unknown, config: Omit<AxiosRequestConfig, 'method' | 'url' | 'data'> = {}): Promise<T> {
  return request<T>(path, { ...config, method: 'POST', data })
}

export async function put<T>(path: string, data?: unknown, config: Omit<AxiosRequestConfig, 'method' | 'url' | 'data'> = {}): Promise<T> {
  return request<T>(path, { ...config, method: 'PUT', data })
}

export async function del<T>(path: string, config: Omit<AxiosRequestConfig, 'method' | 'url'> = {}): Promise<T> {
  return request<T>(path, { ...config, method: 'DELETE' })
}
