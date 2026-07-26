<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'
import WorkspaceBusinessCard from '../../../components/workspace/WorkspaceBusinessCard.vue'
import ContactsCategoryNav from '../../../components/workspace/contacts/ContactsCategoryNav.vue'
import ContactsOrgDirectory from '../../../components/workspace/contacts/ContactsOrgDirectory.vue'
import ContactsExternalList from '../../../components/workspace/contacts/ContactsExternalList.vue'
import ContactsRequestsList from '../../../components/workspace/contacts/ContactsRequestsList.vue'
import ContactsStarredList from '../../../components/workspace/contacts/ContactsStarredList.vue'
import ContactsGroupsList, {
  type ContactsGroupRow
} from '../../../components/workspace/contacts/ContactsGroupsList.vue'
import { searchMembers } from '../../../api/app/member-search'
import type { ContactItem } from '../../../api/app/contacts'
import { useClickAnchoredPopover } from '../../../composables/useClickAnchoredPopover'
import { useAuthStore } from '../../../stores/auth'
import {
  useContactsStore,
  type ContactsCategory,
  type ExternalContactItem,
  type StarredContactItem
} from '../../../stores/contacts'
import { useImStore } from '../../../stores/im'
import { usePresenceStore } from '../../../stores/presence'

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
} = useClickAnchoredPopover<{
  id: string
  nickname: string
  username: string
  orgLabel: string
  avatarText: string
}>()

const activeContactId = ref<string>()
const loadingGroups = ref(false)

const tenantLabel = computed(() => auth.activeTenantName)

