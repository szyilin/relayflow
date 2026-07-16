<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import ImAddGroupBotModal from '../../../components/workspace/ImAddGroupBotModal.vue'
import ImAuthenticatedImage from '../../../components/workspace/ImAuthenticatedImage.vue'
import ImCreateGroupModal from '../../../components/workspace/ImCreateGroupModal.vue'
import ImInviteMembersModal from '../../../components/workspace/ImInviteMembersModal.vue'
import ImMessageCard from '../../../components/im/ImMessageCard.vue'
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'
import { downloadAuthenticatedFile } from '../../../api/app/file'
import type { MessageItem } from '../../../api/app/im'
import { useImStore } from '../../../stores/im'
import { usePresenceStore } from '../../../stores/presence'
import { useUserPreferenceStore } from '../../../stores/userPreference'

const im = useImStore()
const presence = usePresenceStore()
const preference = useUserPreferenceStore()
const route = useRoute()
const toast = useToast()
const draft = ref('')
const messageListRef = ref<HTMLElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const createGroupOpen = ref(false)
const inviteMembersOpen = ref(false)
const addGroupBotOpen = ref(false)
const groupMembersOpen = ref(false)
const removingBotCode = ref<string | null>(null)
const pendingMentions = ref<import('../../../api/app/im').GroupMemberItem[]>([])

const active = computed(() => im.activeConversation)
const isGroupActive = computed(() => active.value?.type === 'group')
const isBotDmActive = computed(() => active.value?.type === 'bot_dm')
const isGroupOwner = computed(() => im.isCurrentUserGroupOwner())
const groupBotMembers = computed(() =>
  im.groupMembers.filter(member => member.subjectType === 'bot' && member.botCode))
const groupMemberIds = computed(() =>
  im.groupMembers
    .filter(member => member.subjectType === 'user' && member.userId)
    .map(member => member.userId!))
const directPeerUserId = computed(() =>
  active.value?.type === 'direct' ? active.value.peerUserId : undefined)

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
  presence.stopPolling()
})

watch([directPeerUserId, groupMemberIds, isGroupActive], () => {
  syncPresenceWatch()
}, { deep: true })

watch(() => im.messages.length, async () => {
  await nextTick()
  const el = messageListRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
})

async function handleSelect(conversationId: string) {
  await im.selectConversation(conversationId)
}

async function handleSend() {
  const text = draft.value
  if ((!text.trim() && pendingMentions.value.length === 0) || im.sending) {
    return
  }
  const mentions = [...pendingMentions.value]
  draft.value = ''
  pendingMentions.value = []
  try {
    await im.sendText(text, mentions)
  } catch {
    draft.value = text
    pendingMentions.value = mentions
  }
}

function toggleBotMention(bot: import('../../../api/app/im').GroupMemberItem) {
  if (!bot.botCode) {
    return
  }
  const exists = pendingMentions.value.some(item => item.botCode === bot.botCode)
  if (exists) {
    pendingMentions.value = pendingMentions.value.filter(item => item.botCode !== bot.botCode)
    return
  }
  pendingMentions.value = [...pendingMentions.value, bot]
}

function messageBlocks(msg: { content: import('../../../api/app/im').MessageContent }) {
  return msg.content.blocks ?? []
}

function cardBlock(msg: { type: string, content: import('../../../api/app/im').MessageContent }) {
  return msg.content.blocks?.find(block => block.type === 'card')
}

