<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useNotifyStore } from '../../stores/notify'
import { useAuthStore } from '../../stores/auth'

const notifyStore = useNotifyStore()
const authStore = useAuthStore()

const open = ref(false)

const badgeLabel = computed(() => {
  const count = notifyStore.unreadCount
  if (count <= 0) {
    return undefined
  }
  return count > 99 ? '99+' : String(count)
})

function formatTime(iso: string) {
  return new Date(iso).toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function handleOpen() {
  open.value = true
  await notifyStore.fetchInbox()
}

async function handleMarkRead(itemId: string) {
  await notifyStore.markItemsRead([itemId])
}

  watch(
  () => authStore.isAuthenticated,
  (authenticated) => {
    if (authenticated) {
      void notifyStore.fetchUnreadCount()
    }
  },
  { immediate: true }
)
</script>

<template>
  <UButton
    v-if="authStore.isAuthenticated"
    color="neutral"
    variant="ghost"
    icon="i-lucide-bell"
    size="sm"
    class="relative shrink-0"
    aria-label="通知"
    @click="handleOpen"
  >
    <span
      v-if="badgeLabel"
      class="absolute -right-0.5 -top-0.5 flex min-w-4 items-center justify-center rounded-full bg-error px-1 text-[10px] font-medium leading-4 text-white"
    >
      {{ badgeLabel }}
    </span>
  </UButton>

  <UModal v-model:open="open" title="通知" :ui="{ content: 'max-w-md' }">
    <template #body>
      <div v-if="notifyStore.inboxLoading" class="flex justify-center py-8">
        <UIcon name="i-lucide-loader-circle" class="size-6 animate-spin text-[var(--ws-text-muted)]" />
      </div>
      <UEmpty
        v-else-if="!notifyStore.items.length"
        icon="i-lucide-bell-off"
        title="暂无通知"
        description="企业邀请等消息会显示在这里"
      />
      <ul v-else class="max-h-80 space-y-2 overflow-y-auto">
        <li
          v-for="item in notifyStore.items"
          :key="item.id"
          class="rounded-lg border border-[var(--ws-border-subtle)] px-3 py-2.5"
          :class="item.read ? 'opacity-70' : 'bg-[var(--ws-rail-hover)]/40'"
        >
          <div class="flex items-start justify-between gap-2">
            <div class="min-w-0 flex-1">
              <p class="text-sm font-medium">
                {{ item.title }}
              </p>
              <p class="mt-0.5 text-sm text-[var(--ws-text-muted)]">
                {{ item.body }}
              </p>
              <p class="mt-1 text-xs text-[var(--ws-text-muted)]">
                {{ formatTime(item.createTime) }}
              </p>
            </div>
            <UButton
              v-if="!item.read"
              color="neutral"
              variant="soft"
              size="xs"
              @click="handleMarkRead(item.id)"
            >
              标为已读
            </UButton>
          </div>
        </li>
      </ul>
      <p
        v-if="notifyStore.items.some(item => item.type === 'MEMBER_INVITE')"
        class="mt-3 text-xs text-[var(--ws-text-muted)]"
      >
        已加入的企业可在左上角切换；注册时填写的手机号会自动激活待加入组织。
      </p>
    </template>
  </UModal>
</template>
