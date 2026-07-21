<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { LOGIN_PATH } from '../../router/guards'
import { useAccountDockStore, type AccountDockEntry } from '../../stores/accountDock'
import { useAuthStore } from '../../stores/auth'
import { avatarTextFromName, resolveAvatarUrl, tenantTileColor } from '../../utils/avatar'
import { idsEqual } from '../../utils/id'

const emit = defineEmits<{
  back: []
  close: []
}>()

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const dockStore = useAccountDockStore()
const toast = useToast()

const switchingKey = ref<string | null>(null)
const avatarBrokenKeys = ref<Set<string>>(new Set())

function markAvatarBroken(key: string) {
  if (avatarBrokenKeys.value.has(key)) {
    return
  }
  avatarBrokenKeys.value = new Set([...avatarBrokenKeys.value, key])
}

function entryAvatarUrl(entry: AccountDockEntry) {
  if (avatarBrokenKeys.value.has(entry.key)) {
    return undefined
  }
  return resolveAvatarUrl(entry.avatar)
}

function tenantDockAvatar(tenantId: string) {
  if (authStore.userId == null) {
    return undefined
  }
  const key = `${authStore.userId}:${tenantId}`
  if (avatarBrokenKeys.value.has(key)) {
    return undefined
  }
  const entry = dockStore.entries.find(item => item.key === key)
  return resolveAvatarUrl(entry?.avatar)
}

const currentKey = computed(() => {
  if (authStore.userId == null || authStore.tenantId == null) {
    return null
  }
  return `${authStore.userId}:${authStore.tenantId}`
})

const otherTenants = computed(() =>
  authStore.tenants.filter(tenant => !idsEqual(tenant.tenantId, authStore.tenantId)))