function onCardUpdated(message: MessageItem) {
  im.applyMessageUpdate(message)
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    void handleSend()
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

function formatMessageTime(iso: string) {
  return new Date(iso).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

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
  return conversationSubtitle(active.value)
}

function showSenderNickname(msg: { senderId: string, senderNickname?: string, senderType: string }) {
  if (isOwnMessage(msg.senderId) || !msg.senderNickname) {
    return false
  }
  return isGroupActive.value || isBotDmActive.value || msg.senderType === 'bot'
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
    await im.sendFileMessage(file)
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

async function handleFileDownload(msg: { content: import('../../../api/app/im').MessageContent }) {
  const block = im.fileBlockFromContent(msg.content)
  if (!block?.downloadUrl) {
    return
  }
  try {
    await downloadAuthenticatedFile(block.downloadUrl, block.filename ?? '附件')
  } catch {
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
          <h1 class="truncate font-semibold">
            {{ active.title }}
          </h1>
          <p
            v-if="headerSubtitle()"
            class="text-xs text-[var(--ws-text-muted)]"
          >
            {{ headerSubtitle() }}
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

      <div ref="messageListRef" class="flex-1 space-y-3 overflow-y-auto px-5 py-4">
        <div v-if="im.loadingMessages" class="space-y-3">
          <USkeleton v-for="i in 5" :key="i" class="h-10 w-2/3" />
        </div>

        <template v-else-if="im.messages.length">
          <template v-for="msg in im.messages" :key="msg.id">
            <div
              v-if="im.isSystemMessage(msg)"
              class="flex justify-center py-1"
            >
              <p class="rounded-full bg-[var(--ws-input-bar-bg)] px-3 py-1 text-xs text-[var(--ws-text-muted)]">
                {{ im.textFromContent(msg.content) }}
              </p>
            </div>

            <div
              v-else-if="cardBlock(msg)"
              class="flex"
              :class="messageRowJustify(msg.senderId)"
            >
              <div class="max-w-[75%] space-y-1">
                <p
                  v-if="showSenderNickname(msg)"
                  class="px-1 text-xs font-medium text-[var(--ws-text-muted)]"
                >
                  {{ msg.senderNickname }}
                </p>
                <ImMessageCard
                  :message="msg"
                  :card="cardBlock(msg)!"
                  @updated="onCardUpdated"
                />
                <p class="px-1 text-[10px] text-[var(--ws-text-muted)]">
                  {{ formatMessageTime(msg.createTime) }}
                </p>
              </div>
            </div>

            <div
              v-else
              class="flex"
              :class="messageRowJustify(msg.senderId)"
            >
              <div
                class="ws-msg-bubble max-w-[75%] px-3.5 py-2 text-sm"
                :class="isOwnMessage(msg.senderId)
                  ? 'ws-msg-bubble--out bg-primary text-primary-foreground'
                  : 'ws-msg-bubble--in bg-[var(--ws-input-bar-bg)] text-[var(--ws-text)]'"
              >
                <p
                  v-if="showSenderNickname(msg)"
                  class="mb-1 text-xs font-medium opacity-80"
                >
                  {{ msg.senderNickname }}
                </p>
                <template v-if="im.isFileMessage(msg)">
                  <ImAuthenticatedImage
                    v-if="im.isImageMessage(msg) && fileDownloadUrl(msg)"
                    :src="fileDownloadUrl(msg)"
                    :alt="fileName(msg)"
                  />
                  <button
                    v-else-if="fileDownloadUrl(msg)"
                    type="button"
                    class="flex items-center gap-2 rounded-lg bg-black/10 px-3 py-2 hover:underline"
                    :class="isOwnMessage(msg.senderId) ? 'text-primary-foreground' : 'text-[var(--ws-text)]'"
                    @click="handleFileDownload(msg)"
                  >
                    <UIcon name="i-lucide-file" class="size-5 shrink-0" />
                    <span class="truncate">{{ fileName(msg) }}</span>
                  </button>
                  <p v-else class="whitespace-pre-wrap break-words">
                    {{ im.messagePreviewFromContent(msg.type, msg.content) }}
                  </p>
                </template>
                <p v-else class="whitespace-pre-wrap break-words">
                  <template v-for="(block, idx) in messageBlocks(msg)" :key="idx">
                    <span
                      v-if="block.type === 'mention'"
                      class="mx-0.5 inline font-medium"
                      :class="isOwnMessage(msg.senderId) ? 'underline decoration-white/70' : 'text-primary'"
                    >{{ block.text }}</span>
                    <span v-else-if="block.type === 'text'">{{ block.text }}</span>
                  </template>
                </p>
                <p
                  class="mt-1 text-[10px] opacity-70"
                  :class="isOwnMessage(msg.senderId) ? 'text-right' : 'text-left'"
                >
                  {{ formatMessageTime(msg.createTime) }}
                  <span v-if="msg.localStatus === 'sending'"> · 发送中</span>
                  <span v-else-if="msg.localStatus === 'failed'"> · 失败</span>
                  <span
                    v-else-if="isOwnMessage(msg.senderId) && active?.type === 'direct' && im.isMessageReadByPeer(msg.seq)"
                  >
                    · 已读
                  </span>
                </p>
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

      <footer class="border-t border-[var(--ws-border-subtle)] p-4">
        <input
          ref="fileInputRef"
          type="file"
          class="hidden"
          accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip"
          @change="handleFileSelected"
        >
        <div class="workspace-input-bar flex flex-col gap-2 px-3 py-2.5">
          <div
            v-if="isGroupActive && (groupBotMembers.length || pendingMentions.length)"
            class="flex flex-wrap items-center gap-1.5"
          >
            <UButton
              v-for="bot in groupBotMembers"
              :key="bot.botCode"
              size="xs"
              :color="pendingMentions.some(m => m.botCode === bot.botCode) ? 'primary' : 'neutral'"
              :variant="pendingMentions.some(m => m.botCode === bot.botCode) ? 'soft' : 'ghost'"
              icon="i-lucide-at-sign"
              @click="toggleBotMention(bot)"
            >
              {{ bot.nickname }}
            </UButton>
          </div>
          <div class="flex items-center gap-2">
            <UButton
              icon="i-lucide-paperclip"
              color="neutral"
              variant="ghost"
              square
              size="sm"
              :disabled="im.sending"
              aria-label="发送附件"
              @click="openFilePicker"
            />
            <input
              v-model="draft"
              class="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-[var(--ws-text-muted)]"
              :placeholder="isGroupActive && groupBotMembers.length ? '发送消息，可点上方 @机器人' : '发送消息，Enter 发送'"
              :disabled="im.sending"
              @keydown="handleKeydown"
            >
            <UButton
              icon="i-lucide-send"
              color="primary"
              square
              size="sm"
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
        <div class="mb-3 flex items-center justify-end gap-1">
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

  <ImCreateGroupModal v-model:open="createGroupOpen" />
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
</template>
