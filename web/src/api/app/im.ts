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

export function getConversationList(keyword?: string): Promise<ConversationItem[]> {
  return get<ConversationItem[]>('/app-api/im/conversation/list', {
    params: keyword ? { keyword } : undefined
  })
}

export function getMessageList(conversationId: string, afterSeq = 0): Promise<MessageItem[]> {
  return get<MessageItem[]>('/app-api/im/message/list', {
    params: { conversationId, afterSeq }
  })
}

export function sendMessage(payload: SendMessagePayload): Promise<SendMessageResult> {
  return post<SendMessageResult>('/app-api/im/message/send', payload)
}
