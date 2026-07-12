import type {
  AddGroupMembersPayload,
  ConversationItem,
  CreateGroupPayload,
  CreateGroupResult,
  GroupMemberItem,
  MessageItem,
  SendMessagePayload,
  SendMessageResult
} from '../api/app/im'

export interface MockUserContext {
  userId: string
  nickname: string
  avatarText: string
}

export interface MockMemberMeta {
  userId: string
  nickname: string
  avatarText: string
}

const MOCK_CURRENT_USER_ID = '100'
const MOCK_CURRENT_USER: MockUserContext = {
  userId: MOCK_CURRENT_USER_ID,
  nickname: '当前用户',
  avatarText: '当'
}

const mockConversations: ConversationItem[] = [
  {
    id: '201',
    type: 'direct',
    title: '李晓明',
    avatarText: '李',
    lastMsgPreview: '下午的评审会别忘了带原型',
    lastMsgAt: new Date(Date.now() - 15 * 60_000).toISOString(),
    unreadCount: 2,
    peerUserId: '102'
  },
  {
    id: '202',
    type: 'direct',
    title: '王芳',
    avatarText: '王',
    lastMsgPreview: '文档我已经更新到最新版了',
    lastMsgAt: new Date(Date.now() - 2 * 3600_000).toISOString(),
    unreadCount: 0,
    peerUserId: '103'
  },
  {
    id: '401',
    type: 'group',
    title: '产品讨论组',
    avatarText: '产',
    memberCount: 3,
    lastMsgPreview: '欢迎加入产品讨论组',
    lastMsgAt: new Date(Date.now() - 45 * 60_000).toISOString(),
    unreadCount: 0
  }
]

const mockMessages: Record<string, MessageItem[]> = {
  '201': [
    {
      id: '301',
      conversationId: '201',
      senderId: '102',
      senderNickname: '李晓明',
      senderType: 'user',
      type: 'text',
      content: { version: 1, blocks: [{ type: 'text', text: '你好，下午的评审会别忘了带原型。' }] },
      seq: 1,
      createTime: new Date(Date.now() - 20 * 60_000).toISOString()
    },
    {
      id: '302',
      conversationId: '201',
      senderId: MOCK_CURRENT_USER_ID,
      senderType: 'user',
      type: 'text',
      content: { version: 1, blocks: [{ type: 'text', text: '收到，我会准时参加。' }] },
      seq: 2,
      createTime: new Date(Date.now() - 18 * 60_000).toISOString()
    },
    {
      id: '303',
      conversationId: '201',
      senderId: '102',
      senderNickname: '李晓明',
      senderType: 'user',
      type: 'text',
      content: { version: 1, blocks: [{ type: 'text', text: '下午的评审会别忘了带原型' }] },
      seq: 3,
      createTime: new Date(Date.now() - 15 * 60_000).toISOString()
    }
  ],
  '202': [
    {
      id: '304',
      conversationId: '202',
      senderId: '103',
      senderNickname: '王芳',
      senderType: 'user',
      type: 'text',
      content: { version: 1, blocks: [{ type: 'text', text: '文档我已经更新到最新版了' }] },
      seq: 1,
      createTime: new Date(Date.now() - 2 * 3600_000).toISOString()
    }
  ],
  '401': [
    {
      id: '501',
      conversationId: '401',
      senderId: '0',
      senderType: 'system',
      type: 'system',
      content: { version: 1, blocks: [{ type: 'text', text: '李晓明 加入了群聊' }] },
      seq: 1,
      createTime: new Date(Date.now() - 50 * 60_000).toISOString()
    },
    {
      id: '502',
      conversationId: '401',
      senderId: '0',
      senderType: 'system',
      type: 'system',
      content: { version: 1, blocks: [{ type: 'text', text: '王芳 加入了群聊' }] },
      seq: 2,
      createTime: new Date(Date.now() - 48 * 60_000).toISOString()
    },
    {
      id: '503',
      conversationId: '401',
      senderId: MOCK_CURRENT_USER_ID,
      senderType: 'user',
      type: 'text',
      content: { version: 1, blocks: [{ type: 'text', text: '欢迎加入产品讨论组' }] },
      seq: 3,
      createTime: new Date(Date.now() - 45 * 60_000).toISOString()
    }
  ]
}

