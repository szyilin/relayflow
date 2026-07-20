import { del, get, post, put } from '../request'

export const DOCS_BODY_FORMAT = 'tiptap_json_v1' as const

export type DocsBodyFormat = typeof DOCS_BODY_FORMAT
export type DocsObjectType = 'RICH_DOC'

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
}

export interface DocsRecentItem {
  objectId: string
  title: string
  lastOpenedAt?: string | null
}

export interface DocsBodySaveResp {
  contentVersion: number
}

export interface DocsExportMdResp {
  markdown: string
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
  payload: { title?: string, parentId?: string | null, sortOrder?: number }
): Promise<DocsNodeCreateResp> {
  return put<DocsNodeCreateResp>(`/app-api/docs/library/nodes/${nodeId}`, payload)
}

export function deleteLibraryNode(nodeId: string): Promise<void> {
  return del<void>(`/app-api/docs/library/nodes/${nodeId}`)
}

export function getDocument(objectId: string): Promise<DocsDocument> {
  return get<DocsDocument>(`/app-api/docs/documents/${objectId}`)
}

export function saveDocumentBody(
  objectId: string,
  payload: { body: TipTapDocJson, contentVersion: number }
): Promise<DocsBodySaveResp> {
  return put<DocsBodySaveResp>(`/app-api/docs/documents/${objectId}/body`, payload)
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
