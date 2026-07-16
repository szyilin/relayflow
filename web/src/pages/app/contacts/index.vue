<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'
import WorkspaceBusinessCard from '../../../components/workspace/WorkspaceBusinessCard.vue'
import { searchMembers } from '../../../api/app/member-search'
import { useClickAnchoredPopover } from '../../../composables/useClickAnchoredPopover'
import { useAuthStore } from '../../../stores/auth'
import { useContactsStore, type ContactDeptTreeNode } from '../../../stores/contacts'
import { useImStore } from '../../../stores/im'
import { usePresenceStore } from '../../../stores/presence'
import type { ContactItem } from '../../../api/app/contacts'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const auth = useAuthStore()
const contacts = useContactsStore()
const im = useImStore()
const presence = usePresenceStore()
const {
  open: profileOpen,
  payload: profileContact,
  reference: profileReference,
  content: profileContent,
  show: showProfilePopover,
  close: closeProfilePopover
} = useClickAnchoredPopover<ContactItem>()

const selectedDeptNode = ref<ContactDeptTreeNode | undefined>()
const searchInput = ref('')
const activeContactId = ref<string>()

function selectDefaultDept() {
  const rootId = contacts.rootDeptId()
  if (!rootId) {
    return undefined
  }
  const node = contacts.findTreeNode(contacts.tree, rootId)
  if (node) {
    selectedDeptNode.value = node
  }
  return rootId
}

function onDeptSelect(_event: unknown, item: ContactDeptTreeNode) {
  if (item.id === contacts.selectedDeptId) {
    return
  }
  selectedDeptNode.value = item
  activeContactId.value = undefined
  closeProfilePopover()
  void loadMembers({ deptId: item.id, keyword: '' })
}

async function loadMembers(options?: { deptId?: string, keyword?: string }) {
  try {
    await contacts.fetchMembers(options)
  } catch {
    toast.add({
      title: '加载失败',
      description: contacts.lastError ?? '无法获取成员列表',
      color: 'error'
    })
  }
}

async function focusMemberFromQuery(memberId: string) {
  try {
    const results = await searchMembers(memberId, 1)
    const match = results.find(item => item.id === memberId) ?? results[0]
    if (!match?.deptId) {
      activeContactId.value = memberId
      return
    }
    const node = contacts.findTreeNode(contacts.tree, match.deptId)
    if (node) {
      selectedDeptNode.value = node
    }
    await loadMembers({ deptId: match.deptId, keyword: '' })
    activeContactId.value = memberId
  } catch {
    activeContactId.value = memberId
  }
}

async function initPage() {
  try {
    await contacts.fetchDepts()
  } catch {
    toast.add({
      title: '加载失败',
      description: contacts.lastError ?? '无法获取组织架构',
      color: 'error'
    })
    return
  }
  const rootId = selectDefaultDept()
  if (rootId) {
    searchInput.value = ''
    await loadMembers({ deptId: rootId, keyword: '' })
  }
  const memberId = typeof route.query.memberId === 'string' ? route.query.memberId : undefined
  if (memberId) {
    await focusMemberFromQuery(memberId)
  }
}

function handleSearch() {
  activeContactId.value = undefined
  closeProfilePopover()
  void loadMembers({ keyword: searchInput.value })
}

function handleContactClick(person: ContactItem, event: MouseEvent) {
  if (profileOpen.value && activeContactId.value === person.id) {
    activeContactId.value = undefined
    closeProfilePopover()
    return
  }
  activeContactId.value = person.id
  showProfilePopover(person, event)
}

function handleProfileOpenChange(nextOpen: boolean) {
  profileOpen.value = nextOpen
  if (!nextOpen) {
    activeContactId.value = undefined
  }
}

function openMessage(contact: ContactItem) {
  closeProfilePopover()
  activeContactId.value = undefined
  im.openDirectChat(contact.id, {
    title: contact.nickname,
    avatarText: contact.avatarText
  })
  void router.push('/app/messages')
}

const memberUserIds = computed(() => contacts.members.map(member => member.id))

