<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import WorkspaceProfileCard from './WorkspaceProfileCard.vue'
import WorkspaceSearchModal from './WorkspaceSearchModal.vue'
import WorkspaceSettingsPanel from './WorkspaceSettingsPanel.vue'
import WorkspaceCreateGroupModal from './WorkspaceCreateGroupModal.vue'
import WorkspaceOrgInviteModal from './WorkspaceOrgInviteModal.vue'
import WorkspaceAddExternalContactModal from './WorkspaceAddExternalContactModal.vue'
import { useAuthStore } from '../../stores/auth'
import { useCreateHubStore } from '../../stores/createHub'
import { useDocsStore } from '../../stores/docs'
import { useOrgInviteStore } from '../../stores/orgInvite'
import { useProfileStore } from '../../stores/profile'
import { useUserPreferenceStore } from '../../stores/userPreference'
import { useWorkspaceSearchShortcut } from '../../composables/useWorkspaceSearchShortcut'
import { avatarTextFromName, resolveAvatarUrl } from '../../utils/avatar'

const router = useRouter()
const authStore = useAuthStore()
const profileStore = useProfileStore()
const preferenceStore = useUserPreferenceStore()
const createHub = useCreateHubStore()
const orgInvite = useOrgInviteStore()
const {
  createGroupOpen,
  orgInviteOpen,
  addExternalOpen
} = storeToRefs(createHub)
const docs = useDocsStore()
const toast = useToast()

const profileOpen = ref(false)
const settingsOpen = ref(false)
const searchOpen = ref(false)
const keyword = ref('')

function openSearch() {
  searchOpen.value = true
}

function openSettings() {
  settingsOpen.value = true
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

const createMenuItems = computed(() => {
  const items: Array<{
    label: string
    icon: string
    onSelect: () => void
  }> = [
    {
      label: '创建群组',
      icon: 'i-lucide-users',
      onSelect: () => createHub.openCreateGroup()
    }
  ]
  if (createHub.canInvite) {
    items.push({
      label: '添加企业成员',
      icon: 'i-lucide-user-plus',
      onSelect: () => createHub.openOrgInvite()
    })
  }
  items.push(
    {
      label: '添加外部联系人',
      icon: 'i-lucide-contact',
      onSelect: () => createHub.openAddExternal()
    },
    {
      label: '创建文档',
      icon: 'i-lucide-file-plus',
      onSelect: () => {
        void createDocument()
      }
    }
  )
  return [items]
})

async function createDocument() {
  try {
    await router.push('/app/docs')
    await docs.createPage(null)
    toast.add({ title: '已创建文档', color: 'success' })
  } catch (error) {
    toast.add({
      title: '创建文档失败',
      description: error instanceof Error ? error.message : '请稍后重试',
      color: 'error'
    })
  }
}

onMounted(() => {
  if (!authStore.isAuthenticated) {
    return
  }

  preferenceStore.fetchFromServer()

  void Promise.all([
    authStore.fetchMyTenants().catch(() => {}),
    profileStore.fetchProfile().catch(() => {}),
    orgInvite.loadCapability().catch(() => {}),
    authStore.dockSyncAfterSession()
  ])
})
</script>

<template>
  <div class="space-y-3 border-b border-[var(--ws-border-subtle)] p-3">
    <div class="flex items-center gap-2.5">
      <UPopover
        v-model:open="profileOpen"
        :content="{ side: 'right', align: 'start', sideOffset: 10 }"
        :ui="{ content: 'p-0 overflow-hidden' }"
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
          <WorkspaceProfileCard
            v-model:open="profileOpen"
            @open-settings="openSettings"
          />
        </template>
      </UPopover>

      <div class="min-w-0 flex-1">
        <span class="block truncate text-sm font-semibold leading-tight">
          {{ displayNickname }}
        </span>

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

      <UDropdownMenu
        :items="createMenuItems"
        :content="{ side: 'bottom', align: 'end', sideOffset: 6 }"
      >
        <UButton
          color="neutral"
          variant="ghost"
          icon="i-lucide-plus"
          size="sm"
          square
          aria-label="创建"
        />
      </UDropdownMenu>
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
    <WorkspaceSettingsPanel v-model:open="settingsOpen" />
    <WorkspaceCreateGroupModal v-model:open="createGroupOpen" />
    <WorkspaceOrgInviteModal v-model:open="orgInviteOpen" />
    <WorkspaceAddExternalContactModal v-model:open="addExternalOpen" />
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
