import { ApiError, del, get, post, put } from '../request'
import { getAccessToken } from '../../utils/session-storage'

export interface AgentCapabilityProfile {
  hotPacks?: string[]
  deferredDomains?: string[]
}

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
  capabilityProfile?: AgentCapabilityProfile
}

export interface AgentDefSavePayload {
  id?: string
  name: string
  description?: string
  systemPrompt?: string
  modelRef: string
  status?: 'active' | 'disabled'
  skillIds?: string[]
  capabilityProfile?: AgentCapabilityProfile
}

export interface AgentSession {
  id: string
  agentId: string
  agentName?: string | null
  title?: string | null
  status: string
  updateTime?: string | null
}

/** Aligns with Nuxt UI Chat / AI SDK UIMessage parts (subset). */
export type AgentMessagePart =
  | { type: 'text'; text: string; state?: 'streaming' | 'done' }
  | { type: 'reasoning'; text: string; state?: 'streaming' | 'done' }
  | {
      type: 'tool'
      toolName: string
      state: 'running' | 'done'
      input?: string
      output?: string
    }

export interface AgentSessionMessage {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  seq: number
  parts: AgentMessagePart[]
}

export function ensureMessageParts(message: Omit<AgentSessionMessage, 'parts'> & { parts?: AgentMessagePart[] }): AgentMessagePart[] {
  if (Array.isArray(message.parts) && message.parts.length > 0) {
    return message.parts
  }
  const text = message.content ?? ''
  return text ? [{ type: 'text', text, state: 'done' }] : []
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

type RawMessage = Omit<AgentSessionMessage, 'id' | 'role' | 'parts'> & {
  id?: string | number
  role?: string
  parts?: AgentMessagePart[]
}

function normalizeDef(row: RawAgentDef): AgentDef {
  const status = row.status === 'disabled' ? 'disabled' : 'active'
  const profile = row.capabilityProfile
  return {
    id: String(row.id),
    name: row.name,
    description: row.description ?? null,
    systemPrompt: row.systemPrompt ?? '',
    modelRef: row.modelRef,
    status,
    ownerUserId: row.ownerUserId != null ? String(row.ownerUserId) : undefined,
    visibility: row.visibility,
    skillIds: Array.isArray(row.skillIds) ? row.skillIds.map(String) : [],
    capabilityProfile: {
      hotPacks: Array.isArray(profile?.hotPacks) ? profile.hotPacks.map(String) : [],
      deferredDomains: Array.isArray(profile?.deferredDomains) ? profile.deferredDomains.map(String) : []
    }
  }
}

function normalizeSession(row: RawSession): AgentSession {
  return {
    id: String(row.id),
    agentId: String(row.agentId),
    agentName: row.agentName ?? null,
    title: row.title ?? null,
    status: row.status,
    updateTime: row.updateTime ?? null
  }
}

function normalizeMessage(row: RawMessage): AgentSessionMessage {
  const role: AgentSessionMessage['role'] =
    row.role === 'assistant' || row.role === 'system' ? row.role : 'user'
  const content = row.content ?? ''
  const base: Omit<AgentSessionMessage, 'parts'> = {
    id: String(row.id),
    role,
    content,
    seq: Number(row.seq) || 0
  }
  return {
    ...base,
    parts: ensureMessageParts({ ...base, parts: row.parts })
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

export function switchAgentSession(sessionId: string, agentId: string): Promise<AgentSession> {
  return post<RawSession>('/app-api/agent/session/switch-agent', { sessionId, agentId }).then(normalizeSession)
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

/** SSE event shapes from POST /app-api/agent/session/send/stream (lane contract). */
export type AgentChatStreamEvent =
  | {
      type: 'message.user' | 'message.assistant'
      id: string
      role: string
      seq: number
      content: string
      parts?: AgentMessagePart[]
    }
  | { type: 'reasoning-delta'; text?: string }
  | { type: 'reasoning-end' }
  | { type: 'tool-start'; toolCallId?: string; toolName: string; input?: string }
  | { type: 'tool-result'; toolCallId?: string; toolName: string; output?: string }
  | { type: 'text-delta'; text?: string }
  | { type: 'done' }
  | { type: 'error'; message?: string; code?: number }

function apiBaseUrl(): string {
  return import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') ?? ''
}

function resolveApiUrl(path: string): string {
  const base = apiBaseUrl()
  if (!base) {
    return path
  }
  return `${base}${path.startsWith('/') ? path : `/${path}`}`
}

function parseSseDataPayload(raw: string): unknown {
  const trimmed = raw.trim()
  if (!trimmed) {
    return null
  }
  try {
    const once = JSON.parse(trimmed) as unknown
    // Spring may JSON-encode a string body → double-encoded object
    if (typeof once === 'string') {
      try {
        return JSON.parse(once) as unknown
      } catch {
        return once
      }
    }
    return once
  } catch {
    return null
  }
}

function coerceStreamEvent(payload: unknown): AgentChatStreamEvent | null {
  if (!payload || typeof payload !== 'object' || !('type' in payload)) {
    return null
  }
  const row = payload as Record<string, unknown>
  const type = String(row.type)
  if (type === 'message.user' || type === 'message.assistant') {
    return {
      type,
      id: String(row.id ?? ''),
      role: String(row.role ?? ''),
      seq: Number(row.seq) || 0,
      content: String(row.content ?? ''),
      parts: Array.isArray(row.parts) ? (row.parts as AgentMessagePart[]) : undefined
    }
  }
  if (type === 'reasoning-delta' || type === 'text-delta') {
    return { type, text: row.text != null ? String(row.text) : '' }
  }
  if (type === 'reasoning-end' || type === 'done') {
    return { type }
  }
  if (type === 'tool-start') {
    return {
      type,
      toolCallId: row.toolCallId != null ? String(row.toolCallId) : undefined,
      toolName: String(row.toolName ?? 'tool'),
      input: row.input != null ? String(row.input) : undefined
    }
  }
  if (type === 'tool-result') {
    return {
      type,
      toolCallId: row.toolCallId != null ? String(row.toolCallId) : undefined,
      toolName: String(row.toolName ?? 'tool'),
      output: row.output != null ? String(row.output) : ''
    }
  }
  if (type === 'error') {
    return {
      type,
      message: row.message != null ? String(row.message) : '流式发送失败',
      code: typeof row.code === 'number' ? row.code : undefined
    }
  }
  return null
}

/** Emit events from one SSE frame; tolerates glued `data:{...}data:{...}` (proxy chunking). */
function emitSseFrame(frame: string, onEvent: (event: AgentChatStreamEvent) => void) {
  const trimmed = frame.trim()
  if (!trimmed || trimmed.startsWith(':')) {
    return
  }
  const parts = trimmed.includes('data:')
    ? trimmed.split(/(?=data:)/).map(part => part.trim()).filter(Boolean)
    : [trimmed]
  for (const part of parts) {
    const raw = part.startsWith('data:') ? part.slice(5).trimStart() : part
    if (!raw) {
      continue
    }
    // Multi-line SSE data: join continuation lines that are not fields
    const payloadText = raw
      .split('\n')
      .filter(line => !/^(id|event|retry):/i.test(line))
      .map(line => (line.startsWith('data:') ? line.slice(5).trimStart() : line))
      .join('\n')
    const event = coerceStreamEvent(parseSseDataPayload(payloadText))
    if (event) {
      onEvent(event)
    }
  }
}

function consumeSseBuffer(buffer: string, onEvent: (event: AgentChatStreamEvent) => void): string {
  let rest = buffer.replace(/\r\n/g, '\n')
  let sep: number
  while ((sep = rest.indexOf('\n\n')) >= 0) {
    const frame = rest.slice(0, sep)
    rest = rest.slice(sep + 2)
    emitSseFrame(frame, onEvent)
  }
  return rest
}

function flushSseBuffer(buffer: string, onEvent: (event: AgentChatStreamEvent) => void) {
  const rest = consumeSseBuffer(buffer.endsWith('\n\n') ? buffer : `${buffer}\n\n`, onEvent)
  if (rest.trim()) {
    emitSseFrame(rest, onEvent)
  }
}

function isStreamTerminatedError(error: unknown): boolean {
  if (!error || typeof error !== 'object') {
    return false
  }
  const err = error as { name?: string, message?: string }
  if (err.name === 'AbortError') {
    return false
  }
  const message = String(err.message || '')
  return /terminated|network error|Failed to fetch|other side closed/i.test(message)
}

export async function sendAgentSessionMessageStream(
  sessionId: string,
  content: string,
  options: {
    onEvent: (event: AgentChatStreamEvent) => void
    signal?: AbortSignal
  }
): Promise<void> {
  const token = getAccessToken()
  const response = await fetch(resolveApiUrl('/app-api/agent/session/send/stream'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify({ sessionId, content }),
    signal: options.signal
  })

  if (!response.ok) {
    let message = `流式发送失败（HTTP ${response.status}）`
    try {
      const body = await response.json() as { msg?: string; message?: string }
      message = body.msg || body.message || message
    } catch {
      // ignore
    }
    throw new ApiError(response.status, message)
  }

  if (!response.body) {
    throw new ApiError(0, '流式响应为空')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        break
      }
      buffer += decoder.decode(value, { stream: true })
      buffer = consumeSseBuffer(buffer, options.onEvent)
    }
    buffer += decoder.decode()
    flushSseBuffer(buffer, options.onEvent)
  } catch (error) {
    buffer += decoder.decode()
    flushSseBuffer(buffer, options.onEvent)
    if (options.signal?.aborted || (error as Error)?.name === 'AbortError') {
      throw error
    }
    // Spring/SseEmitter often closes the socket right after `done`; treat as normal EOF
    // once we have flushed any buffered frames.
    if (isStreamTerminatedError(error)) {
      return
    }
    throw error
  }
}

export function listEnabledLlmModels(): Promise<EnabledLlmModel[]> {
  return get<EnabledLlmModel[]>('/app-api/agent/llm/model/list-enabled').then(rows => rows ?? [])
}

export type SkillTier = 'HOT' | 'DEFERRED'

export interface SkillCatalogItem {
  code: string
  name: string
  description: string
  inputSchema?: string
  /** Business domain, e.g. im / task / capability */
  domain: string
  tier: SkillTier
}

/** Known capability packs (aligned with backend CapabilityPackRegistry). */
export const KNOWN_CAPABILITY_PACKS = [
  { id: 'im-carrier', name: 'IM Carrier 热路径', description: 'IM 通道强制热路径：会话消息读' },
  { id: 'workspace-default', name: '工作台默认热路径', description: '工作台 Session 默认起点（当前可为空）' }
] as const

type RawSkillCatalogItem = Omit<SkillCatalogItem, 'tier' | 'domain'> & {
  domain?: string | null
  tier?: string | null
}

function normalizeSkillCatalogItem(row: RawSkillCatalogItem): SkillCatalogItem {
  const tier = row.tier === 'HOT' ? 'HOT' : 'DEFERRED'
  return {
    code: String(row.code ?? ''),
    name: row.name ?? '',
    description: row.description ?? '',
    inputSchema: row.inputSchema,
    domain: String(row.domain ?? '').trim() || 'unknown',
    tier
  }
}

export function listSkillCatalog(): Promise<SkillCatalogItem[]> {
  return get<RawSkillCatalogItem[]>('/app-api/agent/skill/catalog')
    .then(rows => (rows ?? []).map(normalizeSkillCatalogItem))
}

/** Group catalog items by domain (stable order: first-seen domains). */
export function groupSkillsByDomain(items: SkillCatalogItem[]): Array<{ domain: string, items: SkillCatalogItem[] }> {
  const order: string[] = []
  const map = new Map<string, SkillCatalogItem[]>()
  for (const item of items) {
    const domain = item.domain || 'unknown'
    if (!map.has(domain)) {
      order.push(domain)
      map.set(domain, [])
    }
    map.get(domain)!.push(item)
  }
  return order.map(domain => ({ domain, items: map.get(domain)! }))
}

export interface AgentAutomation {
  id: string
  agentId: string
  agentName?: string | null
  title: string
  prompt: string
  cronExpr: string
  status: 'active' | 'disabled'
  targetUserId?: string
  nextRunAt?: string | null
  lastRunAt?: string | null
  lastStatus?: string | null
  lastError?: string | null
}

export interface AgentAutomationSavePayload {
  id?: string
  title: string
  prompt: string
  agentId: string
  cronExpr: string
  status?: 'active' | 'disabled'
  targetUserId?: string
}

type RawAutomation = Omit<AgentAutomation, 'id' | 'agentId' | 'targetUserId' | 'status'> & {
  id?: string | number
  agentId?: string | number
  targetUserId?: string | number
  status?: string
}

function normalizeAutomation(row: RawAutomation): AgentAutomation {
  return {
    id: String(row.id),
    agentId: String(row.agentId),
    agentName: row.agentName ?? null,
    title: row.title,
    prompt: row.prompt,
    cronExpr: row.cronExpr,
    status: row.status === 'disabled' ? 'disabled' : 'active',
    targetUserId: row.targetUserId != null ? String(row.targetUserId) : undefined,
    nextRunAt: row.nextRunAt ?? null,
    lastRunAt: row.lastRunAt ?? null,
    lastStatus: row.lastStatus ?? null,
    lastError: row.lastError ?? null
  }
}

export function listAgentAutomations(): Promise<AgentAutomation[]> {
  return get<RawAutomation[]>('/app-api/agent/automation/list')
    .then(rows => (rows ?? []).map(normalizeAutomation))
}

export function createAgentAutomation(data: AgentAutomationSavePayload): Promise<string> {
  return post<string | number>('/app-api/agent/automation/create', data).then(id => String(id))
}

export function updateAgentAutomation(data: AgentAutomationSavePayload): Promise<boolean> {
  return put<boolean>('/app-api/agent/automation/update', data)
}

export function deleteAgentAutomation(id: string): Promise<boolean> {
  return del<boolean>('/app-api/agent/automation/delete', { params: { id } })
}

export function runAgentAutomationNow(id: string): Promise<boolean> {
  return post<boolean>('/app-api/agent/automation/run-now', null, { params: { id } })
}
