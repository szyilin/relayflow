import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  deleteFile,
  getFilePage,
  type FileListItem,
  type FilePageQuery
} from '../api/admin/file'
import { ApiError } from '../api/request'

export interface FileListRecord {
  id: string
  originalName: string
  mimeType: string
  size: number
  sizeLabel: string
  accessLevel: string
  provider: string
  storageUri: string
  createTime: string
}

function formatSize(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`
  }
  if (bytes < 1024 * 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  }
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`
}

function normalizeItem(item: FileListItem): FileListRecord {
  return {
    id: String(item.id),
    originalName: item.originalName,
    mimeType: item.mimeType || '—',
    size: item.size,
    sizeLabel: formatSize(item.size),
    accessLevel: item.accessLevel === 'public' ? '公开' : '私有',
    provider: item.provider?.toUpperCase() ?? '—',
    storageUri: item.storageUri,
    createTime: item.createTime?.slice(0, 19).replace('T', ' ') ?? '—'
  }
}

export const useFileStore = defineStore('file', () => {
  const list = ref<FileListRecord[]>([])
  const total = ref(0)
  const loading = ref(false)
  const page = ref(1)
  const pageSize = ref(10)
  const keyword = ref('')
  const lastError = ref<string | null>(null)

  async function fetchPage(options?: FilePageQuery) {
    if (options?.pageNo !== undefined) {
      page.value = options.pageNo
    }
    if (options?.pageSize !== undefined) {
      pageSize.value = options.pageSize
    }
    if (options?.keyword !== undefined) {
      keyword.value = options.keyword
    }

    loading.value = true
    lastError.value = null
    try {
      const data = await getFilePage({
        pageNo: page.value,
        pageSize: pageSize.value,
        keyword: keyword.value.trim() || undefined
      })
      list.value = data.list.map(normalizeItem)
      total.value = data.total
    } catch (error) {
      list.value = []
      total.value = 0
      lastError.value = error instanceof ApiError
        ? error.message
        : '加载文件列表失败，请确认后端服务已启动'
      throw error
    } finally {
      loading.value = false
    }
  }

  async function remove(id: string) {
    await deleteFile(id)
    if (list.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await fetchPage()
  }

  return {
    list,
    total,
    loading,
    page,
    pageSize,
    keyword,
    lastError,
    fetchPage,
    remove
  }
})
