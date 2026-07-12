import { post } from '../request'

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

export async function uploadPublicFile(file: File): Promise<string> {
  const session = await createUploadSession({
    filename: file.name,
    size: file.size,
    mimeType: file.type || 'application/octet-stream',
    accessLevel: 'public'
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
    etag: etag?.replaceAll('"', '')
  })

  return String(confirmed.fileId)
}
