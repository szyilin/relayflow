<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import WorkspaceAccountSwitcher from './WorkspaceAccountSwitcher.vue'
import WorkspaceNotifyBell from './WorkspaceNotifyBell.vue'
import WorkspaceProfileCard from './WorkspaceProfileCard.vue'
import WorkspaceSearchModal from './WorkspaceSearchModal.vue'
import { useAuthStore } from '../../stores/auth'
import { useProfileStore } from '../../stores/profile'
import { useWorkspaceSearchShortcut } from '../../composables/useWorkspaceSearchShortcut'
import { avatarTextFromName, resolveAvatarUrl } from '../../utils/avatar'

const authStore = useAuthStore()
const profileStore = useProfileStore()

const profileOpen = ref(false)
const searchOpen = ref(false)
const keyword = ref('')

function openSearch() {
  searchOpen.value = true
}

useWorkspaceSearchShortcut(searchOpen)

const displayNickname = computed(() =>
  profileStore.profile?.nickname
  ?? authStore.user?.nickname
  ?? '员工')

const tenantName = computed(() =>
  profileStore.profile?.tenantName
  ?? authStore.activeTenantName)

const avatarUrl = computed(() =>
  resolveAvatarUrl(profileStore.profile?.avatar ?? authStore.user?.avatar))

const avatarText = computed(() => avatarTextFromName(displayNickname.value))

onMounted(() => {
  if (!authStore.isAuthenticated) {
    return
  }

  void Promise.all([
    authStore.fetchMyTenants().catch(() => {}),
    profileStore.fetchProfile().catch(() => {}),
    authStore.dockSyncAfterSession()
  ])
})
</script>

<template>
  <div class="space-y-3 border-b border-[var(--ws-border-subtle)] p-3">
    <div class="flex items-start gap-2.5">
      <UPopover
        v-model:open="profileOpen"
        :content="{ side: 'right', align: 'start', sideOffset: 10 }"
      >
        <button
          type="button"
          class="workspace-rail-avatar-btn shrink-0"
          :class="{ 'is-open': profileOpen }"
          :aria-label="displayNickname"
        >
          <UAvatar
            :src="avatarUrl"
            :alt="displayNickname"
            :text="avatarText"
            size="lg"
            class="ring-2 ring-primary/25 transition-shadow duration-200"
            :class="{ 'ring-primary/50': profileOpen }"
          />
        </button>

        <template #content>
          <WorkspaceProfileCard v-model:open="profileOpen" />
        </template>
      </UPopover>

      <div class="min-w-0 flex-1 pt-0.5">
        <div class="flex items-center gap-0.5">
          <span class="min-w-0 flex-1 truncate text-sm font-semibold leading-tight">
            {{ displayNickname }}
          </span>
          <WorkspaceNotifyBell />
          <WorkspaceAccountSwitcher />
        </div>

        <UTooltip
          :text="tenantName"
          :delay-duration="400"
          :content="{ side: 'top', align: 'start' }"
        >
          <p class="mt-0.5 max-w-full cursor-default truncate text-xs text-[var(--ws-text-muted)]">
            {{ tenantName }}
          </p>
        </UTooltip>
      </div>
    </div>

    <UInput
      v-model="keyword"
      placeholder="搜索 (⌘K)"
      icon="i-lucide-search"
      size="sm"
      class="workspace-search cursor-pointer"
      readonly
      @click="openSearch"
      @keydown.enter.prevent="openSearch"
    />

    <WorkspaceSearchModal v-model:open="searchOpen" />
  </div>
</template>

<style scoped>
.workspace-rail-avatar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  padding: 0.125rem;
  transition: transform 0.2s ease;
}

.workspace-rail-avatar-btn:hover {
  transform: translateY(-1px);
}

.workspace-rail-avatar-btn.is-open {
  transform: scale(1.02);
}
</style>
