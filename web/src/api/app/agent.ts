import { del, get, post, put } from '../request'

export interface AgentDef {
  id: string
  name: string
  description?: string | null
  systemPrompt: string
  modelRef: string
  status: 'active' | 'disabled'
  ownerUserId?: string
  visibility?: string
}

export interface AgentDefSavePayload {
  id?: string
  name: string
  description?: string
  systemPrompt?: string
  modelRef: string
  status?: 'active' | 'disabled'
}

export interface AgentSession {
  id: string
  agentId: string
  agentName?: string | null
  title?: string | null
  status: string
}

export interface AgentSessionMessage {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  seq: number
}

export interface AgentSessionSendResult {
  userMessage: AgentSessionMessage
  assistantMessage: AgentSessionMessage
}

export interface EnabledLlmModel {
  code: string
  name: string
  modelName: string
  providerType: string
}

export function listAgentDefs(): Promise<AgentDef[]> {
  return get<AgentDef[]>('/app-api/agent/def/list')
}

export function getAgentDef(id: string): Promise<AgentDef> {
  return get<AgentDef>('/app-api/agent/def/get', { params: { id } })
}

export function createAgentDef(data: AgentDefSavePayload): Promise<string> {
  return post<string>('/app-api/agent/def/create', data).then(String)
}

export function updateAgentDef(data: AgentDefSavePayload): Promise<boolean> {
  return put<boolean>('/app-api/agent/def/update', data)
}

export function deleteAgentDef(id: string): Promise<boolean> {
  return del<boolean>('/app-api/agent/def/delete', { params: { id } })
}

export function createAgentSession(agentId: string, title?: string): Promise<string> {
  return post<string>('/app-api/agent/session/create', { agentId, title }).then(String)
}

export function listAgentSessions(): Promise<AgentSession[]> {
  return get<AgentSession[]>('/app-api/agent/session/list')
}

export function getAgentSession(id: string): Promise<AgentSession> {
  return get<AgentSession>('/app-api/agent/session/get', { params: { id } })
}

export function listAgentSessionMessages(sessionId: string): Promise<AgentSessionMessage[]> {
  return get<AgentSessionMessage[]>('/app-api/agent/session/messages', { params: { sessionId } })
}

export function sendAgentSessionMessage(sessionId: string, content: string): Promise<AgentSessionSendResult> {
  return post<AgentSessionSendResult>('/app-api/agent/session/send', { sessionId, content })
}

export function listEnabledLlmModels(): Promise<EnabledLlmModel[]> {
  return get<EnabledLlmModel[]>('/app-api/agent/llm/model/list-enabled')
}
