import { ref } from 'vue'
import {
  confirmUpload,
  createUploadSession,
  type FileAccessLevel,
  type UploadConfirmResponse
} from '../api/admin/file'
import { ApiError } from '../api/request'

export function useDirectUpload() {
  const uploading = ref(false)
  const progress = ref(0)

  async function upload(file: File, options?: { accessLevel?: FileAccessLevel }): Promise<UploadConfirmResponse> {
    if (!file.size) {
      throw new ApiError(0, '不能上传空文件')
    }

    uploading.value = true
    progress.value = 0

    try {
      const session = await createUploadSession({
        filename: file.name,
        size: file.size,
        mimeType: file.type || 'application/octet-stream',
        accessLevel: options?.accessLevel ?? 'private'
      })

      progress.value = 30

      const response = await fetch(session.uploadUrl, {
        method: 'PUT',
        body: file,
        headers: session.headers
      })

      if (!response.ok) {
        throw new ApiError(0, `对象存储上传失败（HTTP ${response.status}）`)
      }

      progress.value = 80

      const etag = response.headers.get('ETag')
        ?? response.headers.get('etag')
        ?? undefined

      const result = await confirmUpload({
        uploadId: session.uploadId,
        etag,
        size: file.size
      })

      progress.value = 100
      return result
    } finally {
      uploading.value = false
    }
  }

  return {
    uploading,
    progress,
    upload
  }
}
