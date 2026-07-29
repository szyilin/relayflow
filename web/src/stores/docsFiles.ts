import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  deleteDocsAdminFiles,
  getDocsAdminFiles,
  purgeDocsAdminFiles,
  restoreDocsAdminFiles,
  transferDocsOwner,
  type DocsAdminFileItem,
  type DocsAdminFilePageQuery,
  type DocsFileLifecycle,
  type DocsFileType
} from '../api/admin/docsFiles'
import { ApiError } from '../api/request'

/** Admin enterprise docs files — admin-api is the source of truth. */
export const useDocsFilesStore = defineStore('docsFiles', () => {
  const list = ref<DocsAdminFileItem[]>([])
  const total = ref(0)
  const pageNo = ref(1)
  const pageSize = ref(20)
  const loading = ref(false)
  const acting = ref(false)
  const lastError = ref<string | null>(null)

  const query = ref<DocsAdminFilePageQuery>({
    pageNo: 1,
    pageSize: 20,
    keyword: '',
    type: '',
    lifecycle: '',
    ownerUserId: ''
  })

  const selectedIds = ref<string[]>([])

  const selectedItems = computed(() =>
    list.value.filter(i => selectedIds.value.includes(i.objectId)))

  function resetForTenantSwitch() {
    list.value = []
    total.value = 0
    selectedIds.value = []
    lastError.value = null
    query.value = {
      pageNo: 1,
      pageSize: pageSize.value,
      keyword: '',
      type: '',
      lifecycle: '',
      ownerUserId: ''
    }
  }

  async function fetchPage(overrides?: Partial<DocsAdminFilePageQuery>) {
    loading.value = true
    lastError.value = null
    const next: DocsAdminFilePageQuery = {
      ...query.value,
      ...overrides,
      pageNo: overrides?.pageNo ?? query.value.pageNo ?? 1,
      pageSize: overrides?.pageSize ?? query.value.pageSize ?? pageSize.value
    }
    query.value = next
    try {
      const data = await getDocsAdminFiles(next)
      list.value = data.list
      total.value = data.total
      pageNo.value = data.pageNo
      pageSize.value = data.pageSize
      selectedIds.value = []
    } catch (error) {
      list.value = []
      total.value = 0
      selectedIds.value = []
      lastError.value = error instanceof ApiError
        ? error.message
        : '加载文件列表失败，请确认后端服务已启动'
      throw error
    } finally {
      loading.value = false
    }
  }

  async function transferOwner(objectIds: string[], newOwnerUserId: string) {
    acting.value = true
    lastError.value = null
    try {
      await transferDocsOwner({ objectIds, newOwnerUserId })
      await fetchPage({ pageNo: pageNo.value })
    } catch (error) {
      lastError.value = error instanceof ApiError || error instanceof Error
        ? error.message
        : '转移所有者失败'
      throw error
    } finally {
      acting.value = false
    }
  }

  async function softDelete(objectIds: string[]) {
    acting.value = true
    lastError.value = null
    try {
      await deleteDocsAdminFiles({ objectIds })
      await fetchPage({ pageNo: pageNo.value })
    } catch (error) {
      lastError.value = error instanceof ApiError || error instanceof Error
        ? error.message
        : '删除失败'
      throw error
    } finally {
      acting.value = false
    }
  }

  async function restore(objectIds: string[], newOwnerUserId?: string | null) {
    acting.value = true
    lastError.value = null
    try {
      await restoreDocsAdminFiles({ objectIds, newOwnerUserId })
      await fetchPage({ pageNo: pageNo.value })
    } catch (error) {
      lastError.value = error instanceof ApiError || error instanceof Error
        ? error.message
        : '恢复失败'
      throw error
    } finally {
      acting.value = false
    }
  }

  async function purge(objectIds: string[]) {
    acting.value = true
    lastError.value = null
    try {
      await purgeDocsAdminFiles({ objectIds })
      await fetchPage({ pageNo: pageNo.value })
    } catch (error) {
      lastError.value = error instanceof ApiError || error instanceof Error
        ? error.message
        : '销毁失败'
      throw error
    } finally {
      acting.value = false
    }
  }

  function toggleSelect(objectId: string, checked: boolean) {
    if (checked) {
      if (!selectedIds.value.includes(objectId)) {
        selectedIds.value = [...selectedIds.value, objectId]
      }
      return
    }
    selectedIds.value = selectedIds.value.filter(id => id !== objectId)
  }

  function toggleSelectAll(checked: boolean) {
    selectedIds.value = checked ? list.value.map(i => i.objectId) : []
  }

  function setTypeFilter(type: DocsFileType | '') {
    query.value = { ...query.value, type }
  }

  function setLifecycleFilter(lifecycle: DocsFileLifecycle | '') {
    query.value = { ...query.value, lifecycle }
  }

  return {
    list,
    total,
    pageNo,
    pageSize,
    loading,
    acting,
    lastError,
    query,
    selectedIds,
    selectedItems,
    resetForTenantSwitch,
    fetchPage,
    transferOwner,
    softDelete,
    restore,
    purge,
    toggleSelect,
    toggleSelectAll,
    setTypeFilter,
    setLifecycleFilter
  }
})
