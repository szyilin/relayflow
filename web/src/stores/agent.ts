import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  createAgentAutomation,
  createAgentDef,
  createAgentSession,
  deleteAgentAutomation,
  deleteAgentDef,
  ensureMessageParts,
  listAgentAutomations,
  listAgentDefs,
  listAgentSessionMessages,
  listAgentSessions,
  listEnabledLlmModels,
  listSkillCatalog,
  runAgentAutomationNow,
  sendAgentSessionMessageStream,
  switchAgentSession,
  updateAgentAutomation,
  updateAgentDef,
  type AgentAutomation,
  type AgentAutomationSavePayload,
  type AgentChatStreamEvent,
  type AgentDef,
  type AgentDefSavePayload,
  type AgentMessagePart,
  type AgentSession,
  type AgentSessionMessage,
  type EnabledLlmModel,
  type SkillCatalogItem
} from '../api/app/agent'
import { ApiError } from '../api/request'

export type AgentPanelMode = 'home' | 'agents' | 'automations' | 'chat'

/** Matches Nuxt UI / AI SDK chat status. */
export type AgentStreamStatus = 'ready' | 'submitted' | 'streaming' | 'error'

let tempIdSeq = 0
function nextTempId(prefix: string) {
  tempIdSeq += 1
  return `tmp-${prefix}-${Date.now()}-${tempIdSeq}`
}

