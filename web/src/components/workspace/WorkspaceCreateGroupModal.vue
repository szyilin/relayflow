<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import WorkspaceMemberPicker, {
  type PickerMember
} from './WorkspaceMemberPicker.vue'
import WorkspaceModalBody from './WorkspaceModalBody.vue'
import { useDeptStore } from '../../stores/dept'
import { useImStore } from '../../stores/im'
import type { GroupMemberCandidate } from '../../stores/im'

type GroupKind = 'NORMAL' | 'DEPT'

const open = defineModel<boolean>('open', { required: true })
const emit = defineEmits<{
  created: [conversationId: string]
}>()

const router = useRouter()
const im = useImStore()
const deptStore = useDeptStore()
const toast = useToast()

const name = ref('')
const groupKind = ref<GroupKind>('NORMAL')
const deptId = ref<string>()
const selected = ref<PickerMember[]>([])
const submitting = ref(false)
const errorMessage = ref<string | null>(null)
const pickerRef = ref<{ reset: () => void } | null>(null)

const isDeptGroup = computed(() => groupKind.value === 'DEPT')

const deptOptions = computed(() =>
  deptStore.list.map(d => ({ label: d.name, value: d.id })))

const groupKindOptions: Array<{ value: GroupKind, label: string, description: string }> = [
  { value: 'NORMAL', label: '普通群', description: '手动选择成员创建群聊' },
  { value: 'DEPT', label: '部门群', description: '按部门自动拉取成员（可额外邀请）' }
]

watch(open, (isOpen) => {
  if (!isOpen) {
    return
  }
  name.value = ''
  groupKind.value = 'NORMAL'
  deptId.value = undefined
  selected.value = []
  errorMessage.value = null
  pickerRef.value?.reset()
  if (deptStore.list.length === 0) {
    void deptStore.fetchList().catch(() => {})
  }
})

async function handleSubmit() {
  const trimmed = name.value.trim()
  if (!trimmed) {
    errorMessage.value = '请填写群名称'
    return
  }
  if (isDeptGroup.value && !deptId.value) {
    errorMessage.value = '请选择部门'
    return
  }

  // Skip client-only draft ids from contact-add search demos
  const realMembers = selected.value.filter(
    m => !m.userId.startsWith('draft-') && !m.userId.startsWith('out_')
  )
  const members: GroupMemberCandidate[] = realMembers.map(m => ({
    userId: m.userId,
    nickname: m.displayName,
    avatarText: m.avatarText
  }))

  if (!isDeptGroup.value && members.length === 0) {
    errorMessage.value = '请至少选择一名成员'
    return
  }
  if (!isDeptGroup.value && realMembers.length === 0) {
    errorMessage.value = '请选择真实成员（演示草稿账号不可建群）'
    return
  }

  submitting.value = true
  errorMessage.value = null
  try {
    const result = await im.createGroup(trimmed, members, isDeptGroup.value
      ? { groupKind: 'DEPT', deptId: deptId.value }
      : { groupKind: 'NORMAL' })
    emit('created', result.conversationId)
    open.value = false
    await router.push({
      path: '/app/messages',
      query: { conversationId: result.conversationId }
    })
    toast.add({ title: '群组已创建', color: 'success' })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '创建群聊失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <UModal
    v-model:open="open"
    title="创建群组"
    description="选择组织内、外部或我管理的群组成员"
    :ui="{ content: 'max-w-3xl w-[min(860px,94vw)]' }"
  >
    <template #body>
      <WorkspaceModalBody
        min-height-class="min-h-[28rem]"
        max-height-class="max-h-[min(72vh,40rem)]"
      >
        <form
          id="workspace-create-group-form"
          class="space-y-4"
          @submit.prevent="handleSubmit"
        >
          <UFormField label="群类型">
            <div class="space-y-2">
              <label
                v-for="opt in groupKindOptions"
                :key="opt.value"
                class="flex cursor-pointer gap-3 rounded-lg border border-[var(--ws-border-subtle)] p-3"
                :class="groupKind === opt.value ? 'border-primary bg-primary/5' : ''"
              >
                <input
                  v-model="groupKind"
                  type="radio"
                  class="mt-1"
                  :value="opt.value"
                >
                <span>
                  <span class="block text-sm font-medium text-[var(--ws-text)]">{{ opt.label }}</span>
                  <span class="mt-0.5 block text-xs text-[var(--ws-text-muted)]">{{ opt.description }}</span>
                </span>
              </label>
            </div>
          </UFormField>

          <!-- Reserve dept field height so NORMAL ↔ DEPT does not resize the shell -->
          <div class="min-h-[4.75rem]">
            <UFormField
              v-if="isDeptGroup"
              label="部门"
            >
              <USelectMenu
                v-model="deptId"
                :items="deptOptions"
                value-key="value"
                placeholder="选择部门"
                :loading="deptStore.loading"
                class="w-full"
              />
            </UFormField>
          </div>

          <UFormField label="群名称">
            <UInput
              v-model="name"
              placeholder="输入群名称"
              maxlength="128"
            />
          </UFormField>

          <UFormField :label="isDeptGroup ? '额外成员（可选）' : '选择成员'">
            <WorkspaceMemberPicker
              ref="pickerRef"
              v-model="selected"
            />
          </UFormField>

          <p
            v-if="errorMessage"
            class="text-sm text-error"
          >
            {{ errorMessage }}
          </p>
        </form>
      </WorkspaceModalBody>
    </template>

    <template #footer>
      <div class="flex w-full justify-end gap-2">
        <UButton
          color="neutral"
          variant="ghost"
          @click="open = false"
        >
          取消
        </UButton>
        <UButton
          type="submit"
          form="workspace-create-group-form"
          :loading="submitting"
        >
          创建
        </UButton>
      </div>
    </template>
  </UModal>
</template>
