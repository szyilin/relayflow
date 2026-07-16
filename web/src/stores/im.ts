import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  addGroupBot as addGroupBotRequest,
  addGroupMembers,
  createGroup as createGroupRequest,
  getConversationList,
  getGroupBotCatalog,
  getGroupMembers,
  getMessageList,
  getReadStatus,
  markConversationRead,
  removeGroupBot as removeGroupBotRequest,
  sendMessage,
  type ConversationItem,
  type GroupBotCatalogItem,
  type GroupMemberItem,
  type MessageContent,
  type MessageItem
} from '../api/app/im'
import { ApiError } from '../api/request'
import { uploadPrivateFile } from '../api/app/file'
import { useAuthStore } from './auth'

export type MessageViewItem = MessageItem & {
  localStatus?: 'sending' | 'sent' | 'failed'
}

export type PendingDirectChat = {
  peerUserId: string
  title: string
  avatarText?: string
}

export type GroupMemberCandidate = {
  userId: string
  nickname: string
  avatarText: string
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
  return content.blocks
    .filter(block => (block.type === 'text' || block.type === 'mention') && block.text)
    .map(block => block.text!.trim())
    .filter(Boolean)
    .join(' ')
}

function fileBlockFromContent(content: MessageContent) {
  return content.blocks.find(block => block.type === 'file')
}

function isImageMessage(message: Pick<MessageItem, 'type' | 'content'>): boolean {
  if (message.type === 'image') {
    return true
  }
  const fileBlock = fileBlockFromContent(message.content)
  return fileBlock?.mimeType?.startsWith('image/') ?? false
}

function isFileMessage(message: Pick<MessageItem, 'type' | 'content'>): boolean {
  return message.type === 'file' || message.type === 'image'
    || fileBlockFromContent(message.content) != null
}

