<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import ImAuthenticatedImage from '../../../components/workspace/ImAuthenticatedImage.vue'
import ImCreateGroupModal from '../../../components/workspace/ImCreateGroupModal.vue'
import ImInviteMembersModal from '../../../components/workspace/ImInviteMembersModal.vue'
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'
import { downloadAuthenticatedFile } from '../../../api/app/file'
import { useImStore } from '../../../stores/im'
import { usePresenceStore } from '../../../stores/presence'

const im = useImStore()
const presence = usePresenceStore()
const draft = ref('')
const messageListRef = ref<HTMLElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const createGroupOpen = ref(false)
const inviteMembersOpen = ref(false)

const active = computed(() => im.activeConversation)
const isGroupActive = computed(() => active.value?.type === 'group')
const groupMemberIds = computed(() => im.groupMembers.map(member => member.userId))
const directPeerUserId = computed(() =>
  active.value?.type === 'direct' ? active.value.peerUserId : undefined)
const directPeerOnline = computed(() => presence.isOnline(directPeerUserId.value))

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
  if (im.pendingDirectChat || im.activeConversationId) {
    syncPresenceWatch()
    return
  }
  if (im.filteredConversations.length > 0) {
    await im.selectConversation(im.filteredConversations[0].id)
  }
  syncPresenceWatch()
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
  if (!text.trim() || im.sending) {
    return
  }
  draft.value = ''
  try {
    await im.sendText(text)
  } catch {
    draft.value = text
  }
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

function formatMessageTime(iso: string) {
  return new Date(iso).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function conversationSubtitle(thread: { type: string, memberCount?: number }) {
  if (thread.type === 'group') {
    return `群聊 · ${thread.memberCount ?? 0} 人`
  }
  return '单聊'
}

function headerSubtitle() {
  if (!active.value) {
    return ''
  }
  return conversationSubtitle(active.value)
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
  <WorkspaceShell show-aside>
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
            :icon="thread.type === 'group' ? 'i-lucide-users' : undefined"
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
        <UEmpty icon="i-lucide-message-square" title="暂无会话" description="开始与同事发起单聊或建群吧" />
      </div>
    </template>

    <div v-if="active" class="flex h-full flex-col">
      <header class="flex items-center gap-3 border-b border-[var(--ws-border-subtle)] px-5 py-3">
        <UAvatar
          :text="active.avatarText ?? active.title.slice(0, 1)"
          :icon="active.type === 'group' ? 'i-lucide-users' : undefined"
        />
        <div class="min-w-0 flex-1">
          <h1 class="truncate font-semibold">
            {{ active.title }}
          </h1>
          <p class="text-xs text-[var(--ws-text-muted)]">
            {{ headerSubtitle() }}
          </p>
        </div>
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
              v-else
              class="flex"
              :class="isOwnMessage(msg.senderId) ? 'justify-end' : 'justify-start'"
            >
              <div
                class="max-w-[75%] rounded-2xl px-3 py-2 text-sm"
                :class="isOwnMessage(msg.senderId)
                  ? 'bg-primary text-primary-foreground'
                  : 'bg-[var(--ws-input-bar-bg)] text-[var(--ws-text)]'"
              >
                <p
                  v-if="isGroupActive && !isOwnMessage(msg.senderId) && msg.senderNickname"
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
                  {{ im.textFromContent(msg.content) }}
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
        <div class="workspace-input-bar flex items-center gap-2 px-3 py-2.5">
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
            placeholder="发送消息，Enter 发送"
            :disabled="im.sending"
            @keydown="handleKeydown"
          >
          <UButton
            icon="i-lucide-send"
            color="primary"
            square
            size="sm"
            :loading="im.sending"
            :disabled="!draft.trim()"
            @click="handleSend"
          />
        </div>
      </footer>
    </div>

    <div v-else class="flex h-full items-center justify-center p-8">
      <UEmpty icon="i-lucide-message-circle" title="选择会话" description="从左侧列表选择会话开始聊天" />
    </div>

    <template #aside>
      <template v-if="isGroupActive">
        <div class="flex items-center justify-between border-b border-[var(--ws-border-subtle)] px-4 py-3">
          <span class="font-semibold">群成员</span>
          <UButton
            icon="i-lucide-user-plus"
            color="neutral"
            variant="ghost"
            size="xs"
            @click="inviteMembersOpen = true"
          />
        </div>

        <div v-if="im.loadingGroupMembers" class="space-y-2 p-3">
          <USkeleton v-for="i in 5" :key="i" class="h-10 w-full" />
        </div>

        <div v-else-if="im.groupMembers.length" class="flex-1 space-y-1 overflow-y-auto p-2">
          <div
            v-for="member in im.groupMembers"
            :key="member.userId"
            class="flex items-center gap-3 rounded-md px-3 py-2"
          >
            <UAvatar :text="member.avatarText" size="sm" />
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-medium">
                {{ member.nickname }}
              </p>
              <p class="text-xs text-[var(--ws-text-muted)]">
                {{ member.role === 'owner' ? '群主' : '成员' }}
              </p>
            </div>
          </div>
        </div>

        <UEmpty
          v-else
          icon="i-lucide-users"
          title="暂无成员"
          description="邀请同事加入群聊"
          class="p-6"
        />
      </template>

      <template v-else>
        <div class="border-b border-[var(--ws-border-subtle)] px-4 py-3 font-semibold">
          活跃状态
        </div>
        <div v-if="active?.type === 'direct' && directPeerUserId" class="p-4">
          <div class="flex items-center gap-3 rounded-lg bg-[var(--ws-input-bar-bg)] px-3 py-2.5">
            <span
              class="size-2.5 shrink-0 rounded-full"
              :class="directPeerOnline ? 'bg-success' : 'bg-[var(--ws-text-muted)]'"
            />
            <div class="min-w-0">
              <p class="truncate text-sm font-medium">
                {{ active.title }}
              </p>
              <p class="text-xs text-[var(--ws-text-muted)]">
                {{ directPeerOnline ? '在线' : '离线' }}
              </p>
            </div>
          </div>
        </div>
        <div
          v-else-if="isGroupActive && im.groupMembers.length"
          class="space-y-1 overflow-y-auto p-2"
        >
          <div
            v-for="member in im.groupMembers"
            :key="member.userId"
            class="flex items-center gap-2 rounded-md px-3 py-2 text-sm"
          >
            <span
              class="size-2 shrink-0 rounded-full"
              :class="presence.isOnline(member.userId) ? 'bg-success' : 'bg-[var(--ws-text-muted)]'"
            />
            <span class="truncate">{{ member.nickname }}</span>
          </div>
        </div>
        <div v-else class="flex flex-1 flex-col items-center justify-center gap-2 p-6 text-center text-sm text-[var(--ws-text-muted)]">
          <UIcon name="i-lucide-sparkles" class="size-8 opacity-40" />
          <p>选择会话查看在线状态</p>
        </div>
      </template>
    </template>
  </WorkspaceShell>

  <ImCreateGroupModal v-model:open="createGroupOpen" />
  <ImInviteMembersModal
    v-if="active && isGroupActive"
    v-model:open="inviteMembersOpen"
    :conversation-id="active.id"
    :exclude-user-ids="groupMemberIds"
  />
</template>
