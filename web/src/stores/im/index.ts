import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  addGroupBot as addGroupBotRequest,
  addGroupMembers,
  assignUserTag as assignUserTagRequest,
  createAndAssignUserTag as createAndAssignUserTagRequest,
  createGroup as createGroupRequest,
  createUserTag as createUserTagRequest,
  editMessage as editMessageRequest,
  exportGroupMembers as exportGroupMembersRequest,
  flagConversation as flagConversationRequest,
  forwardMessage as forwardMessageRequest,
  getConversationList,
  getGroupBotCatalog,
  getGroupInfo as getGroupInfoRequest,
  getGroupMembers,
  getImEffectiveSettings,
  getMessageList,
  getMyStickers,
  getReadStatus,
  getStickerPacks,
  getUserTags as getUserTagsRequest,
  addMySticker,
  addMyStickerFromMessage,
  deleteMyStickers,
  reorderMyStickers,
  joinPublicGroup as joinPublicGroupRequest,
  leaveGroup as leaveGroupRequest,
  markConversationRead,
  markInboxRead as markInboxReadRequest,
  markInboxUnread as markInboxUnreadRequest,
  muteConversation as muteConversationRequest,
  pinConversation as pinConversationRequest,
  recallMessage as recallMessageRequest,
  removeGroupBot as removeGroupBotRequest,
  searchPublicGroups as searchPublicGroupsRequest,
  sendMessage,
  toggleMessageReaction,
  updateGroupPublic as updateGroupPublicRequest,
  type ContentBlock,
  type ConversationItem,
  type GroupBotCatalogItem,
  type GroupInfo,
  type GroupMemberItem,
  type ImEffectiveSettings,
  type ImUserTagItem,
  type MessageContent,
  type MessageItem,
  type MessageReactionAgg,
  type PublicGroupSearchItem,
  type StickerPackItem,
  type UserStickerItem
} from '../../api/app/im'
import { ApiError } from '../../api/request'
import { uploadPrivateFile } from '../../api/app/file'
import { useAuthStore } from '../auth'
import {
  buildMessageTimeline,
  fileBlockFromContent,
  formatClockTime,
  formatDayLabel,
  formatMessageTimeHeader,
  formatRelativeTime,
  isFileMessage,
  isImageMessage,
  isStickerMessage,
  messagePreviewFromContent,
  normalizeIncomingMessage,
  stickerBlockFromContent,
  textFromContent
} from './helpers'
import type { GroupMemberCandidate, MessageViewItem, PendingDirectChat } from './types'
import {
  conversationMatchesSidebarSelection,
  type ImSidebarSelection
} from '../../composables/imConversationGroups'
import {
  MESSAGE_AROUND_AFTER,
  MESSAGE_AROUND_BEFORE,
  MESSAGE_WINDOW_LIMIT,
  mergeMessagesBySeq
} from './messageWindow'

/** Merged fetch cache only — not a full history dump; pages come from server windows. */

export type { GroupMemberCandidate, MessageViewItem, PendingDirectChat } from './types'

export type ImUserTag = ImUserTagItem

