import type { ConversationItem, MessageItem, SendMessagePayload, SendMessageResult } from '../api/app/im'

const MOCK_CURRENT_USER_ID = '100'

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
    id: '203',
    type: 'direct',
    title: '系统通知',
    avatarText: '系',
    lastMsgPreview: '你有一条待处理的审批',
    lastMsgAt: new Date(Date.now() - 24 * 3600_000).toISOString(),
    unreadCount: 1,
    peerUserId: '104'
  }
]

const mockMessages: Record<string, MessageItem[]> = {
  '201': [
    {
      id: '301',
      conversationId: '201',
      senderId: '102',
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
      senderType: 'user',
      type: 'text',
      content: { version: 1, blocks: [{ type: 'text', text: '文档我已经更新到最新版了' }] },
      seq: 1,
      createTime: new Date(Date.now() - 2 * 3600_000).toISOString()
    }
  ],
  '203': [
    {
      id: '305',
      conversationId: '203',
      senderId: '104',
      senderType: 'system',
      type: 'system',
      content: { version: 1, blocks: [{ type: 'text', text: '你有一条待处理的审批' }] },
      seq: 1,
      createTime: new Date(Date.now() - 24 * 3600_000).toISOString()
    }
  ]
}

let mockSeq = 400

function cloneConversations(): ConversationItem[] {
  return mockConversations.map(item => ({ ...item }))
}

function cloneMessages(conversationId: string): MessageItem[] {
  return (mockMessages[conversationId] ?? []).map(item => ({ ...item, content: { ...item.content, blocks: [...item.content.blocks] } }))
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

export function mockGetMessageList(conversationId: string, afterSeq = 0): MessageItem[] {
  return cloneMessages(conversationId).filter(item => item.seq > afterSeq)
}

export function mockSendMessage(payload: SendMessagePayload): SendMessageResult {
  const conversationId = payload.conversationId ?? '201'
  const list = mockMessages[conversationId] ?? (mockMessages[conversationId] = [])
  const seq = (list.at(-1)?.seq ?? 0) + 1
  mockSeq += 1
  const now = new Date().toISOString()
  const message: MessageItem = {
    id: String(mockSeq),
    conversationId,
    senderId: MOCK_CURRENT_USER_ID,
    senderType: 'user',
    type: payload.type ?? 'text',
    content: payload.content,
    clientMsgId: payload.clientMsgId,
    seq,
    createTime: now
  }
  list.push(message)

  const conversation = mockConversations.find(item => item.id === conversationId)
  if (conversation) {
    const preview = payload.content.blocks.find(block => block.type === 'text')?.text ?? '[消息]'
    conversation.lastMsgPreview = preview
    conversation.lastMsgAt = now
    conversation.unreadCount = 0
  }

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
