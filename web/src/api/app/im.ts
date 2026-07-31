import { get, post, put } from '../request'

export type ConversationType = 'direct' | 'group' | 'channel' | 'bot_dm'

export interface ContentBlock {
  type: 'text' | 'file' | 'image' | 'system' | 'deeplink' | 'card' | 'mention' | 'sticker'
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
  /** Present when type=card ({@code generic.v1}). */
  schema?: number
  cardId?: string
  template?: string
  header?: CardHeader
  fields?: CardField[]
  actions?: CardActionItem[]
  meta?: CardMeta
  /** Present when type=mention. */
  subjectType?: 'user' | 'bot'
  subjectId?: string
  botCode?: string
  /** Present when type=sticker. */
  stickerId?: string
  packId?: string
  userStickerId?: string
  /** Web mock / fallback glyph when image URL missing. */
  emoji?: string
}

export interface CardHeader {
  title: string
  subtitle?: string
}

export interface CardField {
  label: string
  value: string
}

export interface CardFormField {
  name: string
  label: string
  required?: boolean
  control?: 'text' | 'textarea'
}

export interface CardBehavior {
  type: 'open_url' | 'callback'
  route?: string
  actionKey?: string
  payload?: Record<string, unknown>
  form?: CardFormField[]
}

export interface CardActionItem {
  id: string
  label: string
  style?: 'default' | 'primary' | 'danger' | string
  behavior: CardBehavior
}

export interface CardMeta {
  expiresAt?: string
  source?: {
    domain?: string
    entityType?: string
    entityId?: string
  }
}

export interface CardActionPayload {
  messageId: string
  conversationId: string
  actionId: string
  actionKey: string
  payload?: Record<string, unknown>
  formValues?: Record<string, unknown>
  clientActionId: string
}

export interface CardActionResult {
  toast?: {
    type?: string
    content?: string
  }
  message?: MessageItem
}

export interface MessageContent {
  version: number
  blocks: ContentBlock[]
  forwardedFrom?: MessageForwardedFrom
}

export interface ConversationItem {
  id: string
  type: ConversationType
  title: string
  avatarText?: string
  /** File id or URL; omit when unset. */
  avatar?: string | null
  lastMsgPreview?: string
  lastMsgAt?: string
  unreadCount: number
  /** Peer-visible read watermark. */
  readSeq?: number
  /** Inbox badge watermark; first unread ≈ badgeSeq + 1 when unreadCount > 0. */
  badgeSeq?: number
  /** True when current user has unread @mentions in this conversation. */
  mentionedMe?: boolean
  /** Member-private pin. */
  pinned?: boolean
  pinnedAt?: string | null
  /** Member-private flag. */
  flagged?: boolean
  /** Message mute / DND for current member. */
  muted?: boolean
  muteUntil?: string | null
  /** User-private tag ids applied to this conversation. */
  tagIds?: string[]
  peerUserId?: string
  memberCount?: number
  /** Present when type=bot_dm. */
  botId?: string
  botCode?: string
}

export type GroupMemberRole = 'owner' | 'admin' | 'member'
export type GroupMemberSubjectType = 'user' | 'bot'

export interface GroupMemberItem {
  subjectType: GroupMemberSubjectType
  userId?: string
  botId?: string
  botCode?: string
  nickname: string
  avatarText: string
  avatar?: string | null
  role: GroupMemberRole
}

export interface GroupBotCatalogItem {
  botId: string
  botCode: string
  name: string
  avatarText: string
  avatar?: string | null
  alreadyMember: boolean
}

export interface GroupBotMembershipPayload {
  conversationId: string
  botCode: string
}

export interface GroupBotAddResult {
  added: boolean
}

export interface GroupBotRemoveResult {
  removed: boolean
}

