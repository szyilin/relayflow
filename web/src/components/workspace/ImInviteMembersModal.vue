<script setup lang="ts">
import { ref, watch } from 'vue'
import WorkspaceMemberPicker, {
  type PickerMember
} from './WorkspaceMemberPicker.vue'
import WorkspaceModalBody from './WorkspaceModalBody.vue'
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
const selected = ref<PickerMember[]>([])
const submitting = ref(false)
const errorMessage = ref<string | null>(null)
const pickerRef = ref<{ reset: () => void } | null>(null)

watch(open, (isOpen) => {
  if (!isOpen) {
    return
  }
  selected.value = []
  errorMessage.value = null
  pickerRef.value?.reset()
})

async function handleSubmit() {
  if (selected.value.length === 0) {
    errorMessage.value = '请至少选择一名成员'
    return
  }

  const realMembers = selected.value.filter(
    m => !m.userId.startsWith('draft-') && !m.userId.startsWith('out_')
  )
  if (realMembers.length === 0) {
    errorMessage.value = '请选择真实成员'
    return
  }

  const members: GroupMemberCandidate[] = realMembers.map(m => ({
    userId: m.userId,
    nickname: m.displayName,
    avatarText: m.avatarText
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
  <UModal
    v-model:open="open"
    title="邀请成员"
    description="从组织内、外部或我管理的群组中选择"
    :ui="{ content: 'max-w-3xl w-[min(860px,94vw)]' }"
  >
    <template #body>
      <WorkspaceModalBody
        min-height-class="min-h-[22rem]"
        max-height-class="max-h-[min(70vh,36rem)]"
      >
        <form
          id="im-invite-members-form"
          class="space-y-4"
          @submit.prevent="handleSubmit"
        >
          <WorkspaceMemberPicker
            ref="pickerRef"
            v-model="selected"
            :exclude-user-ids="excludeUserIds"
          />

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
          form="im-invite-members-form"
          :loading="submitting"
        >
          邀请
        </UButton>
      </div>
    </template>
  </UModal>
</template>
