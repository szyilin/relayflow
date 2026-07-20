import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  createDriveFolder,
  deleteDriveFolder,
  deleteDriveItem,
  listDriveItems,
  moveDocPlacement,
  registerDriveFile,
  updateDriveFolder,
  updateDriveItem,
  type DocsDriveListing
} from '../api/app/docs-drive'
import { uploadPrivateFile } from '../api/app/file'
import { ApiError } from '../api/request'

function toStoreError(error: unknown, fallback: string): Error {
  if (error instanceof ApiError) {
    return new Error(error.message || fallback)
  }
  if (error instanceof Error) {
    return error
  }
  return new Error(fallback)
}

export const useDocsDriveStore = defineStore('docsDrive', () => {
  const currentFolderId = ref<string | null>(null)
  const listing = ref<DocsDriveListing>({ folders: [], items: [] })
  const pathStack = ref<{ folderId: string | null, name: string }[]>([
    { folderId: null, name: '我的文件夹' }
  ])
  const loading = ref(false)
  const error = ref<string | null>(null)

  const breadcrumbs = computed(() => pathStack.value)

  async function loadListing(folderId: string | null = currentFolderId.value) {
    loading.value = true
    error.value = null
    try {
      listing.value = await listDriveItems(folderId)
      currentFolderId.value = folderId
    } catch (e) {
      error.value = toStoreError(e, '加载云盘失败').message
      throw e
    } finally {
      loading.value = false
    }
  }

  async function enterFolder(folderId: string | null, folderName?: string) {
    if (folderId === null) {
      pathStack.value = [{ folderId: null, name: '我的文件夹' }]
      await loadListing(null)
      return
    }
    const existingIndex = pathStack.value.findIndex(c => c.folderId === folderId)
    if (existingIndex >= 0) {
      pathStack.value = pathStack.value.slice(0, existingIndex + 1)
    } else {
      const name = folderName
        ?? listing.value.folders.find(f => f.folderId === folderId)?.name
        ?? '文件夹'
      pathStack.value = [...pathStack.value, { folderId, name }]
    }
    await loadListing(folderId)
  }

  async function createFolder(name: string) {
    const created = await createDriveFolder({
      parentId: currentFolderId.value,
      name: name.trim() || '新建文件夹'
    })
    await loadListing(currentFolderId.value)
    return created
  }

  async function renameFolder(folderId: string, name: string) {
    await updateDriveFolder(folderId, { name: name.trim() || '新建文件夹' })
    pathStack.value = pathStack.value.map(c =>
      c.folderId === folderId ? { ...c, name: name.trim() || '新建文件夹' } : c
    )
    await loadListing(currentFolderId.value)
  }

  async function deleteFolder(folderId: string) {
    try {
      await deleteDriveFolder(folderId)
    } catch (e) {
      throw toStoreError(e, '删除文件夹失败')
    }
    if (currentFolderId.value === folderId) {
      const parent = pathStack.value.length > 1
        ? pathStack.value[pathStack.value.length - 2]
        : { folderId: null, name: '我的文件夹' }
      pathStack.value = pathStack.value.slice(0, Math.max(1, pathStack.value.length - 1))
      await loadListing(parent.folderId)
      return
    }
    await loadListing(currentFolderId.value)
  }

  async function registerUploadedFile(file: File) {
    const fileId = await uploadPrivateFile(file)
    const item = await registerDriveFile({
      folderId: currentFolderId.value,
      fileId,
      title: file.name || '未命名文件'
    })
    await loadListing(currentFolderId.value)
    return item
  }

  async function renameItem(itemId: string, title: string) {
    await updateDriveItem(itemId, { title: title.trim() || '未命名文件' })
    await loadListing(currentFolderId.value)
  }

  async function deleteItem(itemId: string) {
    await deleteDriveItem(itemId)
    await loadListing(currentFolderId.value)
  }

  async function moveToDrive(objectId: string, folderId: string | null = null) {
    const resp = await moveDocPlacement({
      objectId,
      target: 'DRIVE',
      folderId
    })
    await loadListing(currentFolderId.value)
    return resp
  }

  async function moveToLibrary(objectId: string, parentId: string | null = null) {
    const resp = await moveDocPlacement({
      objectId,
      target: 'LIBRARY',
      parentId
    })
    await loadListing(currentFolderId.value)
    return resp
  }

  function formatSize(bytes: number | null | undefined): string {
    if (bytes == null || Number.isNaN(Number(bytes))) {
      return '—'
    }
    const n = Number(bytes)
    if (n < 1024) {
      return `${n} B`
    }
    if (n < 1024 * 1024) {
      return `${(n / 1024).toFixed(1)} KB`
    }
    return `${(n / (1024 * 1024)).toFixed(1)} MB`
  }

  function resetLocal() {
    currentFolderId.value = null
    listing.value = { folders: [], items: [] }
    pathStack.value = [{ folderId: null, name: '我的文件夹' }]
    loading.value = false
    error.value = null
  }

  return {
    currentFolderId,
    listing,
    breadcrumbs,
    loading,
    error,
    loadListing,
    enterFolder,
    createFolder,
    renameFolder,
    deleteFolder,
    registerUploadedFile,
    renameItem,
    deleteItem,
    moveToDrive,
    moveToLibrary,
    formatSize,
    resetLocal
  }
})