const mockGroupMembers: Record<string, GroupMemberItem[]> = {
  '401': [
    {
      userId: MOCK_CURRENT_USER_ID,
      nickname: '当前用户',
      avatarText: '当',
      role: 'owner'
    },
    {
      userId: '102',
      nickname: '李晓明',
      avatarText: '李',
      role: 'member'
    },
    {
      userId: '103',
      nickname: '王芳',
      avatarText: '王',
      role: 'member'
    }
  ]
}

let mockMessageIdSeq = 600
let mockConversationIdSeq = 500
let mockGroupIdSeq = 800

function cloneConversations(): ConversationItem[] {
  return mockConversations.map(item => ({ ...item }))
}

function cloneMessages(conversationId: string): MessageItem[] {
  return (mockMessages[conversationId] ?? []).map(item => ({
    ...item,
    content: { ...item.content, blocks: [...item.content.blocks] }
  }))
}

function cloneGroupMembers(conversationId: string): GroupMemberItem[] {
  return (mockGroupMembers[conversationId] ?? []).map(item => ({ ...item }))
}

function nextMessageId(): string {
  mockMessageIdSeq += 1
  return String(mockMessageIdSeq)
}

function nextConversationId(): string {
  mockConversationIdSeq += 1
  return String(mockConversationIdSeq)
}

function nextGroupId(): string {
  mockGroupIdSeq += 1
  return String(mockGroupIdSeq)
}

function systemMessage(conversationId: string, seq: number, text: string): MessageItem {
  return {
    id: nextMessageId(),
    conversationId,
    senderId: '0',
    senderType: 'system',
    type: 'system',
    content: { version: 1, blocks: [{ type: 'text', text }] },
    seq,
    createTime: new Date().toISOString()
  }
}

function updateConversationPreview(conversationId: string, preview: string, at?: string) {
  const conversation = mockConversations.find(item => item.id === conversationId)
  if (conversation) {
    conversation.lastMsgPreview = preview
    conversation.lastMsgAt = at ?? new Date().toISOString()
    conversation.unreadCount = 0
  }
}

function ensureGroupMembers(conversationId: string): GroupMemberItem[] {
  if (!mockGroupMembers[conversationId]) {
    mockGroupMembers[conversationId] = []
  }
  return mockGroupMembers[conversationId]
}

function ensureMessages(conversationId: string): MessageItem[] {
  if (!mockMessages[conversationId]) {
    mockMessages[conversationId] = []
  }
  return mockMessages[conversationId]
}

export function mockGetConversationList(keyword?: string): ConversationItem[] {
  const q = keyword?.trim().toLowerCase()
  const list = cloneConversations()
  if (!q) {
    return list
  }
  return list.filter(item =>
    item.title.toLowerCase().includes(q) || (item.lastMsgPreview ?? '').toLowerCase().includes(q))
}

export function mockGetGroupConversationList(): ConversationItem[] {
  return cloneConversations().filter(item => item.type === 'group')
}

export function mockGetMessageList(conversationId: string, afterSeq = 0): MessageItem[] {
  return cloneMessages(conversationId).filter(item => item.seq > afterSeq)
}

export function mockGetGroupMembers(conversationId: string): GroupMemberItem[] {
  return cloneGroupMembers(conversationId)
}

