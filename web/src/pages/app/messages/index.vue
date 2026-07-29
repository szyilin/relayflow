<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { DropdownMenuItem } from '@nuxt/ui'
import ImAddGroupBotModal from '../../../components/workspace/ImAddGroupBotModal.vue'
import ImAuthenticatedImage from '../../../components/workspace/ImAuthenticatedImage.vue'
import ImMediaLightbox from '../../../components/im/ImMediaLightbox.vue'
import ImMessageContextMenu from '../../../components/im/ImMessageContextMenu.vue'
import type { ImMessageMenuAction } from '../../../components/im/ImMessageContextMenu.vue'
import WorkspaceCreateGroupModal from '../../../components/workspace/WorkspaceCreateGroupModal.vue'
import ImInviteMembersModal from '../../../components/workspace/ImInviteMembersModal.vue'
import ImMentionPicker from '../../../components/workspace/ImMentionPicker.vue'
import ImMessageCard from '../../../components/im/ImMessageCard.vue'
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'
import { downloadAuthenticatedFile } from '../../../api/app/file'
import type { ContentBlock, GroupMemberItem, MessageItem, PublicGroupSearchItem } from '../../../api/app/im'
import { useMessageConversationRoute } from '../../../composables/im/useMessageConversationRoute'
import { sendPresenceTyping } from '../../../composables/useImWebSocket'
import { useImStore } from '../../../stores/im'
import { usePresenceStore } from '../../../stores/presence'
import { useTypingStore } from '../../../stores/typing'
import { useUserPreferenceStore } from '../../../stores/userPreference'

const im = useImStore()
const presence = usePresenceStore()
const typing = useTypingStore()
const preference = useUserPreferenceStore()
const route = useRoute()
const router = useRouter()
const { selectConversation: selectConversationWithRoute } = useMessageConversationRoute(im)
const toast = useToast()
const draft = ref('')
const messageListRef = ref<HTMLElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const draftInputRef = ref<HTMLTextAreaElement | null>(null)
const createGroupOpen = ref(false)
const publicGroupSearchOpen = ref(false)
const publicGroupKeyword = ref('')
const publicGroupResults = ref<PublicGroupSearchItem[]>([])
const publicGroupSearching = ref(false)
const joiningGroupId = ref<string | null>(null)
const inviteMembersOpen = ref(false)
const addGroupBotOpen = ref(false)
const groupMembersOpen = ref(false)
const groupSearchableUpdating = ref(false)
const exportingMembers = ref(false)
const leavingGroup = ref(false)
const editMessageOpen = ref(false)
const editMessageId = ref<string | null>(null)
const editMessageText = ref('')
const editMessageSubmitting = ref(false)
const removingBotCode = ref<string | null>(null)
const pendingMentions = ref<GroupMemberItem[]>([])
const pendingReplyTo = ref<MessageItem | null>(null)
const contextMenu = ref<{ open: boolean, x: number, y: number, items: ImMessageMenuAction[] }>({
  open: false,
  x: 0,
  y: 0,
  items: []
})
const mediaLightbox = ref<{
  open: boolean
  kind: 'image' | 'pdf' | null
  src?: string
  title?: string
  downloadUrl?: string
  filename?: string
}>({ open: false, kind: null })
const highlightMsgId = ref<string | null>(null)
let highlightTimer: ReturnType<typeof setTimeout> | null = null
const mentionPickerOpen = ref(false)
const mentionQuery = ref('')
const mentionActiveIndex = ref(0)
const mentionTriggerStart = ref<number | null>(null)
const isNearBottom = ref(true)
const unreadFabDismissed = ref(false)
let readReportTimer: ReturnType<typeof setTimeout> | null = null
let typingStartTimer: ReturnType<typeof setTimeout> | null = null
let typingActive = false

const TYPING_DEBOUNCE_MS = 300
/** IME composition (e.g. Chinese Pinyin) — v-model stays stale until compositionend. */
const isComposing = ref(false)

function clearTypingStartTimer() {
  if (typingStartTimer) {
    clearTimeout(typingStartTimer)
    typingStartTimer = null
  }
}

function hasTypingActivity() {
  return isComposing.value || Boolean(draft.value.trim())
}

function stopTypingSignal() {
  clearTypingStartTimer()
  const conversationId = im.activeConversationId
  if (!conversationId || !typingActive || isBotDmActive.value) {
    typingActive = false
    return
  }
  typingActive = false
  sendPresenceTyping(conversationId, false)
}

function scheduleTypingStart() {
  if (isBotDmActive.value || !im.activeConversationId) {
    return
  }
  if (!hasTypingActivity()) {
    stopTypingSignal()
    return
  }
  clearTypingStartTimer()
  typingStartTimer = setTimeout(() => {
    typingStartTimer = null
    const conversationId = im.activeConversationId
    if (!conversationId || !hasTypingActivity() || isBotDmActive.value) {
      return
    }
    typingActive = true
    sendPresenceTyping(conversationId, true)
  }, TYPING_DEBOUNCE_MS)
}

const NEAR_BOTTOM_PX = 80

function isListNearBottom(el: HTMLElement) {
  return el.scrollHeight - el.scrollTop - el.clientHeight <= NEAR_BOTTOM_PX
}

async function scrollToBottom(force = false) {
  await nextTick()
  const el = messageListRef.value
  if (!el) {
    return
  }
  if (!force && !isNearBottom.value) {
    return
  }
  el.scrollTop = el.scrollHeight
  isNearBottom.value = true
}

async function scrollToUnread() {
  await nextTick()
  const seq = im.firstUnreadSeq
  if (seq == null) {
    await scrollToBottom(true)
    return
  }
  const el = messageListRef.value
  const target = el?.querySelector(`[data-msg-seq="${seq}"]`) as HTMLElement | null
  if (target) {
    target.scrollIntoView({ block: 'start', behavior: 'smooth' })
    unreadFabDismissed.value = true
    return
  }
  await scrollToBottom(true)
  unreadFabDismissed.value = true
}

function scheduleReadReport(readSeq: number) {
  if (readReportTimer) {
    clearTimeout(readReportTimer)
  }
  readReportTimer = setTimeout(() => {
    void im.reportReadUpTo(readSeq)
  }, 300)
}

function reportVisibleRead() {
  const el = messageListRef.value
  if (!el || !im.activeConversationId) {
    return
  }
  const nodes = el.querySelectorAll<HTMLElement>('[data-msg-seq]')
  let maxVisible = 0
  const viewTop = el.scrollTop
  const viewBottom = viewTop + el.clientHeight
  nodes.forEach((node) => {
    const seq = Number(node.dataset.msgSeq)
    if (!Number.isFinite(seq) || seq <= 0) {
      return
    }
    const top = node.offsetTop
    const bottom = top + node.offsetHeight
    if (bottom > viewTop && top < viewBottom) {
      maxVisible = Math.max(maxVisible, seq)
    }
  })
  if (isNearBottom.value) {
    const last = im.messages.at(-1)?.seq ?? 0
    maxVisible = Math.max(maxVisible, last)
  }
  if (maxVisible > 0) {
    scheduleReadReport(maxVisible)
  }
}

function onMessageListScroll() {
  const el = messageListRef.value
  if (!el) {
    return
  }
  isNearBottom.value = isListNearBottom(el)
  reportVisibleRead()
}

