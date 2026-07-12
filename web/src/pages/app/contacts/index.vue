<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'
import { useContactsStore, type ContactDeptTreeNode } from '../../../stores/contacts'
import { useImStore } from '../../../stores/im'

const router = useRouter()
const toast = useToast()
const contacts = useContactsStore()
const im = useImStore()

const selectedDeptNode = ref<ContactDeptTreeNode | undefined>()
const searchInput = ref('')

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
  contacts.selectContact(undefined)
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
}

function handleSearch() {
  void loadMembers({ keyword: searchInput.value })
}

function openMessage(contact: { id: string, nickname: string, avatarText: string }) {
  im.openDirectChat(contact.id, {
    title: contact.nickname,
    avatarText: contact.avatarText
  })
  void router.push('/app/messages')
}

onMounted(() => {
  void initPage()
})
</script>

<route lang="yaml">
meta:
  layout: workspace
</route>

<template>
  <WorkspaceShell show-aside>
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

      <div class="flex min-h-0 flex-1">
        <div class="min-w-0 flex-1 overflow-y-auto p-3">
          <div v-if="contacts.loadingMembers" class="space-y-2">
            <USkeleton v-for="i in 6" :key="i" class="h-14 w-full" />
          </div>

          <div v-else-if="contacts.members.length" class="space-y-0.5">
            <button
              v-for="person in contacts.members"
              :key="person.id"
              type="button"
              class="workspace-list-item flex w-full items-center gap-3 px-3 py-2.5 text-left"
              :data-active="contacts.selectedContactId === person.id"
              @click="contacts.selectContact(person.id)"
            >
              <UAvatar :text="person.avatarText" size="md" />
              <div class="min-w-0">
                <p class="truncate font-medium">
                  {{ person.nickname }}
                </p>
                <p class="truncate text-sm text-[var(--ws-text-muted)]">
                  {{ person.deptName }} · @{{ person.username }}
                </p>
              </div>
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

        <aside
          v-if="contacts.selectedContact"
          class="hidden w-72 shrink-0 border-l border-[var(--ws-border-subtle)] p-5 lg:block"
        >
          <UCard>
            <div class="flex flex-col items-center gap-3 py-2 text-center">
              <UAvatar :text="contacts.selectedContact.avatarText" size="3xl" />
              <div>
                <h3 class="text-lg font-semibold">
                  {{ contacts.selectedContact.nickname }}
                </h3>
                <p class="text-sm text-[var(--ws-text-muted)]">
                  @{{ contacts.selectedContact.username }}
                </p>
              </div>
              <p class="text-sm text-[var(--ws-text-muted)]">
                {{ contacts.selectedContact.deptName }}
              </p>
              <UButton
                icon="i-lucide-message-circle"
                block
                @click="openMessage(contacts.selectedContact)"
              >
                消息
              </UButton>
            </div>
          </UCard>
        </aside>
      </div>

      <div
        v-if="contacts.selectedContact"
        class="border-t border-[var(--ws-border-subtle)] p-4 lg:hidden"
      >
        <UCard>
          <div class="flex items-center gap-4">
            <UAvatar :text="contacts.selectedContact.avatarText" size="lg" />
            <div class="min-w-0 flex-1">
              <p class="font-semibold">
                {{ contacts.selectedContact.nickname }}
              </p>
              <p class="text-sm text-[var(--ws-text-muted)]">
                {{ contacts.selectedContact.deptName }}
              </p>
            </div>
            <UButton
              icon="i-lucide-message-circle"
              @click="openMessage(contacts.selectedContact)"
            >
              消息
            </UButton>
          </div>
        </UCard>
      </div>
    </div>

    <template #aside>
      <div class="border-b border-[var(--ws-border-subtle)] px-4 py-3 font-semibold">
        活跃状态
      </div>
      <div class="flex flex-1 flex-col items-center justify-center gap-2 p-6 text-center text-sm text-[var(--ws-text-muted)]">
        <UIcon name="i-lucide-sparkles" class="size-8 opacity-40" />
        <p>在线状态将在后续切片接入</p>
      </div>
    </template>
  </WorkspaceShell>
</template>
