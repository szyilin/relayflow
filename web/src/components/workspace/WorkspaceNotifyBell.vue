<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useNotifyStore } from '../../stores/notify'
import { useAuthStore } from '../../stores/auth'
import {
  NOTIFY_FILTER_OPTIONS,
  notifyTypeIcon,
  resolveNotifyRoute,
  type NotifyFilterKey
} from '../../utils/notify'

const notifyStore = useNotifyStore()
const authStore = useAuthStore()
const router = useRouter()

const open = ref(false)
const markingAll = ref(false)

const badgeLabel = computed(() => {
  const count = notifyStore.unreadCount
  if (count <= 0) {
    return undefined
  }
  return count > 99 ? '99+' : String(count)
})

const hasUnreadInList = computed(() => notifyStore.items.some(item => !item.read))

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
  notifyStore.inboxModalOpen = true
  await notifyStore.fetchInbox()
}

async function handleClose() {
  notifyStore.inboxModalOpen = false
}

async function handleFilterChange(key: NotifyFilterKey) {
  await notifyStore.setFilterType(key)
}

async function handleMarkAllRead() {
  markingAll.value = true
  try {
    await notifyStore.markAllRead()
  } finally {
    markingAll.value = false
  }
}

async function handleItemClick(item: { id: string, read: boolean, payload?: Record<string, unknown> }) {
  if (!item.read) {
    await notifyStore.markItemsRead([item.id])
  }
  const route = resolveNotifyRoute(item.payload)
  if (route) {
    open.value = false
    notifyStore.inboxModalOpen = false
    await router.push(route)
  }
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

watch(open, (isOpen) => {
  notifyStore.inboxModalOpen = isOpen
  if (!isOpen) {
    void handleClose()
  }
})
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
      <div class="mb-3 flex flex-wrap items-center gap-2">
        <button
          v-for="option in NOTIFY_FILTER_OPTIONS"
          :key="option.key"
          type="button"
          class="rounded-full px-2.5 py-1 text-xs transition-colors"
          :class="notifyStore.filterType === option.key
            ? 'bg-primary/15 font-medium text-primary'
            : 'bg-[var(--ws-rail-hover)] text-[var(--ws-text-muted)] hover:text-[var(--ws-text)]'"
          @click="handleFilterChange(option.key)"
        >
          {{ option.label }}
        </button>
        <UButton
          v-if="hasUnreadInList"
          color="neutral"
          variant="soft"
          size="xs"
          class="ml-auto"
          :loading="markingAll"
          @click="handleMarkAllRead"
        >
          全部标已读
        </UButton>
      </div>

      <div v-if="notifyStore.inboxLoading" class="flex justify-center py-8">
        <UIcon name="i-lucide-loader-circle" class="size-6 animate-spin text-[var(--ws-text-muted)]" />
      </div>
      <UEmpty
        v-else-if="!notifyStore.items.length"
        icon="i-lucide-bell-off"
        title="暂无通知"
        description="企业邀请、任务提醒等会显示在这里"
      />
      <ul v-else class="max-h-80 space-y-2 overflow-y-auto">
        <li
          v-for="item in notifyStore.items"
          :key="item.id"
          class="rounded-lg border border-[var(--ws-border-subtle)] px-3 py-2.5 transition-colors"
          :class="[
            item.read ? 'opacity-70' : 'bg-[var(--ws-rail-hover)]/40',
            resolveNotifyRoute(item.payload) ? 'cursor-pointer hover:border-primary/30' : ''
          ]"
          @click="handleItemClick(item)"
        >
          <div class="flex items-start gap-2.5">
            <UIcon
              :name="notifyTypeIcon(item.type)"
              class="mt-0.5 size-4 shrink-0 text-[var(--ws-text-muted)]"
            />
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