const showUnreadFab = computed(() =>
  Boolean(im.firstUnreadSeq)
  && im.enterUnreadCount > 0
  && !unreadFabDismissed.value
  && !im.loadingMessages)

const showBackToLatestFab = computed(() =>
  Boolean(im.activeConversationId)
  && im.messages.length > 0
  && !isNearBottom.value
  && !im.loadingMessages)

const active = computed(() => im.activeConversation)
const isGroupActive = computed(() => active.value?.type === 'group')
const isBotDmActive = computed(() => active.value?.type === 'bot_dm')
const isGroupOwner = computed(() => im.isCurrentUserGroupOwner())
const isGroupModerator = computed(() => {
  const role = im.groupInfo?.myRole
  return role === 'owner' || role === 'admin'
})
const allowPublicGroup = computed(() => im.effectiveSettings?.allowPublicGroup ?? false)
const canExportMembers = computed(() =>
  im.effectiveSettings?.memberExportPolicy === 'ALLOW_OWNER_ADMIN' && isGroupModerator.value)
const canLeaveGroup = computed(() => {
  if (!isGroupActive.value) {
    return false
  }
  if (im.groupInfo?.groupKind === 'DEPT' && im.effectiveSettings?.deptGroupLeavePolicy === 'DENY') {
    return false
  }
  return true
})
const groupSearchable = computed({
  get: () => im.groupInfo?.searchable ?? false,
  set: (value: boolean) => {
    void handleGroupSearchableChange(value)
  }
})
const mentionableMembers = computed(() => {
  const me = im.currentUserId()
  return im.groupMembers.filter((member) => {
    if (member.subjectType === 'bot') {
      return Boolean(member.botCode)
    }
    return Boolean(member.userId) && member.userId !== me
  })
})
const filteredMentionMembers = computed(() => {
  const q = mentionQuery.value.trim().toLowerCase()
  if (!q) {
    return mentionableMembers.value
  }
  return mentionableMembers.value.filter((member) => {
    const nick = member.nickname.toLowerCase()
    const code = (member.botCode ?? '').toLowerCase()
    return nick.includes(q) || code.includes(q)
  })
})
const groupMemberIds = computed(() =>
  im.groupMembers
    .filter(member => member.subjectType === 'user' && member.userId)
    .map(member => member.userId!))
const directPeerUserId = computed(() =>
  active.value?.type === 'direct' ? active.value.peerUserId : undefined)

function memberMentionKey(member: GroupMemberItem) {
  if (member.subjectType === 'bot') {
    return `bot:${member.botId ?? member.botCode}`
  }
  return `user:${member.userId}`
}

function closeMentionPicker() {
  mentionPickerOpen.value = false
  mentionQuery.value = ''
  mentionTriggerStart.value = null
  mentionActiveIndex.value = 0
}

function detectMentionTrigger(text: string, cursor: number) {
  const before = text.slice(0, cursor)
  const match = before.match(/(?:^|[\s])@([^\s@]*)$/)
  if (!match) {
    return null
  }
  const query = match[1] ?? ''
  const start = before.length - query.length - 1
  return { start, query }
}

function syncMentionPickerFromDraft() {
  if (!isGroupActive.value) {
    closeMentionPicker()
    return
  }
  const input = draftInputRef.value
  const cursor = input?.selectionStart ?? draft.value.length
  const trigger = detectMentionTrigger(draft.value, cursor)
  if (!trigger) {
    if (mentionTriggerStart.value != null) {
      closeMentionPicker()
    }
    return
  }
  mentionPickerOpen.value = true
  mentionTriggerStart.value = trigger.start
  mentionQuery.value = trigger.query
  mentionActiveIndex.value = 0
}

async function openMentionPickerFromButton() {
  if (!isGroupActive.value || im.sending || !active.value) {
    return
  }
  if (im.groupMembers.length === 0 && !im.loadingGroupMembers) {
    await im.fetchGroupMembers(active.value.id)
  }
  const input = draftInputRef.value
  const cursor = input?.selectionStart ?? draft.value.length
  const before = draft.value.slice(0, cursor)
  const after = draft.value.slice(cursor)
  const needsSpace = before.length > 0 && !/\s$/.test(before)
  const insert = `${needsSpace ? ' ' : ''}@`
  draft.value = `${before}${insert}${after}`
  mentionPickerOpen.value = true
  mentionTriggerStart.value = before.length + (needsSpace ? 1 : 0)
  mentionQuery.value = ''
  mentionActiveIndex.value = 0
  void nextTick(() => {
    const el = draftInputRef.value
    if (!el) {
      return
    }
    const pos = mentionTriggerStart.value! + 1
    el.focus()
    el.setSelectionRange(pos, pos)
  })
}

function selectMention(member: GroupMemberItem) {
  const key = memberMentionKey(member)
  if (!pendingMentions.value.some(item => memberMentionKey(item) === key)) {
    pendingMentions.value = [...pendingMentions.value, member]
  }

  const start = mentionTriggerStart.value
  const input = draftInputRef.value
  const cursor = input?.selectionStart ?? draft.value.length
  if (start != null && start >= 0) {
    const before = draft.value.slice(0, start)
    const after = draft.value.slice(cursor)
    const token = `@${member.nickname} `
    draft.value = `${before}${token}${after}`
    void nextTick(() => {
      const el = draftInputRef.value
      if (!el) {
        return
      }
      const pos = before.length + token.length
      el.focus()
      el.setSelectionRange(pos, pos)
    })
  }
  closeMentionPicker()
}

function removePendingMention(member: GroupMemberItem) {
  const key = memberMentionKey(member)
  pendingMentions.value = pendingMentions.value.filter(item => memberMentionKey(item) !== key)
  const token = `@${member.nickname}`
  draft.value = draft.value
    .split(token)
    .join('')
    .replace(/\s{2,}/g, ' ')
    .replace(/^\s+/, '')
}

function memberRowKey(member: { subjectType: string, userId?: string, botId?: string, botCode?: string }) {
  if (member.subjectType === 'bot') {
    return `bot:${member.botId ?? member.botCode}`
  }
  return `user:${member.userId}`
}

function memberRoleLabel(member: { subjectType: string, role: string }) {
  if (member.subjectType === 'bot') {
    return '机器人'
  }
  return member.role === 'owner' ? '群主' : '成员'
}

async function handleRemoveGroupBot(botCode: string) {
  if (!active.value || active.value.type !== 'group') {
    return
  }
  removingBotCode.value = botCode
  try {
    await im.removeGroupBot(active.value.id, botCode)
    toast.add({ title: '已移除机器人', color: 'success' })
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '移除失败',
      color: 'error'
    })
  } finally {
    removingBotCode.value = null
  }
}

function conversationAvatarIcon(type: string) {
  if (type === 'group') {
    return 'i-lucide-users'
  }
  if (type === 'bot_dm') {
    return 'i-lucide-bot'
  }
  return undefined
}

function syncPresenceWatch() {
  const userIds: string[] = []
  if (directPeerUserId.value) {
    userIds.push(directPeerUserId.value)
  }
  if (isGroupActive.value) {
    userIds.push(...groupMemberIds.value)
  }
  presence.startPolling(userIds)
}