export const useAgentStore = defineStore('agent', () => {
  const agents = ref<AgentDef[]>([])
  const sessions = ref<AgentSession[]>([])
  const automations = ref<AgentAutomation[]>([])
  const messagesBySession = ref<Record<string, AgentSessionMessage[]>>({})
  const models = ref<EnabledLlmModel[]>([])
  const skillCatalog = ref<SkillCatalogItem[]>([])

  const panelMode = ref<AgentPanelMode>('home')
  const activeSessionId = ref<string | null>(null)
  const selectedAgentId = ref<string | null>(null)
  const draft = ref('')
  const loading = ref(false)
  const loadingMessages = ref(false)
  const sending = ref(false)
  const streamStatus = ref<AgentStreamStatus>('ready')
  const streamError = ref<Error | null>(null)
  const automationBusy = ref(false)
  const lastError = ref<string | null>(null)

  let streamAbort: AbortController | null = null

  const activeAgent = computed(() =>
    agents.value.find(a => a.id === selectedAgentId.value) ?? null
  )

  const activeSession = computed(() =>
    sessions.value.find(s => s.id === activeSessionId.value) ?? null
  )

  const activeMessages = computed(() => {
    if (!activeSessionId.value) {
      return [] as AgentSessionMessage[]
    }
    return messagesBySession.value[activeSessionId.value] ?? []
  })

  function errorMessage(error: unknown, fallback: string) {
    if (error instanceof ApiError) {
      return error.message || fallback
    }
    return error instanceof Error ? error.message : fallback
  }

  function patchSessionMessages(sessionId: string, next: AgentSessionMessage[]) {
    messagesBySession.value = {
      ...messagesBySession.value,
      [sessionId]: next
    }
  }

  function updateAssistantParts(sessionId: string, assistantId: string, updater: (parts: AgentMessagePart[]) => AgentMessagePart[]) {
    const list = messagesBySession.value[sessionId] ?? []
    const next = list.map((msg) => {
      if (msg.id !== assistantId) {
        return msg
      }
      const parts = updater(msg.parts.map(part => ({ ...part })))
      const textContent = parts
        .filter((p): p is Extract<AgentMessagePart, { type: 'text' }> => p.type === 'text')
        .map(p => p.text)
        .join('')
      return { ...msg, parts, content: textContent }
    })
    patchSessionMessages(sessionId, next)
  }

  function applyStreamEvent(sessionId: string, assistantId: string, event: AgentChatStreamEvent) {
    if (event.type === 'message.user' || event.type === 'message.assistant' || event.type === 'error') {
      return
    }
    updateAssistantParts(sessionId, assistantId, (parts) => {
      if (event.type === 'reasoning-delta') {
        const last = parts[parts.length - 1]
        if (last?.type === 'reasoning') {
          parts[parts.length - 1] = {
            ...last,
            text: last.text + (event.text ?? ''),
            state: 'streaming'
          }
          return parts
        }
        parts.push({ type: 'reasoning', text: event.text ?? '', state: 'streaming' })
        return parts
      }
      if (event.type === 'reasoning-end') {
        for (let i = parts.length - 1; i >= 0; i -= 1) {
          const part = parts[i]
          if (part?.type === 'reasoning') {
            parts[i] = { ...part, state: 'done' }
            break
          }
        }
        return parts
      }
      if (event.type === 'tool-start') {
        parts.push({
          type: 'tool',
          toolName: event.toolName,
          state: 'running',
          input: event.input
        })
        return parts
      }
      if (event.type === 'tool-result') {
        let toolIndex = -1
        for (let i = parts.length - 1; i >= 0; i -= 1) {
          const part = parts[i]
          if (part?.type === 'tool' && part.toolName === event.toolName && part.state === 'running') {
            toolIndex = i
            break
          }
        }
        if (toolIndex >= 0) {
          const tool = parts[toolIndex] as Extract<AgentMessagePart, { type: 'tool' }>
          parts[toolIndex] = {
            ...tool,
            state: 'done',
            output: event.output
          }
        } else {
          parts.push({
            type: 'tool',
            toolName: event.toolName,
            state: 'done',
            output: event.output
          })
        }
        return parts
      }
      if (event.type === 'text-delta') {
        const last = parts[parts.length - 1]
        if (last?.type === 'text') {
          parts[parts.length - 1] = {
            ...last,
            text: last.text + (event.text ?? ''),
            state: 'streaming'
          }
          return parts
        }
        parts.push({ type: 'text', text: event.text ?? '', state: 'streaming' })
        return parts
      }
      if (event.type === 'done') {
        return parts.map((part) => {
          if (part.type === 'text' || part.type === 'reasoning') {
            return { ...part, state: 'done' as const }
          }
          if (part.type === 'tool' && part.state === 'running') {
            return { ...part, state: 'done' as const }
          }
          return part
        })
      }
      return parts
    })
  }

  function replaceMessageById(sessionId: string, fromId: string, next: AgentSessionMessage) {
    const list = messagesBySession.value[sessionId] ?? []
    patchSessionMessages(
      sessionId,
      list.map(msg => (msg.id === fromId ? next : msg))
    )
  }

  async function refreshAgents() {
    agents.value = await listAgentDefs()
    if (selectedAgentId.value && !agents.value.some(a => a.id === selectedAgentId.value)) {
      selectedAgentId.value = agents.value[0]?.id ?? null
    } else if (!selectedAgentId.value && agents.value[0]) {
      selectedAgentId.value = agents.value[0].id
    }
  }

  async function refreshSessions() {
    sessions.value = await listAgentSessions()
  }

  async function refreshModels() {
    models.value = await listEnabledLlmModels()
  }

  async function refreshSkillCatalog() {
    skillCatalog.value = await listSkillCatalog()
  }

  async function refreshAutomations() {
    automations.value = await listAgentAutomations()
  }

  async function bootstrap() {
    loading.value = true
    lastError.value = null
    try {
      await Promise.all([
        refreshAgents(),
        refreshSessions(),
        refreshModels(),
        refreshAutomations(),
        refreshSkillCatalog()
      ])
    } catch (error) {
      lastError.value = errorMessage(error, '加载智能体失败，请确认后端已启动')
      throw error
    } finally {
      loading.value = false
    }
  }

  function openHome() {
    panelMode.value = 'home'
    activeSessionId.value = null
    draft.value = ''
  }

  function openAgents() {
    panelMode.value = 'agents'
    activeSessionId.value = null
  }

  function openHistory() {
    panelMode.value = 'home'
    activeSessionId.value = null
  }

  function openAutomations() {
    panelMode.value = 'automations'
    activeSessionId.value = null
  }

  function selectAgent(agentId: string) {
    selectedAgentId.value = agentId
  }

  async function saveAgent(payload: AgentDefSavePayload) {
    lastError.value = null
    try {
      if (payload.id) {
        await updateAgentDef(payload)
      } else {
        const id = await createAgentDef(payload)
        selectedAgentId.value = id
      }
      await refreshAgents()
      panelMode.value = 'agents'
    } catch (error) {
      lastError.value = errorMessage(error, '保存智能体失败')
      throw error
    }
  }

  async function removeAgent(id: string) {
    lastError.value = null
    try {
      await deleteAgentDef(id)
      if (selectedAgentId.value === id) {
        selectedAgentId.value = null
      }
      if (activeSession.value?.agentId === id) {
        activeSessionId.value = null
      }
      await Promise.all([refreshAgents(), refreshSessions()])
    } catch (error) {
      lastError.value = errorMessage(error, '删除智能体失败')
      throw error
    }
  }

  async function startNewTask(agentId?: string) {
    const aid = agentId ?? selectedAgentId.value
    if (!aid) {
      throw new Error('请先选择智能体')
    }
    // Abort any in-flight stream so send isn't blocked by a stuck `sending` flag.
    stopStreaming()
    streamError.value = null
    lastError.value = null
    try {
      const sessionId = await createAgentSession(aid)
      await refreshSessions()
      selectedAgentId.value = aid
      activeSessionId.value = sessionId
      messagesBySession.value = {
        ...messagesBySession.value,
        [sessionId]: []
      }
      panelMode.value = 'chat'
      draft.value = ''
      return sessionId
    } catch (error) {
      lastError.value = errorMessage(error, '创建会话失败')
      throw error
    }
  }

  async function openSession(sessionId: string) {
    stopStreaming()
    streamError.value = null
    activeSessionId.value = sessionId
    panelMode.value = 'chat'
    const session = sessions.value.find(s => s.id === sessionId)
    if (session) {
      selectedAgentId.value = session.agentId
    }
    loadingMessages.value = true
    lastError.value = null
    try {
      const messages = await listAgentSessionMessages(sessionId)
      messagesBySession.value = {
        ...messagesBySession.value,
        [sessionId]: messages.map(m => ({
          ...m,
          parts: ensureMessageParts(m)
        }))
      }
    } catch (error) {
      lastError.value = errorMessage(error, '加载会话消息失败')
      throw error
    } finally {
      loadingMessages.value = false
    }
  }

  function stopStreaming() {
    streamAbort?.abort()
    streamAbort = null
    sending.value = false
    if (streamStatus.value === 'streaming' || streamStatus.value === 'submitted') {
      streamStatus.value = 'ready'
    }
  }

  async function switchSessionAgent(agentId: string) {
    const sessionId = activeSessionId.value
    if (!sessionId) {
      selectedAgentId.value = agentId
      return
    }
    lastError.value = null
    try {
      const updated = await switchAgentSession(sessionId, agentId)
      selectedAgentId.value = agentId
      sessions.value = sessions.value.map(s => (s.id === sessionId ? { ...s, ...updated } : s))
    } catch (error) {
      lastError.value = errorMessage(error, '切换智能体失败')
      throw error
    }
  }

  async function sendMessage(content: string) {
    const text = content.trim()
    if (!text) {
      return
    }
    if (streamStatus.value === 'streaming' || streamStatus.value === 'submitted') {
      return
    }

    let sessionId = activeSessionId.value
    if (!sessionId) {
      sessionId = await startNewTask()
    }
    if (!sessionId) {
      throw new Error('无法创建会话')
    }

    streamAbort?.abort()
    streamAbort = new AbortController()
    const { signal } = streamAbort

    const userId = nextTempId('user')
    const assistantId = nextTempId('assistant')
    let liveUserId = userId
    let liveAssistantId = assistantId
    const prev = messagesBySession.value[sessionId] ?? []
    const userMessage: AgentSessionMessage = {
      id: userId,
      role: 'user',
      content: text,
      seq: prev.length + 1,
      parts: [{ type: 'text', text, state: 'done' }]
    }
    const assistantMessage: AgentSessionMessage = {
      id: assistantId,
      role: 'assistant',
      content: '',
      seq: prev.length + 2,
      // Empty parts → UChatMessages shows submitted indicator until first delta
      parts: []
    }
    patchSessionMessages(sessionId, [...prev, userMessage, assistantMessage])
    draft.value = ''
    sending.value = true
    streamStatus.value = 'submitted'
    streamError.value = null
    lastError.value = null

    let endedWithError = false
    try {
      await sendAgentSessionMessageStream(sessionId, text, {
        signal,
        onEvent: (event) => {
          if (signal.aborted) {
            return
          }
          // Keep Nuxt UI "submitted" dots until first content/tool/reasoning delta
          if (
            streamStatus.value === 'submitted'
            && (event.type === 'text-delta'
              || event.type === 'reasoning-delta'
              || event.type === 'tool-start')
          ) {
            streamStatus.value = 'streaming'
          }

          if (event.type === 'message.user') {
            const normalized = ensureMessageParts({
              id: event.id,
              role: 'user',
              content: event.content,
              seq: event.seq,
              parts: event.parts
            })
            replaceMessageById(sessionId!, liveUserId, {
              id: event.id,
              role: 'user',
              content: event.content,
              seq: event.seq,
              parts: normalized
            })
            liveUserId = event.id
            return
          }

          if (event.type === 'message.assistant') {
            const normalized = ensureMessageParts({
              id: event.id,
              role: 'assistant',
              content: event.content,
              seq: event.seq,
              parts: event.parts
            }).map((p) => {
              if (p.type === 'tool') {
                return { ...p, state: 'done' as const }
              }
              if (p.type === 'text' || p.type === 'reasoning') {
                return { ...p, state: 'done' as const }
              }
              return p
            })
            replaceMessageById(sessionId!, liveAssistantId, {
              id: event.id,
              role: 'assistant',
              content: event.content,
              seq: event.seq,
              parts: normalized
            })
            liveAssistantId = event.id
            return
          }

          if (event.type === 'error') {
            endedWithError = true
            streamStatus.value = 'error'
            streamError.value = new Error(event.message || '流式发送失败')
            lastError.value = event.message || '流式发送失败'
            return
          }

          if (event.type === 'done') {
            // Unlock send immediately; post-stream sync runs after the await below.
            sending.value = false
            streamStatus.value = 'ready'
            applyStreamEvent(sessionId!, liveAssistantId, event)
            return
          }

          applyStreamEvent(sessionId!, liveAssistantId, event)
        }
      })

      if (signal.aborted) {
        sending.value = false
        streamStatus.value = 'ready'
        return
      }
      if (!endedWithError) {
        sending.value = false
        streamStatus.value = 'ready'
      }
      // Re-sync from server so the final assistant message is always visible
      // even if some live deltas were dropped by proxy/chunk framing.
      try {
        const messages = await listAgentSessionMessages(sessionId)
        patchSessionMessages(
          sessionId,
          messages.map(m => ({
            ...m,
            parts: ensureMessageParts(m)
          }))
        )
      } catch {
        // keep local stream state
      }
      await refreshSessions()
    } catch (error) {
      if ((error as Error)?.name === 'AbortError') {
        sending.value = false
        streamStatus.value = 'ready'
        return
      }
      streamStatus.value = 'error'
      streamError.value = error instanceof Error ? error : new Error(String(error))
      lastError.value = errorMessage(error, '发送失败')
      // Still try to show persisted assistant reply after a transport error
      try {
        const messages = await listAgentSessionMessages(sessionId)
        if (messages.length) {
          patchSessionMessages(
            sessionId,
            messages.map(m => ({
              ...m,
              parts: ensureMessageParts(m)
            }))
          )
        }
      } catch {
        // ignore
      }
      throw error
    } finally {
      sending.value = false
      if (streamAbort?.signal === signal) {
        streamAbort = null
      }
    }
  }

  async function saveAutomation(payload: AgentAutomationSavePayload) {
    automationBusy.value = true
    lastError.value = null
    try {
      if (payload.id) {
        await updateAgentAutomation(payload)
      } else {
        await createAgentAutomation(payload)
      }
      await refreshAutomations()
      panelMode.value = 'automations'
    } catch (error) {
      lastError.value = errorMessage(error, '保存自动化失败')
      throw error
    } finally {
      automationBusy.value = false
    }
  }

  async function removeAutomation(id: string) {
    automationBusy.value = true
    lastError.value = null
    try {
      await deleteAgentAutomation(id)
      await refreshAutomations()
    } catch (error) {
      lastError.value = errorMessage(error, '删除自动化失败')
      throw error
    } finally {
      automationBusy.value = false
    }
  }

  async function runAutomationNow(id: string) {
    automationBusy.value = true
    lastError.value = null
    try {
      await runAgentAutomationNow(id)
      await refreshAutomations()
    } catch (error) {
      lastError.value = errorMessage(error, '执行失败')
      throw error
    } finally {
      automationBusy.value = false
    }
  }

  return {
    agents,
    sessions,
    automations,
    models,
    skillCatalog,
    panelMode,
    activeSessionId,
    selectedAgentId,
    draft,
    loading,
    loadingMessages,
    sending,
    streamStatus,
    streamError,
    automationBusy,
    lastError,
    activeAgent,
    activeSession,
    activeMessages,
    bootstrap,
    openHome,
    openAgents,
    openHistory,
    openAutomations,
    selectAgent,
    saveAgent,
    removeAgent,
    startNewTask,
    openSession,
    sendMessage,
    switchSessionAgent,
    stopStreaming,
    saveAutomation,
    removeAutomation,
    runAutomationNow,
    refreshAutomations
  }
})
