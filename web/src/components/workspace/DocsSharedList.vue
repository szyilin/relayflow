<script setup lang="ts">
import type { DocsSharedWithMeItem } from '../../api/app/docs'
import WorkspaceOwnerChip from './WorkspaceOwnerChip.vue'

const props = withDefaults(defineProps<{
  items: DocsSharedWithMeItem[]
  activeObjectId?: string | null
  embedded?: boolean
}>(), {
  embedded: false
})

const emit = defineEmits<{
  open: [objectId: string]
  leave: [objectId: string]
  star: [objectId: string]
  copyLink: [objectId: string]
  exportMd: [objectId: string]
}>()

function formatTime(value?: string | null) {
  if (!value) {
    return '—'
  }
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) {
    return '—'
  }
  return d.toLocaleString('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function rowActions(item: DocsSharedWithMeItem) {
  return [
    [
      {
        label: '复制链接',
        icon: 'i-lucide-link',
        onSelect: () => emit('copyLink', item.objectId)
      },
      {
        label: item.starred ? '取消星标' : '星标',
        icon: 'i-lucide-star',
        onSelect: () => emit('star', item.objectId)
      },
      {
        label: '导出 Markdown',
        icon: 'i-lucide-download',
        onSelect: () => emit('exportMd', item.objectId)
      }
    ],
    [
      {
        label: '离开分享',
        icon: 'i-lucide-log-out',
        color: 'error' as const,
        onSelect: () => emit('leave', item.objectId)
      }
    ]
  ]
}
</script>

<template>
  <div class="flex h-full min-h-0 flex-col">
    <div
      v-if="!embedded"
      class="flex items-center gap-2 border-b border-[var(--ws-border-subtle)] px-5 py-3"
    >
      <UIcon
        name="i-lucide-users"
        class="size-4 shrink-0 text-[var(--ws-text-muted)]"
      />
      <h2 class="text-base font-semibold">
        与我共享
      </h2>
    </div>
    <div class="min-h-0 flex-1 overflow-y-auto p-3">
      <p
        v-if="!props.items.length"
        class="px-2 py-8 text-center text-sm text-[var(--ws-text-muted)]"
      >
        他人分享给你的文档会出现在这里。
      </p>
      <button
        v-for="item in props.items"
        :key="item.objectId"
        type="button"
        class="workspace-list-item mb-0.5 flex w-full items-center gap-3 px-3 py-2.5 text-left"
        :data-active="props.activeObjectId === item.objectId ? 'true' : undefined"
        @click="emit('open', item.objectId)"
      >
        <UIcon
          name="i-lucide-file-text"
          class="size-4 shrink-0 text-[var(--ws-text-muted)]"
        />
        <div class="min-w-0 flex-1">
          <div class="truncate text-sm font-medium">
            {{ item.title || '未命名文档' }}
          </div>
          <div class="mt-0.5 flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1 text-xs text-[var(--ws-text-muted)]">
            <WorkspaceOwnerChip
              v-if="item.ownerUserId"
              :user-id="item.ownerUserId"
              :display-name="item.ownerDisplayName"
            />
            <span>{{ item.permission === 'EDIT' ? '可编辑' : '可阅读' }}</span>
            <span>{{ formatTime(item.sharedAt) }}</span>
          </div>
        </div>
        <UDropdownMenu :items="rowActions(item)">
          <UButton
            color="neutral"
            variant="ghost"
            size="xs"
            icon="i-lucide-ellipsis"
            square
            @click.stop
          />
        </UDropdownMenu>
      </button>
    </div>
  </div>
</template>
