import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getConversationList,
  getMessageList,
  sendMessage,
  type ConversationItem,
  type MessageContent,
  type MessageItem
} from '../api/app/im'
import { ApiError } from '../api/request'
import {
  mockCurrentUserId,
  mockGetConversationList,
  mockGetMessageList,
  mockSendMessage
} from '../mocks/im'
import { useAuthStore } from './auth'

export type MessageViewItem = MessageItem & {
  localStatus?: 'sending' | 'sent' | 'failed'
}

function isApiUnavailable(error: unknown): boolean {
  if (error instanceof ApiError) {
    return error.code === 0 || error.code === 404
  }
  return true
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

export const useImStore = defineStore('im', () => {
  const conversations = ref<ConversationItem[]>([])
  const messages = ref<MessageViewItem[]>([])
  const activeConversationId = ref<string>()
  const keyword = ref('')
  const loadingConversations = ref(false)
  const loadingMessages = ref(false)
  const sending = ref(false)
  const usingMock = ref(false)
  const lastError = ref<string | null>(null)

  const activeConversation = computed(() =>
    conversations.value.find(item => item.id === activeConversationId.value))

  const filteredConversations = computed(() => {
    const q = keyword.value.trim().toLowerCase()
    if (!q) {
      return conversations.value
    }
    return conversations.value.filter(item =>
      item.title.toLowerCase().includes(q) || (item.lastMsgPreview ?? '').toLowerCase().includes(q))
  })

  function currentUserId(): string {
    const auth = useAuthStore()
    return auth.userId != null ? String(auth.userId) : mockCurrentUserId()
  }

  async function fetchConversations(search?: string) {
    if (search !== undefined) {
      keyword.value = search
    }
    loadingConversations.value = true
    lastError.value = null
    try {
      conversations.value = await getConversationList(keyword.value.trim() || undefined)
      usingMock.value = false
    } catch (error) {
      if (!isApiUnavailable(error)) {
        lastError.value = error instanceof ApiError ? error.message : '加载会话失败'
        throw error
      }
      conversations.value = mockGetConversationList(keyword.value.trim() || undefined)
      usingMock.value = true
    } finally {
      loadingConversations.value = false
    }
  }

  async function selectConversation(conversationId: string) {
    activeConversationId.value = conversationId
    await fetchMessages(conversationId)

    const item = conversations.value.find(conv => conv.id === conversationId)
    if (item) {
      item.unreadCount = 0
    }
  }

  async function fetchMessages(conversationId: string, afterSeq = 0) {
    loadingMessages.value = true
    lastError.value = null
    try {
      const list = await getMessageList(conversationId, afterSeq)
      if (afterSeq === 0) {
        messages.value = list
      } else {
        messages.value = [...messages.value, ...list]
      }
      if (!usingMock.value) {
        usingMock.value = false
      }
    } catch (error) {
      if (!isApiUnavailable(error)) {
        lastError.value = error instanceof ApiError ? error.message : '加载消息失败'
        throw error
      }
      const list = mockGetMessageList(conversationId, afterSeq)
      if (afterSeq === 0) {
        messages.value = list
      } else {
        messages.value = [...messages.value, ...list]
      }
      usingMock.value = true
    } finally {
      loadingMessages.value = false
    }
  }

  async function sendText(text: string) {
    const trimmed = text.trim()
    const conversation = activeConversation.value
    if (!trimmed || !conversation) {
      return
    }

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
    conversation.lastMsgPreview = trimmed
    conversation.lastMsgAt = optimistic.createTime

    sending.value = true
    try {
      const result = usingMock.value
        ? mockSendMessage({
            conversationId: conversation.id,
            peerUserId: conversation.peerUserId,
            clientMsgId,
            content
          })
        : await sendMessage({
            conversationId: conversation.id,
            peerUserId: conversation.peerUserId,
            clientMsgId,
            content
          })

      messages.value = messages.value.map(item =>
        item.clientMsgId === clientMsgId
          ? {
              ...item,
              id: result.id,
              seq: result.seq,
              createTime: result.createTime,
              localStatus: 'sent'
            }
          : item)
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
    usingMock,
    lastError,
    activeConversation,
    filteredConversations,
    currentUserId,
    fetchConversations,
    selectConversation,
    fetchMessages,
    sendText,
    formatRelativeTime,
    textFromContent
  }
})
