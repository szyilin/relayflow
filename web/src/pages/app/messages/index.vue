<script setup lang="ts">
import { computed, ref } from 'vue'
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'

interface WorkspaceThread {
  id: string
  name: string
  preview: string
  time: string
  avatarText: string
  tag?: string
  tagColor?: 'bot' | 'official' | 'external'
  unread?: number
}

const threads = ref<WorkspaceThread[]>([])
const activeId = ref<string>()
const keyword = ref('')

const filtered = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  if (!q) {
    return threads.value
  }
  return threads.value.filter(t =>
    t.name.toLowerCase().includes(q) || t.preview.toLowerCase().includes(q))
})

function tagClass(tag?: WorkspaceThread['tagColor']) {
  if (tag === 'bot') {
    return 'bg-warning/15 text-warning'
  }
  if (tag === 'official') {
    return 'bg-primary/15 text-primary'
  }
  if (tag === 'external') {
    return 'bg-violet-500/15 text-violet-500'
  }
  return ''
}

const active = computed(() => threads.value.find(t => t.id === activeId.value))
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
        <UButton icon="i-lucide-plus" color="neutral" variant="ghost" square size="sm" />
      </div>

      <div class="p-3">
        <UInput
          v-model="keyword"
          placeholder="搜索会话、联系人"
          icon="i-lucide-search"
          class="workspace-search"
        />
      </div>

      <div v-if="filtered.length" class="flex-1 space-y-0.5 overflow-y-auto px-2 pb-3">
        <button
          v-for="thread in filtered"
          :key="thread.id"
          type="button"
          class="workspace-list-item flex w-full gap-3 px-3 py-2.5 text-left"
          :data-active="activeId === thread.id"
          @click="activeId = thread.id"
        >
          <UAvatar :text="thread.avatarText" size="md" />
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2">
              <span class="truncate font-medium">{{ thread.name }}</span>
              <span v-if="thread.tag" class="rounded px-1.5 py-0.5 text-[10px]" :class="tagClass(thread.tagColor)">
                {{ thread.tag }}
              </span>
              <span class="ml-auto shrink-0 text-xs text-[var(--ws-text-muted)]">{{ thread.time }}</span>
            </div>
            <p class="truncate text-sm text-[var(--ws-text-muted)]">
              {{ thread.preview }}
            </p>
          </div>
          <span
            v-if="thread.unread"
            class="mt-1 flex size-5 shrink-0 items-center justify-center rounded-full bg-error text-[10px] text-white"
          >
            {{ thread.unread }}
          </span>
        </button>
      </div>

      <div v-else class="flex flex-1 flex-col items-center justify-center p-6">
        <UEmpty
          icon="i-lucide-message-square"
          title="暂无会话"
          description="IM 聊天将在后续切片接入"
        />
      </div>
    </template>

    <div v-if="active" class="flex h-full flex-col">
      <header class="flex items-center gap-3 border-b border-[var(--ws-border-subtle)] px-5 py-3">
        <UAvatar :text="active.avatarText" />
        <div class="min-w-0 flex-1">
          <h1 class="truncate font-semibold">
            {{ active.name }}
          </h1>
          <p class="text-xs text-[var(--ws-text-muted)]">
            会话详情
          </p>
        </div>
        <UButton icon="i-lucide-phone" color="neutral" variant="ghost" square />
        <UButton icon="i-lucide-video" color="neutral" variant="ghost" square />
        <UButton icon="i-lucide-info" color="neutral" variant="ghost" square />
      </header>

      <div class="flex flex-1 flex-col items-center justify-center gap-3 p-8 text-center">
        <UEmpty
          icon="i-lucide-messages-square"
          title="暂无消息"
          description="消息内容将在 IM 切片接入后展示"
        />
      </div>

      <footer class="border-t border-[var(--ws-border-subtle)] p-4">
        <div class="workspace-input-bar flex items-center gap-2 px-3 py-2.5">
          <UButton icon="i-lucide-plus" color="neutral" variant="ghost" square size="sm" />
          <input
            class="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-[var(--ws-text-muted)]"
            placeholder="发送消息，@ 提及同事…"
            disabled
          >
          <UButton icon="i-lucide-smile" color="neutral" variant="ghost" square size="sm" />
          <UButton icon="i-lucide-send" color="primary" square size="sm" disabled />
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
        <p>现在还没有同事在协作</p>
      </div>
    </template>
  </WorkspaceShell>
</template>
