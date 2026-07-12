import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getConversationList,
  getMessageList,
  markConversationRead,
  sendMessage,
  type ConversationItem,
  type MessageContent,
  type MessageItem
} from '../api/app/im'
import { ApiError } from '../api/request'
import { useAuthStore } from './auth'

export type MessageViewItem = MessageItem & {
  localStatus?: 'sending' | 'sent' | 'failed'
}

export type PendingDirectChat = {
  peerUserId: string
  title: string
  avatarText?: string
}

function formatRelativeTime(iso?: string): string {
  if (!iso) {
    return ''
  }
  const date = new Date(iso)
  const diffMs = Date.now() - date.getTime()
  const diffMin = Math.floor(diffMs / 60_000)
  if (diffMin < 1) {
    return '刚刚'
  }
  if (diffMin < 60) {
    return `${diffMin} 分钟前`
  }
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) {
    return `${diffHour} 小时前`
  }
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

function textFromContent(content: MessageContent): string {
  const textBlock = content.blocks.find(block => block.type === 'text')
  return textBlock?.text ?? ''
}

function normalizeIncomingMessage(raw: MessageItem): MessageViewItem {
  return {
    ...raw,
    id: String(raw.id),
    conversationId: String(raw.conversationId),
    senderId: String(raw.senderId),
    localStatus: 'sent'
  }
}

