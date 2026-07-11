import { del, get, post, put } from '../request'

export interface StorageProviderItem {
  provider: string
  status?: string
  isDefault?: boolean
  endpoint?: string
  bucket?: string
  accessKey?: string
  useSsl?: boolean
  pathPrefix?: string
  secretKeyConfigured?: boolean
}

export interface StorageConfigResponse {
  providers: StorageProviderItem[]
}

export interface StorageProviderSavePayload {
  provider: string
  endpoint: string
  bucket: string
  accessKey: string
  secretKey?: string
  useSsl?: boolean
  pathPrefix?: string
  isDefault?: boolean
}

export interface StorageTestConnectionPayload {
  provider: string
  endpoint?: string
  bucket?: string
  accessKey?: string
  secretKey?: string
  useSsl?: boolean
  pathPrefix?: string
}

export function getStorageConfig(): Promise<StorageConfigResponse> {
  return get<StorageConfigResponse>('/admin-api/infra/storage/config')
}

export function saveStorageConfig(data: StorageProviderSavePayload): Promise<boolean> {
  return put<boolean>('/admin-api/infra/storage/config', data)
}

export function deleteStorageConfig(provider: string): Promise<boolean> {
  return del<boolean>('/admin-api/infra/storage/config', {
    params: { provider }
  })
}

export function testStorageConnection(data: StorageTestConnectionPayload): Promise<boolean> {
  return post<boolean>('/admin-api/infra/storage/test-connection', data)
}
