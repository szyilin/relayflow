import { get, post } from '../request'

export type ConversationType = 'direct' | 'group' | 'channel' | 'bot_dm'

export interface ContentBlock {
  type: 'text' | 'file' | 'image' | 'system' | 'deeplink'
  text?: string
  fileId?: string
  filename?: string
  mimeType?: string
  size?: number
  downloadUrl?: string
  /** Present when type=deeplink (Bot business reach). */
  route?: string
  entityType?: string
  entityId?: string
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
  memberCount?: number
  /** Present when type=bot_dm. */
  botId?: string
  botCode?: string
}

export type GroupMemberRole = 'owner' | 'admin' | 'member'

export interface GroupMemberItem {
  userId: string
  nickname: string
  avatarText: string
  role: GroupMemberRole
}

export interface CreateGroupPayload {
  name: string
  memberUserIds: string[]
}

export interface CreateGroupResult {
  conversationId: string
  groupId: string
}

export interface AddGroupMembersPayload {
  conversationId: string
  memberUserIds: string[]
}

export interface AddGroupMembersResult {
  addedCount: number
}

export interface MessageItem {
  id: string
  conversationId: string
  senderId: string
  senderNickname?: string
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

type RawConversationItem = Omit<ConversationItem, 'id' | 'peerUserId' | 'botId'> & {
  id: number | string
  peerUserId?: number | string
  botId?: number | string
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
    peerUserId: item.peerUserId != null ? String(item.peerUserId) : undefined,
    botId: item.botId != null ? String(item.botId) : undefined
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

export interface ConversationMemberReadStatus {
  userId: string
  readSeq: number
}

export interface ConversationReadStatus {
  conversationId: string
  members: ConversationMemberReadStatus[]
}

type RawConversationMemberReadStatus = Omit<ConversationMemberReadStatus, 'userId' | 'readSeq'> & {
  userId: number | string
  readSeq: number | string
}

type RawConversationReadStatus = Omit<ConversationReadStatus, 'conversationId' | 'members'> & {
  conversationId: number | string
  members: RawConversationMemberReadStatus[]
}

function normalizeReadStatus(result: RawConversationReadStatus): ConversationReadStatus {
  return {
    conversationId: String(result.conversationId),
    members: result.members.map(member => ({
      userId: String(member.userId),
      readSeq: parseSeq(member.readSeq)
    }))
  }
}

export function getReadStatus(conversationId: string): Promise<ConversationReadStatus> {
  return get<RawConversationReadStatus>('/app-api/im/conversation/read-status', {
    params: { conversationId }
  }).then(normalizeReadStatus)
}

export function createGroup(payload: CreateGroupPayload): Promise<CreateGroupResult> {
  return post<RawCreateGroupResult>('/app-api/im/group/create', payload).then(normalizeCreateGroupResult)
}

export function addGroupMembers(payload: AddGroupMembersPayload): Promise<AddGroupMembersResult> {
  return post<AddGroupMembersResult>('/app-api/im/group/members/add', payload)
}

type RawCreateGroupResult = {
  conversationId: number | string
  groupId: number | string
}

function normalizeCreateGroupResult(result: RawCreateGroupResult): CreateGroupResult {
  return {
    conversationId: String(result.conversationId),
    groupId: String(result.groupId)
  }
}

type RawGroupMemberItem = Omit<GroupMemberItem, 'userId'> & {
  userId: number | string
}

function normalizeGroupMember(item: RawGroupMemberItem): GroupMemberItem {
  return {
    ...item,
    userId: String(item.userId)
  }
}

export function getGroupMembers(conversationId: string): Promise<GroupMemberItem[]> {
  return get<RawGroupMemberItem[]>('/app-api/im/group/members', {
    params: { conversationId }
  }).then(list => list.map(normalizeGroupMember))
}