export const useImStore = defineStore('im', () => {
  const conversations = ref<ConversationItem[]>([])
  const messages = ref<MessageViewItem[]>([])
  const activeConversationId = ref<string>()
  const keyword = ref('')
  const loadingConversations = ref(false)
  const loadingMessages = ref(false)
  const sending = ref(false)
  const lastError = ref<string | null>(null)
  const pendingDirectChat = ref<PendingDirectChat | null>(null)

  const activeConversation = computed(() => {
    if (activeConversationId.value) {
      return conversations.value.find(item => item.id === activeConversationId.value)
    }
    if (pendingDirectChat.value) {
      return {
        id: `pending-${pendingDirectChat.value.peerUserId}`,
        type: 'direct' as const,
        title: pendingDirectChat.value.title,
        avatarText: pendingDirectChat.value.avatarText,
        peerUserId: pendingDirectChat.value.peerUserId,
        unreadCount: 0
      }
    }
    return undefined
  })

  const filteredConversations = computed(() => {
    const q = keyword.value.trim().toLowerCase()
    if (!q) {
      return conversations.value
    }
    return conversations.value.filter(item =>
      item.title.toLowerCase().includes(q) || (item.lastMsgPreview ?? '').toLowerCase().includes(q))
  })

  const totalUnreadCount = computed(() =>
    conversations.value.reduce((sum, item) => sum + (item.unreadCount > 0 ? item.unreadCount : 0), 0))

  function currentUserId(): string {
    const auth = useAuthStore()
    return auth.userId != null ? String(auth.userId) : ''
  }

  function resetForTenantSwitch() {
    conversations.value = []
    messages.value = []
    activeConversationId.value = undefined
    pendingDirectChat.value = null
    keyword.value = ''
    lastError.value = null
  }

  function clearLocalUnread(conversationId: string) {
    const item = conversations.value.find(conv => conv.id === conversationId)
    if (item) {
      item.unreadCount = 0
    }
  }

  function latestMessageSeq(): number {
    return messages.value.reduce((max, item) => Math.max(max, item.seq ?? 0), 0)
  }

  async function reportConversationRead(conversationId: string, readSeq: number) {
    if (readSeq <= 0) {
      clearLocalUnread(conversationId)
      return
    }
    clearLocalUnread(conversationId)
    try {
      await markConversationRead(conversationId, readSeq)
    } catch {
      // 已读上报失败不阻断 UI；下次拉列表会校正
    }
  }

  async function fetchConversations(search?: string) {
    if (search !== undefined) {
      keyword.value = search
    }
    loadingConversations.value = true
    lastError.value = null
    try {
      conversations.value = await getConversationList(keyword.value.trim() || undefined)
    } catch (error) {
      lastError.value = error instanceof ApiError ? error.message : '加载会话失败'
      throw error
    } finally {
      loadingConversations.value = false
    }
  }

  async function selectConversation(conversationId: string) {
    pendingDirectChat.value = null
    activeConversationId.value = conversationId
    await fetchMessages(conversationId)
    await reportConversationRead(conversationId, latestMessageSeq())
  }

  async function fetchMessages(conversationId: string, afterSeq = 0) {
    loadingMessages.value = true
    lastError.value = null
    try {
      const list = await getMessageList(conversationId, afterSeq)
      const normalized = list.map(item => ({ ...item, localStatus: 'sent' as const }))
      if (afterSeq === 0) {
        messages.value = normalized
      } else {
        messages.value = [...messages.value, ...normalized]
      }
    } catch (error) {
      lastError.value = error instanceof ApiError ? error.message : '加载消息失败'
      throw error
    } finally {
      loadingMessages.value = false
    }
  }

  function handleMessageNew(raw: MessageItem) {
    const message = normalizeIncomingMessage(raw)
    const preview = textFromContent(message.content)
    const conversationId = message.conversationId

    let conversation = conversations.value.find(item => item.id === conversationId)
    if (conversation) {
      conversation.lastMsgPreview = preview
      conversation.lastMsgAt = message.createTime
      if (activeConversationId.value !== conversationId) {
        conversation.unreadCount += 1
      }
    } else {
      void fetchConversations()
    }

    if (activeConversationId.value !== conversationId) {
      return
    }

    const exists = messages.value.some(item =>
      item.id === message.id
      || (item.clientMsgId && message.clientMsgId && item.clientMsgId === message.clientMsgId))
    if (exists) {
      return
    }

    messages.value = [...messages.value, message]

    if (activeConversationId.value === conversationId) {
      void reportConversationRead(conversationId, message.seq)
    }
  }

  function openDirectChat(peerUserId: string, meta?: { title?: string, avatarText?: string }) {
    const existing = conversations.value.find(item =>
      item.type === 'direct' && item.peerUserId === peerUserId)
    if (existing) {
      pendingDirectChat.value = null
      activeConversationId.value = existing.id
      clearLocalUnread(existing.id)
      void fetchMessages(existing.id).then(() => reportConversationRead(existing.id, latestMessageSeq()))
      return
    }

    activeConversationId.value = undefined
    messages.value = []
    pendingDirectChat.value = {
      peerUserId,
      title: meta?.title ?? meta?.avatarText ?? '会话',
      avatarText: meta?.avatarText
    }
  }

  async function sendText(text: string) {
    const trimmed = text.trim()
    const conversation = activeConversation.value
    if (!trimmed || !conversation) {
      return
    }

    const isPending = conversation.id.startsWith('pending-')
    const clientMsgId = crypto.randomUUID()
    const content: MessageContent = {
      version: 1,
      blocks: [{ type: 'text', text: trimmed }]
    }

    const optimistic: MessageViewItem = {
      id: `local-${clientMsgId}`,
      conversationId: conversation.id,
      senderId: currentUserId(),
      senderType: 'user',
      type: 'text',
      content,
      clientMsgId,
      seq: (messages.value.at(-1)?.seq ?? 0) + 1,
      createTime: new Date().toISOString(),
      localStatus: 'sending'
    }
    messages.value = [...messages.value, optimistic]
    if (!isPending) {
      conversation.lastMsgPreview = trimmed
      conversation.lastMsgAt = optimistic.createTime
    }

    sending.value = true
    try {
      const result = await sendMessage({
        conversationId: isPending ? undefined : conversation.id,
        peerUserId: conversation.peerUserId,
        clientMsgId,
        content
      })

      if (isPending) {
        pendingDirectChat.value = null
        activeConversationId.value = result.conversationId
        await fetchConversations()
      }

      messages.value = messages.value.map(item =>
        item.clientMsgId === clientMsgId
          ? {
              ...item,
              id: result.id,
              conversationId: result.conversationId,
              seq: result.seq,
              createTime: result.createTime,
              localStatus: 'sent'
            }
          : item)

      const updatedConversation = conversations.value.find(item => item.id === result.conversationId)
      if (updatedConversation) {
        updatedConversation.lastMsgPreview = trimmed
        updatedConversation.lastMsgAt = result.createTime
      }
    } catch (error) {
      messages.value = messages.value.map(item =>
        item.clientMsgId === clientMsgId ? { ...item, localStatus: 'failed' } : item)
      lastError.value = error instanceof ApiError ? error.message : '发送失败'
      throw error
    } finally {
      sending.value = false
    }
  }

  return {
    conversations,
    messages,
    activeConversationId,
    keyword,
    loadingConversations,
    loadingMessages,
    sending,
    lastError,
    activeConversation,
    filteredConversations,
    totalUnreadCount,
    pendingDirectChat,
    currentUserId,
    fetchConversations,
    selectConversation,
    openDirectChat,
    fetchMessages,
    handleMessageNew,
    sendText,
    resetForTenantSwitch,
    formatRelativeTime,
    textFromContent
  }
})