onMounted(async () => {
  void im.ensureEffectiveSettings().catch(() => {})
  await im.fetchConversations()
  const conversationId = typeof route.query.conversationId === 'string'
    ? route.query.conversationId
    : undefined
  if (conversationId) {
    await im.selectConversation(conversationId)
    syncPresenceWatch()
    return
  }
  if (im.pendingDirectChat || im.activeConversationId) {
    syncPresenceWatch()
    return
  }
  if (im.filteredConversations.length > 0) {
    await im.selectConversation(im.filteredConversations[0].id)
  }
  syncPresenceWatch()
})

watch(() => route.query.conversationId, async (raw) => {
  const conversationId = typeof raw === 'string' ? raw : undefined
  if (conversationId) {
    await im.selectConversation(conversationId)
  }
})

onUnmounted(() => {
  if (readReportTimer) {
    clearTimeout(readReportTimer)
  }
  void im.flushConversationRead()
  presence.stopPolling()
})

watch([directPeerUserId, groupMemberIds, isGroupActive], () => {
  syncPresenceWatch()
}, { deep: true })

watch(
  () => [im.activeConversationId, im.messages.length, im.loadingMessages] as const,
  async ([conversationId, length, loading], prev) => {
    if (!conversationId || loading || length === 0) {
      return
    }
    const switched = !prev || prev[0] !== conversationId
    if (switched) {
      unreadFabDismissed.value = false
      isNearBottom.value = true
      await scrollToBottom(true)
      reportVisibleRead()
      return
    }
    if (isNearBottom.value) {
      await scrollToBottom(true)
    }
  }
)

async function handleSelect(conversationId: string) {
  pendingReplyTo.value = null
  await selectConversationWithRoute(conversationId)
}

function focusDraftInput() {
  nextTick(() => {
    draftInputRef.value?.focus()
  })
}

async function handleSend() {
  const text = draft.value
  if ((!text.trim() && pendingMentions.value.length === 0) || im.sending) {
    return
  }
  stopTypingSignal()
  const mentions = [...pendingMentions.value]
  const replyTo = pendingReplyTo.value
  draft.value = ''
  pendingMentions.value = []
  pendingReplyTo.value = null
  closeMentionPicker()
  try {
    isNearBottom.value = true
    await im.sendText(text, mentions, replyTo?.id)
    await scrollToBottom(true)
  } catch {
    draft.value = text
    pendingMentions.value = mentions
    pendingReplyTo.value = replyTo
  } finally {
    focusDraftInput()
  }
}

function messageBlocks(msg: { content: import('../../../api/app/im').MessageContent }) {
  return msg.content.blocks ?? []
}

function cardBlock(msg: { type: string, content: import('../../../api/app/im').MessageContent }) {
  return msg.content.blocks?.find(block => block.type === 'card')
}

function deeplinkBlock(msg: { content: import('../../../api/app/im').MessageContent }): ContentBlock | undefined {
  return msg.content.blocks?.find(block => block.type === 'deeplink' && !!block.route?.trim())
}

function deeplinkLabel(block: ContentBlock): string {
  if (block.entityType === 'task') {
    return '查看任务'
  }
  if (block.entityType === 'calendar' || block.entityType === 'event') {
    return '查看日程'
  }
  return '查看详情'
}

async function openDeeplink(targetRoute?: string) {
  const path = targetRoute?.trim()
  if (!path) {
    return
  }
  if (path.startsWith('http://') || path.startsWith('https://')) {
    window.open(path, '_blank', 'noopener,noreferrer')
    return
  }
  await router.push(path)
}

function onCardUpdated(message: MessageItem) {
  im.applyMessageUpdate(message)
}

function handleKeydown(event: KeyboardEvent) {
  // Let IME own keys while composing (Enter confirms candidates, arrows move them).
  if (event.isComposing || isComposing.value) {
    return
  }
  if (mentionPickerOpen.value && isGroupActive.value) {
    const list = filteredMentionMembers.value
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      if (list.length === 0) {
        return
      }
      mentionActiveIndex.value = (mentionActiveIndex.value + 1) % list.length
      return
    }
    if (event.key === 'ArrowUp') {
      event.preventDefault()
      if (list.length === 0) {
        return
      }
      mentionActiveIndex.value = (mentionActiveIndex.value - 1 + list.length) % list.length
      return
    }
    if (event.key === 'Escape') {
      event.preventDefault()
      closeMentionPicker()
      return
    }
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      const selected = list[mentionActiveIndex.value]
      if (selected) {
        selectMention(selected)
      }
      return
    }
    if (event.key === 'Tab' && list.length > 0) {
      event.preventDefault()
      const selected = list[mentionActiveIndex.value]
      if (selected) {
        selectMention(selected)
      }
      return
    }
  }
  // Enter sends; Shift+Enter inserts newline (textarea default).
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    void handleSend()
  }
}

function handleDraftInput() {
  syncMentionPickerFromDraft()
  scheduleTypingStart()
}

function handleCompositionStart() {
  isComposing.value = true
  scheduleTypingStart()
}

function handleCompositionUpdate() {
  isComposing.value = true
  scheduleTypingStart()
}

function handleCompositionEnd() {
  isComposing.value = false
  syncMentionPickerFromDraft()
  scheduleTypingStart()
}

function handleDraftBlur() {
  isComposing.value = false
  stopTypingSignal()
}

function handleDraftClick() {
  syncMentionPickerFromDraft()
}

watch(() => im.activeConversationId, (conversationId, prevId) => {
  stopTypingSignal()
  if (prevId) {
    typing.clearConversation(prevId)
  }
  pendingMentions.value = []
  pendingReplyTo.value = null
  closeMessageContextMenu()
  closeMediaLightbox()
  closeMentionPicker()
  if (conversationId) {
    void im.ensureEffectiveSettings().catch(() => {})
  }
})

watch(publicGroupSearchOpen, (isOpen) => {
  if (!isOpen) {
    publicGroupKeyword.value = ''
    publicGroupResults.value = []
    publicGroupSearching.value = false
  }
})

let publicGroupSearchTimer: ReturnType<typeof setTimeout> | null = null
watch(publicGroupKeyword, (keyword) => {
  if (publicGroupSearchTimer) {
    clearTimeout(publicGroupSearchTimer)
  }
  const trimmed = keyword.trim()
  if (!trimmed) {
    publicGroupResults.value = []
    publicGroupSearching.value = false
    return
  }
  publicGroupSearching.value = true
  publicGroupSearchTimer = setTimeout(() => {
    void im.searchPublicGroups(trimmed)
      .then(results => { publicGroupResults.value = results })
      .catch(() => { publicGroupResults.value = [] })
      .finally(() => { publicGroupSearching.value = false })
  }, 300)
})

function hoursSince(iso: string): number {
  return (Date.now() - new Date(iso).getTime()) / (1000 * 60 * 60)
}

function isTextMessage(msg: Pick<MessageItem, 'type' | 'content'>) {
  return msg.type === 'text' && !im.isFileMessage(msg)
}

function canRecallMessage(msg: MessageItem): boolean {
  if (msg.recalled || !isTextMessage(msg)) {
    return false
  }
  const settings = im.effectiveSettings
  if (!settings) {
    return false
  }
  const withinHours = hoursSince(msg.createTime) <= settings.recallWithinHours
  if (!withinHours) {
    return false
  }
  if (isOwnMessage(msg.senderId)) {
    return settings.recallEnabled
  }
  return isGroupActive.value && isGroupModerator.value
}

