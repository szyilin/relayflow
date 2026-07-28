import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  createAgentAutomation,
  createAgentDef,
  createAgentSession,
  deleteAgentAutomation,
  deleteAgentDef,
  listAgentAutomations,
  listAgentDefs,
  listAgentSessionMessages,
  listAgentSessions,
  listEnabledLlmModels,
  runAgentAutomationNow,
  sendAgentSessionMessage,
  updateAgentAutomation,
  updateAgentDef,
  type AgentAutomation,
  type AgentAutomationSavePayload,
  type AgentDef,
  type AgentDefSavePayload,
  type AgentSession,
  type AgentSessionMessage,
  type EnabledLlmModel
} from '../api/app/agent'
import { ApiError } from '../api/request'

export type AgentPanelMode = 'home' | 'agents' | 'history' | 'automations'

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
  const automationBusy = ref(false)
  const lastError = ref<string | null>(null)

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
        [sessionId]: messages
      }
    } catch (error) {
      lastError.value = errorMessage(error, '加载会话消息失败')
      throw error
    } finally {
      loadingMessages.value = false
    }
  }

  async function sendMessage(content: string) {
    const text = content.trim()
    if (!text) {
      return
    }
    let sessionId = activeSessionId.value
    if (!sessionId) {
      sessionId = await startNewTask()
    }
    if (!sessionId) {
      throw new Error('无法创建会话')
    }

    sending.value = true
    lastError.value = null
    try {
      const result = await sendAgentSessionMessage(sessionId, text)
      const prev = messagesBySession.value[sessionId] ?? []
      messagesBySession.value = {
        ...messagesBySession.value,
        [sessionId]: [...prev, result.userMessage, result.assistantMessage]
      }
      draft.value = ''
      await refreshSessions()
    } catch (error) {
      lastError.value = errorMessage(error, '发送失败')
      throw error
    } finally {
      sending.value = false
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
    saveAutomation,
    removeAutomation,
    runAutomationNow,
    refreshAutomations
  }
})
