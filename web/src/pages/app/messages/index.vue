<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'
import { useImStore } from '../../../stores/im'

const im = useImStore()
const draft = ref('')
const messageListRef = ref<HTMLElement | null>(null)

const active = computed(() => im.activeConversation)

onMounted(async () => {
  await im.fetchConversations()
  if (im.pendingDirectChat || im.activeConversationId) {
    return
  }
  if (im.filteredConversations.length > 0) {
    await im.selectConversation(im.filteredConversations[0].id)
  }
})

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
          <UAvatar :text="thread.avatarText ?? thread.title.slice(0, 1)" size="md" />
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2">
              <span class="truncate font-medium">{{ thread.title }}</span>
              <span class="ml-auto shrink-0 text-xs text-[var(--ws-text-muted)]">
                {{ im.formatRelativeTime(thread.lastMsgAt) }}
              </span>
            </div>
            <p class="truncate text-sm text-[var(--ws-text-muted)]">
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
        <UEmpty icon="i-lucide-message-square" title="暂无会话" description="开始与同事发起单聊吧" />
      </div>
    </template>

    <div v-if="active" class="flex h-full flex-col">
      <header class="flex items-center gap-3 border-b border-[var(--ws-border-subtle)] px-5 py-3">
        <UAvatar :text="active.avatarText ?? active.title.slice(0, 1)" />
        <div class="min-w-0 flex-1">
          <h1 class="truncate font-semibold">
            {{ active.title }}
          </h1>
          <p class="text-xs text-[var(--ws-text-muted)]">
            单聊
          </p>
        </div>
      </header>

      <div ref="messageListRef" class="flex-1 space-y-3 overflow-y-auto px-5 py-4">
        <div v-if="im.loadingMessages" class="space-y-3">
          <USkeleton v-for="i in 5" :key="i" class="h-10 w-2/3" />
        </div>

        <template v-else-if="im.messages.length">
          <div
            v-for="msg in im.messages"
            :key="msg.id"
            class="flex"
            :class="isOwnMessage(msg.senderId) ? 'justify-end' : 'justify-start'"
          >
            <div
              class="max-w-[75%] rounded-2xl px-3 py-2 text-sm"
              :class="isOwnMessage(msg.senderId)
                ? 'bg-primary text-primary-foreground'
                : 'bg-[var(--ws-input-bar-bg)] text-[var(--ws-text)]'"
            >
              <p class="whitespace-pre-wrap break-words">
                {{ im.textFromContent(msg.content) }}
              </p>
              <p
                class="mt-1 text-[10px] opacity-70"
                :class="isOwnMessage(msg.senderId) ? 'text-right' : 'text-left'"
              >
                {{ formatMessageTime(msg.createTime) }}
                <span v-if="msg.localStatus === 'sending'"> · 发送中</span>
                <span v-else-if="msg.localStatus === 'failed'"> · 失败</span>
              </p>
            </div>
          </div>
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
        <div class="workspace-input-bar flex items-center gap-2 px-3 py-2.5">
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
      <div class="border-b border-[var(--ws-border-subtle)] px-4 py-3 font-semibold">
        活跃状态
      </div>
      <div class="flex flex-1 flex-col items-center justify-center gap-2 p-6 text-center text-sm text-[var(--ws-text-muted)]">
        <UIcon name="i-lucide-sparkles" class="size-8 opacity-40" />
        <p>在线状态将在后续切片接入</p>
      </div>
    </template>
  </WorkspaceShell>
</template>
