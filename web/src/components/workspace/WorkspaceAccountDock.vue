<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import WorkspaceProfileCard from './WorkspaceProfileCard.vue'
import { useAccountDockStore, type AccountDockEntry } from '../../stores/accountDock'
import { useAuthStore } from '../../stores/auth'
import { avatarTextFromName, resolveAvatarUrl, tenantTileColor } from '../../utils/avatar'

const router = useRouter()
const authStore = useAuthStore()
const dockStore = useAccountDockStore()
const toast = useToast()

const profileOpen = ref(false)
const switchingKey = ref<string | null>(null)

const avatarUrl = computed(() => resolveAvatarUrl(authStore.user?.avatar))
const avatarText = computed(() => avatarTextFromName(authStore.user?.nickname))

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

const quickSwitchEntries = computed(() =>
  dockEntries.value.filter(entry => entry.key !== dockStore.currentKey))

function tileLabel(entry: AccountDockEntry) {
  return `${entry.tenantName} · ${entry.nickname}`
}

function tileStyle(entry: AccountDockEntry) {
  const color = tenantTileColor(entry.tenantId)
  return {
    backgroundColor: color,
    color: '#fff'
  }
}

async function switchEntry(entry: AccountDockEntry) {
  if (entry.key === dockStore.currentKey || switchingKey.value) {
    return
  }

  switchingKey.value = entry.key
  try {
    const result = await authStore.switchToDockEntry(entry)
    if (!result.ok) {
      toast.add({ title: '切换失败', description: result.message, color: 'error' })
      return
    }
    toast.add({
      title: '已切换',
      description: entry.tenantName,
      color: 'success'
    })
    profileOpen.value = false
    await router.replace('/app/messages')
  } finally {
    switchingKey.value = null
  }
}

onMounted(() => {
  if (authStore.isAuthenticated) {
    void authStore.dockSyncAfterSession()
  }
})
</script>

<template>
  <div class="flex flex-col items-center gap-2 px-2 pb-2 pt-1">
    <div
      v-if="quickSwitchEntries.length > 0"
      class="flex max-w-full flex-wrap justify-center gap-1.5 px-1"
    >
      <button
        v-for="entry in quickSwitchEntries"
        :key="entry.key"
        type="button"
        class="workspace-dock-tile"
        :class="{ 'is-switching': switchingKey === entry.key }"
        :title="tileLabel(entry)"
        :style="tileStyle(entry)"
        :disabled="Boolean(switchingKey)"
        @click="switchEntry(entry)"
      >
        <img
          v-if="resolveAvatarUrl(entry.avatar)"
          :src="resolveAvatarUrl(entry.avatar)"
          :alt="entry.nickname"
          class="size-full rounded-[inherit] object-cover"
        >
        <span v-else class="text-xs font-semibold">
          {{ avatarTextFromName(entry.tenantName) }}
        </span>
      </button>
    </div>

    <UPopover
      v-model:open="profileOpen"
      :content="{ side: 'right', align: 'end', sideOffset: 12 }"
    >
      <button
        type="button"
        class="workspace-dock-avatar"
        :aria-label="authStore.user?.nickname ?? '个人资料'"
      >
        <UAvatar
          :src="avatarUrl"
          :alt="authStore.user?.nickname ?? '员工'"
          :text="avatarText"
          size="md"
          class="ring-2 ring-primary/30"
        />
      </button>

      <template #content>
        <WorkspaceProfileCard v-model:open="profileOpen" />
      </template>
    </UPopover>
  </div>
</template>

<style scoped>
.workspace-dock-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  padding: 0.125rem;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.workspace-dock-avatar:hover {
  transform: translateY(-1px);
}

.workspace-dock-tile {
  display: flex;
  size: 1.75rem;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 0.5rem;
  border: 2px solid transparent;
  transition: transform 0.15s ease, border-color 0.15s ease, opacity 0.15s ease;
}

.workspace-dock-tile:hover {
  transform: scale(1.06);
}

.workspace-dock-tile.is-switching {
  opacity: 0.6;
}
</style>