const otherDockEntries = computed(() => {
  const auth = authStore
  const list = [...dockStore.entries]
  const key = currentKey.value

  if (key && !list.some(entry => entry.key === key) && auth.token) {
    list.unshift({
      key,
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

  return list.filter((entry) => {
    if (entry.key === key) {
      return false
    }
    // 同身份多企业已在「我的企业」列出，dock 仅保留其他账号会话
    const sameUserOtherTenant = idsEqual(entry.userId, auth.userId)
      && otherTenants.value.some(tenant => idsEqual(tenant.tenantId, entry.tenantId))
    return !sameUserOtherTenant
  })
})

const displayIdentity = computed(() =>
  authStore.user?.nickname
  || authStore.user?.username
  || '当前账号')

function resolveEntryMeta(entry: AccountDockEntry) {
  const tenant = authStore.tenants.find(item => idsEqual(item.tenantId, entry.tenantId))
  return {
    tenantName: entry.tenantName?.trim() || tenant?.tenantName || '企业',
    nickname: entry.nickname?.trim() || entry.username?.trim() || '员工'
  }
}

function entryTileStyle(entry: { tenantId: string }) {
  return {
    backgroundColor: tenantTileColor(entry.tenantId),
    color: '#fff'
  }
}

async function handleForceLogin(message: string) {
  emit('close')
  toast.add({
    title: '登录已失效',
    description: message,
    color: 'warning'
  })
  await router.replace({
    path: LOGIN_PATH,
    query: { redirect: route.fullPath }
  })
}

async function switchTenant(tenantId: string, tenantName: string) {
  if (switchingKey.value) {
    return
  }
  switchingKey.value = `tenant:${tenantId}`
  try {
    const result = await authStore.switchTenant(tenantId)
    if (!result.ok) {
      if (result.forceLogin) {
        await handleForceLogin(result.message)
        return
      }
      toast.add({ title: '切换失败', description: result.message, color: 'error' })
      return
    }
    toast.add({
      title: `已切换至 ${tenantName}`,
      icon: 'i-lucide-check',
      color: 'success',
      close: false
    })
    emit('close')
  } finally {
    switchingKey.value = null
  }
}

async function switchEntry(entry: AccountDockEntry) {
  if (switchingKey.value) {
    return
  }
  switchingKey.value = entry.key
  try {
    const result = await authStore.switchToDockEntry(entry)
    if (!result.ok) {
      if (result.forceLogin) {
        await handleForceLogin(result.message)
        return
      }
      toast.add({ title: '切换失败', description: result.message, color: 'error' })
      return
    }
    toast.add({
      title: `已切换至 ${resolveEntryMeta(entry).tenantName}`,
      icon: 'i-lucide-check',
      color: 'success',
      close: false
    })
    emit('close')
    if (route.path.startsWith('/app/messages')) {
      return
    }
    await router.replace('/app/messages')
  } finally {
    switchingKey.value = null
  }
}

function onJoinEnterprise() {
  toast.add({
    title: '功能即将推出',
    description: '加入企业（占位）',
    color: 'neutral'
  })
}

function createAccount() {
  emit('close')
  void router.push({ path: '/app/register', query: { addAccount: '1' } })
}

onMounted(() => {
  if (authStore.isAuthenticated) {
    void authStore.fetchMyTenants().catch(() => {})
  }
})
</script>

<template>
  <div class="workspace-more-accounts w-72 py-2">
    <div class="mb-1 flex items-center gap-1 px-2">
      <button
        type="button"
        class="workspace-more-action !w-auto !gap-1 !px-2"
        aria-label="返回"
        @click="$emit('back')"
      >
        <UIcon name="i-lucide-chevron-left" class="size-4" />
        <span class="text-sm font-medium">登录更多账号</span>
      </button>
    </div>

    <p class="mb-2 px-3 text-xs text-[var(--ws-text-muted)]">
      已绑定企业 · {{ displayIdentity }}
    </p>

    <div class="max-h-64 space-y-2 overflow-y-auto px-1">
      <div>
        <p class="px-2.5 py-1 text-xs font-medium text-[var(--ws-text-muted)]">
          我的企业
        </p>
        <div v-if="otherTenants.length" class="space-y-0.5">
          <button
            v-for="tenant in otherTenants"
            :key="tenant.tenantId"
            type="button"
            class="workspace-more-item"
            :class="{ 'is-switching': switchingKey === `tenant:${tenant.tenantId}` }"
            :disabled="Boolean(switchingKey)"
            @click="switchTenant(tenant.tenantId, tenant.tenantName)"
          >
            <span
              class="workspace-more-tile"
              :style="entryTileStyle(tenant)"
            >
              <img
                v-if="tenantDockAvatar(tenant.tenantId)"
                :src="tenantDockAvatar(tenant.tenantId)"
                :alt="tenant.tenantName"
                class="h-full w-full rounded-[inherit] object-cover"
                @error="markAvatarBroken(`${authStore.userId}:${tenant.tenantId}`)"
              >
              <span v-else class="text-xs font-semibold">
                {{ avatarTextFromName(tenant.tenantName) }}
              </span>
            </span>
            <span class="min-w-0 flex-1 text-left">
              <span class="block truncate text-sm font-medium">{{ tenant.tenantName }}</span>
              <span class="block truncate text-xs text-[var(--ws-text-muted)]">{{ displayIdentity }}</span>
            </span>
          </button>
        </div>
        <p v-else class="px-2.5 py-2 text-xs text-[var(--ws-text-muted)]">
          暂无其他企业
        </p>
      </div>

      <div>
        <p class="px-2.5 py-1 text-xs font-medium text-[var(--ws-text-muted)]">
          本机已登录账号
        </p>
        <div v-if="otherDockEntries.length" class="space-y-0.5">
          <button
            v-for="entry in otherDockEntries"
            :key="entry.key"
            type="button"
            class="workspace-more-item"
            :class="{ 'is-switching': switchingKey === entry.key }"
            :disabled="Boolean(switchingKey)"
            @click="switchEntry(entry)"
          >
            <span
              class="workspace-more-tile"
              :style="entryTileStyle(entry)"
            >
              <img
                v-if="entryAvatarUrl(entry)"
                :src="entryAvatarUrl(entry)"
                :alt="resolveEntryMeta(entry).nickname"
                class="h-full w-full rounded-[inherit] object-cover"
                @error="markAvatarBroken(entry.key)"
              >
              <span v-else class="text-xs font-semibold">
                {{ avatarTextFromName(resolveEntryMeta(entry).tenantName) }}
              </span>
            </span>
            <span class="min-w-0 flex-1 text-left">
              <span class="block truncate text-sm font-medium">{{ resolveEntryMeta(entry).tenantName }}</span>
              <span class="block truncate text-xs text-[var(--ws-text-muted)]">{{ resolveEntryMeta(entry).nickname }}</span>
            </span>
          </button>
        </div>
        <p v-else class="px-2.5 py-2 text-xs text-[var(--ws-text-muted)]">
          暂无其他登录会话
        </p>
      </div>
    </div>

    <div class="mt-1 space-y-0.5 border-t border-[var(--ws-border-subtle)] px-1 pt-1">
      <button
        type="button"
        class="workspace-more-action"
        @click="onJoinEnterprise"
      >
        <UIcon name="i-lucide-building-2" class="size-4 shrink-0" />
        <span>加入企业</span>
        <span class="ml-auto text-xs text-[var(--ws-text-muted)]">（占位）</span>
      </button>
      <button
        type="button"
        class="workspace-more-action"
        @click="createAccount"
      >
        <UIcon name="i-lucide-user-round-plus" class="size-4 shrink-0" />
        <span>创建新账号</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.workspace-more-item {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 0.625rem;
  border-radius: 0.625rem;
  padding: 0.5rem 0.625rem;
  text-align: left;
  transition: background-color 0.15s ease, opacity 0.15s ease;
}

.workspace-more-item:hover {
  background: var(--ws-rail-hover);
}

.workspace-more-item.is-switching {
  opacity: 0.6;
}

.workspace-more-tile {
  display: flex;
  width: 2rem;
  height: 2rem;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 0.5rem;
}

.workspace-more-action {
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

.workspace-more-action:hover {
  background: var(--ws-rail-hover);
}
</style>
