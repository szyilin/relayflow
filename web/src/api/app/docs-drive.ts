import { del, get, post, put } from '../request'

export type DocsDriveObjectType = 'FILE' | 'RICH_DOC'
export type DocsPlacementTarget = 'DRIVE' | 'LIBRARY'

export interface DocsDriveFolder {
  folderId: string
  parentId: string | null
  name: string
  sortOrder: number
  updateTime: string
}

export interface DocsDriveItem {
  itemId: string
  folderId: string | null
  objectId: string
  type: DocsDriveObjectType
  title: string
  storageFileId?: string | null
  sizeBytes?: number | null
  mimeType?: string | null
  sortOrder: number
  updateTime: string
}

export interface DocsDriveListing {
  folders: DocsDriveFolder[]
  items: DocsDriveItem[]
}

export interface DocsDriveFolderListResp {
  folders: DocsDriveFolder[]
}

export interface DocsPlacementMoveResp {
  objectId: string
  target: DocsPlacementTarget
  placementId: string
}

export function infraFileDownloadPath(fileId: string): string {
  return `/app-api/infra/file/download?fileId=${encodeURIComponent(fileId)}`
}

export function listDriveItems(folderId?: string | null): Promise<DocsDriveListing> {
  return get<DocsDriveListing>('/app-api/docs/drive/items', {
    params: folderId ? { folderId } : {}
  })
}

export function listDriveFolders(parentId?: string | null): Promise<DocsDriveFolderListResp> {
  return get<DocsDriveFolderListResp>('/app-api/docs/drive/folders', {
    params: parentId ? { parentId } : {}
  })
}

export function createDriveFolder(payload: {
  parentId?: string | null
  name?: string
}): Promise<DocsDriveFolder> {
  return post<DocsDriveFolder>('/app-api/docs/drive/folders', {
    parentId: payload.parentId ?? null,
    name: payload.name ?? '新建文件夹'
  })
}

export function updateDriveFolder(
  folderId: string,
  payload: { name?: string, parentId?: string | null, sortOrder?: number }
): Promise<DocsDriveFolder> {
  return put<DocsDriveFolder>(`/app-api/docs/drive/folders/${folderId}`, payload)
}

export function deleteDriveFolder(folderId: string): Promise<void> {
  return del<void>(`/app-api/docs/drive/folders/${folderId}`)
}

export function registerDriveFile(payload: {
  folderId?: string | null
  fileId: string
  title?: string
}): Promise<DocsDriveItem> {
  return post<DocsDriveItem>('/app-api/docs/drive/files', {
    folderId: payload.folderId ?? null,
    fileId: payload.fileId,
    title: payload.title
  })
}

export function updateDriveItem(
  itemId: string,
  payload: { title?: string, folderId?: string | null, sortOrder?: number }
): Promise<DocsDriveItem> {
  return put<DocsDriveItem>(`/app-api/docs/drive/items/${itemId}`, payload)
}

export function deleteDriveItem(itemId: string): Promise<void> {
  return del<void>(`/app-api/docs/drive/items/${itemId}`)
}

export function moveDocPlacement(payload: {
  objectId: string
  target: DocsPlacementTarget
  folderId?: string | null
  parentId?: string | null
}): Promise<DocsPlacementMoveResp> {
  return post<DocsPlacementMoveResp>('/app-api/docs/drive/placements/move', {
    objectId: payload.objectId,
    target: payload.target,
    folderId: payload.target === 'DRIVE' ? (payload.folderId ?? null) : undefined,
    parentId: payload.target === 'LIBRARY' ? (payload.parentId ?? null) : undefined
  })
}
