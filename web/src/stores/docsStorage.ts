import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getDocsStorageQuota,
  getDocsStorageUsage,
  updateDocsStorageQuota,
  type DocsStorageQuota,
  type DocsStorageQuotaDeptRule,
  type DocsStorageQuotaUserRule,
  type DocsStorageUsageItem,
  type DocsStorageUsagePageQuery
} from '../api/admin/docsStorage'
import { ApiError } from '../api/request'

/** Admin docs storage usage / quota — admin-api is the source of truth. */
export const useDocsStorageStore = defineStore('docsStorage', () => {
  const list = ref<DocsStorageUsageItem[]>([])
  const total = ref(0)
  const pageNo = ref(1)
  const pageSize = ref(20)
  const loading = ref(false)
  const saving = ref(false)
  const lastError = ref<string | null>(null)

  const quota = ref<DocsStorageQuota>({
    defaultQuotaBytes: null,
    userRules: [],
    deptRules: []
  })
  const quotaLoading = ref(false)

  const query = ref<DocsStorageUsagePageQuery>({
    pageNo: 1,
    pageSize: 20,
    keyword: ''
  })

  function resetForTenantSwitch() {
    list.value = []
    total.value = 0
    lastError.value = null
    quota.value = { defaultQuotaBytes: null, userRules: [], deptRules: [] }
    query.value = { pageNo: 1, pageSize: pageSize.value, keyword: '' }
  }

  async function fetchUsage(overrides?: Partial<DocsStorageUsagePageQuery>) {
    loading.value = true
    lastError.value = null
    const next: DocsStorageUsagePageQuery = {
      ...query.value,
      ...overrides,
      pageNo: overrides?.pageNo ?? query.value.pageNo ?? 1,
      pageSize: overrides?.pageSize ?? query.value.pageSize ?? pageSize.value
    }
    query.value = next
    try {
      const data = await getDocsStorageUsage(next)
      list.value = data.list
      total.value = data.total
      pageNo.value = data.pageNo
      pageSize.value = data.pageSize
    } catch (error) {
      list.value = []
      total.value = 0
      lastError.value = error instanceof ApiError
        ? error.message
        : '加载存储用量失败，请确认后端服务已启动'
      throw error
    } finally {
      loading.value = false
    }
  }

  async function loadQuota() {
    quotaLoading.value = true
    try {
      quota.value = await getDocsStorageQuota()
    } catch (error) {
      lastError.value = error instanceof ApiError
        ? error.message
        : '加载存储配额失败'
      throw error
    } finally {
      quotaLoading.value = false
    }
  }

  async function saveQuota(next: DocsStorageQuota) {
    saving.value = true
    lastError.value = null
    try {
      const payload: DocsStorageQuota = {
        defaultQuotaBytes: next.defaultQuotaBytes,
        userRules: next.userRules.map((rule): DocsStorageQuotaUserRule => ({
          userId: rule.userId,
          quotaBytes: rule.quotaBytes
        })),
        deptRules: (next.deptRules ?? []).map((rule): DocsStorageQuotaDeptRule => ({
          deptId: rule.deptId,
          quotaBytes: rule.quotaBytes
        }))
      }
      quota.value = await updateDocsStorageQuota(payload)
      await fetchUsage({ pageNo: pageNo.value })
    } catch (error) {
      lastError.value = error instanceof ApiError || error instanceof Error
        ? error.message
        : '保存配额失败'
      throw error
    } finally {
      saving.value = false
    }
  }

  return {
    list,
    total,
    pageNo,
    pageSize,
    loading,
    saving,
    lastError,
    quota,
    quotaLoading,
    query,
    resetForTenantSwitch,
    fetchUsage,
    loadQuota,
    saveQuota
  }
})
