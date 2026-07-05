import { ref } from 'vue'
import { defineStore } from 'pinia'
import { mockTenant } from '../mocks/tenant'

export const useTenantStore = defineStore('tenant', () => {
  const tenantName = ref(mockTenant.name)

  return {
    tenantName
  }
})
