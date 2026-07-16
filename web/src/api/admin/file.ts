import { del, get, post } from '../request'
import { getAccessToken } from '../../utils/session-storage'

export type FileAccessLevel = 'public' | 'private'

export interface FileListItem {
  id: number | string
  originalName: string
  mimeType: string
  size: number
  accessLevel: FileAccessLevel
  provider: string
  storageUri: string
  createTime: string
}

export interface FilePageResult {
  list: FileListItem[]
  total: number
}

export interface FilePageQuery {
  pageNo?: number
  pageSize?: number
  keyword?: string
}

export interface UploadSessionCreatePayload {
  filename: string
  size: number
  mimeType: string
  accessLevel?: FileAccessLevel
}

export interface UploadSessionResponse {
  uploadId: number | string
  mode: string
  objectKey: string
  uploadUrl: string
  headers: Record<string, string>
  expiresAt: string
}

export interface UploadConfirmPayload {
  uploadId: number | string
  etag?: string
  size: number
}

export interface UploadConfirmResponse {
  fileId: number | string
  storageUri: string
}

export function getFilePage(params: FilePageQuery): Promise<FilePageResult> {
  return get<FilePageResult>('/admin-api/infra/file/page', { params })
}

export function deleteFile(id: string | number): Promise<boolean> {
  return del<boolean>(`/admin-api/infra/file/${id}`)
}

export function createUploadSession(data: UploadSessionCreatePayload): Promise<UploadSessionResponse> {
  return post<UploadSessionResponse>('/admin-api/infra/file/upload-session', data)
}

export function confirmUpload(data: UploadConfirmPayload): Promise<UploadConfirmResponse> {
  return post<UploadConfirmResponse>('/admin-api/infra/file/confirm', data)
}

export async function openAdminFileDownload(id: string | number): Promise<void> {
  const baseURL = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') ?? ''
  const token = getAccessToken()
  const response = await fetch(`${baseURL}/admin-api/infra/file/${id}/download`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    redirect: 'manual'
  })

  if (response.status === 302) {
    const location = response.headers.get('Location')
    if (location) {
      window.open(location, '_blank', 'noopener,noreferrer')
      return
    }
  }

  let message = '下载失败'
  try {
    const payload = await response.json() as { msg?: string }
    if (payload.msg) {
      message = payload.msg
    }
  } catch {
    // ignore parse errors
  }
  throw new Error(message)
}