export function mockCreateGroup(
  payload: CreateGroupPayload,
  memberMeta: MockMemberMeta[],
  currentUser: MockUserContext = MOCK_CURRENT_USER
): CreateGroupResult {
  const conversationId = nextConversationId()
  const groupId = nextGroupId()
  const members = ensureGroupMembers(conversationId)
  const messages = ensureMessages(conversationId)

  members.push({
    userId: currentUser.userId,
    nickname: currentUser.nickname,
    avatarText: currentUser.avatarText,
    role: 'owner'
  })

  let seq = 0
  for (const member of memberMeta) {
    if (member.userId === currentUser.userId) {
      continue
    }
    members.push({
      userId: member.userId,
      nickname: member.nickname,
      avatarText: member.avatarText,
      role: 'member'
    })
    seq += 1
    messages.push(systemMessage(conversationId, seq, `${member.nickname} 加入了群聊`))
  }

  const preview = messages.at(-1)?.content.blocks[0]?.text ?? '新群聊已创建'
  mockConversations.unshift({
    id: conversationId,
    type: 'group',
    title: payload.name.trim(),
    avatarText: payload.name.trim().slice(0, 1) || '群',
    memberCount: members.length,
    lastMsgPreview: preview,
    lastMsgAt: new Date().toISOString(),
    unreadCount: 0
  })

  return { conversationId, groupId }
}

export function mockAddGroupMembers(
  payload: AddGroupMembersPayload,
  memberMeta: MockMemberMeta[]
): { addedCount: number } {
  const members = ensureGroupMembers(payload.conversationId)
  const messages = ensureMessages(payload.conversationId)
  const existingIds = new Set(members.map(item => item.userId))
  let addedCount = 0

  for (const member of memberMeta) {
    if (existingIds.has(member.userId)) {
      continue
    }
    members.push({
      userId: member.userId,
      nickname: member.nickname,
      avatarText: member.avatarText,
      role: 'member'
    })
    existingIds.add(member.userId)
    addedCount += 1
    const seq = (messages.at(-1)?.seq ?? 0) + 1
    const joinMessage = systemMessage(payload.conversationId, seq, `${member.nickname} 加入了群聊`)
    messages.push(joinMessage)
    updateConversationPreview(payload.conversationId, joinMessage.content.blocks[0]?.text ?? '成员变更')
  }

  const conversation = mockConversations.find(item => item.id === payload.conversationId)
  if (conversation) {
    conversation.memberCount = members.length
  }

  return { addedCount }
}

export function mockSendMessage(
  payload: SendMessagePayload,
  currentUser: MockUserContext = MOCK_CURRENT_USER
): SendMessageResult {
  const conversationId = payload.conversationId ?? '201'
  const list = ensureMessages(conversationId)
  const seq = (list.at(-1)?.seq ?? 0) + 1
  const now = new Date().toISOString()
  const message: MessageItem = {
    id: nextMessageId(),
    conversationId,
    senderId: currentUser.userId,
    senderNickname: currentUser.nickname,
    senderType: 'user',
    type: payload.type ?? 'text',
    content: payload.content,
    clientMsgId: payload.clientMsgId,
    seq,
    createTime: now
  }
  list.push(message)

  const preview = messagePreview(payload.type ?? 'text', payload.content)
  updateConversationPreview(conversationId, preview, now)

  return {
    id: message.id,
    conversationId,
    seq,
    clientMsgId: payload.clientMsgId,
    createTime: now
  }
}

export function mockCurrentUserId(): string {
  return MOCK_CURRENT_USER_ID
}

function messagePreview(type: string, content: SendMessagePayload['content']): string {
  if (type === 'image') {
    return '[图片]'
  }
  if (type === 'file') {
    const fileBlock = content.blocks.find(block => block.type === 'file')
    return fileBlock?.filename ? `[文件] ${fileBlock.filename}` : '[文件]'
  }
  return content.blocks.find(block => block.type === 'text')?.text ?? '[消息]'
}

export function mockSendFileMessage(
  payload: SendMessagePayload,
  previewUrl: string | undefined,
  currentUser: MockUserContext = MOCK_CURRENT_USER
): SendMessageResult {
  const enriched: SendMessagePayload = {
    ...payload,
    content: {
      ...payload.content,
      blocks: payload.content.blocks.map(block => ({
        ...block,
        downloadUrl: previewUrl ?? block.downloadUrl
      }))
    }
  }
  return mockSendMessage(enriched, currentUser)
}
