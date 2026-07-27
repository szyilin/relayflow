<script setup lang="ts">
import type { DocsStarredItem } from '../../api/app/docs'
import WorkspaceOwnerChip from './WorkspaceOwnerChip.vue'
import { resolveAvatarUrl } from '../../utils/avatar'

const props = withDefaults(defineProps<{
  items: DocsStarredItem[]
  activeObjectId?: string | null
  embedded?: boolean
}>(), {
  embedded: false
})

const emit = defineEmits<{
  open: [objectId: string]
  unstar: [objectId: string]
  share: [objectId: string]
  copyLink: [objectId: string]
  exportMd: [objectId: string]
}>()

function rowActions(item: DocsStarredItem) {
  const owned = item.locationLabel === '我的文档库' || item.locationLabel === '云盘'
  const primary = [
    {
      label: '复制链接',
      icon: 'i-lucide-link',
      onSelect: () => emit('copyLink', item.objectId)
    },
    {
      label: '导出 Markdown',
      icon: 'i-lucide-download',
      onSelect: () => emit('exportMd', item.objectId)
    },
    {
      label: '取消星标',
      icon: 'i-lucide-star',
      onSelect: () => emit('unstar', item.objectId)
    }
  ]
  if (owned) {
    primary.unshift({
      label: '分享',
      icon: 'i-lucide-share-2',
      onSelect: () => emit('share', item.objectId)
    })
  }
  return [primary]
}
</script>

<template>
  <div class="flex h-full min-h-0 flex-col">
    <div
      v-if="!embedded"
      class="flex items-center gap-2 border-b border-[var(--ws-border-subtle)] px-5 py-3"
    >
      <UIcon
        name="i-lucide-star"
        class="size-4 shrink-0 text-[var(--ws-text-muted)]"
      />
      <h2 class="text-base font-semibold">
        星标
      </h2>
    </div>
    <div class="min-h-0 flex-1 overflow-y-auto p-3">
      <p
        v-if="!props.items.length"
        class="px-2 py-8 text-center text-sm text-[var(--ws-text-muted)]"
      >
        星标文档会出现在这里。
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
          name="i-lucide-star"
          class="size-4 shrink-0 text-amber-500"
        />
        <div class="min-w-0 flex-1">
          <div class="truncate text-sm font-medium">
            {{ item.title || '未命名文档' }}
          </div>
          <div class="mt-0.5 flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1 text-xs text-[var(--ws-text-muted)]">
            <span class="truncate">{{ item.locationLabel || '—' }}</span>
            <WorkspaceOwnerChip
              v-if="item.ownerUserId"
              :user-id="item.ownerUserId"
              :display-name="item.ownerDisplayName"
              :avatar-url="resolveAvatarUrl(item.ownerAvatarFileId)"
            />
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
