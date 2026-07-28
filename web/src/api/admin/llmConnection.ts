import { del, get, post, put } from '../request'

export interface LlmConnectionItem {
  id: string | number
  code: string
  name: string
  providerType: string
  baseUrl: string
  modelName: string
  enabled: boolean
  apiKeyConfigured: boolean
}

export interface LlmConnectionSavePayload {
  id?: string | number
  code: string
  name: string
  providerType?: string
  baseUrl: string
  apiKey?: string
  modelName: string
  enabled: boolean
}

export interface LlmConnectionTestPayload {
  id?: string | number
  baseUrl?: string
  apiKey?: string
  providerType?: string
}

export function listLlmConnections(): Promise<LlmConnectionItem[]> {
  return get<LlmConnectionItem[]>('/admin-api/agent/llm/connection/list')
}

export function getLlmConnection(id: string | number): Promise<LlmConnectionItem> {
  return get<LlmConnectionItem>('/admin-api/agent/llm/connection/get', {
    params: { id }
  })
}

export function createLlmConnection(data: LlmConnectionSavePayload): Promise<string | number> {
  return post<string | number>('/admin-api/agent/llm/connection/create', data)
}

export function updateLlmConnection(data: LlmConnectionSavePayload): Promise<boolean> {
  return put<boolean>('/admin-api/agent/llm/connection/update', data)
}

export function deleteLlmConnection(id: string | number): Promise<boolean> {
  return del<boolean>('/admin-api/agent/llm/connection/delete', {
    params: { id }
  })
}

export function testLlmConnection(data: LlmConnectionTestPayload): Promise<boolean> {
  return post<boolean>('/admin-api/agent/llm/connection/test', data)
}
