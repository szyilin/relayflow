import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getDefaultTenant } from '../api/admin/tenant'
import { ApiError } from '../api/request'

const FALLBACK_TENANT_NAME = '默认企业'

export const useTenantStore = defineStore('tenant', () => {
  const tenantName = ref(FALLBACK_TENANT_NAME)
  const loading = ref(false)
  const loaded = ref(false)
  const lastError = ref<string | null>(null)

  async function fetchDefaultTenant() {
    if (loading.value) {
      return
    }

    loading.value = true
    lastError.value = null
    try {
      const data = await getDefaultTenant()
      tenantName.value = data.name
      loaded.value = true
    } catch (error) {
      tenantName.value = FALLBACK_TENANT_NAME
      lastError.value = error instanceof ApiError
        ? error.message
        : '无法加载租户信息'
    } finally {
      loading.value = false
    }
  }

  return {
    tenantName,
    loading,
    loaded,
    lastError,
    fetchDefaultTenant
  }
})
