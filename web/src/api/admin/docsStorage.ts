import { get, put } from '../request'

export interface DocsStorageUsageItem {
  userId: string
  displayName: string
  usedBytes: number
  /** null = unlimited */
  quotaBytes: number | null
  /** null when unlimited */
  usageRatio: number | null
}

export interface DocsStorageUsagePageQuery {
  pageNo?: number
  pageSize?: number
  keyword?: string
}

export interface DocsStorageUsagePageResult {
  list: DocsStorageUsageItem[]
  total: number
  pageNo: number
  pageSize: number
}

export interface DocsStorageQuotaUserRule {
  userId: string
  displayName?: string
  /** null = unlimited for this user */
  quotaBytes: number | null
}

export interface DocsStorageQuota {
  /** null = unlimited default */
  defaultQuotaBytes: number | null
  userRules: DocsStorageQuotaUserRule[]
}

export function getDocsStorageUsage(params: DocsStorageUsagePageQuery): Promise<DocsStorageUsagePageResult> {
  const cleaned: Record<string, string | number> = {}
  if (params.pageNo != null) {
    cleaned.pageNo = params.pageNo
  }
  if (params.pageSize != null) {
    cleaned.pageSize = params.pageSize
  }
  if (params.keyword?.trim()) {
    cleaned.keyword = params.keyword.trim()
  }
  return get<DocsStorageUsagePageResult>('/admin-api/docs/storage-usage', { params: cleaned })
}

export function getDocsStorageQuota(): Promise<DocsStorageQuota> {
  return get<DocsStorageQuota>('/admin-api/docs/storage-quota')
}

export function updateDocsStorageQuota(payload: DocsStorageQuota): Promise<DocsStorageQuota> {
  return put<DocsStorageQuota>('/admin-api/docs/storage-quota', payload)
}
