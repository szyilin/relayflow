<script setup lang="ts">
import type { ContactRequestItem } from '../../../stores/contacts'
import { resolveAvatarUrl } from '../../../utils/avatar'
import WorkspaceAvatar from '../WorkspaceAvatar.vue'

defineProps<{
  requests: ContactRequestItem[]
  loading?: boolean
}>()

const emit = defineEmits<{
  accept: [requestId: string]
  reject: [requestId: string]
}>()

function statusLabel(status: ContactRequestItem['status']) {
  if (status === 'accepted') {
    return '已同意'
  }
  if (status === 'rejected') {
    return '已拒绝'
  }
  return '待处理'
}
</script>

<template>
  <div class="flex h-full flex-col">
    <div class="border-b border-[var(--ws-border-subtle)] px-5 py-4">
      <h2 class="font-semibold">
        新的联系人
      </h2>
      <p class="mt-1 text-sm text-[var(--ws-text-muted)]">
        好友添加请求与处理状态
      </p>
    </div>

    <div class="min-h-0 flex-1 overflow-y-auto p-3">
      <div
        v-if="loading"
        class="space-y-2 p-3"
      >
        <USkeleton class="h-12 w-full" />
        <USkeleton class="h-12 w-full" />
      </div>
      <div
        v-else-if="requests.length"
        class="space-y-0.5"
      >
        <div
          v-for="item in requests"
          :key="item.id"
          class="workspace-list-item flex w-full items-center gap-3 px-3 py-2.5"
        >
          <WorkspaceAvatar
            :src="resolveAvatarUrl(item.avatar)"
            :text="item.avatarText"
            :seed="item.fromUserId || item.id"
            size="md"
          />
          <div class="min-w-0 flex-1">
            <p class="truncate font-medium">
              {{ item.displayName }}
            </p>
            <p class="truncate text-sm text-[var(--ws-text-muted)]">
              {{ item.subtitle }}
            </p>
          </div>
          <div
            v-if="item.status === 'pending'"
            class="flex shrink-0 items-center gap-1.5"
          >
            <UButton
              size="sm"
              color="neutral"
              variant="ghost"
              @click="emit('reject', item.id)"
            >
              拒绝
            </UButton>
            <UButton
              size="sm"
              color="primary"
              variant="soft"
              @click="emit('accept', item.id)"
            >
              同意
            </UButton>
          </div>
          <span
            v-else
            class="shrink-0 text-sm text-[var(--ws-text-muted)]"
          >
            {{ statusLabel(item.status) }}
          </span>
        </div>
      </div>

      <UEmpty
        v-else
        icon="i-lucide-user-plus"
        title="暂无新的联系人"
        description="收到添加请求时会显示在这里"
        class="py-12"
      />
    </div>
  </div>
</template>