function canEditMessage(msg: MessageItem): boolean {
  if (msg.recalled || !isOwnMessage(msg.senderId) || !isTextMessage(msg)) {
    return false
  }
  const settings = im.effectiveSettings
  if (!settings?.editEnabled) {
    return false
  }
  return hoursSince(msg.createTime) <= settings.editWithinHours
}

function messagePlainText(msg: MessageItem): string {
  if (im.isFileMessage(msg)) {
    return fileName(msg)
  }
  return im.textFromContent(msg.content) || im.messagePreviewFromContent(msg.type, msg.content)
}

function buildMessageActions(msg: MessageItem): ImMessageMenuAction[] {
  if (msg.type === 'system' || msg.recalled) {
    return []
  }
  const actions: ImMessageMenuAction[] = []
  const copyText = messagePlainText(msg).trim()
  if (copyText) {
    actions.push({
      label: '复制',
      icon: 'i-lucide-copy',
      onSelect: () => { void copyMessageText(msg) }
    })
  }
  actions.push({
    label: '引用',
    icon: 'i-lucide-reply',
    onSelect: () => { setPendingReply(msg) }
  })
  if (im.isFileMessage(msg) && fileDownloadUrl(msg)) {
    actions.push({
      label: '下载',
      icon: 'i-lucide-download',
      onSelect: () => { void handleFileDownload(msg) }
    })
  }
  if (canRecallMessage(msg)) {
    actions.push({
      label: '撤回',
      icon: 'i-lucide-undo-2',
      onSelect: () => { void handleRecallMessage(msg.id) }
    })
  }
  if (canEditMessage(msg)) {
    actions.push({
      label: '编辑',
      icon: 'i-lucide-pencil',
      onSelect: () => { openEditMessage(msg) }
    })
  }
  return actions
}

function messageActionItems(msg: MessageItem): DropdownMenuItem[][] {
  const actions = buildMessageActions(msg)
  if (actions.length === 0) {
    return []
  }
  return [actions.map(action => ({
    label: action.label,
    icon: action.icon,
    onSelect: action.onSelect
  }))]
}

function hasMessageActions(msg: MessageItem) {
  return buildMessageActions(msg).length > 0
}

function openMessageContextMenu(event: MouseEvent, msg: MessageItem) {
  const items = buildMessageActions(msg)
  if (items.length === 0) {
    return
  }
  event.preventDefault()
  contextMenu.value = {
    open: true,
    x: event.clientX,
    y: event.clientY,
    items
  }
}

function closeMessageContextMenu() {
  contextMenu.value = { ...contextMenu.value, open: false, items: [] }
}

async function copyMessageText(msg: MessageItem) {
  const text = messagePlainText(msg)
  if (!text) {
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    toast.add({ title: '已复制', color: 'success' })
  } catch {
    toast.add({ title: '复制失败', color: 'error' })
  }
}

function setPendingReply(msg: MessageItem) {
  if (msg.recalled) {
    return
  }
  pendingReplyTo.value = msg
  focusDraftInput()
}

function clearPendingReply() {
  pendingReplyTo.value = null
}

function replyPreviewText(msg: MessageItem): string {
  const preview = im.textFromContent(msg.content) || im.messagePreviewFromContent(msg.type, msg.content)
  return preview.length > 80 ? `${preview.slice(0, 80)}…` : preview
}

function jumpToQuotedMessage(replyTo: { messageId: string }) {
  const targetId = replyTo.messageId
  if (!targetId) {
    return
  }
  const exists = im.messages.some(item => item.id === targetId)
  if (!exists) {
    toast.add({ title: '原消息不在当前列表', color: 'neutral' })
    return
  }
  const el = messageListRef.value?.querySelector(`[data-msg-id="${CSS.escape(targetId)}"]`) as HTMLElement | null
  if (!el) {
    toast.add({ title: '原消息不在当前列表', color: 'neutral' })
    return
  }
  el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  highlightMsgId.value = targetId
  if (highlightTimer) {
    clearTimeout(highlightTimer)
  }
  highlightTimer = setTimeout(() => {
    highlightMsgId.value = null
    highlightTimer = null
  }, 1600)
}

async function handleRecallMessage(messageId: string) {
  try {
    await im.recallMessage(messageId)
    toast.add({ title: '消息已撤回', color: 'success' })
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '撤回失败',
      color: 'error'
    })
  }
}

function openEditMessage(msg: MessageItem) {
  editMessageId.value = msg.id
  editMessageText.value = im.textFromContent(msg.content)
  editMessageOpen.value = true
}

async function submitEditMessage() {
  const messageId = editMessageId.value
  const text = editMessageText.value.trim()
  if (!messageId || !text || editMessageSubmitting.value) {
    return
  }
  editMessageSubmitting.value = true
  try {
    await im.editMessage(messageId, text)
    editMessageOpen.value = false
    toast.add({ title: '消息已更新', color: 'success' })
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '编辑失败',
      color: 'error'
    })
  } finally {
    editMessageSubmitting.value = false
  }
}

async function handleGroupSearchableChange(searchable: boolean) {
  if (!active.value || groupSearchableUpdating.value) {
    return
  }
  groupSearchableUpdating.value = true
  try {
    await im.setGroupPublic(active.value.id, searchable)
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '更新失败',
      color: 'error'
    })
  } finally {
    groupSearchableUpdating.value = false
  }
}

async function handleExportMembers() {
  exportingMembers.value = true
  try {
    await im.exportActiveGroupMembersCsv()
    toast.add({ title: '成员列表已导出', color: 'success' })
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '导出失败',
      color: 'error'
    })
  } finally {
    exportingMembers.value = false
  }
}

async function handleLeaveGroup() {
  if (!canLeaveGroup.value || leavingGroup.value) {
    return
  }
  leavingGroup.value = true
  try {
    await im.leaveActiveGroup()
    groupMembersOpen.value = false
    toast.add({ title: '已退出群聊', color: 'success' })
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '退出失败',
      color: 'error'
    })
  } finally {
    leavingGroup.value = false
  }
}

async function handleJoinPublicGroup(conversationId: string) {
  joiningGroupId.value = conversationId
  try {
    await im.joinPublicGroup(conversationId)
    publicGroupSearchOpen.value = false
    toast.add({ title: '已加入群聊', color: 'success' })
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '加入失败',
      color: 'error'
    })
  } finally {
    joiningGroupId.value = null
  }
}

function isOwnMessage(senderId: string) {
  return senderId === im.currentUserId()
}

function messageRowJustify(senderId: string) {
  if (preference.chatBubbleLayout === 'left') {
    return 'justify-start'
  }
  return isOwnMessage(senderId) ? 'justify-end' : 'justify-start'
}

const messageTimeline = computed(() =>
  im.buildMessageTimeline(im.messages, msg => im.isSystemMessage(msg)))

function conversationSubtitle(thread: { type: string, memberCount?: number, botCode?: string }) {
  if (thread.type === 'group') {
    return `群聊 · ${thread.memberCount ?? 0} 人`
  }
  if (thread.type === 'bot_dm') {
    return '助手'
  }
  return ''
}