export const useImStore = defineStore('im', () => {
  const conversations = ref<ConversationItem[]>([])
  const messages = ref<MessageViewItem[]>([])
  /** In-memory cache keyed by conversationId — avoids skeleton flash on revisit. */
  const messagesByConversation = ref<Record<string, MessageViewItem[]>>({})
  /** Merged seq cache for the active window + paginated pages / realtime inserts. */
  const messagePools = ref<Record<string, MessageViewItem[]>>({})
  const hasMoreBefore = ref(false)
  const hasMoreAfter = ref(false)
  const loadingOlder = ref(false)
  const loadingNewer = ref(false)
  const groupMembers = ref<GroupMemberItem[]>([])
  const groupInfo = ref<GroupInfo | null>(null)
  const effectiveSettings = ref<ImEffectiveSettings | null>(null)
  const activeConversationId = ref<string>()
  const keyword = ref('')
  const loadingConversations = ref(false)
  const loadingMessages = ref(false)
  const loadingGroupMembers = ref(false)
  const sending = ref(false)
  const lastError = ref<string | null>(null)
  const pendingDirectChat = ref<PendingDirectChat | null>(null)
  const peerReadSeq = ref(0)
  /** Unread count captured when entering a conversation (before local clear). */
  const enterUnreadCount = ref(0)
  /** First unread seq for jump-to-unread FAB; null when none. */
  const firstUnreadSeq = ref<number | null>(null)
  /** Highest read seq already reported for the active conversation. */
  const reportedReadSeq = ref(0)
  /** Current user's tag library. */
  const userTags = ref<ImUserTag[]>([])
  /** Active conversation-list group / tag filter. */
  const sidebarSelection = ref<ImSidebarSelection>({ kind: 'group', groupId: 'all' })

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
        avatar: pendingDirectChat.value.avatar,
        peerUserId: pendingDirectChat.value.peerUserId,
        unreadCount: 0
      }
    }
    return undefined
  })

  const filteredConversations = computed(() => {
    const q = keyword.value.trim().toLowerCase()
    let list = conversations.value.filter(item =>
      conversationMatchesSidebarSelection(item, sidebarSelection.value))
    if (q) {
      list = list.filter(item =>
        item.title.toLowerCase().includes(q) || (item.lastMsgPreview ?? '').toLowerCase().includes(q))
    }
    return [...list].sort((a, b) => {
      const ap = a.pinned ? 1 : 0
      const bp = b.pinned ? 1 : 0
      if (ap !== bp) {
        return bp - ap
      }
      if (ap && bp) {
        const at = a.pinnedAt ? Date.parse(a.pinnedAt) : 0
        const bt = b.pinnedAt ? Date.parse(b.pinnedAt) : 0
        if (at !== bt) {
          return bt - at
        }
      }
      const aTime = a.lastMsgAt ? Date.parse(a.lastMsgAt) : 0
      const bTime = b.lastMsgAt ? Date.parse(b.lastMsgAt) : 0
      return bTime - aTime
    })
  })

  const pinnedConversations = computed(() => filteredConversations.value.filter(item => !!item.pinned))
  const unpinnedConversations = computed(() => filteredConversations.value.filter(item => !item.pinned))

  /** Nav badge excludes muted conversations. */
  const totalUnreadCount = computed(() =>
    conversations.value.reduce((sum, item) => {
      if (item.muted) {
        return sum
      }
      return sum + (item.unreadCount > 0 ? item.unreadCount : 0)
    }, 0))

  const flaggedCount = computed(() =>
    conversations.value.reduce((sum, item) => sum + (item.flagged ? 1 : 0), 0))

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
    messagesByConversation.value = {}
    messagePools.value = {}
    hasMoreBefore.value = false
    hasMoreAfter.value = false
    loadingOlder.value = false
    loadingNewer.value = false
    groupMembers.value = []
    groupInfo.value = null
    effectiveSettings.value = null
    activeConversationId.value = undefined
    pendingDirectChat.value = null
    keyword.value = ''
    lastError.value = null
    peerReadSeq.value = 0
    enterUnreadCount.value = 0
    firstUnreadSeq.value = null
    reportedReadSeq.value = 0
    stickerPacks.value = []
    myStickers.value = []
    userTags.value = []
    sidebarSelection.value = { kind: 'group', groupId: 'all' }
  }

  function clearLocalUnread(conversationId: string) {
    const item = conversations.value.find(conv => conv.id === conversationId)
    if (item) {
      item.unreadCount = 0
      item.mentionedMe = false
    }
  }

  function setSidebarSelection(selection: ImSidebarSelection) {
    sidebarSelection.value = selection
  }

  function messageMentionsCurrentUser(message: MessageItem): boolean {
    const me = currentUserId()
    if (!me) {
      return false
    }
    return (message.content?.blocks ?? []).some(block =>
      block.type === 'mention'
      && block.subjectType === 'user'
      && block.subjectId != null
      && String(block.subjectId) === me)
  }

  function latestMessageSeq(list: MessageViewItem[] = messages.value): number {
    return list.reduce((max, item) => Math.max(max, item.seq ?? 0), 0)
  }

  function putMessageCache(conversationId: string, list: MessageViewItem[]) {
    messagesByConversation.value = {
      ...messagesByConversation.value,
      [conversationId]: list
    }
  }

  function putMessagePool(conversationId: string, list: MessageViewItem[]) {
    messagePools.value = {
      ...messagePools.value,
      [conversationId]: list
    }
  }

  function getPool(conversationId: string): MessageViewItem[] {
    return messagePools.value[conversationId] ?? []
  }

  function toViewMessages(list: MessageItem[]): MessageViewItem[] {
    return list.map(item => ({ ...item, localStatus: 'sent' as const }))
  }

  function applyServerWindow(
    conversationId: string,
    list: MessageViewItem[],
    mode: 'latest' | 'around'
  ) {
    const pool = mergeMessagesBySeq(getPool(conversationId), list)
    putMessagePool(conversationId, pool)
    syncActiveMessages(conversationId, list)
    if (mode === 'latest') {
      hasMoreBefore.value = list.length >= MESSAGE_WINDOW_LIMIT
      hasMoreAfter.value = false
    } else if (mode === 'around') {
      hasMoreBefore.value = list.length > 0 && (list[0]?.seq ?? 0) > 1
      hasMoreAfter.value = list.length > 0
    }
  }

  function syncPageFlags(fetchedCount: number, direction: 'before' | 'after') {
    if (direction === 'before') {
      hasMoreBefore.value = fetchedCount >= MESSAGE_WINDOW_LIMIT
    } else {
      hasMoreAfter.value = fetchedCount >= MESSAGE_WINDOW_LIMIT
    }
  }

  function replaceActiveMessages(updater: (prev: MessageViewItem[]) => MessageViewItem[]) {
    const conversationId = activeConversationId.value
    const next = updater(messages.value)
    messages.value = next
    if (conversationId && !conversationId.startsWith('pending-')) {
      putMessageCache(conversationId, next)
      const pool = mergeMessagesBySeq(getPool(conversationId), next)
      putMessagePool(conversationId, pool)
    }
  }

  function syncActiveMessages(conversationId: string, list: MessageViewItem[]) {
    if (activeConversationId.value === conversationId) {
      messages.value = list
    }
    putMessageCache(conversationId, list)
  }

  function computeFirstUnreadSeq(badgeSeq: number | undefined, unreadCount: number): number | null {
    if (unreadCount <= 0) {
      return null
    }
    return (badgeSeq ?? 0) + 1
  }

  /** Enter + stay: advance peer-visible read to conversation max seq (not viewport-gated). */
  async function advanceReadToMax(conversationId: string, maxSeqHint?: number) {
    let maxSeq = maxSeqHint ?? 0
    if (maxSeq <= 0) {
      maxSeq = latestMessageSeq(getPool(conversationId).length ? getPool(conversationId) : messages.value)
    }
    if (maxSeq > 0) {
      await reportConversationRead(conversationId, maxSeq)
      return
    }
    clearLocalUnread(conversationId)
  }

  async function reportConversationRead(conversationId: string, readSeq: number) {
    if (readSeq <= 0) {
      clearLocalUnread(conversationId)
      return
    }
    if (conversationId === activeConversationId.value && readSeq <= reportedReadSeq.value) {
      clearLocalUnread(conversationId)
      return
    }
    clearLocalUnread(conversationId)
    if (conversationId === activeConversationId.value) {
      reportedReadSeq.value = Math.max(reportedReadSeq.value, readSeq)
    }
    try {
      await markConversationRead(conversationId, readSeq)
    } catch {
      // 已读上报失败不阻断 UI；下次拉列表会校正
    }
  }

  /** Report read up to a visible seq (debounce caller). */
  async function reportReadUpTo(readSeq: number) {
    const conversationId = activeConversationId.value
    if (!conversationId || readSeq <= 0) {
      return
    }
    await reportConversationRead(conversationId, readSeq)
  }

  /** On leaving a conversation: treat remaining as read (user scrolled away / switched). */
  async function flushConversationRead(conversationId?: string) {
    const id = conversationId ?? activeConversationId.value
    if (!id) {
      return
    }
    const cached = messagesByConversation.value[id] ?? (id === activeConversationId.value ? messages.value : [])
    const maxSeq = latestMessageSeq(cached)
    if (maxSeq > 0) {
      await reportConversationRead(id, maxSeq)
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

  function patchConversation(conversationId: string, patch: Partial<ConversationItem>) {
    const item = conversations.value.find(conv => conv.id === conversationId)
    if (!item) {
      return
    }
    Object.assign(item, patch)
  }

  async function togglePin(conversationId: string, pinned?: boolean) {
    const item = conversations.value.find(conv => conv.id === conversationId)
    if (!item) {
      return
    }
    const next = pinned ?? !item.pinned
    const prev = { pinned: item.pinned, pinnedAt: item.pinnedAt }
    patchConversation(conversationId, {
      pinned: next,
      pinnedAt: next ? new Date().toISOString() : null
    })
    try {
      await pinConversationRequest(conversationId, next)
    } catch (error) {
      patchConversation(conversationId, prev)
      lastError.value = error instanceof ApiError ? error.message : '置顶失败'
      throw error
    }
  }

  async function toggleFlag(conversationId: string, flagged?: boolean) {
    const item = conversations.value.find(conv => conv.id === conversationId)
    if (!item) {
      return
    }
    const next = flagged ?? !item.flagged
    const prev = item.flagged
    patchConversation(conversationId, { flagged: next })
    try {
      await flagConversationRequest(conversationId, next)
    } catch (error) {
      patchConversation(conversationId, { flagged: prev })
      lastError.value = error instanceof ApiError ? error.message : '旗标失败'
      throw error
    }
  }

  async function toggleMute(conversationId: string, muted?: boolean) {
    const item = conversations.value.find(conv => conv.id === conversationId)
    if (!item) {
      return
    }
    const next = muted ?? !item.muted
    const prev = { muted: item.muted, muteUntil: item.muteUntil }
    patchConversation(conversationId, { muted: next, muteUntil: next ? item.muteUntil ?? undefined : null })
    try {
      await muteConversationRequest(conversationId, next)
    } catch (error) {
      patchConversation(conversationId, prev)
      lastError.value = error instanceof ApiError ? error.message : '免打扰失败'
      throw error
    }
  }

  /** List mark-as-read: clear inbox badge only (does not change read_seq). */
  async function markInboxRead(conversationId: string) {
    const item = conversations.value.find(conv => conv.id === conversationId)
    if (!item) {
      return
    }
    const prev = {
      unreadCount: item.unreadCount,
      mentionedMe: item.mentionedMe,
      badgeSeq: item.badgeSeq
    }
    patchConversation(conversationId, { unreadCount: 0, mentionedMe: false })
    try {
      await markInboxReadRequest(conversationId)
    } catch (error) {
      patchConversation(conversationId, prev)
      lastError.value = error instanceof ApiError ? error.message : '标记已读失败'
      throw error
    }
  }

  /** List mark-as-unread: always shows exactly 1; does not change read_seq. */
  async function markInboxUnread(conversationId: string) {
    const item = conversations.value.find(conv => conv.id === conversationId)
    if (!item) {
      return
    }
    const prev = { unreadCount: item.unreadCount, badgeSeq: item.badgeSeq }
    patchConversation(conversationId, { unreadCount: 1 })
    try {
      await markInboxUnreadRequest(conversationId)
    } catch (error) {
      patchConversation(conversationId, prev)
      lastError.value = error instanceof ApiError ? error.message : '标记未读失败'
      throw error
    }
  }

  async function fetchUserTags() {
    try {
      userTags.value = await getUserTagsRequest()
    } catch (error) {
      lastError.value = error instanceof ApiError ? error.message : '加载标签失败'
      throw error
    }
  }

  async function createUserTag(name: string): Promise<ImUserTag | null> {
    const trimmed = name.trim()
    if (!trimmed) {
      return null
    }
    const existing = userTags.value.find(tag => tag.name === trimmed)
    if (existing) {
      return existing
    }
    try {
      const tag = await createUserTagRequest(trimmed)
      if (!userTags.value.some(item => item.id === tag.id)) {
        userTags.value = [...userTags.value, tag]
      }
      return tag
    } catch (error) {
      lastError.value = error instanceof ApiError ? error.message : '创建标签失败'
      throw error
    }
  }

  async function assignTagToConversation(conversationId: string, tagId: string) {
    const item = conversations.value.find(conv => conv.id === conversationId)
    if (!item) {
      return
    }
    const prevTagIds = [...(item.tagIds ?? [])]
    const tagIds = new Set(prevTagIds)
    tagIds.add(tagId)
    patchConversation(conversationId, { tagIds: [...tagIds] })
    try {
      await assignUserTagRequest(conversationId, tagId)
    } catch (error) {
      patchConversation(conversationId, { tagIds: prevTagIds })
      lastError.value = error instanceof ApiError ? error.message : '打标失败'
      throw error
    }
  }

  async function createAndAssignTag(conversationId: string, name: string): Promise<ImUserTag | null> {
    const trimmed = name.trim()
    if (!trimmed) {
      return null
    }
    try {
      const tag = await createAndAssignUserTagRequest(conversationId, trimmed)
      if (!userTags.value.some(item => item.id === tag.id || item.name === tag.name)) {
        userTags.value = [...userTags.value, tag]
      } else if (!userTags.value.some(item => item.id === tag.id)) {
        userTags.value = userTags.value.map(item => (item.name === tag.name ? tag : item))
      }
      const item = conversations.value.find(conv => conv.id === conversationId)
      if (item) {
        const tagIds = new Set(item.tagIds ?? [])
        tagIds.add(tag.id)
        patchConversation(conversationId, { tagIds: [...tagIds] })
      }
      return tag
    } catch (error) {
      lastError.value = error instanceof ApiError ? error.message : '创建并打标失败'
      throw error
    }
  }

  async function fetchGroupMembers(conversationId: string) {
    loadingGroupMembers.value = true
    lastError.value = null
    try {
      const [members, info] = await Promise.all([
        getGroupMembers(conversationId),
        getGroupInfoRequest(conversationId).catch(() => null)
      ])
      groupMembers.value = members
      groupInfo.value = info
    } catch (error) {
      groupMembers.value = []
      groupInfo.value = null
      lastError.value = error instanceof ApiError ? error.message : '加载群成员失败'
      throw error
    } finally {
      loadingGroupMembers.value = false
    }
  }

  async function ensureEffectiveSettings() {
    if (effectiveSettings.value) {
      return effectiveSettings.value
    }
    effectiveSettings.value = await getImEffectiveSettings()
    return effectiveSettings.value
  }

  async function setGroupPublic(conversationId: string, searchable: boolean) {
    await updateGroupPublicRequest(conversationId, searchable)
    if (groupInfo.value?.conversationId === conversationId) {
      groupInfo.value = { ...groupInfo.value, searchable }
    }
  }

  async function searchPublicGroups(keywordText: string): Promise<PublicGroupSearchItem[]> {
    return searchPublicGroupsRequest(keywordText)
  }

  async function joinPublicGroup(conversationId: string) {
    await joinPublicGroupRequest(conversationId)
    await fetchConversations()
    await selectConversation(conversationId)
  }

  async function leaveActiveGroup() {
    const id = activeConversationId.value
    if (!id) {
      return
    }
    await leaveGroupRequest(id)
    groupInfo.value = null
    groupMembers.value = []
    await fetchConversations()
    activeConversationId.value = undefined
    messages.value = []
  }

  async function exportActiveGroupMembersCsv() {
    const id = activeConversationId.value
    if (!id) {
      return
    }
    const rows = await exportGroupMembersRequest(id)
    const header = 'userId,nickname,mobile,deptName'
    const body = rows.map(r =>
      [r.userId, r.nickname, r.mobile ?? '', r.deptName ?? '']
        .map(cell => `"${String(cell).replaceAll('"', '""')}"`)
        .join(',')
    ).join('\n')
    const blob = new Blob([`${header}\n${body}`], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `group-members-${id}.csv`
    a.click()
    URL.revokeObjectURL(url)
  }

  async function recallMessage(messageId: string) {
    await recallMessageRequest(messageId)
    const existing = messages.value.find(m => m.id === messageId)
    if (existing) {
      handleMessageUpdated({
        ...existing,
        recalled: true,
        content: { version: 1, blocks: [{ type: 'text', text: '消息已撤回' }] }
      })
    }
  }

  async function editMessage(messageId: string, text: string) {
    await editMessageRequest(messageId, text)
    const existing = messages.value.find(m => m.id === messageId)
    if (existing) {
      handleMessageUpdated({
        ...existing,
        edited: true,
        content: { version: 1, blocks: [{ type: 'text', text }] }
      })
    }
  }

  async function selectConversation(conversationId: string) {
    // Soft backup only — correctness is enter + stay continue, not leave flush.
    if (activeConversationId.value && activeConversationId.value !== conversationId) {
      void flushConversationRead(activeConversationId.value)
    }

    pendingDirectChat.value = null
    activeConversationId.value = conversationId
    const conversation = conversations.value.find(item => item.id === conversationId)
    enterUnreadCount.value = conversation?.unreadCount ?? 0
    reportedReadSeq.value = 0
    hasMoreBefore.value = false
    hasMoreAfter.value = false
    firstUnreadSeq.value = computeFirstUnreadSeq(conversation?.badgeSeq, enterUnreadCount.value)

    const cachedWindow = messagesByConversation.value[conversationId]
    if (cachedWindow?.length) {
      messages.value = cachedWindow
      loadingMessages.value = false
    } else {
      messages.value = []
    }

    const loadMessages = loadConversationWindow(conversationId, {
      aroundSeq: firstUnreadSeq.value ?? undefined,
      silent: Boolean(cachedWindow?.length)
    })
    if (conversation?.type === 'group') {
      await Promise.all([loadMessages, fetchGroupMembers(conversationId)])
    } else {
      groupMembers.value = []
      await loadMessages
    }

    await advanceReadToMax(conversationId, latestMessageSeq(getPool(conversationId)))

    if (conversation?.type === 'direct') {
      await fetchReadStatus(conversationId)
    } else {
      peerReadSeq.value = 0
    }
  }

  /** Load latest or around-unread window from server (hard-cap 50). */
  async function loadConversationWindow(
    conversationId: string,
    options?: { aroundSeq?: number, silent?: boolean }
  ) {
    const silent = options?.silent ?? false
    if (!silent) {
      loadingMessages.value = true
    }
    lastError.value = null
    try {
      const aroundSeq = options?.aroundSeq
      const list = aroundSeq != null
        ? await getMessageList({
            conversationId,
            aroundSeq,
            before: MESSAGE_AROUND_BEFORE,
            after: MESSAGE_AROUND_AFTER
          })
        : await getMessageList({
            conversationId,
            limit: MESSAGE_WINDOW_LIMIT
          })
      const normalized = toViewMessages(list)
      applyServerWindow(conversationId, normalized, aroundSeq != null ? 'around' : 'latest')
      firstUnreadSeq.value = aroundSeq ?? null
      return normalized
    } catch (error) {
      lastError.value = error instanceof ApiError ? error.message : '加载消息失败'
      throw error
    } finally {
      if (!silent) {
        loadingMessages.value = false
      }
    }
  }

  /** @deprecated prefer loadConversationWindow; kept for invite/bot refresh paths. */
  async function fetchMessages(
    conversationId: string,
    afterSeq = 0,
    options?: { silent?: boolean }
  ) {
    if (afterSeq > 0) {
      const silent = options?.silent ?? false
      if (!silent) {
        loadingMessages.value = true
      }
      try {
        const list = await getMessageList({ conversationId, afterSeq, limit: MESSAGE_WINDOW_LIMIT })
        const normalized = toViewMessages(list)
        const pool = mergeMessagesBySeq(getPool(conversationId), normalized)
        putMessagePool(conversationId, pool)
        if (activeConversationId.value === conversationId) {
          const merged = mergeMessagesBySeq(messages.value, normalized)
          syncActiveMessages(conversationId, merged)
          syncPageFlags(normalized.length, 'after')
        }
      } finally {
        if (!silent) {
          loadingMessages.value = false
        }
      }
      return
    }
    await loadConversationWindow(conversationId, {
      silent: options?.silent,
      aroundSeq: undefined
    })
  }

  async function loadOlderMessages(): Promise<boolean> {
    const conversationId = activeConversationId.value
    if (!conversationId || !hasMoreBefore.value || loadingOlder.value) {
      return false
    }
    const first = messages.value[0]
    if (!first?.seq) {
      return false
    }
    loadingOlder.value = true
    try {
      const remote = await getMessageList({
        conversationId,
        beforeSeq: first.seq,
        limit: MESSAGE_WINDOW_LIMIT
      })
      const older = toViewMessages(remote)
      if (!older.length) {
        hasMoreBefore.value = false
        return false
      }
      putMessagePool(conversationId, mergeMessagesBySeq(getPool(conversationId), older))
      const merged = mergeMessagesBySeq(older, messages.value)
      syncActiveMessages(conversationId, merged)
      syncPageFlags(older.length, 'before')
      return true
    } catch (error) {
      lastError.value = error instanceof ApiError ? error.message : '加载更早消息失败'
      return false
    } finally {
      loadingOlder.value = false
    }
  }

  async function loadNewerMessages(): Promise<boolean> {
    const conversationId = activeConversationId.value
    if (!conversationId || !hasMoreAfter.value || loadingNewer.value) {
      return false
    }
    const last = messages.value[messages.value.length - 1]
    if (!last?.seq) {
      return false
    }
    loadingNewer.value = true
    try {
      const remote = await getMessageList({
        conversationId,
        afterSeq: last.seq,
        limit: MESSAGE_WINDOW_LIMIT
      })
      const newer = toViewMessages(remote)
      if (!newer.length) {
        hasMoreAfter.value = false
        return false
      }
      putMessagePool(conversationId, mergeMessagesBySeq(getPool(conversationId), newer))
      const merged = mergeMessagesBySeq(messages.value, newer)
      syncActiveMessages(conversationId, merged)
      syncPageFlags(newer.length, 'after')
      return true
    } catch (error) {
      lastError.value = error instanceof ApiError ? error.message : '加载更新消息失败'
      return false
    } finally {
      loadingNewer.value = false
    }
  }

  /** Jump to latest window only — does not fill seq holes in between. */
  async function jumpToLatestWindow() {
    const conversationId = activeConversationId.value
    if (!conversationId) {
      return
    }
    try {
      await loadConversationWindow(conversationId, { silent: true })
    } catch {
      // keep current window
    }
    firstUnreadSeq.value = null
    await advanceReadToMax(conversationId, latestMessageSeq(getPool(conversationId)))
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
        if (messageMentionsCurrentUser(message)) {
          conversation.mentionedMe = true
        }
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

    const pool = mergeMessagesBySeq(getPool(conversationId), [message])
    putMessagePool(conversationId, pool)
    if (!hasMoreAfter.value) {
      const next = mergeMessagesBySeq(messages.value, [message])
      syncActiveMessages(conversationId, next)
      // stay on latest edge
      hasMoreAfter.value = false
    } else {
      hasMoreAfter.value = true
    }
    // Stay-continue: advance watermarks while panel stays active.
    void advanceReadToMax(conversationId, latestMessageSeq(pool))
  }

  function handleMessageUpdated(raw: MessageItem) {
    const message = normalizeIncomingMessage(raw)
    const conversationId = message.conversationId
    const preview = messagePreviewFromContent(message.type, message.content)
    const conversation = conversations.value.find(item => item.id === conversationId)
    if (conversation) {
      conversation.lastMsgPreview = preview
    }
    if (activeConversationId.value !== conversationId) {
      return
    }
    const index = messages.value.findIndex(item => item.id === message.id)
    if (index >= 0) {
      const next = messages.value.map(item =>
        item.id === message.id ? { ...message, localStatus: item.localStatus ?? 'sent' } : item)
      syncActiveMessages(conversationId, next)
    }
  }

  function applyMessageUpdate(message: MessageItem) {
    handleMessageUpdated(message)
  }

  async function createGroup(
    name: string,
    members: GroupMemberCandidate[],
    options?: { groupKind?: 'NORMAL' | 'DEPT', deptId?: string }
  ) {
    const trimmed = name.trim()
    const isDept = options?.groupKind === 'DEPT'
    if (!trimmed) {
      throw new Error('请填写群名称')
    }
    if (!isDept && members.length === 0) {
      throw new Error('请填写群名称并至少选择一名成员')
    }
    if (isDept && !options?.deptId) {
      throw new Error('请选择部门')
    }

    const result = await createGroupRequest({
      name: trimmed,
      memberUserIds: members.map(item => item.userId),
      groupKind: options?.groupKind ?? 'NORMAL',
      deptId: options?.deptId
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

  function openDirectChat(
    peerUserId: string,
    meta?: { title?: string, avatarText?: string, avatar?: string | null }
  ) {
    const existing = conversations.value.find(item =>
      item.type === 'direct' && item.peerUserId === peerUserId)
    if (existing) {
      void selectConversation(existing.id)
      return
    }

    if (activeConversationId.value) {
      void flushConversationRead(activeConversationId.value)
    }
    activeConversationId.value = undefined
    messages.value = []
    enterUnreadCount.value = 0
    firstUnreadSeq.value = null
    pendingDirectChat.value = {
      peerUserId,
      title: meta?.title ?? meta?.avatarText ?? '会话',
      avatarText: meta?.avatarText,
      avatar: meta?.avatar ?? null
    }
  }

  async function sendText(text: string, mentions: GroupMemberItem[] = [], replyToMsgId?: string) {
    const trimmed = text.trim()
    const conversation = activeConversation.value
    if ((!trimmed && mentions.length === 0) || !conversation) {
      return
    }

    const isPending = conversation.id.startsWith('pending-')
    const clientMsgId = crypto.randomUUID()
    const mentionBlocks: ContentBlock[] = []
    for (const member of mentions) {
      if (member.subjectType === 'bot' && member.botCode) {
        mentionBlocks.push({
          type: 'mention',
          subjectType: 'bot',
          subjectId: member.botId,
          botCode: member.botCode,
          text: `@${member.nickname}`
        })
        continue
      }
      if (member.subjectType === 'user' && member.userId) {
        mentionBlocks.push({
          type: 'mention',
          subjectType: 'user',
          subjectId: member.userId,
          text: `@${member.nickname}`
        })
      }
    }
    let bodyText = trimmed
    for (const member of mentions) {
      const token = `@${member.nickname}`
      bodyText = bodyText.split(token).join(' ')
    }
    bodyText = bodyText.replace(/\s+/g, ' ').trim()
    const textBlock: ContentBlock = {
      type: 'text',
      text: bodyText || (mentionBlocks.length > 0 ? ' ' : (trimmed || ' '))
    }
    const content: MessageContent = {
      version: 1,
      blocks: [...mentionBlocks, textBlock]
    }
    const preview = textFromContent(content)
    const quoted = replyToMsgId
      ? messages.value.find(item => item.id === replyToMsgId)
      : undefined

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
      localStatus: 'sending',
      replyTo: quoted
        ? {
            messageId: quoted.id,
            seq: quoted.seq,
            senderType: quoted.senderType,
            senderId: quoted.senderId,
            senderNickname: quoted.senderNickname,
            preview: textFromContent(quoted.content).slice(0, 80)
          }
        : undefined
    }
    replaceActiveMessages(prev => [...prev, optimistic])
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
        content,
        replyToMsgId
      })

      if (isPending) {
        pendingDirectChat.value = null
        activeConversationId.value = result.conversationId
        await fetchConversations()
      }

      replaceActiveMessages(prev => prev.map(item =>
        item.clientMsgId === clientMsgId
          ? {
              ...item,
              id: result.id,
              conversationId: result.conversationId,
              seq: result.seq,
              createTime: result.createTime,
              localStatus: 'sent'
            }
          : item))
      if (activeConversationId.value) {
        putMessageCache(activeConversationId.value, messages.value)
      }
      void reportConversationRead(result.conversationId, result.seq)

      const updatedConversation = conversations.value.find(item => item.id === result.conversationId)
      if (updatedConversation) {
        updatedConversation.lastMsgPreview = preview
        updatedConversation.lastMsgAt = result.createTime
      }
    } catch (error) {
      replaceActiveMessages(prev => prev.map(item =>
        item.clientMsgId === clientMsgId ? { ...item, localStatus: 'failed' } : item))
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
    replaceActiveMessages(prev => [...prev, optimistic])
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
      replaceActiveMessages(prev => prev.map(item =>
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
          : item))
      if (activeConversationId.value) {
        putMessageCache(activeConversationId.value, messages.value)
      }
      void reportConversationRead(result.conversationId, result.seq)

      const updatedConversation = conversations.value.find(item => item.id === result.conversationId)
      if (updatedConversation) {
        updatedConversation.lastMsgPreview = previewText
        updatedConversation.lastMsgAt = result.createTime
      }
    } catch (error) {
      URL.revokeObjectURL(previewUrl)
      replaceActiveMessages(prev => prev.map(item =>
        item.clientMsgId === clientMsgId ? { ...item, localStatus: 'failed' } : item))
      lastError.value = error instanceof ApiError ? error.message : '发送附件失败'
      throw error
    } finally {
      sending.value = false
    }
  }

  const stickerPacks = ref<StickerPackItem[]>([])
  const myStickers = ref<UserStickerItem[]>([])
  const uploadingSticker = ref(false)

  async function fetchStickerPacks() {
    try {
      stickerPacks.value = await getStickerPacks()
    } catch (error) {
      lastError.value = error instanceof ApiError ? error.message : '加载表情包失败'
      stickerPacks.value = []
    }
    return stickerPacks.value
  }

  function listStickerPacks(): StickerPackItem[] {
    return stickerPacks.value
  }

  async function fetchMyStickers() {
    try {
      myStickers.value = await getMyStickers()
    } catch (error) {
      lastError.value = error instanceof ApiError ? error.message : '加载自定义表情失败'
      myStickers.value = []
    }
    return myStickers.value
  }

  async function uploadMySticker(file: File) {
    const mime = (file.type || '').toLowerCase()
    if (!mime.startsWith('image/')) {
      throw new Error('仅支持图片或动图')
    }
    if (file.size > 2 * 1024 * 1024) {
      throw new Error('表情文件不能超过 2MB')
    }
    uploadingSticker.value = true
    try {
      const fileId = await uploadPrivateFile(file)
      const item = await addMySticker({ fileId, name: file.name })
      await fetchMyStickers()
      return item
    } finally {
      uploadingSticker.value = false
    }
  }

  async function collectStickerFromMessage(messageId: string) {
    const item = await addMyStickerFromMessage(messageId)
    await fetchMyStickers()
    return item
  }

  async function removeMyStickers(ids: string[]) {
    await deleteMyStickers(ids)
    await fetchMyStickers()
  }

  async function reorderStickers(ids: string[]) {
    await reorderMyStickers(ids)
    await fetchMyStickers()
  }

  async function toggleReaction(messageId: string, emoji: string) {
    const normalized = emoji.trim()
    if (!normalized) {
      return
    }
    const target = messages.value.find(item => item.id === messageId)
    if (!target || target.recalled) {
      lastError.value = '无法回应此消息'
      return
    }
    try {
      const result = await toggleMessageReaction(messageId, normalized)
      applyReactionUpdated({
        conversationId: result.conversationId,
        messageId: result.messageId,
        reactions: result.reactions
      })
    } catch (error) {
      lastError.value = error instanceof ApiError ? error.message : '表情回应失败'
    }
  }

  function applyReactionUpdated(payload: {
    conversationId: string
    messageId: string
    reactions: MessageReactionAgg[]
  }) {
    const { conversationId, messageId, reactions } = payload
    const apply = (list: MessageViewItem[]) =>
      list.map(item => (item.id === messageId ? { ...item, reactions } : item))
    if (conversationId === activeConversationId.value) {
      replaceActiveMessages(apply)
      putMessageCache(conversationId, messages.value)
      return
    }
    const cached = messagesByConversation.value[conversationId]
    if (cached) {
      messagesByConversation.value = {
        ...messagesByConversation.value,
        [conversationId]: apply(cached)
      }
    }
  }

  async function forwardMessage(messageId: string, targetConversationId: string) {
    try {
      const created = await forwardMessageRequest({
        messageId,
        targetConversationId,
        clientMsgId: crypto.randomUUID()
      })
      const view = normalizeIncomingMessage(created)
      const cached = messagesByConversation.value[targetConversationId] ?? []
      if (!cached.some(item => item.id === view.id)) {
        const nextCached = [...cached, view].sort((a, b) => a.seq - b.seq)
        messagesByConversation.value = {
          ...messagesByConversation.value,
          [targetConversationId]: nextCached
        }
        if (activeConversationId.value === targetConversationId) {
          messages.value = nextCached
        }
      }
      const preview = messagePreviewFromContent(view.type, view.content)
      const conv = conversations.value.find(item => item.id === targetConversationId)
      if (conv) {
        conv.lastMsgPreview = preview.startsWith('[') ? preview : `[转发] ${preview}`
        conv.lastMsgAt = view.createTime
      }
    } catch (error) {
      lastError.value = error instanceof ApiError ? error.message : '转发失败'
      throw error
    }
  }

  async function sendSticker(stickerId: string) {
    const conversationId = activeConversationId.value
    if (!conversationId || conversationId.startsWith('pending-')) {
      throw new Error('请先选择会话')
    }
    sending.value = true
    lastError.value = null
    const clientMsgId = crypto.randomUUID()
    try {
      const result = await sendMessage({
        conversationId,
        clientMsgId,
        type: 'sticker',
        content: {
          version: 1,
          blocks: [{ type: 'sticker', stickerId }]
        }
      })
      const sticker = stickerPacks.value.flatMap(pack => pack.stickers).find(item => item.id === stickerId)
      const msg: MessageViewItem = {
        id: result.id,
        conversationId: result.conversationId,
        senderId: currentUserId(),
        senderNickname: '我',
        senderType: 'user',
        type: 'sticker',
        content: {
          version: 1,
          blocks: [{
            type: 'sticker',
            stickerId,
            packId: sticker?.packId,
            downloadUrl: sticker?.downloadUrl,
            emoji: sticker?.emoji,
            text: sticker?.emoji
          }]
        },
        clientMsgId: result.clientMsgId,
        seq: result.seq,
        createTime: result.createTime,
        localStatus: 'sent',
        reactions: []
      }
      const exists = messages.value.some(item => item.id === msg.id || item.clientMsgId === clientMsgId)
      if (!exists) {
        replaceActiveMessages(prev => [...prev, msg])
      } else {
        replaceActiveMessages(prev => prev.map(item =>
          item.clientMsgId === clientMsgId || item.id === msg.id ? { ...item, ...msg } : item))
      }
      putMessageCache(conversationId, messages.value)
      const conv = conversations.value.find(item => item.id === conversationId)
      if (conv) {
        conv.lastMsgPreview = '[表情]'
        conv.lastMsgAt = msg.createTime
      }
    } catch (error) {
      lastError.value = error instanceof ApiError ? error.message : '发送表情失败'
      throw error
    } finally {
      sending.value = false
    }
  }

  async function sendUserSticker(userStickerId: string) {
    const conversationId = activeConversationId.value
    if (!conversationId || conversationId.startsWith('pending-')) {
      throw new Error('请先选择会话')
    }
    sending.value = true
    lastError.value = null
    const clientMsgId = crypto.randomUUID()
    const sticker = myStickers.value.find(item => item.id === userStickerId)
    try {
      const result = await sendMessage({
        conversationId,
        clientMsgId,
        type: 'sticker',
        content: {
          version: 1,
          blocks: [{ type: 'sticker', userStickerId }]
        }
      })
      const msg: MessageViewItem = {
        id: result.id,
        conversationId: result.conversationId,
        senderId: currentUserId(),
        senderNickname: '我',
        senderType: 'user',
        type: 'sticker',
        content: {
          version: 1,
          blocks: [{
            type: 'sticker',
            userStickerId,
            fileId: sticker?.fileId,
            downloadUrl: sticker?.downloadUrl,
            text: sticker?.name
          }]
        },
        clientMsgId: result.clientMsgId,
        seq: result.seq,
        createTime: result.createTime,
        localStatus: 'sent',
        reactions: []
      }
      const exists = messages.value.some(item => item.id === msg.id || item.clientMsgId === clientMsgId)
      if (!exists) {
        replaceActiveMessages(prev => [...prev, msg])
      } else {
        replaceActiveMessages(prev => prev.map(item =>
          item.clientMsgId === clientMsgId || item.id === msg.id ? { ...item, ...msg } : item))
      }
      putMessageCache(conversationId, messages.value)
      const conv = conversations.value.find(item => item.id === conversationId)
      if (conv) {
        conv.lastMsgPreview = '[表情]'
        conv.lastMsgAt = msg.createTime
      }
    } catch (error) {
      lastError.value = error instanceof ApiError ? error.message : '发送表情失败'
      throw error
    } finally {
      sending.value = false
    }
  }

  return {
    conversations,
    messages,
    groupMembers,
    groupInfo,
    effectiveSettings,
    activeConversationId,
    keyword,
    loadingConversations,
    loadingMessages,
    loadingGroupMembers,
    sending,
    lastError,
    activeConversation,
    filteredConversations,
    pinnedConversations,
    unpinnedConversations,
    totalUnreadCount,
    flaggedCount,
    sidebarSelection,
    pendingDirectChat,
    peerReadSeq,
    enterUnreadCount,
    firstUnreadSeq,
    hasMoreBefore,
    hasMoreAfter,
    loadingOlder,
    loadingNewer,
    currentUserId,
    fetchConversations,
    setSidebarSelection,
    togglePin,
    toggleFlag,
    toggleMute,
    markInboxRead,
    markInboxUnread,
    userTags,
    fetchUserTags,
    createUserTag,
    assignTagToConversation,
    createAndAssignTag,
    selectConversation,
    openDirectChat,
    fetchMessages,
    loadOlderMessages,
    loadNewerMessages,
    jumpToLatestWindow,
    advanceReadToMax,
    fetchGroupMembers,
    ensureEffectiveSettings,
    setGroupPublic,
    searchPublicGroups,
    joinPublicGroup,
    leaveActiveGroup,
    exportActiveGroupMembersCsv,
    recallMessage,
    editMessage,
    createGroup,
    inviteGroupMembers,
    fetchGroupBotCatalog,
    addGroupBot,
    removeGroupBot,
    isCurrentUserGroupOwner,
    handleMessageNew,
    handleMessageUpdated,
    applyMessageUpdate,
    handleReadUpdated,
    fetchReadStatus,
    isMessageReadByPeer,
    reportReadUpTo,
    flushConversationRead,
    sendText,
    sendFileMessage,
    stickerPacks,
    myStickers,
    uploadingSticker,
    fetchStickerPacks,
    listStickerPacks,
    fetchMyStickers,
    uploadMySticker,
    collectStickerFromMessage,
    removeMyStickers,
    reorderStickers,
    sendUserSticker,
    toggleReaction,
    applyReactionUpdated,
    forwardMessage,
    sendSticker,
    resetForTenantSwitch,
    formatRelativeTime,
    formatClockTime,
    formatDayLabel,
    formatMessageTimeHeader,
    buildMessageTimeline,
    textFromContent,
    fileBlockFromContent,
    stickerBlockFromContent,
    isImageMessage,
    isFileMessage,
    isStickerMessage,
    messagePreviewFromContent,
    isSystemMessage
  }
})
