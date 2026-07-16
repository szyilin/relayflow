<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { LOGIN_PATH } from '../../router/guards'
import { useAccountDockStore, type AccountDockEntry } from '../../stores/accountDock'
import { useAuthStore } from '../../stores/auth'
import { avatarTextFromName, resolveAvatarUrl, tenantTileColor } from '../../utils/avatar'
import { idsEqual } from '../../utils/id'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const dockStore = useAccountDockStore()
const toast = useToast()

const open = ref(false)
const switchingKey = ref<string | null>(null)

const dockEntries = computed(() => {
  const auth = authStore
  const list = [...dockStore.entries]
  const currentKey = auth.userId != null && auth.tenantId != null
    ? `${auth.userId}:${auth.tenantId}`
    : null

  if (currentKey && !list.some(entry => entry.key === currentKey) && auth.token) {
    list.unshift({
      key: currentKey,
      userId: auth.userId!,
      username: auth.user?.username ?? '',
      nickname: auth.user?.nickname ?? '',
      avatar: auth.user?.avatar,
      tenantId: auth.tenantId!,
      tenantName: auth.activeTenantName,
      token: auth.token,
      isAdmin: auth.isAdmin
    })
  }

  return list
})

function resolveEntryMeta(entry: AccountDockEntry) {
  const tenant = authStore.tenants.find(item => idsEqual(item.tenantId, entry.tenantId))
  return {
    tenantName: entry.tenantName?.trim() || tenant?.tenantName || '企业',
    nickname: entry.nickname?.trim() || entry.username?.trim() || '员工'
  }
}

function entryTileStyle(entry: AccountDockEntry) {
  return {
    backgroundColor: tenantTileColor(entry.tenantId),
    color: '#fff'
  }
}

function isCurrentEntry(entry: AccountDockEntry) {
  return entry.key === dockStore.currentKey
}

async function switchEntry(entry: AccountDockEntry) {
  if (isCurrentEntry(entry) || switchingKey.value) {
    return
  }

  switchingKey.value = entry.key
  try {
    const result = await authStore.switchToDockEntry(entry)
    if (!result.ok) {
      open.value = false
      if (result.forceLogin) {
        toast.add({
          title: '登录已失效',
          description: result.message,
          color: 'warning'
        })
        await router.replace({
          path: LOGIN_PATH,
          query: { redirect: route.fullPath }
        })
        return
      }
      toast.add({ title: '切换失败', description: result.message, color: 'error' })
      return
    }
    toast.add({
      title: '已切换',
      description: resolveEntryMeta(entry).tenantName,
      color: 'success'
    })
    open.value = false
    if (route.path.startsWith('/app/messages')) {
      return
    }
    await router.replace('/app/messages')
  } finally {
    switchingKey.value = null
  }
}

function loginMoreAccounts() {
  open.value = false
  void router.push({ path: '/app/login', query: { addAccount: '1' } })
}

function registerAccount() {
  open.value = false
  void router.push({ path: '/app/register', query: { addAccount: '1' } })
}
</script>

<template>
  <UPopover
    v-model:open="open"
    :content="{ side: 'bottom', align: 'end', sideOffset: 6 }"
  >
    <button
      type="button"
      class="workspace-switcher-trigger"
      :class="{ 'is-open': open }"
      aria-label="切换账号"
    >
      <UIcon name="i-lucide-chevron-down" class="size-4" />
    </button>

    <template #content>
      <div class="workspace-account-switcher w-64 origin-top-right py-1 animate-in fade-in-0 zoom-in-95 duration-200">
        <p class="px-3 py-1.5 text-xs font-medium text-[var(--ws-text-muted)]">
          切换账号
        </p>

        <div class="max-h-56 space-y-0.5 overflow-y-auto px-1">
          <button
            v-for="entry in dockEntries"
            :key="entry.key"
            type="button"
            class="workspace-account-switcher-item"
            :class="{ 'is-current': isCurrentEntry(entry), 'is-switching': switchingKey === entry.key }"
            :disabled="Boolean(switchingKey)"
            @click="switchEntry(entry)"
          >
            <span
              class="workspace-account-switcher-tile"
              :style="entryTileStyle(entry)"
            >
              <img
                v-if="resolveAvatarUrl(entry.avatar)"
                :src="resolveAvatarUrl(entry.avatar)"
                :alt="resolveEntryMeta(entry).nickname"
                class="h-full w-full rounded-[inherit] object-cover"
              >
              <span v-else class="text-xs font-semibold">
                {{ avatarTextFromName(resolveEntryMeta(entry).tenantName) }}
              </span>
            </span>
            <span class="min-w-0 flex-1 text-left">
              <span class="block truncate text-sm font-medium">{{ resolveEntryMeta(entry).tenantName }}</span>
              <span class="block truncate text-xs text-[var(--ws-text-muted)]">{{ resolveEntryMeta(entry).nickname }}</span>
            </span>
            <UIcon
              v-if="isCurrentEntry(entry)"
              name="i-lucide-check"
              class="size-4 shrink-0 text-primary"
            />
          </button>
        </div>

        <div class="mt-1 space-y-0.5 border-t border-[var(--ws-border-subtle)] px-1 pt-1">
          <button
            type="button"
            class="workspace-account-switcher-action"
            @click="loginMoreAccounts"
          >
            <UIcon name="i-lucide-user-plus" class="size-4 shrink-0" />
            <span>登录更多账号</span>
          </button>
          <button
            type="button"
            class="workspace-account-switcher-action"
            @click="registerAccount"
          >
            <UIcon name="i-lucide-user-round-plus" class="size-4 shrink-0" />
            <span>注册新账号</span>
          </button>
        </div>
      </div>
    </template>
  </UPopover>
</template>

<style scoped>
.workspace-switcher-trigger {
  display: inline-flex;
  width: 1.5rem;
  height: 1.5rem;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 0.375rem;
  color: var(--ws-text-muted);
  transition: background-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.workspace-switcher-trigger:hover {
  background: var(--ws-rail-hover);
  color: var(--ws-text);
}

.workspace-switcher-trigger.is-open {
  background: color-mix(in srgb, var(--color-primary) 10%, transparent);
  color: var(--color-primary);
}

.workspace-switcher-trigger.is-open .size-4 {
  transform: rotate(180deg);
  transition: transform 0.2s ease;
}

.workspace-switcher-trigger .size-4 {
  transition: transform 0.2s ease;
}

.workspace-account-switcher-item {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 0.625rem;
  border-radius: 0.625rem;
  padding: 0.5rem 0.625rem;
  text-align: left;
  transition: background-color 0.15s ease, opacity 0.15s ease;
}

.workspace-account-switcher-item:hover {
  background: var(--ws-rail-hover);
}

.workspace-account-switcher-item.is-current {
  background: color-mix(in srgb, var(--color-primary) 8%, transparent);
}

.workspace-account-switcher-item.is-switching {
  opacity: 0.6;
}

.workspace-account-switcher-tile {
  display: flex;
  width: 2rem;
  height: 2rem;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 0.5rem;
}

.workspace-account-switcher-action {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 0.625rem;
  border-radius: 0.625rem;
  padding: 0.5rem 0.625rem;
  font-size: 0.875rem;
  line-height: 1.25rem;
  color: var(--ws-text);
  transition: background-color 0.15s ease;
}

.workspace-account-switcher-action:hover {
  background: var(--ws-rail-hover);
}
</style>
