import { post } from '../request'
import { getAccessToken } from '../../utils/session-storage'

export interface FileUploadSessionCreateReq {
  filename: string
  size: number
  mimeType: string
  accessLevel?: 'public' | 'private'
}

export interface FileUploadSessionResp {
  uploadId: number
  uploadUrl: string
  headers?: Record<string, string>
  objectKey: string
  expiresAt: string
}

export interface FileUploadConfirmReq {
  uploadId: number
  size: number
  etag?: string
}

export interface FileUploadConfirmResp {
  fileId: number
  storageUri: string
}

export function createUploadSession(data: FileUploadSessionCreateReq): Promise<FileUploadSessionResp> {
  return post<FileUploadSessionResp>('/app-api/infra/file/upload-session', {
    ...data,
    accessLevel: data.accessLevel ?? 'public'
  })
}

export function confirmUpload(data: FileUploadConfirmReq): Promise<FileUploadConfirmResp> {
  return post<FileUploadConfirmResp>('/app-api/infra/file/upload-confirm', data)
}

async function uploadFileWithAccessLevel(file: File, accessLevel: 'public' | 'private'): Promise<string> {
  const session = await createUploadSession({
    filename: file.name,
    size: file.size,
    mimeType: file.type || 'application/octet-stream',
    accessLevel
  })

  const headers = new Headers(session.headers ?? {})
  if (!headers.has('Content-Type')) {
    headers.set('Content-Type', file.type || 'application/octet-stream')
  }

  const uploadResponse = await fetch(session.uploadUrl, {
    method: 'PUT',
    headers,
    body: file
  })

  if (!uploadResponse.ok) {
    throw new Error('头像上传失败')
  }

  const etag = uploadResponse.headers.get('ETag') ?? undefined
  const confirmed = await confirmUpload({
    uploadId: session.uploadId,
    size: file.size,
    etag: etag?.replace(/"/g, '')
  })

  return String(confirmed.fileId)
}

export async function uploadPublicFile(file: File): Promise<string> {
  return uploadFileWithAccessLevel(file, 'public')
}

export async function uploadPrivateFile(file: File): Promise<string> {
  return uploadFileWithAccessLevel(file, 'private')
}

function apiBaseUrl(): string {
  return import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') ?? ''
}

function resolveRequestUrl(path: string): string {
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path
  }
  const base = apiBaseUrl()
  if (base) {
    return `${base}${path.startsWith('/') ? path : `/${path}`}`
  }
  return path.startsWith('/') ? path : `/${path}`
}

export async function fetchAuthenticatedBlobUrl(path: string): Promise<string> {
  const token = getAccessToken()
  const response = await fetch(resolveRequestUrl(path), {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    redirect: 'follow'
  })
  if (!response.ok) {
    throw new Error('附件加载失败')
  }
  const blob = await response.blob()
  return URL.createObjectURL(blob)
}

export async function downloadAuthenticatedFile(path: string, filename: string): Promise<void> {
  const blobUrl = await fetchAuthenticatedBlobUrl(path)
  try {
    const anchor = document.createElement('a')
    anchor.href = blobUrl
    anchor.download = filename
    anchor.rel = 'noopener'
    document.body.appendChild(anchor)
    anchor.click()
    anchor.remove()
  } finally {
    URL.revokeObjectURL(blobUrl)
  }
}