export interface CreateGroupPayload {
  name: string
  memberUserIds: string[]
  groupKind?: 'NORMAL' | 'DEPT'
  deptId?: string
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

export interface MessageReplyTo {
  messageId: string
  seq?: number
  senderType?: string
  senderId?: string
  senderNickname?: string
  preview?: string
}

export interface MessageReactionAgg {
  emoji: string
  count: number
  me: boolean
}

export interface MessageForwardedFrom {
  conversationId: string
  messageId: string
  senderNickname?: string
  preview?: string
}

export interface StickerItem {
  id: string
  packId: string
  code: string
  name: string
  /** Image URL when backed by infra_file; optional in web mock. */
  downloadUrl?: string
  /** Glyph used by web mock / text fallback. */
  emoji?: string
}

export interface StickerPackItem {
  id: string
  code: string
  name: string
  scope: 'system' | 'tenant'
  stickers: StickerItem[]
}

export interface UserStickerItem {
  id: string
  fileId: string
  name?: string
  downloadUrl?: string
  sort?: number
}

export interface MessageItem {
  id: string
  conversationId: string
  senderId: string
  senderNickname?: string
  senderType: 'user' | 'system' | 'bot' | 'app'
  type: 'text' | 'image' | 'file' | 'system' | 'card' | 'sticker'
  content: MessageContent
  clientMsgId?: string
  seq: number
  createTime: string
  recalled?: boolean
  edited?: boolean
  recalledAt?: string
  editedAt?: string
  replyTo?: MessageReplyTo
  reactions?: MessageReactionAgg[]
  forwardedFrom?: MessageForwardedFrom
}

export interface SendMessagePayload {
  conversationId?: string
  peerUserId?: string
  clientMsgId: string
  type?: 'text' | 'image' | 'file' | 'sticker'
  content: MessageContent
  replyToMsgId?: string
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

type RawConversationItem = Omit<ConversationItem, 'id' | 'peerUserId' | 'botId' | 'readSeq' | 'badgeSeq' | 'tagIds'> & {
  id: number | string
  peerUserId?: number | string
  botId?: number | string
  readSeq?: number | string
  badgeSeq?: number | string
  tagIds?: Array<number | string>
}

type RawMessageItem = Omit<MessageItem, 'id' | 'conversationId' | 'senderId' | 'seq' | 'replyTo' | 'forwardedFrom'> & {
  id: number | string
  conversationId: number | string
  senderId: number | string
  seq: number | string
  replyTo?: Omit<MessageReplyTo, 'messageId' | 'senderId' | 'seq'> & {
    messageId?: number | string
    senderId?: number | string
    seq?: number | string
  }
  forwardedFrom?: Omit<MessageForwardedFrom, 'conversationId' | 'messageId'> & {
    conversationId?: number | string
    messageId?: number | string
  }
  reactions?: MessageReactionAgg[]
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
    botId: item.botId != null ? String(item.botId) : undefined,
    avatar: item.avatar ?? null,
    readSeq: item.readSeq != null ? parseSeq(item.readSeq) : 0,
    badgeSeq: item.badgeSeq != null ? parseSeq(item.badgeSeq) : 0,
    pinned: !!item.pinned,
    pinnedAt: item.pinnedAt ?? null,
    flagged: !!item.flagged,
    muted: !!item.muted,
    muteUntil: item.muteUntil ?? null,
    tagIds: item.tagIds?.map(String) ?? []
  }
}

function normalizeMessage(item: RawMessageItem): MessageItem {
  const replyTo = item.replyTo?.messageId != null
    ? {
        messageId: String(item.replyTo.messageId),
        seq: item.replyTo.seq != null ? parseSeq(item.replyTo.seq) : undefined,
        senderType: item.replyTo.senderType,
        senderId: item.replyTo.senderId != null ? String(item.replyTo.senderId) : undefined,
        senderNickname: item.replyTo.senderNickname,
        preview: item.replyTo.preview
      }
    : undefined
  const contentForwarded = item.content?.forwardedFrom
  const forwardedFrom = item.forwardedFrom?.messageId != null
    ? {
        conversationId: String(item.forwardedFrom.conversationId ?? ''),
        messageId: String(item.forwardedFrom.messageId),
        senderNickname: item.forwardedFrom.senderNickname,
        preview: item.forwardedFrom.preview
      }
    : contentForwarded?.messageId != null
      ? {
          conversationId: String(contentForwarded.conversationId ?? ''),
          messageId: String(contentForwarded.messageId),
          senderNickname: contentForwarded.senderNickname,
          preview: contentForwarded.preview
        }
      : undefined
  return {
    ...item,
    id: String(item.id),
    conversationId: String(item.conversationId),
    senderId: String(item.senderId),
    seq: parseSeq(item.seq),
    recalled: Boolean(item.recalled),
    edited: Boolean(item.edited),
    replyTo,
    forwardedFrom,
    reactions: (item.reactions ?? []).map(r => ({
      emoji: r.emoji,
      count: Number(r.count) || 0,
      me: Boolean(r.me)
    }))
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

export type MessageListQuery = {
  conversationId: string
  /** Messages with seq > afterSeq (legacy / forward page). */
  afterSeq?: number
  /** Messages with seq < beforeSeq. */
  beforeSeq?: number
  /** Anchor seq for around window. */
  aroundSeq?: number
  /** Count before aroundSeq (around mode). */
  before?: number
  /** Count after aroundSeq (around mode). */
  after?: number
  /** Max items (latest / before / after modes). */
  limit?: number
}

export function getMessageList(conversationId: string, afterSeq?: number): Promise<MessageItem[]>
export function getMessageList(query: MessageListQuery): Promise<MessageItem[]>
export function getMessageList(
  conversationIdOrQuery: string | MessageListQuery,
  afterSeq = 0
): Promise<MessageItem[]> {
  const query: MessageListQuery = typeof conversationIdOrQuery === 'string'
    ? { conversationId: conversationIdOrQuery, afterSeq }
    : conversationIdOrQuery

  const params: Record<string, string | number> = {
    conversationId: query.conversationId
  }
  if (query.afterSeq != null) {
    params.afterSeq = query.afterSeq
  }
  if (query.beforeSeq != null) {
    params.beforeSeq = query.beforeSeq
  }
  if (query.aroundSeq != null) {
    params.aroundSeq = query.aroundSeq
  }
  if (query.before != null) {
    params.before = query.before
  }
  if (query.after != null) {
    params.after = query.after
  }
  if (query.limit != null) {
    params.limit = query.limit
  }

  // Server enforces window modes (around / before / after / latest) with hard-cap 50.
  return get<RawMessageItem[]>('/app-api/im/message/list', { params })
    .then(list => list.map(normalizeMessage))
}


export function sendMessage(payload: SendMessagePayload): Promise<SendMessageResult> {
  return post<RawSendMessageResult>('/app-api/im/message/send', payload).then(normalizeSendResult)
}

export interface ToggleReactionResult {
  messageId: string
  conversationId: string
  reactions: MessageReactionAgg[]
}

export function toggleMessageReaction(messageId: string, emoji: string): Promise<ToggleReactionResult> {
  return post<{
    messageId: number | string
    conversationId: number | string
    reactions?: MessageReactionAgg[]
  }>('/app-api/im/message/reaction/toggle', { messageId, emoji }).then(raw => ({
    messageId: String(raw.messageId),
    conversationId: String(raw.conversationId),
    reactions: (raw.reactions ?? []).map(r => ({
      emoji: r.emoji,
      count: Number(r.count) || 0,
      me: Boolean(r.me)
    }))
  }))
}

export function forwardMessage(payload: {
  messageId: string
  targetConversationId: string
  clientMsgId?: string
}): Promise<MessageItem> {
  return post<RawMessageItem>('/app-api/im/message/forward', payload).then(normalizeMessage)
}

type RawStickerItem = Omit<StickerItem, 'id' | 'packId'> & {
  id: number | string
  packId: number | string
}

type RawStickerPackItem = Omit<StickerPackItem, 'id' | 'stickers'> & {
  id: number | string
  stickers?: RawStickerItem[]
}

export function getStickerPacks(): Promise<StickerPackItem[]> {
  return get<RawStickerPackItem[]>('/app-api/im/sticker/packs').then(list =>
    (list ?? []).map(pack => ({
      id: String(pack.id),
      code: pack.code,
      name: pack.name,
      scope: pack.scope,
      stickers: (pack.stickers ?? []).map(sticker => ({
        id: String(sticker.id),
        packId: String(sticker.packId),
        code: sticker.code,
        name: sticker.name,
        downloadUrl: sticker.downloadUrl,
        emoji: sticker.emoji
      }))
    })))
}

type RawUserStickerItem = Omit<UserStickerItem, 'id' | 'fileId'> & {
  id: number | string
  fileId: number | string
}

function normalizeUserSticker(item: RawUserStickerItem): UserStickerItem {
  return {
    id: String(item.id),
    fileId: String(item.fileId),
    name: item.name,
    downloadUrl: item.downloadUrl,
    sort: item.sort
  }
}

export function getMyStickers(): Promise<UserStickerItem[]> {
  return get<RawUserStickerItem[]>('/app-api/im/sticker/mine').then(list =>
    (list ?? []).map(normalizeUserSticker))
}

export function addMySticker(payload: { fileId: string, name?: string }): Promise<UserStickerItem> {
  return post<RawUserStickerItem>('/app-api/im/sticker/mine', payload).then(normalizeUserSticker)
}

export function addMyStickerFromMessage(messageId: string): Promise<UserStickerItem> {
  return post<RawUserStickerItem>('/app-api/im/sticker/mine/from-message', { messageId })
    .then(normalizeUserSticker)
}

export function reorderMyStickers(ids: string[]): Promise<void> {
  return put<void>('/app-api/im/sticker/mine/reorder', { ids })
}

export function deleteMyStickers(ids: string[]): Promise<void> {
  return post<void>('/app-api/im/sticker/mine/delete', { ids })
}

export function markConversationRead(conversationId: string, readSeq: number): Promise<void> {
  return post<void>('/app-api/im/conversation/read', { conversationId, readSeq })
}

export function pinConversation(conversationId: string, pinned: boolean): Promise<void> {
  return post<void>('/app-api/im/conversation/pin', { conversationId, pinned })
}

export function flagConversation(conversationId: string, flagged: boolean): Promise<void> {
  return post<void>('/app-api/im/conversation/flag', { conversationId, flagged })
}

export function muteConversation(conversationId: string, muted: boolean): Promise<void> {
  return post<void>('/app-api/im/conversation/mute', { conversationId, muted })
}

export function markInboxRead(conversationId: string): Promise<void> {
  return post<void>('/app-api/im/conversation/inbox/mark-read', { conversationId })
}

export function markInboxUnread(conversationId: string): Promise<void> {
  return post<void>('/app-api/im/conversation/inbox/mark-unread', { conversationId })
}

export interface ImUserTagItem {
  id: string
  name: string
}

type RawImUserTagItem = Omit<ImUserTagItem, 'id'> & { id: number | string }

function normalizeUserTag(item: RawImUserTagItem): ImUserTagItem {
  return { id: String(item.id), name: item.name }
}

export function getUserTags(): Promise<ImUserTagItem[]> {
  return get<RawImUserTagItem[]>('/app-api/im/tag/list').then(list => list.map(normalizeUserTag))
}

export function createUserTag(name: string): Promise<ImUserTagItem> {
  return post<RawImUserTagItem>('/app-api/im/tag/create', { name }).then(normalizeUserTag)
}

export function assignUserTag(conversationId: string, tagId: string): Promise<void> {
  return post<void>('/app-api/im/tag/assign', { conversationId, tagId })
}

export function createAndAssignUserTag(conversationId: string, name: string): Promise<ImUserTagItem> {
  return post<RawImUserTagItem>('/app-api/im/tag/create-and-assign', { conversationId, name })
    .then(normalizeUserTag)
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

type RawGroupMemberItem = Omit<GroupMemberItem, 'userId' | 'botId'> & {
  userId?: number | string
  botId?: number | string
  subjectType?: GroupMemberSubjectType
}

function normalizeGroupMember(item: RawGroupMemberItem): GroupMemberItem {
  return {
    subjectType: item.subjectType ?? 'user',
    userId: item.userId != null ? String(item.userId) : undefined,
    botId: item.botId != null ? String(item.botId) : undefined,
    botCode: item.botCode,
    nickname: item.nickname,
    avatarText: item.avatarText,
    avatar: item.avatar ?? null,
    role: item.role
  }
}

export function getGroupMembers(conversationId: string): Promise<GroupMemberItem[]> {
  return get<RawGroupMemberItem[]>('/app-api/im/group/members', {
    params: { conversationId }
  }).then(list => list.map(normalizeGroupMember))
}

type RawGroupBotCatalogItem = Omit<GroupBotCatalogItem, 'botId'> & {
  botId: number | string
}

function normalizeGroupBotCatalogItem(item: RawGroupBotCatalogItem): GroupBotCatalogItem {
  return {
    ...item,
    botId: String(item.botId),
    avatar: item.avatar ?? null
  }
}

export function getGroupBotCatalog(conversationId: string): Promise<GroupBotCatalogItem[]> {
  return get<RawGroupBotCatalogItem[]>('/app-api/im/group/bots/catalog', {
    params: { conversationId }
  }).then(list => list.map(normalizeGroupBotCatalogItem))
}

export function addGroupBot(payload: GroupBotMembershipPayload): Promise<GroupBotAddResult> {
  return post<GroupBotAddResult>('/app-api/im/group/bots/add', payload)
}

export function removeGroupBot(payload: GroupBotMembershipPayload): Promise<GroupBotRemoveResult> {
  return post<GroupBotRemoveResult>('/app-api/im/group/bots/remove', payload)
}

export interface ImEffectiveSettings {
  groupMemberLimitDefault: number
  allowPublicGroup: boolean
  deptGroupLeavePolicy: 'ALLOW' | 'DENY'
  memberExportPolicy: 'ALLOW_OWNER_ADMIN' | 'DENY_ALL' | 'ALLOW_DESIGNATED'
  recallEnabled: boolean
  recallWithinHours: number
  editEnabled: boolean
  editWithinHours: number
}

export function getImEffectiveSettings(): Promise<ImEffectiveSettings> {
  return get<ImEffectiveSettings>('/app-api/im/settings/effective')
}

export interface GroupInfo {
  conversationId: string
  name: string
  searchable: boolean
  groupKind: 'NORMAL' | 'DEPT'
  deptId?: string | null
  myRole: GroupMemberRole
}

type RawGroupInfo = Omit<GroupInfo, 'conversationId' | 'deptId'> & {
  conversationId: number | string
  deptId?: number | string | null
}

export function getGroupInfo(conversationId: string): Promise<GroupInfo> {
  return get<RawGroupInfo>('/app-api/im/group/info', {
    params: { conversationId }
  }).then(info => ({
    ...info,
    conversationId: String(info.conversationId),
    deptId: info.deptId != null ? String(info.deptId) : null
  }))
}

export function updateGroupPublic(conversationId: string, searchable: boolean): Promise<boolean> {
  return put<boolean>('/app-api/im/group/public', { conversationId, searchable })
}

export interface PublicGroupSearchItem {
  conversationId: string
  name: string
  memberCount: number
}

export function searchPublicGroups(keyword: string): Promise<PublicGroupSearchItem[]> {
  return get<Array<{ conversationId: number | string, name: string, memberCount: number }>>(
    '/app-api/im/group/search-public',
    { params: { keyword } }
  ).then(list => list.map(item => ({
    conversationId: String(item.conversationId),
    name: item.name,
    memberCount: item.memberCount
  })))
}

export function joinPublicGroup(conversationId: string): Promise<boolean> {
  return post<boolean>('/app-api/im/group/join', { conversationId })
}

export function leaveGroup(conversationId: string): Promise<boolean> {
  return post<boolean>('/app-api/im/group/leave', { conversationId })
}

export interface GroupMemberExportItem {
  userId: string
  nickname: string
  mobile?: string | null
  deptName?: string | null
}

export function exportGroupMembers(conversationId: string): Promise<GroupMemberExportItem[]> {
  return get<Array<{ userId: number | string, nickname: string, mobile?: string | null, deptName?: string | null }>>(
    '/app-api/im/group/members/export',
    { params: { conversationId } }
  ).then(list => list.map(item => ({
    userId: String(item.userId),
    nickname: item.nickname,
    mobile: item.mobile,
    deptName: item.deptName
  })))
}

export function recallMessage(messageId: string): Promise<boolean> {
  return post<boolean>('/app-api/im/message/recall', { messageId })
}

export function editMessage(messageId: string, content: string): Promise<boolean> {
  return post<boolean>('/app-api/im/message/edit', { messageId, content })
}

export function postCardAction(payload: CardActionPayload): Promise<CardActionResult> {
  return post<CardActionResult>('/app-api/im/card/action', payload).then((result) => {
    if (result?.message) {
      return {
        ...result,
        message: normalizeMessage(result.message as unknown as RawMessageItem)
      }
    }
    return result
  })
}