const groupRows = computed<ContactsGroupRow[]>(() => {
  const fromIm = im.conversations
    .filter(item => item.type === 'group')
    .map(item => ({
      id: item.id,
      title: item.title,
      avatarText: item.avatarText || item.title.slice(0, 1) || '群',
      memberCount: item.memberCount
    }))
  if (fromIm.length > 0) {
    return fromIm
  }
  return [
    {
      id: 'placeholder-group-1',
      title: '产品协作群',
      avatarText: '产',
      subtitle: '示例群组（临时）',
      isPlaceholder: true
    }
  ]
})

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
  contacts.setCategory('org')
  try {
    const results = await searchMembers(memberId, 1)
    const match = results.find(item => item.id === memberId) ?? results[0]
    if (!match?.deptId) {
      activeContactId.value = memberId
      return
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
  const rootId = contacts.rootDeptId()
  if (rootId) {
    await loadMembers({ deptId: rootId, keyword: '' })
  }
  const memberId = typeof route.query.memberId === 'string' ? route.query.memberId : undefined
  if (memberId) {
    await focusMemberFromQuery(memberId)
  }
}

async function onCategorySelect(next: ContactsCategory) {
  if (contacts.category === next) {
    return
  }
  closeProfilePopover()
  activeContactId.value = undefined
  contacts.setCategory(next)

  if (next === 'org' && !contacts.selectedDeptId) {
    const rootId = contacts.rootDeptId()
    if (rootId) {
      await loadMembers({ deptId: rootId, keyword: '' })
    }
  }

  if (next === 'groups') {
    loadingGroups.value = true
    try {
      await im.fetchConversations()
    } catch {
      // 临时占位仍可用
    } finally {
      loadingGroups.value = false
    }
  }
}

async function enterDept(deptId: string) {
  closeProfilePopover()
  activeContactId.value = undefined
  try {
    await contacts.enterDept(deptId)
  } catch {
    toast.add({
      title: '加载失败',
      description: contacts.lastError ?? '无法进入该部门',
      color: 'error'
    })
  }
}

function handleContactClick(person: ContactItem, event: MouseEvent) {
  if (profileOpen.value && activeContactId.value === person.id) {
    activeContactId.value = undefined
    closeProfilePopover()
    return
  }
  activeContactId.value = person.id
  showProfilePopover({
    id: person.id,
    nickname: person.nickname,
    username: person.username,
    orgLabel: person.deptName,
    avatarText: person.avatarText
  }, event)
}

function handleExternalClick(person: ExternalContactItem, event: MouseEvent) {
  activeContactId.value = person.userId
  showProfilePopover({
    id: person.userId,
    nickname: person.displayName,
    username: person.displayName,
    orgLabel: person.orgLabel,
    avatarText: person.avatarText
  }, event)
}

function handleStarredClick(person: StarredContactItem, event: MouseEvent) {
  activeContactId.value = person.userId
  showProfilePopover({
    id: person.userId,
    nickname: person.displayName,
    username: person.username ?? person.displayName,
    orgLabel: person.orgLabel,
    avatarText: person.avatarText
  }, event)
}

function handleProfileOpenChange(nextOpen: boolean) {
  profileOpen.value = nextOpen
  if (!nextOpen) {
    activeContactId.value = undefined
  }
}

function openMessage() {
  if (!profileContact.value) {
    return
  }
  const contact = profileContact.value
  closeProfilePopover()
  activeContactId.value = undefined
  im.openDirectChat(contact.id, {
    title: contact.nickname,
    avatarText: contact.avatarText
  })
  void router.push('/app/messages')
}

function onAcceptRequest(requestId: string) {
  contacts.acceptRequest(requestId)
  toast.add({
    title: '已同意',
    description: '对方已加入外部联系人',
    color: 'success'
  })
}

function onUnstar(userId: string) {
  contacts.unstarContact(userId)
  toast.add({
    title: '已取消星标',
    color: 'neutral'
  })
}

async function openGroup(group: ContactsGroupRow) {
  if (group.isPlaceholder) {
    toast.add({
      title: '示例群组',
      description: '创建真实群聊后将显示在此列表',
      color: 'neutral'
    })
    return
  }
  try {
    await im.selectConversation(group.id)
    await router.push({ path: '/app/messages', query: { conversationId: group.id } })
  } catch {
    toast.add({
      title: '打开失败',
      description: '无法进入该群聊',
      color: 'error'
    })
  }
}

const memberUserIds = computed(() => contacts.members.map(member => member.id))

watch(memberUserIds, (userIds) => {
  if (contacts.category === 'org') {
    presence.startPolling(userIds)
  }
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
      <ContactsCategoryNav
        :category="contacts.category"
        :pending-request-count="contacts.pendingRequestCount"
        :tenant-label="tenantLabel"
        @select="onCategorySelect"
      />
    </template>

    <div class="relative flex h-full min-h-0 flex-col">
      <ContactsOrgDirectory
        v-if="contacts.category === 'org'"
        :breadcrumb="contacts.breadcrumb"
        :child-depts="contacts.childDepts"
        :members="contacts.members"
        :loading="contacts.loadingMembers || contacts.loadingDepts"
        :active-contact-id="activeContactId"
        :is-online="presence.isOnline"
        @enter-dept="enterDept"
        @breadcrumb-click="enterDept"
        @contact-click="handleContactClick"
      />

      <ContactsExternalList
        v-else-if="contacts.category === 'external'"
        :groups="contacts.externalGrouped"
        @contact-click="handleExternalClick"
      />

      <ContactsRequestsList
        v-else-if="contacts.category === 'requests'"
        :requests="contacts.contactRequests"
        @accept="onAcceptRequest"
      />

      <ContactsStarredList
        v-else-if="contacts.category === 'starred'"
        :starred="contacts.starredContacts"
        @contact-click="handleStarredClick"
        @unstar="onUnstar"
      />

      <ContactsGroupsList
        v-else
        :groups="groupRows"
        :loading="loadingGroups"
        @open="openGroup"
      />

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
            :org-label="profileContact.orgLabel"
            :avatar-text="profileContact.avatarText"
            @message="openMessage"
          />
        </template>
      </UPopover>
    </div>
  </WorkspaceShell>
</template>
