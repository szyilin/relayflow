<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import WorkspaceMemberPicker, {
  type PickerMember
} from './WorkspaceMemberPicker.vue'
import { useImStore } from '../../stores/im'
import type { GroupMemberCandidate } from '../../stores/im'

const open = defineModel<boolean>('open', { required: true })
const emit = defineEmits<{
  created: [conversationId: string]
}>()

const router = useRouter()
const im = useImStore()
const toast = useToast()

const name = ref('')
const selected = ref<PickerMember[]>([])
const submitting = ref(false)
const errorMessage = ref<string | null>(null)
const pickerRef = ref<{ reset: () => void } | null>(null)

watch(open, (isOpen) => {
  if (!isOpen) {
    return
  }
  name.value = ''
  selected.value = []
  errorMessage.value = null
  pickerRef.value?.reset()
})

async function handleSubmit() {
  const trimmed = name.value.trim()
  if (!trimmed) {
    errorMessage.value = '请填写群名称'
    return
  }
  if (selected.value.length === 0) {
    errorMessage.value = '请至少选择一名成员'
    return
  }

  // Skip client-only draft ids from contact-add search demos
  const realMembers = selected.value.filter(
    m => !m.userId.startsWith('draft-') && !m.userId.startsWith('out_')
  )
  if (realMembers.length === 0) {
    errorMessage.value = '请选择真实成员（演示草稿账号不可建群）'
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
    const result = await im.createGroup(trimmed, members)
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
      <form
        class="space-y-4"
        @submit.prevent="handleSubmit"
      >
        <UFormField label="群模式">
          <p class="text-sm text-[var(--ws-text)]">
            对话
            <span class="text-[var(--ws-text-muted)]">（话题群后续开放）</span>
          </p>
        </UFormField>

        <UFormField label="群名称">
          <UInput
            v-model="name"
            placeholder="输入群名称"
            maxlength="128"
          />
        </UFormField>

        <UFormField label="选择成员">
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

        <div class="flex justify-end gap-2 pt-1">
          <UButton
            color="neutral"
            variant="ghost"
            @click="open = false"
          >
            取消
          </UButton>
          <UButton
            type="submit"
            :loading="submitting"
          >
            创建
          </UButton>
        </div>
      </form>
    </template>
  </UModal>
</template>
