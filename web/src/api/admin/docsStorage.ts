import { get, put } from '../request'

export interface DocsStorageUsageItem {
  userId: string
  displayName: string
  deptName?: string | null
  /** infra file id or URL hint for WorkspaceOwnerChip */
  avatarUrl?: string | null
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

export interface DocsStorageQuotaDeptRule {
  deptId: string
  deptName?: string
  /** null = unlimited for members with this primary dept */
  quotaBytes: number | null
}

export interface DocsStorageQuota {
  /** null = unlimited default */
  defaultQuotaBytes: number | null
  userRules: DocsStorageQuotaUserRule[]
  deptRules: DocsStorageQuotaDeptRule[]
}

/** Max member whitelist rules (aligned with Feishu-style UX). */
export const DOCS_STORAGE_WHITELIST_MAX = 20

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
  return get<DocsStorageQuota>('/admin-api/docs/storage-quota').then(normalizeQuota)
}

export function updateDocsStorageQuota(payload: DocsStorageQuota): Promise<DocsStorageQuota> {
  return put<DocsStorageQuota>('/admin-api/docs/storage-quota', payload).then(normalizeQuota)
}

function normalizeQuota(raw: DocsStorageQuota): DocsStorageQuota {
  return {
    defaultQuotaBytes: raw.defaultQuotaBytes ?? null,
    userRules: (raw.userRules ?? []).map(r => ({
      userId: String(r.userId),
      displayName: r.displayName,
      quotaBytes: r.quotaBytes ?? null
    })),
    deptRules: (raw.deptRules ?? []).map(r => ({
      deptId: String(r.deptId),
      deptName: r.deptName,
      quotaBytes: r.quotaBytes ?? null
    }))
  }
}

const GB = 1024 * 1024 * 1024

export function bytesToGbInput(bytes: number | null): string {
  if (bytes == null) {
    return ''
  }
  return String(Math.round((bytes / GB) * 100) / 100)
}

export function gbInputToBytes(gb: string): number | null {
  const trimmed = gb.trim()
  if (!trimmed) {
    return null
  }
  const n = Number(trimmed)
  if (!Number.isFinite(n) || n < 0) {
    throw new Error('配额须为非负数字')
  }
  return Math.round(n * GB)
}