function headerSubtitle() {
  if (!active.value) {
    return ''
  }
  const typingHint = typing.typingLabel(im.activeConversationId, {
    selfUserId: im.currentUserId(),
    isDirect: active.value.type === 'direct',
    isBotDm: active.value.type === 'bot_dm',
    isGroup: active.value.type === 'group',
    resolveName: (userId) => {
      const member = im.groupMembers.find(m => m.subjectType === 'user' && m.userId === userId)
      return member?.nickname || active.value?.title || '成员'
    },
    resolveBotName: (botId) => {
      const member = im.groupMembers.find(m => m.subjectType === 'bot' && m.botId === botId)
      return member?.nickname || active.value?.title || '助手'
    }
  })
  if (typingHint) {
    return typingHint
  }
  return conversationSubtitle(active.value)
}

function showSenderNickname(
  msg: { senderId: string, senderNickname?: string, senderType: string },
  isGroupStart: boolean
) {
  if (!isGroupStart || isOwnMessage(msg.senderId) || !msg.senderNickname) {
    return false
  }
  return isGroupActive.value || isBotDmActive.value || msg.senderType === 'bot'
}

function messageStatusSuffix(msg: { seq: number, localStatus?: string, senderId: string }) {
  if (msg.localStatus === 'sending') {
    return '发送中'
  }
  if (msg.localStatus === 'failed') {
    return '失败'
  }
  if (isOwnMessage(msg.senderId) && active.value?.type === 'direct' && im.isMessageReadByPeer(msg.seq)) {
    return '已读'
  }
  return ''
}

function openFilePicker() {
  fileInputRef.value?.click()
}

async function handleFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || im.sending) {
    return
  }
  try {
    isNearBottom.value = true
    await im.sendFileMessage(file)
    await scrollToBottom(true)
  } catch {
    // error surfaced via store.lastError
  }
}

function fileDownloadUrl(msg: { content: import('../../../api/app/im').MessageContent }) {
  return im.fileBlockFromContent(msg.content)?.downloadUrl
}

function fileName(msg: { content: import('../../../api/app/im').MessageContent }) {
  return im.fileBlockFromContent(msg.content)?.filename ?? '附件'
}

function isPdfMessage(msg: Pick<MessageItem, 'content'>) {
  const block = im.fileBlockFromContent(msg.content)
  if (!block) {
    return false
  }
  if (block.mimeType === 'application/pdf') {
    return true
  }
  return (block.filename ?? '').toLowerCase().endsWith('.pdf')
}

function openMediaLightbox(kind: 'image' | 'pdf', msg: Pick<MessageItem, 'content'>) {
  const url = fileDownloadUrl(msg)
  if (!url) {
    return
  }
  mediaLightbox.value = {
    open: true,
    kind,
    src: url,
    title: fileName(msg),
    downloadUrl: url,
    filename: fileName(msg)
  }
}

function closeMediaLightbox() {
  mediaLightbox.value = { open: false, kind: null }
}

async function downloadLightboxMedia() {
  const { downloadUrl, filename } = mediaLightbox.value
  if (!downloadUrl) {
    return
  }
  try {
    await downloadAuthenticatedFile(downloadUrl, filename ?? '附件')
    toast.add({ title: '开始下载', color: 'success' })
  } catch {
    toast.add({ title: '下载失败', color: 'error' })
  }
}

async function openFileAttachment(msg: MessageItem) {
  if (!fileDownloadUrl(msg)) {
    return
  }
  if (im.isImageMessage(msg)) {
    openMediaLightbox('image', msg)
    return
  }
  if (isPdfMessage(msg)) {
    openMediaLightbox('pdf', msg)
    return
  }
  await handleFileDownload(msg)
}

async function handleFileDownload(msg: { content: import('../../../api/app/im').MessageContent }) {
  const block = im.fileBlockFromContent(msg.content)
  if (!block?.downloadUrl) {
    return
  }
  try {
    await downloadAuthenticatedFile(block.downloadUrl, block.filename ?? '附件')
    toast.add({ title: '开始下载', color: 'success' })
  } catch {
    toast.add({ title: '附件下载失败', color: 'error' })
    im.lastError = '附件下载失败'
  }
}
</script>

<route lang="yaml">
meta:
  layout: workspace
</route>

