<script setup lang="ts">
import type { DocsRecentItem } from '../../api/app/docs'
import WorkspaceOwnerChip from './WorkspaceOwnerChip.vue'
import WorkspaceEmptyState from './WorkspaceEmptyState.vue'
import { resolveAvatarUrl } from '../../utils/avatar'

const props = withDefaults(defineProps<{
  items: DocsRecentItem[]
  activeObjectId?: string | null
  /** Hide page title when embedded under Home tabs */
  embedded?: boolean
  allowShare?: boolean
  allowExportMd?: boolean
}>(), {
  embedded: false,
  allowShare: true,
  allowExportMd: true
})

const emit = defineEmits<{
  open: [objectId: string]
  remove: [objectId: string]
  deleteOwned: [objectId: string]
  share: [objectId: string]
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

function stateLabel(item: DocsRecentItem) {
  if (item.accessState === 'DELETED') {
    return '已删除'
  }
  if (item.accessState === 'NO_ACCESS') {
    return '无权限'
  }
  return ''
}

function rowActions(item: DocsRecentItem) {
  const ok = !item.accessState || item.accessState === 'OK'
  const groups: Array<Array<{
    label: string
    icon?: string
    color?: 'error'
    onSelect: () => void
  }>> = []

  if (ok) {
    const primary: Array<{ label: string, icon?: string, onSelect: () => void }> = []
    if (item.ownedByMe && props.allowShare) {
      primary.push({
        label: '分享',
        icon: 'i-lucide-share-2',
        onSelect: () => emit('share', item.objectId)
      })
    }
    primary.push({
      label: '复制链接',
      icon: 'i-lucide-link',
      onSelect: () => emit('copyLink', item.objectId)
    })
    if (props.allowExportMd) {
      primary.push({
        label: '导出 Markdown',
        icon: 'i-lucide-download',
        onSelect: () => emit('exportMd', item.objectId)
      })
    }
    primary.push({
      label: item.starred ? '取消星标' : '星标',
      icon: 'i-lucide-star',
      onSelect: () => emit('star', item.objectId)
    })
    groups.push(primary)
  } else {
    groups.push([
      {
        label: '复制链接',
        icon: 'i-lucide-link',
        onSelect: () => emit('copyLink', item.objectId)
      }
    ])
  }

  const danger: Array<{ label: string, icon?: string, color?: 'error', onSelect: () => void }> = [
    {
      label: '从「最近」中移除',
      icon: 'i-lucide-eye-off',
      onSelect: () => emit('remove', item.objectId)
    }
  ]
  if (ok && item.ownedByMe) {
    danger.push({
      label: '删除…',
      icon: 'i-lucide-trash-2',
      color: 'error',
      onSelect: () => emit('deleteOwned', item.objectId)
    })
  }
  groups.push(danger)
  return groups
}
</script>

<template>
  <div class="flex h-full min-h-0 flex-col">
    <div
      v-if="!embedded"
      class="flex items-center gap-2 border-b border-[var(--ws-border-subtle)] px-5 py-3"
    >
      <UIcon
        name="i-lucide-clock"
        class="size-4 shrink-0 text-[var(--ws-text-muted)]"
      />
      <h2 class="text-base font-semibold">
        最近
      </h2>
    </div>
    <div class="min-h-0 flex-1 overflow-x-auto overflow-y-auto p-3">
      <WorkspaceEmptyState
        v-if="!props.items.length"
        icon="i-lucide-clock"
        title="暂无最近访问"
        description="打开或编辑过的文档会显示在这里。"
      />
      <template v-else>
        <div
          class="mb-1 hidden grid-cols-[minmax(10rem,1.4fr)_7rem_6rem_7rem_7rem_2rem] gap-2 px-3 text-[11px] font-medium text-[var(--ws-text-muted)] md:grid"
        >
          <span>标题</span>
          <span>位置</span>
          <span>所有者</span>
          <span>创建时间</span>
          <span>最近访问</span>
          <span />
        </div>
        <div
          v-for="item in props.items"
          :key="item.objectId"
          class="workspace-list-item mb-0.5 grid w-full grid-cols-1 items-center gap-1 px-3 py-2.5 text-left md:grid-cols-[minmax(10rem,1.4fr)_7rem_6rem_7rem_7rem_2rem] md:gap-2"
          :data-active="props.activeObjectId === item.objectId ? 'true' : undefined"
          :class="{ 'opacity-60': item.accessState && item.accessState !== 'OK' }"
        >
          <button
            type="button"
            class="flex min-w-0 items-center gap-2 text-left"
            @click="emit('open', item.objectId)"
          >
            <UIcon
              name="i-lucide-file-text"
              class="size-4 shrink-0 text-[var(--ws-text-muted)]"
            />
            <span class="min-w-0 flex-1 truncate text-sm font-medium">
              {{ item.titleSnapshot || item.title || '未命名文档' }}
            </span>
            <span
              v-if="stateLabel(item)"
              class="shrink-0 rounded bg-[var(--ws-bg-muted)] px-1.5 py-0.5 text-[10px] text-[var(--ws-text-muted)]"
            >{{ stateLabel(item) }}</span>
          </button>
          <button
            type="button"
            class="hidden truncate text-xs text-[var(--ws-text-muted)] md:block md:text-left"
            @click="emit('open', item.objectId)"
          >
            {{ item.locationLabel || '—' }}
          </button>
          <div
            class="hidden min-w-0 md:block"
            @click.stop
          >
            <WorkspaceOwnerChip
              v-if="item.ownerUserId"
              :user-id="item.ownerUserId"
              :display-name="item.ownerDisplayName"
              :avatar-url="resolveAvatarUrl(item.ownerAvatarFileId)"
            />
            <span
              v-else
              class="text-xs text-[var(--ws-text-muted)]"
            >—</span>
          </div>
          <button
            type="button"
            class="hidden truncate text-xs text-[var(--ws-text-muted)] md:block md:text-left"
            @click="emit('open', item.objectId)"
          >
            {{ formatTime(item.createTime) }}
          </button>
          <button
            type="button"
            class="hidden truncate text-xs text-[var(--ws-text-muted)] md:block md:text-left"
            @click="emit('open', item.objectId)"
          >
            {{ formatTime(item.lastOpenedAt) }}
          </button>
          <div class="flex justify-end">
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
          </div>
        </div>
      </template>
    </div>
  </div>
</template>
