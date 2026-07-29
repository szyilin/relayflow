import { get, post } from '../request'

export type DocsFileType = 'RICH_DOC' | 'FILE'
export type DocsFileLifecycle = 'ACTIVE' | 'DELETED' | 'PURGED'

export interface DocsAdminFileItem {
  objectId: string
  title: string
  type: DocsFileType
  lifecycle: DocsFileLifecycle
  ownerUserId: string
  ownerDisplayName: string
  updatedAt: string
  sizeBytes: number | null
  deletedAt: string | null
  /** Remaining days until purge eligibility ends; null when ACTIVE or already purged. */
  daysUntilPurge: number | null
}

export interface DocsAdminFilePageQuery {
  pageNo?: number
  pageSize?: number
  keyword?: string
  type?: DocsFileType | ''
  lifecycle?: DocsFileLifecycle | ''
  ownerUserId?: string
}

export interface DocsAdminFilePageResult {
  list: DocsAdminFileItem[]
  total: number
  pageNo: number
  pageSize: number
}

export interface DocsTransferOwnerReq {
  objectIds: string[]
  newOwnerUserId: string
}

export interface DocsRestoreReq {
  objectIds: string[]
  /** null/omit = previous owner; otherwise new owner */
  newOwnerUserId?: string | null
}

export interface DocsObjectIdsReq {
  objectIds: string[]
}

export function getDocsAdminFiles(params: DocsAdminFilePageQuery): Promise<DocsAdminFilePageResult> {
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
  if (params.type) {
    cleaned.type = params.type
  }
  if (params.lifecycle) {
    cleaned.lifecycle = params.lifecycle
  }
  if (params.ownerUserId?.trim()) {
    cleaned.ownerUserId = params.ownerUserId.trim()
  }
  return get<DocsAdminFilePageResult>('/admin-api/docs/files', { params: cleaned })
}

export function transferDocsOwner(payload: DocsTransferOwnerReq): Promise<boolean> {
  return post<boolean>('/admin-api/docs/files/transfer-owner', payload)
}

export function deleteDocsAdminFiles(payload: DocsObjectIdsReq): Promise<boolean> {
  return post<boolean>('/admin-api/docs/files/delete', payload)
}

export function restoreDocsAdminFiles(payload: DocsRestoreReq): Promise<boolean> {
  return post<boolean>('/admin-api/docs/files/restore', payload)
}

export function purgeDocsAdminFiles(payload: DocsObjectIdsReq): Promise<boolean> {
  return post<boolean>('/admin-api/docs/files/purge', payload)
}

export function docsWorkspaceLink(objectId: string): string {
  if (typeof window !== 'undefined' && window.location?.origin) {
    return `${window.location.origin}/app/docs?docId=${objectId}`
  }
  return `/app/docs?docId=${objectId}`
}
