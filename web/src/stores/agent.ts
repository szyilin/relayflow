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
  runAgentAutomationNow,
  sendAgentSessionMessageStream,
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
  type EnabledLlmModel
} from '../api/app/agent'
import { ApiError } from '../api/request'

export type AgentPanelMode = 'home' | 'agents' | 'history' | 'automations'

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
      const parts = updater([...msg.parts])
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
          last.text += event.text ?? ''
          last.state = 'streaming'
          return parts
        }
        parts.push({ type: 'reasoning', text: event.text ?? '', state: 'streaming' })
        return parts
      }
      if (event.type === 'reasoning-end') {
        for (let i = parts.length - 1; i >= 0; i -= 1) {
          const part = parts[i]
          if (part?.type === 'reasoning') {
            part.state = 'done'
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
        let tool: Extract<AgentMessagePart, { type: 'tool' }> | undefined
        for (let i = parts.length - 1; i >= 0; i -= 1) {
          const part = parts[i]
          if (part?.type === 'tool' && part.toolName === event.toolName && part.state === 'running') {
            tool = part
            break
          }
        }
        if (tool) {
          tool.state = 'done'
          tool.output = event.output
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
          last.text += event.text ?? ''
          last.state = 'streaming'
          return parts
        }
        parts.push({ type: 'text', text: event.text ?? '', state: 'streaming' })
        return parts
      }
      if (event.type === 'done') {
        for (const part of parts) {
          if (part.type === 'text' || part.type === 'reasoning') {
            part.state = 'done'
          }
          if (part.type === 'tool' && part.state === 'running') {
            part.state = 'done'
          }
        }
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

  async function refreshAutomations() {
    automations.value = await listAgentAutomations()
  }

  async function bootstrap() {
    loading.value = true
    lastError.value = null
    try {
      await Promise.all([refreshAgents(), refreshSessions(), refreshModels(), refreshAutomations()])
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
  }

  function openHistory() {
    panelMode.value = 'history'
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
      panelMode.value = 'history'
      draft.value = ''
      return sessionId
    } catch (error) {
      lastError.value = errorMessage(error, '创建会话失败')
      throw error
    }
  }

  async function openSession(sessionId: string) {
    activeSessionId.value = sessionId
    panelMode.value = 'history'
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
          if (streamStatus.value === 'submitted') {
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
            streamStatus.value = 'ready'
            return
          }

          applyStreamEvent(sessionId!, liveAssistantId, event)
        }
      })

      if (signal.aborted) {
        streamStatus.value = 'ready'
        return
      }
      if (!endedWithError) {
        streamStatus.value = 'ready'
      }
      await refreshSessions()
    } catch (error) {
      if ((error as Error)?.name === 'AbortError') {
        streamStatus.value = 'ready'
        return
      }
      streamStatus.value = 'error'
      streamError.value = error instanceof Error ? error : new Error(String(error))
      lastError.value = errorMessage(error, '发送失败')
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
    stopStreaming,
    saveAutomation,
    removeAutomation,
    runAutomationNow,
    refreshAutomations
  }
})
