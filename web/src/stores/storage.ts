import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  deleteStorageConfig,
  getStorageConfig,
  saveStorageConfig,
  setStorageEffectiveSource,
  testStorageConnection,
  type StorageBootstrapSummary,
  type StorageEffectiveSource,
  type StorageProviderItem,
  type StorageProviderSavePayload,
  type StorageTestConnectionPayload
} from '../api/admin/storage'
import { ApiError } from '../api/request'

const DEFAULT_PROVIDER = 'minio'

export const useStorageStore = defineStore('storage', () => {
  const providers = ref<StorageProviderItem[]>([])
  const effectiveSource = ref<StorageEffectiveSource>('bootstrap')
  const bootstrap = ref<StorageBootstrapSummary>({
    available: false,
    credentialsConfigured: false
  })
  const loading = ref(false)
  const lastError = ref<string | null>(null)

  const isTenantEffective = computed(() => effectiveSource.value === 'tenant')

  function findMinioProvider() {
    return providers.value.find(item => item.provider === DEFAULT_PROVIDER) ?? null
  }

  async function fetchConfig() {
    loading.value = true
    lastError.value = null
    try {
      const response = await getStorageConfig()
      providers.value = response.providers ?? []
      effectiveSource.value = response.effectiveSource ?? 'bootstrap'
      bootstrap.value = response.bootstrap ?? { available: false, credentialsConfigured: false }
    } catch (error) {
      providers.value = []
      effectiveSource.value = 'bootstrap'
      bootstrap.value = { available: false, credentialsConfigured: false }
      lastError.value = error instanceof ApiError
        ? error.message
        : '加载存储配置失败，请确认后端服务已启动'
      throw error
    } finally {
      loading.value = false
    }
  }

  async function save(payload: StorageProviderSavePayload) {
    await saveStorageConfig(payload)
    await fetchConfig()
  }

  async function setEffectiveSource(source: StorageEffectiveSource) {
    await setStorageEffectiveSource({ source })
    await fetchConfig()
  }

  async function remove(provider = DEFAULT_PROVIDER) {
    await deleteStorageConfig(provider)
    await fetchConfig()
  }

  async function testConnection(payload: StorageTestConnectionPayload) {
    await testStorageConnection(payload)
  }

  return {
    providers,
    effectiveSource,
    bootstrap,
    loading,
    lastError,
    isTenantEffective,
    findMinioProvider,
    fetchConfig,
    save,
    setEffectiveSource,
    remove,
    testConnection
  }
})
