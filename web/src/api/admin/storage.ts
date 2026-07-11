import { del, get, post, put } from '../request'

export type StorageEffectiveSource = 'bootstrap' | 'tenant'

export interface StorageBootstrapSummary {
  available: boolean
  provider?: string
  credentialsConfigured?: boolean
}

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
  effectiveSource: StorageEffectiveSource
  bootstrap: StorageBootstrapSummary
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
}

export interface StorageEffectiveSourcePayload {
  source: StorageEffectiveSource
}

export interface StorageTestConnectionPayload {
  source?: StorageEffectiveSource
  provider?: string
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

export function setStorageEffectiveSource(data: StorageEffectiveSourcePayload): Promise<boolean> {
  return put<boolean>('/admin-api/infra/storage/effective-source', data)
}

export function deleteStorageConfig(provider: string): Promise<boolean> {
  return del<boolean>('/admin-api/infra/storage/config', {
    params: { provider }
  })
}

export function testStorageConnection(data: StorageTestConnectionPayload): Promise<boolean> {
  return post<boolean>('/admin-api/infra/storage/test-connection', data)
}
