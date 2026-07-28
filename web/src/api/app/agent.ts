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
  skillIds?: string[]
}

export interface AgentDefSavePayload {
  id?: string
  name: string
  description?: string
  systemPrompt?: string
  modelRef: string
  status?: 'active' | 'disabled'
  skillIds?: string[]
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

type RawAgentDef = Omit<AgentDef, 'id' | 'ownerUserId' | 'status'> & {
  id?: string | number
  ownerUserId?: string | number
  status?: string
}

type RawSession = Omit<AgentSession, 'id' | 'agentId'> & {
  id?: string | number
  agentId?: string | number
}

type RawMessage = Omit<AgentSessionMessage, 'id' | 'role'> & {
  id?: string | number
  role?: string
}

function normalizeDef(row: RawAgentDef): AgentDef {
  const status = row.status === 'disabled' ? 'disabled' : 'active'
  return {
    id: String(row.id),
    name: row.name,
    description: row.description ?? null,
    systemPrompt: row.systemPrompt ?? '',
    modelRef: row.modelRef,
    status,
    ownerUserId: row.ownerUserId != null ? String(row.ownerUserId) : undefined,
    visibility: row.visibility,
    skillIds: Array.isArray(row.skillIds) ? row.skillIds.map(String) : []
  }
}

function normalizeSession(row: RawSession): AgentSession {
  return {
    id: String(row.id),
    agentId: String(row.agentId),
    agentName: row.agentName ?? null,
    title: row.title ?? null,
    status: row.status
  }
}

function normalizeMessage(row: RawMessage): AgentSessionMessage {
  const role = row.role === 'assistant' || row.role === 'system' ? row.role : 'user'
  return {
    id: String(row.id),
    role,
    content: row.content ?? '',
    seq: Number(row.seq) || 0
  }
}

export function listAgentDefs(): Promise<AgentDef[]> {
  return get<RawAgentDef[]>('/app-api/agent/def/list').then(rows => (rows ?? []).map(normalizeDef))
}

export function getAgentDef(id: string): Promise<AgentDef> {
  return get<RawAgentDef>('/app-api/agent/def/get', { params: { id } }).then(normalizeDef)
}

export function createAgentDef(data: AgentDefSavePayload): Promise<string> {
  return post<string | number>('/app-api/agent/def/create', data).then(id => String(id))
}

export function updateAgentDef(data: AgentDefSavePayload): Promise<boolean> {
  return put<boolean>('/app-api/agent/def/update', data)
}

export function deleteAgentDef(id: string): Promise<boolean> {
  return del<boolean>('/app-api/agent/def/delete', { params: { id } })
}

export function createAgentSession(agentId: string, title?: string): Promise<string> {
  return post<string | number>('/app-api/agent/session/create', { agentId, title }).then(id => String(id))
}

export function listAgentSessions(): Promise<AgentSession[]> {
  return get<RawSession[]>('/app-api/agent/session/list').then(rows => (rows ?? []).map(normalizeSession))
}

export function getAgentSession(id: string): Promise<AgentSession> {
  return get<RawSession>('/app-api/agent/session/get', { params: { id } }).then(normalizeSession)
}

export function listAgentSessionMessages(sessionId: string): Promise<AgentSessionMessage[]> {
  return get<RawMessage[]>('/app-api/agent/session/messages', { params: { sessionId } })
    .then(rows => (rows ?? []).map(normalizeMessage))
}

export function sendAgentSessionMessage(sessionId: string, content: string): Promise<AgentSessionSendResult> {
  return post<{
    userMessage: RawMessage
    assistantMessage: RawMessage
  }>('/app-api/agent/session/send', { sessionId, content }).then(res => ({
    userMessage: normalizeMessage(res.userMessage),
    assistantMessage: normalizeMessage(res.assistantMessage)
  }))
}

export function listEnabledLlmModels(): Promise<EnabledLlmModel[]> {
  return get<EnabledLlmModel[]>('/app-api/agent/llm/model/list-enabled').then(rows => rows ?? [])
}

export interface SkillCatalogItem {
  code: string
  name: string
  description: string
  inputSchema?: string
}

export function listSkillCatalog(): Promise<SkillCatalogItem[]> {
  return get<SkillCatalogItem[]>('/app-api/agent/skill/catalog').then(rows => rows ?? [])
}
