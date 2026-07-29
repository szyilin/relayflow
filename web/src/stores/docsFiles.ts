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

function daysUntil(fromIso: string, windowDays: number): number {
  const deleted = new Date(fromIso).getTime()
  const end = deleted + windowDays * 24 * 60 * 60 * 1000
  return Math.max(0, Math.ceil((end - Date.now()) / (24 * 60 * 60 * 1000)))
}

function seedMockFiles(): DocsAdminFileItem[] {
  const now = Date.now()
  const iso = (offsetMs: number) => new Date(now + offsetMs).toISOString()
  return [
    {
      objectId: '9001',
      title: '产品路线图',
      type: 'RICH_DOC',
      lifecycle: 'ACTIVE',
      ownerUserId: '100',
      ownerDisplayName: '管理员',
      updatedAt: iso(-2 * 3600_000),
      sizeBytes: null,
      deletedAt: null,
      daysUntilPurge: null
    },
    {
      objectId: '9002',
      title: 'Q3 预算表.xlsx',
      type: 'FILE',
      lifecycle: 'ACTIVE',
      ownerUserId: '101',
      ownerDisplayName: '张三',
      updatedAt: iso(-26 * 3600_000),
      sizeBytes: 1_048_576,
      deletedAt: null,
      daysUntilPurge: null
    },
    {
      objectId: '9003',
      title: '已归档周报',
      type: 'RICH_DOC',
      lifecycle: 'DELETED',
      ownerUserId: '101',
      ownerDisplayName: '张三',
      updatedAt: iso(-10 * 24 * 3600_000),
      sizeBytes: null,
      deletedAt: iso(-10 * 24 * 3600_000),
      daysUntilPurge: daysUntil(iso(-10 * 24 * 3600_000), 90)
    },
    {
      objectId: '9004',
      title: '旧版手册.pdf',
      type: 'FILE',
      lifecycle: 'DELETED',
      ownerUserId: '102',
      ownerDisplayName: '李四',
      updatedAt: iso(-80 * 24 * 3600_000),
      sizeBytes: 2_097_152,
      deletedAt: iso(-80 * 24 * 3600_000),
      daysUntilPurge: daysUntil(iso(-80 * 24 * 3600_000), 90)
    },
    {
      objectId: '9005',
      title: '会议纪要（草稿）',
      type: 'RICH_DOC',
      lifecycle: 'ACTIVE',
      ownerUserId: '102',
      ownerDisplayName: '李四',
      updatedAt: iso(-5 * 3600_000),
      sizeBytes: null,
      deletedAt: null,
      daysUntilPurge: null
    }
  ]
}

function matchesQuery(item: DocsAdminFileItem, q: DocsAdminFilePageQuery): boolean {
  if (q.keyword?.trim()) {
    const kw = q.keyword.trim().toLowerCase()
    if (!item.title.toLowerCase().includes(kw)) {
      return false
    }
  }
  if (q.type && item.type !== q.type) {
    return false
  }
  if (q.lifecycle && item.lifecycle !== q.lifecycle) {
    return false
  }
  if (q.ownerUserId && item.ownerUserId !== q.ownerUserId) {
    return false
  }
  return item.lifecycle !== 'PURGED'
}