function messagePreviewFromContent(type: MessageItem['type'], content: MessageContent): string {
  if (type === 'image') {
    return '[图片]'
  }
  if (type === 'file') {
    const fileBlock = fileBlockFromContent(content)
    return fileBlock?.filename ? `[文件] ${fileBlock.filename}` : '[文件]'
  }
  return textFromContent(content)
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
  const groupMembers = ref<GroupMemberItem[]>([])
  const activeConversationId = ref<string>()
  const keyword = ref('')
  const loadingConversations = ref(false)
  const loadingMessages = ref(false)
  const loadingGroupMembers = ref(false)
  const sending = ref(false)
  const lastError = ref<string | null>(null)
  const pendingDirectChat = ref<PendingDirectChat | null>(null)
  const peerReadSeq = ref(0)

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

  function isSystemMessage(message: Pick<MessageItem, 'senderType' | 'type'>): boolean {
    return message.senderType === 'system' || message.type === 'system'
  }

  function resetForTenantSwitch() {
    conversations.value = []
    messages.value = []
    groupMembers.value = []
    activeConversationId.value = undefined
    pendingDirectChat.value = null
    keyword.value = ''
    lastError.value = null
    peerReadSeq.value = 0
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

  async function fetchReadStatus(conversationId: string) {
    try {
      const status = await getReadStatus(conversationId)
      const me = currentUserId()
      const peer = status.members.find(member => member.userId !== me)
      peerReadSeq.value = peer?.readSeq ?? 0
    } catch {
      peerReadSeq.value = 0
    }
  }

  function handleReadUpdated(conversationId: string, userId: string, readSeq: number) {
    const conversation = conversations.value.find(item => item.id === conversationId)
    if (!conversation || conversation.type !== 'direct') {
      return
    }
    if (conversation.peerUserId !== userId) {
      return
    }
    peerReadSeq.value = Math.max(peerReadSeq.value, readSeq)
  }

  function isMessageReadByPeer(seq: number): boolean {
    const conversation = activeConversation.value
    if (!conversation || conversation.type !== 'direct') {
      return false
    }
    return seq > 0 && seq <= peerReadSeq.value
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

  async function fetchGroupMembers(conversationId: string) {
    loadingGroupMembers.value = true
    lastError.value = null
    try {
      groupMembers.value = await getGroupMembers(conversationId)
    } catch (error) {
      groupMembers.value = []
      lastError.value = error instanceof ApiError ? error.message : '加载群成员失败'
      throw error
    } finally {
      loadingGroupMembers.value = false
    }
  }

  async function selectConversation(conversationId: string) {
    pendingDirectChat.value = null
    activeConversationId.value = conversationId
    const conversation = conversations.value.find(item => item.id === conversationId)
    if (conversation?.type === 'group') {
      await Promise.all([
        fetchMessages(conversationId),
        fetchGroupMembers(conversationId)
      ])
    } else {
      groupMembers.value = []
      await fetchMessages(conversationId)
    }
    await reportConversationRead(conversationId, latestMessageSeq())
    if (conversation?.type === 'direct') {
      await fetchReadStatus(conversationId)
    } else {
      peerReadSeq.value = 0
    }
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
    const preview = messagePreviewFromContent(message.type, message.content)
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

  async function createGroup(name: string, members: GroupMemberCandidate[]) {
    const trimmed = name.trim()
    if (!trimmed || members.length === 0) {
      throw new Error('请填写群名称并至少选择一名成员')
    }

    const result = await createGroupRequest({
      name: trimmed,
      memberUserIds: members.map(item => item.userId)
    })

    await fetchConversations()
    await selectConversation(result.conversationId)
    return result
  }

  async function inviteGroupMembers(conversationId: string, members: GroupMemberCandidate[]) {
    if (members.length === 0) {
      return { addedCount: 0 }
    }

    const result = await addGroupMembers({
      conversationId,
      memberUserIds: members.map(item => item.userId)
    })

    await Promise.all([
      fetchGroupMembers(conversationId),
      fetchMessages(conversationId),
      fetchConversations()
    ])

    const conversation = conversations.value.find(item => item.id === conversationId)
    if (conversation?.type === 'group') {
      conversation.memberCount = groupMembers.value.length
    }

    return result
  }

  async function fetchGroupBotCatalog(conversationId: string): Promise<GroupBotCatalogItem[]> {
    return getGroupBotCatalog(conversationId)
  }

  async function addGroupBot(conversationId: string, botCode: string) {
    const result = await addGroupBotRequest({ conversationId, botCode })
    await Promise.all([
      fetchGroupMembers(conversationId),
      fetchMessages(conversationId),
      fetchConversations()
    ])
    const conversation = conversations.value.find(item => item.id === conversationId)
    if (conversation?.type === 'group') {
      conversation.memberCount = groupMembers.value.length
    }
    return result
  }

  async function removeGroupBot(conversationId: string, botCode: string) {
    const result = await removeGroupBotRequest({ conversationId, botCode })
    await Promise.all([
      fetchGroupMembers(conversationId),
      fetchMessages(conversationId),
      fetchConversations()
    ])
    const conversation = conversations.value.find(item => item.id === conversationId)
    if (conversation?.type === 'group') {
      conversation.memberCount = groupMembers.value.length
    }
    return result
  }

  function isCurrentUserGroupOwner(): boolean {
    const me = currentUserId()
    return groupMembers.value.some(member =>
      member.subjectType === 'user'
      && member.userId === me
      && member.role === 'owner')
  }

  function openDirectChat(peerUserId: string, meta?: { title?: string, avatarText?: string }) {
    const existing = conversations.value.find(item =>
      item.type === 'direct' && item.peerUserId === peerUserId)
    if (existing) {
      pendingDirectChat.value = null
      activeConversationId.value = existing.id
      clearLocalUnread(existing.id)
      void fetchMessages(existing.id).then(async () => {
        await reportConversationRead(existing.id, latestMessageSeq())
        await fetchReadStatus(existing.id)
      })
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

  async function sendText(text: string, mentions: GroupMemberItem[] = []) {
    const trimmed = text.trim()
    const conversation = activeConversation.value
    if ((!trimmed && mentions.length === 0) || !conversation) {
      return
    }

    const isPending = conversation.id.startsWith('pending-')
    const clientMsgId = crypto.randomUUID()
    const mentionBlocks = mentions
      .filter(member => member.subjectType === 'bot' && member.botCode)
      .map(member => ({
        type: 'mention' as const,
        subjectType: 'bot' as const,
        subjectId: member.botId,
        botCode: member.botCode,
        text: `@${member.nickname}`
      }))
    const content: MessageContent = {
      version: 1,
      blocks: [
        ...mentionBlocks,
        ...(trimmed ? [{ type: 'text' as const, text: trimmed }] : [{ type: 'text' as const, text: ' ' }])
      ]
    }
    const preview = textFromContent(content)

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
      conversation.lastMsgPreview = preview
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
        updatedConversation.lastMsgPreview = preview
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

  async function sendFileMessage(file: File) {
    const conversation = activeConversation.value
    if (!conversation || sending.value) {
      return
    }

    const isPending = conversation.id.startsWith('pending-')
    const clientMsgId = crypto.randomUUID()
    const isImage = file.type.startsWith('image/')
    const messageType = isImage ? 'image' : 'file'
    const previewUrl = URL.createObjectURL(file)

    const content: MessageContent = {
      version: 1,
      blocks: [{
        type: 'file',
        fileId: `local-${clientMsgId}`,
        filename: file.name,
        mimeType: file.type || 'application/octet-stream',
        size: file.size,
        downloadUrl: previewUrl
      }]
    }

    const optimistic: MessageViewItem = {
      id: `local-${clientMsgId}`,
      conversationId: conversation.id,
      senderId: currentUserId(),
      senderType: 'user',
      type: messageType,
      content,
      clientMsgId,
      seq: (messages.value.at(-1)?.seq ?? 0) + 1,
      createTime: new Date().toISOString(),
      localStatus: 'sending'
    }
    messages.value = [...messages.value, optimistic]
    const previewText = messagePreviewFromContent(messageType, content)
    if (!isPending) {
      conversation.lastMsgPreview = previewText
      conversation.lastMsgAt = optimistic.createTime
    }

    sending.value = true
    try {
      const fileId = await uploadPrivateFile(file)
      const sendPayload = {
        conversationId: isPending ? undefined : conversation.id,
        peerUserId: conversation.peerUserId,
        clientMsgId,
        type: messageType as 'image' | 'file',
        content: {
          version: 1 as const,
          blocks: [{
            type: 'file' as const,
            fileId,
            filename: file.name,
            mimeType: file.type || 'application/octet-stream',
            size: file.size
          }]
        }
      }

      const result = await sendMessage(sendPayload)

      if (isPending) {
        pendingDirectChat.value = null
        activeConversationId.value = result.conversationId
        await fetchConversations()
      }

      const downloadUrl = `/app-api/infra/file/download?fileId=${fileId}`
      messages.value = messages.value.map(item =>
        item.clientMsgId === clientMsgId
          ? {
              ...item,
              id: result.id,
              conversationId: result.conversationId,
              content: {
                ...item.content,
                blocks: item.content.blocks.map(block => ({
                  ...block,
                  fileId,
                  downloadUrl
                }))
              },
              seq: result.seq,
              createTime: result.createTime,
              localStatus: 'sent'
            }
          : item)

      const updatedConversation = conversations.value.find(item => item.id === result.conversationId)
      if (updatedConversation) {
        updatedConversation.lastMsgPreview = previewText
        updatedConversation.lastMsgAt = result.createTime
      }
    } catch (error) {
      URL.revokeObjectURL(previewUrl)
      messages.value = messages.value.map(item =>
        item.clientMsgId === clientMsgId ? { ...item, localStatus: 'failed' } : item)
      lastError.value = error instanceof ApiError ? error.message : '发送附件失败'
      throw error
    } finally {
      sending.value = false
    }
  }

  return {
    conversations,
    messages,
    groupMembers,
    activeConversationId,
    keyword,
    loadingConversations,
    loadingMessages,
    loadingGroupMembers,
    sending,
    lastError,
    activeConversation,
    filteredConversations,
    totalUnreadCount,
    pendingDirectChat,
    peerReadSeq,
    currentUserId,
    fetchConversations,
    selectConversation,
    openDirectChat,
    fetchMessages,
    fetchGroupMembers,
    createGroup,
    inviteGroupMembers,
    fetchGroupBotCatalog,
    addGroupBot,
    removeGroupBot,
    isCurrentUserGroupOwner,
    handleMessageNew,
    handleReadUpdated,
    fetchReadStatus,
    isMessageReadByPeer,
    sendText,
    sendFileMessage,
    resetForTenantSwitch,
    formatRelativeTime,
    textFromContent,
    fileBlockFromContent,
    isImageMessage,
    isFileMessage,
    messagePreviewFromContent,
    isSystemMessage
  }
})
