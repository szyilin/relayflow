import { del, get, post, put } from '../request'
import type { AxiosRequestConfig } from 'axios'

export const DOCS_BODY_FORMAT = 'tiptap_json_v1' as const

export type DocsBodyFormat = typeof DOCS_BODY_FORMAT
export type DocsObjectType = 'RICH_DOC'
export type DocsSharePermission = 'VIEW' | 'EDIT'
export type DocsAccessState = 'OK' | 'DELETED' | 'NO_ACCESS'

/** TipTap / ProseMirror document JSON */
export type TipTapDocJson = {
  type: 'doc'
  content?: TipTapNodeJson[]
}

export type TipTapNodeJson = {
  type: string
  attrs?: Record<string, unknown>
  content?: TipTapNodeJson[]
  marks?: { type: string, attrs?: Record<string, unknown> }[]
  text?: string
}

export interface DocsLibraryNode {
  nodeId: string
  parentId: string | null
  objectId: string
  title: string
  sortOrder: number
  createTime?: string
  lastOpenedAt?: string
  children: DocsLibraryNode[]
}

export interface DocsLibraryTreeResp {
  nodes: DocsLibraryNode[]
}

export interface DocsNodeCreateResp {
  nodeId: string
  objectId: string
  parentId: string | null
  title: string
  sortOrder: number
  contentVersion: number
  bodyFormat: DocsBodyFormat
}

export interface DocsDocument {
  objectId: string
  title: string
  type: DocsObjectType
  body: TipTapDocJson
  bodyFormat: DocsBodyFormat
  contentVersion: number
  lastOpenedAt?: string | null
  ownerUserId?: string
  ownedByMe?: boolean
  permission?: DocsSharePermission
}

export interface DocsRecentItem {
  objectId: string
  title: string
  titleSnapshot?: string
  type?: DocsObjectType
  accessState?: DocsAccessState
  locationLabel?: string
  ownerUserId?: string
  ownerDisplayName?: string
  ownedByMe?: boolean
  ownerAvatarFileId?: string | null
  createTime?: string | null
  lastOpenedAt?: string | null
  starred?: boolean
}

export interface DocsShareItem {
  shareId: string
  granteeUserId: string
  granteeDisplayName: string
  permission: DocsSharePermission
  grantedByUserId: string
  createTime: string
}

export interface DocsSharedWithMeItem {
  objectId: string
  title: string
  type: DocsObjectType
  permission: DocsSharePermission
  ownerUserId: string
  ownerDisplayName: string
  ownedByMe?: boolean
  ownerAvatarFileId?: string | null
  grantedByUserId: string
  grantedByDisplayName: string
  sharedAt: string
  starred: boolean
  createTime?: string | null
  lastOpenedAt?: string | null
  locationLabel?: string
}

export interface DocsStarredItem {
  objectId: string
  title: string
  type: DocsObjectType
  starredAt: string
  accessState: DocsAccessState
  ownerUserId: string
  ownerDisplayName: string
  ownedByMe?: boolean
  ownerAvatarFileId?: string | null
  locationLabel: string
  createTime?: string | null
  lastOpenedAt?: string | null
}

export interface DocsBodySaveResp {
  contentVersion: number
}

export interface DocsExportMdResp {
  markdown: string
}

export interface DocsEffectiveSettings {
  memberShareEnabled: boolean
  markdownExportEnabled: boolean
  driveUploadEnabled: boolean
}

export function emptyTipTapDoc(): TipTapDocJson {
  return {
    type: 'doc',
    content: [{ type: 'paragraph' }]
  }
}

export function listLibraryTree(): Promise<DocsLibraryTreeResp> {
  return get<DocsLibraryTreeResp>('/app-api/docs/library/tree')
}

export function createLibraryNode(payload: {
  parentId?: string | null
  title?: string
}): Promise<DocsNodeCreateResp> {
  return post<DocsNodeCreateResp>('/app-api/docs/library/nodes', {
    parentId: payload.parentId ?? null,
    title: payload.title ?? '未命名文档'
  })
}

export function updateLibraryNode(
  nodeId: string,
  payload: { title?: string, parentId?: string | null, sortOrder?: number },
  config?: AxiosRequestConfig
): Promise<DocsNodeCreateResp> {
  return put<DocsNodeCreateResp>(`/app-api/docs/library/nodes/${nodeId}`, payload, config)
}

export function deleteLibraryNode(nodeId: string): Promise<void> {
  return del<void>(`/app-api/docs/library/nodes/${nodeId}`)
}

export function getDocument(objectId: string, config?: AxiosRequestConfig): Promise<DocsDocument> {
  return get<DocsDocument>(`/app-api/docs/documents/${objectId}`, config)
}

export function saveDocumentBody(
  objectId: string,
  payload: { body: TipTapDocJson, contentVersion: number },
  config?: AxiosRequestConfig
): Promise<DocsBodySaveResp> {
  return put<DocsBodySaveResp>(`/app-api/docs/documents/${objectId}/body`, payload, config)
}

export function exportDocumentMarkdown(objectId: string): Promise<DocsExportMdResp> {
  return get<DocsExportMdResp>(`/app-api/docs/documents/${objectId}/export`, {
    params: { format: 'md' }
  })
}

export function listRecentDocuments(limit = 20): Promise<DocsRecentItem[]> {
  return get<DocsRecentItem[]>('/app-api/docs/recent', {
    params: { limit }
  })
}

export function removeRecentDocument(objectId: string): Promise<void> {
  return del<void>(`/app-api/docs/recent/${objectId}`)
}

export function listDocumentShares(objectId: string): Promise<{ shares: DocsShareItem[] }> {
  return get<{ shares: DocsShareItem[] }>(`/app-api/docs/documents/${objectId}/shares`)
}

export function createDocumentShare(
  objectId: string,
  payload: { granteeUserId: string, permission: DocsSharePermission }
): Promise<DocsShareItem> {
  return post<DocsShareItem>(`/app-api/docs/documents/${objectId}/shares`, payload)
}

export function updateDocumentShare(
  objectId: string,
  shareId: string,
  payload: { permission: DocsSharePermission }
): Promise<DocsShareItem> {
  return put<DocsShareItem>(`/app-api/docs/documents/${objectId}/shares/${shareId}`, payload)
}

export function deleteDocumentShare(objectId: string, shareId: string): Promise<void> {
  return del<void>(`/app-api/docs/documents/${objectId}/shares/${shareId}`)
}

export function listSharedWithMe(limit = 50): Promise<{ items: DocsSharedWithMeItem[] }> {
  return get<{ items: DocsSharedWithMeItem[] }>('/app-api/docs/shared', {
    params: { limit }
  })
}

export function leaveSharedDocument(objectId: string): Promise<void> {
  return del<void>(`/app-api/docs/shared/${objectId}`)
}

export function listStarredDocuments(limit = 50): Promise<{ items: DocsStarredItem[] }> {
  return get<{ items: DocsStarredItem[] }>('/app-api/docs/starred', {
    params: { limit }
  })
}

export function starDocument(objectId: string): Promise<void> {
  return put<void>(`/app-api/docs/documents/${objectId}/star`)
}

export function unstarDocument(objectId: string): Promise<void> {
  return del<void>(`/app-api/docs/documents/${objectId}/star`)
}

export function getDocsEffectiveSettings(): Promise<DocsEffectiveSettings> {
  return get<DocsEffectiveSettings>('/app-api/docs/settings/effective')
}