/** Admin enterprise docs files — mock until A1-api. */
export const useDocsFilesStore = defineStore('docsFiles', () => {
  const list = ref<DocsAdminFileItem[]>([])
  const total = ref(0)
  const pageNo = ref(1)
  const pageSize = ref(20)
  const loading = ref(false)
  const acting = ref(false)
  const lastError = ref<string | null>(null)
  const usingLocalDraft = ref(true)
  const mockRows = ref<DocsAdminFileItem[]>(seedMockFiles())

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
    mockRows.value = seedMockFiles()
    list.value = []
    total.value = 0
    selectedIds.value = []
    usingLocalDraft.value = true
    lastError.value = null
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
      usingLocalDraft.value = false
      selectedIds.value = []
    } catch (error) {
      // Web phase: filter mock in memory
      const filtered = mockRows.value.filter(i => matchesQuery(i, next))
      const size = next.pageSize ?? 20
      const page = next.pageNo ?? 1
      const start = (page - 1) * size
      list.value = filtered.slice(start, start + size)
      total.value = filtered.length
      pageNo.value = page
      pageSize.value = size
      usingLocalDraft.value = true
      selectedIds.value = []
      lastError.value = error instanceof ApiError
        ? error.message
        : '后端未就绪，已使用本地草稿'
    } finally {
      loading.value = false
    }
  }

  async function transferOwner(objectIds: string[], newOwnerUserId: string, newOwnerDisplayName?: string) {
    acting.value = true
    lastError.value = null
    try {
      await transferDocsOwner({ objectIds, newOwnerUserId })
      usingLocalDraft.value = false
      await fetchPage()
    } catch {
      const label = newOwnerDisplayName?.trim() || newOwnerUserId
      mockRows.value = mockRows.value.map((row) => {
        if (!objectIds.includes(row.objectId) || row.lifecycle !== 'ACTIVE') {
          return row
        }
        return {
          ...row,
          ownerUserId: newOwnerUserId,
          ownerDisplayName: label,
          updatedAt: new Date().toISOString()
        }
      })
      usingLocalDraft.value = true
      await fetchPage({ pageNo: pageNo.value })
    } finally {
      acting.value = false
    }
  }

  async function softDelete(objectIds: string[]) {
    acting.value = true
    try {
      await deleteDocsAdminFiles({ objectIds })
      usingLocalDraft.value = false
      await fetchPage()
    } catch {
      const now = new Date().toISOString()
      mockRows.value = mockRows.value.map((row) => {
        if (!objectIds.includes(row.objectId) || row.lifecycle !== 'ACTIVE') {
          return row
        }
        return {
          ...row,
          lifecycle: 'DELETED' as DocsFileLifecycle,
          deletedAt: now,
          daysUntilPurge: 90,
          updatedAt: now
        }
      })
      usingLocalDraft.value = true
      await fetchPage({ pageNo: pageNo.value })
    } finally {
      acting.value = false
    }
  }

  async function restore(objectIds: string[], newOwnerUserId?: string | null, newOwnerDisplayName?: string) {
    acting.value = true
    try {
      await restoreDocsAdminFiles({ objectIds, newOwnerUserId })
      usingLocalDraft.value = false
      await fetchPage()
    } catch {
      mockRows.value = mockRows.value.map((row) => {
        if (!objectIds.includes(row.objectId) || row.lifecycle !== 'DELETED') {
          return row
        }
        if (row.daysUntilPurge != null && row.daysUntilPurge <= 0) {
          return row
        }
        return {
          ...row,
          lifecycle: 'ACTIVE' as DocsFileLifecycle,
          deletedAt: null,
          daysUntilPurge: null,
          ownerUserId: newOwnerUserId || row.ownerUserId,
          ownerDisplayName: newOwnerUserId
            ? (newOwnerDisplayName?.trim() || newOwnerUserId)
            : row.ownerDisplayName,
          updatedAt: new Date().toISOString()
        }
      })
      usingLocalDraft.value = true
      await fetchPage({ pageNo: pageNo.value })
    } finally {
      acting.value = false
    }
  }

  async function purge(objectIds: string[]) {
    acting.value = true
    try {
      await purgeDocsAdminFiles({ objectIds })
      usingLocalDraft.value = false
      await fetchPage()
    } catch {
      mockRows.value = mockRows.value.map((row) => {
        if (!objectIds.includes(row.objectId)) {
          return row
        }
        return {
          ...row,
          lifecycle: 'PURGED' as DocsFileLifecycle,
          daysUntilPurge: null,
          updatedAt: new Date().toISOString()
        }
      })
      usingLocalDraft.value = true
      await fetchPage({ pageNo: pageNo.value })
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
    usingLocalDraft,
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