<template>
  <WorkspaceShell>
    <template #panel>
      <div class="flex items-center justify-between border-b border-[var(--ws-border-subtle)] px-4 py-3">
        <h2 class="font-semibold">
          消息
        </h2>
        <div class="flex items-center gap-1">
          <UButton
            v-if="allowPublicGroup"
            icon="i-lucide-search"
            color="neutral"
            variant="ghost"
            size="sm"
            @click="publicGroupSearchOpen = true"
          >
            查找公开群
          </UButton>
          <UButton
            icon="i-lucide-users-round"
            color="neutral"
            variant="ghost"
            size="sm"
            @click="createGroupOpen = true"
          >
            建群
          </UButton>
        </div>
      </div>

      <div class="p-3">
        <UInput
          v-model="im.keyword"
          placeholder="搜索会话、联系人"
          icon="i-lucide-search"
          class="workspace-search"
          @update:model-value="im.fetchConversations()"
        />
      </div>

      <div v-if="im.loadingConversations" class="space-y-2 px-3 pb-3">
        <USkeleton v-for="i in 4" :key="i" class="h-14 w-full" />
      </div>

      <div v-else-if="im.filteredConversations.length" class="flex-1 space-y-0.5 overflow-y-auto px-2 pb-3">
        <button
          v-for="thread in im.filteredConversations"
          :key="thread.id"
          type="button"
          class="workspace-list-item flex w-full gap-3 px-3 py-2.5 text-left"
          :data-active="im.activeConversationId === thread.id"
          @click="handleSelect(thread.id)"
        >
          <UAvatar
            :text="thread.avatarText ?? thread.title.slice(0, 1)"
            :icon="conversationAvatarIcon(thread.type)"
            size="md"
          />
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2">
              <span class="truncate font-medium">{{ thread.title }}</span>
              <span class="ml-auto shrink-0 text-xs text-[var(--ws-text-muted)]">
                {{ im.formatRelativeTime(thread.lastMsgAt) }}
              </span>
            </div>
            <p class="truncate text-sm text-[var(--ws-text-muted)]">
              <span v-if="thread.type === 'group'" class="mr-1 text-xs">[群]</span>
              <span v-else-if="thread.type === 'bot_dm'" class="mr-1 text-xs">[助手]</span>
              <span
                v-if="thread.mentionedMe && thread.unreadCount > 0"
                class="mr-1 text-xs font-medium text-error"
              >[有人@我]</span>
              {{ thread.lastMsgPreview || '暂无消息' }}
            </p>
          </div>
          <span
            v-if="thread.unreadCount > 0"
            class="mt-1 flex size-5 shrink-0 items-center justify-center rounded-full bg-error text-[10px] text-white"
          >
            {{ thread.unreadCount > 9 ? '9+' : thread.unreadCount }}
          </span>
        </button>
      </div>

      <div v-else class="flex flex-1 flex-col items-center justify-center p-6">
        <UEmpty icon="i-lucide-message-square" title="暂无会话" description="开始与同事发起单聊、建群，或查看来自助手的消息" />
      </div>
    </template>

    <div v-if="active" class="flex h-full flex-col">
      <header class="flex items-center gap-3 border-b border-[var(--ws-border-subtle)] px-5 py-3">
        <UAvatar
          :text="active.avatarText ?? active.title.slice(0, 1)"
          :icon="conversationAvatarIcon(active.type)"
        />
        <div class="min-w-0 flex-1">
          <h1 class="truncate font-semibold leading-5">
            {{ active.title }}
          </h1>
          <!-- Always reserve one subtitle line so typing hint does not shift the message list. -->
          <p class="h-4 truncate text-xs leading-4 text-[var(--ws-text-muted)]">
            {{ headerSubtitle() || '\u00A0' }}
          </p>
        </div>
        <UButton
          v-if="isGroupActive"
          icon="i-lucide-users"
          color="neutral"
          variant="soft"
          size="sm"
          @click="groupMembersOpen = true"
        >
          成员
        </UButton>
        <UButton
          v-if="isGroupActive"
          icon="i-lucide-user-plus"
          color="neutral"
          variant="soft"
          size="sm"
          @click="inviteMembersOpen = true"
        >
          邀请
        </UButton>
      </header>

      <div class="relative min-h-0 flex-1">
        <div
          ref="messageListRef"
          class="h-full overflow-y-auto px-5 py-4"
          @scroll.passive="onMessageListScroll"
        >
          <div
            v-if="im.loadingMessages && !im.messages.length"
            class="space-y-3"
          >
            <USkeleton
              v-for="i in 5"
              :key="i"
              class="h-10 w-2/3"
            />
          </div>

          <template v-else-if="messageTimeline.length">
            <div
              v-if="im.loadingMessages"
              class="mb-2 flex justify-center"
            >
              <span class="text-xs text-[var(--ws-text-muted)]">同步中…</span>
            </div>
            <template v-for="item in messageTimeline" :key="item.message.id">
            <div
              v-if="item.showTimeHeader"
              class="flex justify-center py-3"
            >
              <span class="rounded-full bg-[var(--ws-input-bar-bg)] px-3 py-0.5 text-xs text-[var(--ws-text-muted)]">
                {{ item.timeHeader }}
              </span>
            </div>

            <div
              v-if="im.isSystemMessage(item.message)"
              :data-msg-seq="item.message.seq"
              class="flex justify-center py-1"
            >
              <p class="rounded-full bg-[var(--ws-input-bar-bg)] px-3 py-1 text-xs text-[var(--ws-text-muted)]">
                {{ im.textFromContent(item.message.content) }}
              </p>
            </div>

            <div
              v-else-if="cardBlock(item.message)"
              :data-msg-seq="item.message.seq"
              class="ws-msg-row group flex"
              :class="[
                messageRowJustify(item.message.senderId),
                item.isGroupEnd ? 'mb-3' : 'mb-0.5'
              ]"
            >
              <div class="relative max-w-[75%] space-y-1">
                <p
                  v-if="showSenderNickname(item.message, item.isGroupStart)"
                  class="px-1 text-xs font-medium text-[var(--ws-text-muted)]"
                >
                  {{ item.message.senderNickname }}
                </p>
                <ImMessageCard
                  :message="item.message"
                  :card="cardBlock(item.message)!"
                  @updated="onCardUpdated"
                />
                <div
                  v-if="item.isGroupEnd || messageStatusSuffix(item.message)"
                  class="flex items-center gap-1.5 px-1 text-[10px] text-[var(--ws-text-muted)]"
                  :class="isOwnMessage(item.message.senderId) ? 'justify-end' : 'justify-start'"
                >
                  <span v-if="item.isGroupEnd">{{ im.formatClockTime(item.message.createTime) }}</span>
                  <span v-if="messageStatusSuffix(item.message)">· {{ messageStatusSuffix(item.message) }}</span>
                </div>
                <span
                  v-if="!item.isGroupEnd"
                  class="pointer-events-none absolute top-1/2 -translate-y-1/2 text-[10px] text-[var(--ws-text-muted)] opacity-0 transition-opacity group-hover:opacity-100"
                  :class="isOwnMessage(item.message.senderId) ? 'right-full mr-2' : 'left-full ml-2'"
                >
                  {{ im.formatClockTime(item.message.createTime) }}
                </span>
              </div>
            </div>

            <div
              v-else
              :data-msg-id="item.message.id"
              :data-msg-seq="item.message.seq"
              class="ws-msg-row group flex"
              :class="[
                messageRowJustify(item.message.senderId),
                item.isGroupEnd ? 'mb-3' : 'mb-0.5',
                highlightMsgId === item.message.id ? 'ws-msg-row--highlight' : ''
              ]"
            >
              <div class="relative max-w-[75%]">
                <div class="flex items-start gap-1">
                  <div
                    class="ws-msg-bubble min-w-0 flex-1 px-3.5 py-2 text-sm"
                    :class="[
                      item.message.recalled
                        ? 'ws-msg-bubble--in bg-[var(--ws-input-bar-bg)] text-[var(--ws-text-muted)] italic'
                        : isOwnMessage(item.message.senderId)
                          ? 'ws-msg-bubble--out bg-primary text-primary-foreground'
                          : 'ws-msg-bubble--in bg-[var(--ws-input-bar-bg)] text-[var(--ws-text)]',
                      !item.message.recalled && (item.isGroupStart && item.isGroupEnd
                        ? ''
                        : item.isGroupStart
                          ? 'ws-msg-bubble--group-start'
                          : item.isGroupEnd
                            ? 'ws-msg-bubble--group-end'
                            : 'ws-msg-bubble--group-mid')
                    ]"
                    @contextmenu="openMessageContextMenu($event, item.message)"
                  >
                    <p
                      v-if="showSenderNickname(item.message, item.isGroupStart)"
                      class="mb-1 text-xs font-medium opacity-80"
                      :class="item.message.recalled ? 'not-italic' : ''"
                    >
                      {{ item.message.senderNickname }}
                    </p>
                    <button
                      v-if="!item.message.recalled && item.message.replyTo"
                      type="button"
                      class="mb-1.5 w-full rounded-md px-2 py-1 text-left text-xs transition-colors"
                      :class="isOwnMessage(item.message.senderId)
                        ? 'bg-black/15 text-primary-foreground/90 hover:bg-black/25'
                        : 'bg-black/5 text-[var(--ws-text-muted)] hover:bg-black/10'"
                      @click.stop="jumpToQuotedMessage(item.message.replyTo!)"
                    >
                      <p class="truncate font-medium">
                        {{ item.message.replyTo.senderNickname || '消息' }}
                      </p>
                      <p class="line-clamp-2 break-words opacity-90">
                        {{ item.message.replyTo.preview || '…' }}
                      </p>
                    </button>
                    <template v-if="item.message.recalled">
                      <p class="text-sm">
                        消息已撤回
                      </p>
                    </template>
                    <template v-else-if="im.isFileMessage(item.message)">
                      <ImAuthenticatedImage
                        v-if="im.isImageMessage(item.message) && fileDownloadUrl(item.message)"
                        :src="fileDownloadUrl(item.message)"
                        :alt="fileName(item.message)"
                        @preview="openMediaLightbox('image', item.message)"
                      />
                      <button
                        v-else-if="fileDownloadUrl(item.message)"
                        type="button"
                        class="flex items-center gap-2 rounded-lg bg-black/10 px-3 py-2 hover:underline"
                        :class="isOwnMessage(item.message.senderId) ? 'text-primary-foreground' : 'text-[var(--ws-text)]'"
                        @click.stop="openFileAttachment(item.message)"
                      >
                        <UIcon
                          :name="isPdfMessage(item.message) ? 'i-lucide-file-text' : 'i-lucide-file'"
                          class="size-5 shrink-0"
                        />
                        <span class="truncate">{{ fileName(item.message) }}</span>
                        <span
                          v-if="isPdfMessage(item.message)"
                          class="shrink-0 text-[10px] opacity-70"
                        >预览</span>
                      </button>
                      <p v-else class="whitespace-pre-wrap break-words">
                        {{ im.messagePreviewFromContent(item.message.type, item.message.content) }}
                      </p>
                    </template>
                    <p v-else class="whitespace-pre-wrap break-words">
                      <template v-for="(block, idx) in messageBlocks(item.message)" :key="idx">
                        <span
                          v-if="block.type === 'mention'"
                          class="mx-0.5 inline font-medium"
                          :class="isOwnMessage(item.message.senderId) ? 'underline decoration-white/70' : 'text-primary'"
                        >{{ block.text }}</span>
                        <span v-else-if="block.type === 'text'">{{ block.text }}</span>
                      </template>
                    </p>
                    <button
                      v-if="!item.message.recalled && deeplinkBlock(item.message)"
                      type="button"
                      class="mt-1.5 inline-flex items-center gap-1 text-xs font-medium underline-offset-2 hover:underline"
                      :class="isOwnMessage(item.message.senderId)
                        ? 'text-primary-foreground/90'
                        : 'text-primary'"
                      @click.stop="openDeeplink(deeplinkBlock(item.message)?.route)"
                    >
                      <UIcon name="i-lucide-arrow-up-right" class="size-3.5" />
                      {{ deeplinkLabel(deeplinkBlock(item.message)!) }}
                    </button>
                  </div>
                  <UDropdownMenu
                    v-if="hasMessageActions(item.message)"
                    :items="messageActionItems(item.message)"
                  >
                    <UButton
                      color="neutral"
                      variant="ghost"
                      size="xs"
                      icon="i-lucide-ellipsis"
                      square
                      class="shrink-0 opacity-0 transition-opacity group-hover:opacity-60 hover:!opacity-100"
                      title="更多操作"
                      @click.stop
                    />
                  </UDropdownMenu>
                </div>
                <div
                  v-if="item.isGroupEnd || messageStatusSuffix(item.message) || item.message.edited"
                  class="mt-0.5 flex items-center gap-1.5 px-1 text-[10px] text-[var(--ws-text-muted)]"
                  :class="isOwnMessage(item.message.senderId) ? 'justify-end' : 'justify-start'"
                >
                  <span v-if="item.isGroupEnd">{{ im.formatClockTime(item.message.createTime) }}</span>
                  <span
                    v-if="item.message.edited && !item.message.recalled"
                    class="rounded bg-[var(--ws-input-bar-bg)] px-1 py-0.5 text-[10px]"
                  >已编辑</span>
                  <span v-if="messageStatusSuffix(item.message)">· {{ messageStatusSuffix(item.message) }}</span>
                </div>
                <span
                  v-if="!item.isGroupEnd"
                  class="pointer-events-none absolute top-1/2 -translate-y-1/2 text-[10px] text-[var(--ws-text-muted)] opacity-0 transition-opacity group-hover:opacity-100"
                  :class="isOwnMessage(item.message.senderId) ? 'right-full mr-2' : 'left-full ml-2'"
                >
                  {{ im.formatClockTime(item.message.createTime) }}
                </span>
              </div>
            </div>
          </template>
        </template>

        <UEmpty
          v-else
          icon="i-lucide-messages-square"
          title="暂无消息"
          description="发送第一条消息开始对话"
          class="py-12"
        />
        </div>

        <div
          v-if="showUnreadFab || showBackToLatestFab"
          class="pointer-events-none absolute right-4 bottom-4 z-10 flex flex-col items-end gap-2"
        >
          <UButton
            v-if="showUnreadFab"
            class="pointer-events-auto shadow-md"
            color="primary"
            variant="solid"
            size="sm"
            icon="i-lucide-arrow-up"
            @click="scrollToUnread"
          >
            查看未读 · {{ im.enterUnreadCount }}
          </UButton>
          <UButton
            v-if="showBackToLatestFab"
            class="pointer-events-auto shadow-md"
            color="neutral"
            variant="solid"
            size="sm"
            icon="i-lucide-arrow-down"
            @click="scrollToBottom(true)"
          >
            回到最新
          </UButton>
        </div>
      </div>

      <footer class="border-t border-[var(--ws-border-subtle)] p-4">
        <input
          ref="fileInputRef"
          type="file"
          class="hidden"
          accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip"
          @change="handleFileSelected"
        >
        <div class="workspace-input-bar relative flex flex-col gap-2 px-3 py-2.5">
          <ImMentionPicker
            v-if="isGroupActive"
            :open="mentionPickerOpen"
            :members="mentionableMembers"
            :query="mentionQuery"
            :active-index="mentionActiveIndex"
            @update:open="val => val ? (mentionPickerOpen = true) : closeMentionPicker()"
            @update:active-index="mentionActiveIndex = $event"
            @select="selectMention"
          />
          <div
            v-if="pendingReplyTo"
            class="flex items-start gap-2 rounded-md bg-[var(--ws-input-bar-bg)] px-2.5 py-2"
          >
            <div class="min-w-0 flex-1 border-l-2 border-primary pl-2">
              <p class="truncate text-xs font-medium text-[var(--ws-text)]">
                {{ pendingReplyTo.senderNickname || '消息' }}
              </p>
              <p class="line-clamp-2 text-xs text-[var(--ws-text-muted)]">
                {{ replyPreviewText(pendingReplyTo) }}
              </p>
            </div>
            <UButton
              color="neutral"
              variant="ghost"
              size="xs"
              icon="i-lucide-x"
              square
              aria-label="取消引用"
              @click="clearPendingReply"
            />
          </div>
          <div
            v-if="isGroupActive && pendingMentions.length"
            class="flex flex-wrap items-center gap-1.5"
          >
            <UButton
              v-for="member in pendingMentions"
              :key="memberMentionKey(member)"
              size="xs"
              color="primary"
              variant="soft"
              icon="i-lucide-at-sign"
              trailing-icon="i-lucide-x"
              @click="removePendingMention(member)"
            >
              {{ member.nickname }}
            </UButton>
          </div>
          <div class="flex items-end gap-2">
            <UButton
              v-if="isGroupActive"
              icon="i-lucide-at-sign"
              color="neutral"
              variant="ghost"
              square
              size="sm"
              class="mb-0.5"
              :disabled="im.sending || mentionableMembers.length === 0"
              aria-label="提及成员"
              title="提及成员"
              @click="openMentionPickerFromButton"
            />
            <UButton
              icon="i-lucide-paperclip"
              color="neutral"
              variant="ghost"
              square
              size="sm"
              class="mb-0.5"
              :disabled="im.sending"
              aria-label="发送附件"
              @click="openFilePicker"
            />
            <textarea
              ref="draftInputRef"
              v-model="draft"
              rows="1"
              class="max-h-32 min-h-9 min-w-0 flex-1 resize-none bg-transparent py-1.5 text-sm outline-none placeholder:text-[var(--ws-text-muted)]"
              :placeholder="isGroupActive ? '发送消息，Enter 发送，Shift+Enter 换行，@ 提及' : '发送消息，Enter 发送，Shift+Enter 换行'"
              @keydown="handleKeydown"
              @input="handleDraftInput"
              @compositionstart="handleCompositionStart"
              @compositionupdate="handleCompositionUpdate"
              @compositionend="handleCompositionEnd"
              @click="handleDraftClick"
              @keyup="handleDraftInput"
              @blur="handleDraftBlur"
            />
            <UButton
              icon="i-lucide-send"
              color="primary"
              square
              size="sm"
              class="mb-0.5"
              :loading="im.sending"
              :disabled="!draft.trim() && pendingMentions.length === 0"
              @click="handleSend"
            />
          </div>
        </div>
      </footer>
    </div>

    <div v-else class="flex h-full items-center justify-center p-8">
      <UEmpty icon="i-lucide-message-circle" title="选择会话" description="从左侧列表选择会话开始聊天" />
    </div>
  </WorkspaceShell>

  <UModal
    v-if="active && isGroupActive"
    v-model:open="groupMembersOpen"
    title="群成员"
    :ui="{ content: 'max-w-md' }"
  >
    <template #body>
      <div class="flex max-h-[60vh] flex-col">
        <div
          v-if="allowPublicGroup && isGroupModerator"
          class="mb-4 flex items-center justify-between gap-3 rounded-lg border border-[var(--ws-border-subtle)] px-3 py-2.5"
        >
          <div>
            <p class="text-sm font-medium">
              允许被搜索
            </p>
            <p class="text-xs text-[var(--ws-text-muted)]">
              开启后企业成员可通过搜索加入
            </p>
          </div>
          <USwitch
            v-model="groupSearchable"
            :loading="groupSearchableUpdating"
            :disabled="!im.groupInfo"
          />
        </div>

        <div class="mb-3 flex items-center justify-end gap-1">
          <UButton
            v-if="canExportMembers"
            icon="i-lucide-download"
            color="neutral"
            variant="ghost"
            size="xs"
            :loading="exportingMembers"
            title="导出成员 CSV"
            @click="handleExportMembers"
          >
            导出
          </UButton>
          <UButton
            v-if="canLeaveGroup"
            icon="i-lucide-log-out"
            color="error"
            variant="ghost"
            size="xs"
            :loading="leavingGroup"
            title="退出群聊"
            @click="handleLeaveGroup"
          >
            退出
          </UButton>
          <UButton
            v-if="isGroupOwner"
            icon="i-lucide-bot"
            color="neutral"
            variant="ghost"
            size="xs"
            title="添加机器人"
            @click="addGroupBotOpen = true"
          >
            添加机器人
          </UButton>
          <UButton
            icon="i-lucide-user-plus"
            color="neutral"
            variant="ghost"
            size="xs"
            title="邀请成员"
            @click="inviteMembersOpen = true"
          >
            邀请
          </UButton>
        </div>

        <div v-if="im.loadingGroupMembers" class="space-y-2">
          <USkeleton v-for="i in 5" :key="i" class="h-10 w-full" />
        </div>

        <div v-else-if="im.groupMembers.length" class="space-y-1 overflow-y-auto">
          <div
            v-for="member in im.groupMembers"
            :key="memberRowKey(member)"
            class="flex items-center gap-3 rounded-md px-3 py-2"
          >
            <UAvatar
              :text="member.avatarText"
              :icon="member.subjectType === 'bot' ? 'i-lucide-bot' : undefined"
              size="sm"
            />
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-medium">
                {{ member.nickname }}
              </p>
              <p class="text-xs text-[var(--ws-text-muted)]">
                {{ memberRoleLabel(member) }}
              </p>
            </div>
            <UButton
              v-if="isGroupOwner && member.subjectType === 'bot' && member.botCode"
              icon="i-lucide-x"
              color="neutral"
              variant="ghost"
              size="xs"
              :loading="removingBotCode === member.botCode"
              title="移除机器人"
              @click="handleRemoveGroupBot(member.botCode)"
            />
          </div>
        </div>

        <UEmpty
          v-else
          icon="i-lucide-users"
          title="暂无成员"
          description="邀请同事加入群聊"
          class="py-6"
        />
      </div>
    </template>
  </UModal>

  <WorkspaceCreateGroupModal v-model:open="createGroupOpen" />

  <UModal
    v-model:open="publicGroupSearchOpen"
    title="查找公开群"
    description="搜索企业内可被发现的公开群并加入"
    :ui="{ content: 'max-w-md' }"
  >
    <template #body>
      <div class="space-y-4">
        <UInput
          v-model="publicGroupKeyword"
          placeholder="输入群名称关键词"
          icon="i-lucide-search"
          autofocus
        />
        <div v-if="publicGroupSearching" class="space-y-2">
          <USkeleton v-for="i in 3" :key="i" class="h-10 w-full" />
        </div>
        <div v-else-if="publicGroupKeyword.trim() && publicGroupResults.length === 0" class="py-4">
          <UEmpty icon="i-lucide-search-x" title="未找到公开群" />
        </div>
        <div v-else-if="publicGroupResults.length" class="max-h-[40vh] space-y-1 overflow-y-auto">
          <div
            v-for="group in publicGroupResults"
            :key="group.conversationId"
            class="flex items-center gap-3 rounded-md px-3 py-2 hover:bg-[var(--ws-input-bar-bg)]"
          >
            <UAvatar
              :text="group.name.slice(0, 1)"
              icon="i-lucide-users"
              size="sm"
            />
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-medium">
                {{ group.name }}
              </p>
              <p class="text-xs text-[var(--ws-text-muted)]">
                {{ group.memberCount }} 人
              </p>
            </div>
            <UButton
              size="xs"
              :loading="joiningGroupId === group.conversationId"
              @click="handleJoinPublicGroup(group.conversationId)"
            >
              加入
            </UButton>
          </div>
        </div>
        <p
          v-else
          class="text-sm text-[var(--ws-text-muted)]"
        >
          输入关键词开始搜索
        </p>
      </div>
    </template>
  </UModal>

  <UModal
    v-model:open="editMessageOpen"
    title="编辑消息"
    :ui="{ content: 'max-w-md' }"
  >
    <template #body>
      <form
        class="space-y-4"
        @submit.prevent="submitEditMessage"
      >
        <UTextarea
          v-model="editMessageText"
          placeholder="输入新内容"
          :rows="4"
          autofocus
        />
        <div class="flex justify-end gap-2">
          <UButton
            color="neutral"
            variant="ghost"
            @click="editMessageOpen = false"
          >
            取消
          </UButton>
          <UButton
            type="submit"
            :loading="editMessageSubmitting"
            :disabled="!editMessageText.trim()"
          >
            保存
          </UButton>
        </div>
      </form>
    </template>
  </UModal>

  <ImInviteMembersModal
    v-if="active && isGroupActive"
    v-model:open="inviteMembersOpen"
    :conversation-id="active.id"
    :exclude-user-ids="groupMemberIds"
  />
  <ImAddGroupBotModal
    v-if="active && isGroupActive"
    v-model:open="addGroupBotOpen"
    :conversation-id="active.id"
  />

  <ImMessageContextMenu
    :open="contextMenu.open"
    :x="contextMenu.x"
    :y="contextMenu.y"
    :items="contextMenu.items"
    @close="closeMessageContextMenu"
  />

  <ImMediaLightbox
    :open="mediaLightbox.open"
    :kind="mediaLightbox.kind"
    :src="mediaLightbox.src"
    :title="mediaLightbox.title"
    @update:open="val => { if (!val) closeMediaLightbox() }"
    @download="downloadLightboxMedia"
  />
</template>
