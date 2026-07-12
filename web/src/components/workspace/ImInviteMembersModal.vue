<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useAuthStore } from '../../stores/auth'
import { useContactsStore } from '../../stores/contacts'
import { useImStore } from '../../stores/im'
import type { GroupMemberCandidate } from '../../stores/im'

const props = defineProps<{
  conversationId: string
  excludeUserIds: string[]
}>()

const open = defineModel<boolean>('open', { required: true })
const emit = defineEmits<{
  invited: []
}>()

const im = useImStore()
const contacts = useContactsStore()
const auth = useAuthStore()

const selectedIds = ref<string[]>([])
const keyword = ref('')
const submitting = ref(false)
const errorMessage = ref<string | null>(null)

const excludeSet = computed(() => new Set(props.excludeUserIds))

watch(open, async (isOpen) => {
  if (!isOpen) {
    return
  }
  selectedIds.value = []
  keyword.value = ''
  errorMessage.value = null
  try {
    await contacts.fetchDepts()
    const rootId = contacts.rootDeptId()
    if (rootId) {
      await contacts.fetchMembers({ deptId: rootId })
    }
  } catch {
    errorMessage.value = contacts.lastError ?? '加载成员失败'
  }
})

const candidateMembers = computed(() => {
  const selfId = auth.userId
  const q = keyword.value.trim().toLowerCase()
  return contacts.members.filter((member) => {
    if (member.id === selfId || excludeSet.value.has(member.id)) {
      return false
    }
    if (!q) {
      return true
    }
    return member.nickname.toLowerCase().includes(q)
      || member.username.toLowerCase().includes(q)
      || member.deptName.toLowerCase().includes(q)
  })
})

function toggleMember(userId: string, checked: boolean) {
  if (checked) {
    if (!selectedIds.value.includes(userId)) {
      selectedIds.value = [...selectedIds.value, userId]
    }
    return
  }
  selectedIds.value = selectedIds.value.filter(id => id !== userId)
}

async function handleSubmit() {
  if (selectedIds.value.length === 0) {
    errorMessage.value = '请至少选择一名成员'
    return
  }

  const members: GroupMemberCandidate[] = candidateMembers.value
    .filter(member => selectedIds.value.includes(member.id))
    .map(member => ({
      userId: member.id,
      nickname: member.nickname,
      avatarText: member.avatarText
    }))

  submitting.value = true
  errorMessage.value = null
  try {
    await im.inviteGroupMembers(props.conversationId, members)
    emit('invited')
    open.value = false
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '邀请成员失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <UModal v-model:open="open" title="邀请成员">
    <template #body>
      <form class="space-y-4" @submit.prevent="handleSubmit">
        <UFormField label="选择成员">
          <UInput
            v-model="keyword"
            placeholder="搜索姓名或部门"
            icon="i-lucide-search"
          />
        </UFormField>

        <div v-if="contacts.loadingMembers" class="space-y-2">
          <USkeleton v-for="i in 4" :key="i" class="h-10 w-full" />
        </div>

        <div
          v-else-if="candidateMembers.length"
          class="max-h-56 space-y-1 overflow-y-auto rounded-lg border border-[var(--ws-border-subtle)] p-2"
        >
          <label
            v-for="member in candidateMembers"
            :key="member.id"
            class="flex cursor-pointer items-center gap-3 rounded-md px-2 py-2 hover:bg-[var(--ws-rail-hover)]"
          >
            <UCheckbox
              :model-value="selectedIds.includes(member.id)"
              @update:model-value="toggleMember(member.id, $event === true)"
            />
            <UAvatar :text="member.avatarText" size="sm" />
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-medium">
                {{ member.nickname }}
              </p>
              <p class="truncate text-xs text-[var(--ws-text-muted)]">
                {{ member.deptName }}
              </p>
            </div>
          </label>
        </div>

        <UEmpty
          v-else
          icon="i-lucide-user-check"
          title="没有可邀请的成员"
          description="组织内成员已全部在群中"
          class="py-6"
        />

        <p v-if="errorMessage" class="text-sm text-error">
          {{ errorMessage }}
        </p>

        <div class="flex justify-end gap-2 pt-2">
          <UButton color="neutral" variant="ghost" @click="open = false">
            取消
          </UButton>
          <UButton type="submit" :loading="submitting">
            邀请
          </UButton>
        </div>
      </form>
    </template>
  </UModal>
</template>
