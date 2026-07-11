import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  deleteStorageConfig,
  getStorageConfig,
  saveStorageConfig,
  testStorageConnection,
  type StorageProviderItem,
  type StorageProviderSavePayload,
  type StorageTestConnectionPayload
} from '../api/admin/storage'
import { ApiError } from '../api/request'

const DEFAULT_PROVIDER = 'minio'

export const useStorageStore = defineStore('storage', () => {
  const providers = ref<StorageProviderItem[]>([])
  const loading = ref(false)
  const lastError = ref<string | null>(null)

  function findMinioProvider() {
    return providers.value.find(item => item.provider === DEFAULT_PROVIDER) ?? null
  }

  async function fetchConfig() {
    loading.value = true
    lastError.value = null
    try {
      const response = await getStorageConfig()
      providers.value = response.providers ?? []
    } catch (error) {
      providers.value = []
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

  async function remove(provider = DEFAULT_PROVIDER) {
    await deleteStorageConfig(provider)
    await fetchConfig()
  }

  async function testConnection(payload: StorageTestConnectionPayload) {
    await testStorageConnection(payload)
  }

  return {
    providers,
    loading,
    lastError,
    findMinioProvider,
    fetchConfig,
    save,
    remove,
    testConnection
  }
})
