import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  createLlmConnection,
  deleteLlmConnection,
  listLlmConnections,
  testLlmConnection,
  updateLlmConnection,
  type LlmConnectionItem,
  type LlmConnectionSavePayload,
  type LlmConnectionTestPayload
} from '../api/admin/llmConnection'
import { ApiError } from '../api/request'

export const useLlmConnectionStore = defineStore('llmConnection', () => {
  const connections = ref<LlmConnectionItem[]>([])
  const loading = ref(false)
  const lastError = ref<string | null>(null)

  async function fetchList() {
    loading.value = true
    lastError.value = null
    try {
      connections.value = await listLlmConnections() ?? []
    } catch (error) {
      connections.value = []
      lastError.value = error instanceof ApiError
        ? error.message
        : '加载模型连接失败，请确认后端服务已启动'
      throw error
    } finally {
      loading.value = false
    }
  }

  async function save(payload: LlmConnectionSavePayload) {
    if (payload.id != null) {
      await updateLlmConnection(payload)
    } else {
      await createLlmConnection(payload)
    }
    await fetchList()
  }

  async function remove(id: string | number) {
    await deleteLlmConnection(id)
    await fetchList()
  }

  async function test(payload: LlmConnectionTestPayload) {
    await testLlmConnection(payload)
  }

  return {
    connections,
    loading,
    lastError,
    fetchList,
    save,
    remove,
    test
  }
})
