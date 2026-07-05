import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getDefaultTenant } from '../api/admin/tenant'
import { isApiUnavailable } from '../api/request'
import { mockTenant } from '../mocks/tenant'

const FALLBACK_TENANT_NAME = '默认企业'

export const useTenantStore = defineStore('tenant', () => {
  const tenantName = ref(FALLBACK_TENANT_NAME)
  const loading = ref(false)
  const loaded = ref(false)

  async function fetchDefaultTenant() {
    if (loading.value) {
      return
    }

    loading.value = true
    try {
      const data = await getDefaultTenant()
      tenantName.value = data.name
      loaded.value = true
    } catch (error) {
      if (isApiUnavailable(error)) {
        tenantName.value = mockTenant.name
        loaded.value = true
        return
      }

      tenantName.value = FALLBACK_TENANT_NAME
    } finally {
      loading.value = false
    }
  }

  return {
    tenantName,
    loading,
    loaded,
    fetchDefaultTenant
  }
})
