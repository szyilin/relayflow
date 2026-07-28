import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type {
  AgentDef,
  AgentDefSavePayload,
  AgentSession,
  AgentSessionMessage,
  EnabledLlmModel
} from '../api/app/agent'

/**
 * workspace-agent-web：临时 Mock 撑 UI。
 * integrate 切片改为调用 `api/app/agent.ts` 真接口并删除本文件内 seed / mock 分支。
 */
let mockSeq = 1000
function nextId(prefix: string) {
  mockSeq += 1
  return `${prefix}_${mockSeq}`
}

function seedAgents(): AgentDef[] {
  return [
    {
      id: 'agent_demo_partner',
      name: '智能伙伴',
      description: '通用办公助手，帮你起草与梳理日常工作',
      systemPrompt: '你是友好的企业办公助手，回答简洁、可执行。',
      modelRef: 'demo-gpt',
      status: 'active',
      visibility: 'private'
    },
    {
      id: 'agent_demo_analyst',
      name: '数据分析师',
      description: '把原始数据变成易懂结论（演示）',
      systemPrompt: '你是数据分析师，擅长总结与图表建议。',
      modelRef: 'demo-gpt',
      status: 'active',
      visibility: 'private'
    }
  ]
}

function seedModels(): EnabledLlmModel[] {
  return [
    {
      code: 'demo-gpt',
      name: '演示模型',
      modelName: 'gpt-4o-mini',
      providerType: 'openai_compatible'
    }
  ]
}

export type AgentPanelMode = 'home' | 'agents' | 'history'

export const useAgentStore = defineStore('agent', () => {
  const agents = ref<AgentDef[]>(seedAgents())
  const sessions = ref<AgentSession[]>([])
  const messagesBySession = ref<Record<string, AgentSessionMessage[]>>({})
  const models = ref<EnabledLlmModel[]>(seedModels())

  const panelMode = ref<AgentPanelMode>('home')
  const activeSessionId = ref<string | null>(null)
  const selectedAgentId = ref<string | null>(agents.value[0]?.id ?? null)
  const draft = ref('')
  const loading = ref(false)
  const sending = ref(false)
  const lastError = ref<string | null>(null)

  /** -web 固定 true；integrate 改为 false 并走真实 API */
  const useMock = ref(true)

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

  async function bootstrap() {
    loading.value = true
    lastError.value = null
    try {
      if (useMock.value) {
        if (!selectedAgentId.value && agents.value[0]) {
          selectedAgentId.value = agents.value[0].id
        }
        return
      }
      // integrate: fetch listAgentDefs / listAgentSessions / listEnabledLlmModels
    } catch (error) {
      lastError.value = error instanceof Error ? error.message : '加载失败'
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

  function selectAgent(agentId: string) {
    selectedAgentId.value = agentId
  }

  async function saveAgent(payload: AgentDefSavePayload) {
    if (useMock.value) {
      if (payload.id) {
        const idx = agents.value.findIndex(a => a.id === payload.id)
        if (idx >= 0) {
          agents.value[idx] = {
            ...agents.value[idx],
            name: payload.name,
            description: payload.description ?? null,
            systemPrompt: payload.systemPrompt ?? '',
            modelRef: payload.modelRef,
            status: payload.status ?? 'active'
          }
        }
      } else {
        const created: AgentDef = {
          id: nextId('agent'),
          name: payload.name,
          description: payload.description ?? null,
          systemPrompt: payload.systemPrompt ?? '',
          modelRef: payload.modelRef,
          status: payload.status ?? 'active',
          visibility: 'private'
        }
        agents.value = [created, ...agents.value]
        selectedAgentId.value = created.id
      }
      return
    }
    // integrate: create/update API
  }

  async function removeAgent(id: string) {
    if (useMock.value) {
      agents.value = agents.value.filter(a => a.id !== id)
      if (selectedAgentId.value === id) {
        selectedAgentId.value = agents.value[0]?.id ?? null
      }
      sessions.value = sessions.value.filter(s => s.agentId !== id)
      return
    }
  }

  async function startNewTask(agentId?: string) {
    const aid = agentId ?? selectedAgentId.value
    if (!aid) {
      throw new Error('请先选择智能体')
    }
    const agent = agents.value.find(a => a.id === aid)
    if (!agent) {
      throw new Error('智能体不存在')
    }
    if (useMock.value) {
      const session: AgentSession = {
        id: nextId('session'),
        agentId: aid,
        agentName: agent.name,
        title: null,
        status: 'active'
      }
      sessions.value = [session, ...sessions.value]
      messagesBySession.value = {
        ...messagesBySession.value,
        [session.id]: []
      }
      selectedAgentId.value = aid
      activeSessionId.value = session.id
      panelMode.value = 'history'
      draft.value = ''
      return session.id
    }
    return ''
  }

  async function openSession(sessionId: string) {
    activeSessionId.value = sessionId
    panelMode.value = 'history'
    const session = sessions.value.find(s => s.id === sessionId)
    if (session) {
      selectedAgentId.value = session.agentId
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
    try {
      if (useMock.value) {
        const list = [...(messagesBySession.value[sessionId] ?? [])]
        const nextSeq = list.length === 0 ? 1 : list[list.length - 1].seq + 1
        const userMsg: AgentSessionMessage = {
          id: nextId('msg'),
          role: 'user',
          content: text,
          seq: nextSeq
        }
        const assistantMsg: AgentSessionMessage = {
          id: nextId('msg'),
          role: 'assistant',
          content: `（演示回复）已收到：「${text}」。接入真实模型后，将由 Spring AI Runtime 生成回答。`,
          seq: nextSeq + 1
        }
        messagesBySession.value = {
          ...messagesBySession.value,
          [sessionId]: [...list, userMsg, assistantMsg]
        }
        const session = sessions.value.find(s => s.id === sessionId)
        if (session && !session.title) {
          session.title = text.length > 40 ? `${text.slice(0, 40)}…` : text
          sessions.value = [...sessions.value]
        }
        draft.value = ''
        return
      }
    } finally {
      sending.value = false
    }
  }

  return {
    agents,
    sessions,
    models,
    panelMode,
    activeSessionId,
    selectedAgentId,
    draft,
    loading,
    sending,
    lastError,
    useMock,
    activeAgent,
    activeSession,
    activeMessages,
    bootstrap,
    openHome,
    openAgents,
    openHistory,
    selectAgent,
    saveAgent,
    removeAgent,
    startNewTask,
    openSession,
    sendMessage
  }
})