watch(memberUserIds, (userIds) => {
  presence.startPolling(userIds)
}, { immediate: true, deep: true })

onMounted(() => {
  void initPage()
})

watch(() => route.query.memberId, async (raw) => {
  const memberId = typeof raw === 'string' ? raw : undefined
  if (memberId) {
    await focusMemberFromQuery(memberId)
  }
})

onUnmounted(() => {
  presence.stopPolling()
})
</script>

<route lang="yaml">
meta:
  layout: workspace
</route>

<template>
  <WorkspaceShell>
    <template #panel>
      <div class="border-b border-[var(--ws-border-subtle)] px-4 py-3 font-semibold">
        组织内联系人
      </div>

      <div v-if="contacts.loadingDepts" class="space-y-2 p-3">
        <USkeleton v-for="i in 4" :key="i" class="h-7 w-full" />
      </div>

      <UEmpty
        v-else-if="contacts.tree.length === 0"
        icon="i-lucide-building-2"
        title="暂无部门"
        description="组织架构将在管理员配置后显示"
        class="py-8"
      />

      <div v-else class="flex-1 overflow-y-auto p-2">
        <UTree
          v-model="selectedDeptNode"
          :items="contacts.tree"
          :get-key="item => item.id"
          @select="onDeptSelect"
        />
      </div>
    </template>

    <div class="flex h-full flex-col">
      <div class="flex flex-col gap-3 border-b border-[var(--ws-border-subtle)] px-5 py-4 sm:flex-row sm:items-center">
        <h2 class="font-semibold">
          {{ selectedDeptNode?.label ?? '成员' }}
        </h2>
        <div class="flex flex-1 gap-2 sm:justify-end">
          <UInput
            v-model="searchInput"
            placeholder="搜索昵称或用户名"
            icon="i-lucide-search"
            class="workspace-search sm:max-w-xs"
            @keyup.enter="handleSearch"
          />
          <UButton
            color="neutral"
            variant="soft"
            :loading="contacts.loadingMembers"
            @click="handleSearch"
          >
            搜索
          </UButton>
        </div>
      </div>

      <div class="min-h-0 flex-1 overflow-y-auto p-3">
        <div v-if="contacts.loadingMembers" class="space-y-2">
          <USkeleton v-for="i in 6" :key="i" class="h-14 w-full" />
        </div>

        <div v-else-if="contacts.members.length" class="space-y-0.5">
          <button
            v-for="person in contacts.members"
            :key="person.id"
            type="button"
            class="workspace-list-item flex w-full items-center gap-3 px-3 py-2.5 text-left"
            :data-active="activeContactId === person.id"
            @click="handleContactClick(person, $event)"
          >
            <UAvatar :text="person.avatarText" size="md" />
            <div class="min-w-0 flex-1">
              <p class="truncate font-medium">
                {{ person.nickname }}
              </p>
              <p class="truncate text-sm text-[var(--ws-text-muted)]">
                {{ person.deptName }} · @{{ person.username }}
              </p>
            </div>
            <span
              class="size-2 shrink-0 rounded-full"
              :class="presence.isOnline(person.id) ? 'bg-success' : 'bg-[var(--ws-text-muted)]'"
            />
          </button>
        </div>

        <UEmpty
          v-else
          icon="i-lucide-users"
          title="暂无成员"
          description="该部门下暂无同事，试试选择其他部门"
          class="py-12"
        />
      </div>

      <UPopover
        :open="profileOpen"
        :reference="profileReference"
        :content="profileContent"
        :ui="{ content: 'p-0 overflow-hidden' }"
        @update:open="handleProfileOpenChange"
      >
        <template #content>
          <WorkspaceBusinessCard
            v-if="profileContact"
            :mode="profileContact.id === auth.userId ? 'self' : 'peer'"
            :user-id="profileContact.id"
            :nickname="profileContact.nickname"
            :username="profileContact.username"
            :org-label="profileContact.deptName"
            :avatar-text="profileContact.avatarText"
            @message="openMessage(profileContact)"
          />
        </template>
      </UPopover>
    </div>
  </WorkspaceShell>
</template>
