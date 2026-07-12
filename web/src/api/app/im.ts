import { get, post } from '../request'

export type ConversationType = 'direct' | 'group' | 'channel'

export interface ContentBlock {
  type: 'text' | 'file' | 'image' | 'system'
  text?: string
  fileId?: string
}

export interface MessageContent {
  version: number
  blocks: ContentBlock[]
}

export interface ConversationItem {
  id: string
  type: ConversationType
  title: string
  avatarText?: string
  lastMsgPreview?: string
  lastMsgAt?: string
  unreadCount: number
  peerUserId?: string
}

export interface MessageItem {
  id: string
  conversationId: string
  senderId: string
  senderType: 'user' | 'system' | 'bot' | 'app'
  type: 'text' | 'image' | 'file' | 'system'
  content: MessageContent
  clientMsgId?: string
  seq: number
  createTime: string
}

export interface SendMessagePayload {
  conversationId?: string
  peerUserId?: string
  clientMsgId: string
  type?: 'text' | 'image' | 'file'
  content: MessageContent
}

export interface SendMessageResult {
  id: string
  conversationId: string
  seq: number
  clientMsgId: string
  createTime: string
}

export interface RealtimeEnvelope {
  domain: string
  type: string
  requestId?: string
  ts?: number
  payload?: unknown
}

type RawConversationItem = Omit<ConversationItem, 'id' | 'peerUserId'> & {
  id: number | string
  peerUserId?: number | string
}

type RawMessageItem = Omit<MessageItem, 'id' | 'conversationId' | 'senderId' | 'seq'> & {
  id: number | string
  conversationId: number | string
  senderId: number | string
  seq: number | string
}

type RawSendMessageResult = Omit<SendMessageResult, 'id' | 'conversationId' | 'seq'> & {
  id: number | string
  conversationId: number | string
  seq: number | string
}

function parseSeq(seq: number | string): number {
  return typeof seq === 'string' ? Number(seq) : seq
}

function normalizeConversation(item: RawConversationItem): ConversationItem {
  return {
    ...item,
    id: String(item.id),
    peerUserId: item.peerUserId != null ? String(item.peerUserId) : undefined
  }
}

function normalizeMessage(item: RawMessageItem): MessageItem {
  return {
    ...item,
    id: String(item.id),
    conversationId: String(item.conversationId),
    senderId: String(item.senderId),
    seq: parseSeq(item.seq)
  }
}

function normalizeSendResult(item: RawSendMessageResult): SendMessageResult {
  return {
    ...item,
    id: String(item.id),
    conversationId: String(item.conversationId),
    seq: parseSeq(item.seq)
  }
}

export function getConversationList(keyword?: string): Promise<ConversationItem[]> {
  return get<RawConversationItem[]>('/app-api/im/conversation/list', {
    params: keyword ? { keyword } : undefined
  }).then(list => list.map(normalizeConversation))
}

export function getMessageList(conversationId: string, afterSeq = 0): Promise<MessageItem[]> {
  return get<RawMessageItem[]>('/app-api/im/message/list', {
    params: { conversationId, afterSeq }
  }).then(list => list.map(normalizeMessage))
}

export function sendMessage(payload: SendMessagePayload): Promise<SendMessageResult> {
  return post<RawSendMessageResult>('/app-api/im/message/send', payload).then(normalizeSendResult)
}

export function markConversationRead(conversationId: string, readSeq: number): Promise<void> {
  return post<void>('/app-api/im/conversation/read', { conversationId, readSeq })
}
