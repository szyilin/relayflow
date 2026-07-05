import axios, { type AxiosRequestConfig } from 'axios'

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

/** 后端未启动、Nginx 无代理、网络异常等 —— 可回退 Mock，与业务错误（密码错等）区分 */
export function isApiUnavailable(error: unknown): boolean {
  if (error instanceof TypeError) {
    return true
  }

  if (axios.isAxiosError(error) && !error.response) {
    return true
  }

  if (error instanceof ApiError && error.message === '服务响应格式错误') {
    return true
  }

  return false
}

const TOKEN_KEY = 'relayflow:admin:access-token'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') ?? '',
  headers: {
    'Content-Type': 'application/json'
  }
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
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
      throw new ApiError(payload.code, payload.msg || '请求失败')
    }

    return payload.data
  } catch (error) {
    if (error instanceof ApiError) {
      throw error
    }

    if (axios.isAxiosError(error) && error.response?.data) {
      try {
        const payload = parseApiResult<T>(error.response.data)
        throw new ApiError(payload.code, payload.msg || '请求失败')
      } catch (inner) {
        if (inner instanceof ApiError) {
          throw inner
        }
      }
    }

    if (axios.isAxiosError(error)) {
      throw new ApiError(error.response?.status ?? 0, '服务响应格式错误')
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
