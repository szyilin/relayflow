<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { CardActionItem, CardFormField, ContentBlock, MessageItem } from '../../api/app/im'
import { postCardAction } from '../../api/app/im'
import { ApiError } from '../../api/request'

const props = defineProps<{
  message: MessageItem
  card: ContentBlock
}>()

const emit = defineEmits<{
  updated: [message: MessageItem]
}>()

const toast = useToast()
const router = useRouter()
const submittingActionId = ref<string | null>(null)
const formOpen = ref(false)
const formAction = ref<CardActionItem | null>(null)
const formValues = reactive<Record<string, string>>({})

const expired = computed(() => {
  const expiresAt = props.card.meta?.expiresAt
  if (!expiresAt) {
    return false
  }
  const ts = Date.parse(expiresAt)
  return Number.isFinite(ts) && ts < Date.now()
})

const actions = computed(() => props.card.actions ?? [])

function buttonColor(style?: string): 'primary' | 'error' | 'neutral' {
  if (style === 'primary') {
    return 'primary'
  }
  if (style === 'danger') {
    return 'error'
  }
  return 'neutral'
}

function openForm(action: CardActionItem) {
  formAction.value = action
  for (const key of Object.keys(formValues)) {
    delete formValues[key]
  }
  for (const field of action.behavior.form ?? []) {
    formValues[field.name] = ''
  }
  formOpen.value = true
}

async function handleAction(action: CardActionItem) {
  if (expired.value || submittingActionId.value) {
    return
  }
  const behavior = action.behavior
  if (!behavior) {
    return
  }
  if (behavior.type === 'open_url') {
    const route = behavior.route?.trim()
    if (!route) {
      return
    }
    if (route.startsWith('http://') || route.startsWith('https://')) {
      window.open(route, '_blank', 'noopener,noreferrer')
      return
    }
    await router.push(route)
    return
  }
  if (behavior.type !== 'callback' || !behavior.actionKey) {
    return
  }
  if (behavior.form?.length) {
    openForm(action)
    return
  }
  await submitCallback(action)
}

function validateForm(fields: CardFormField[]): boolean {
  for (const field of fields) {
    if (field.required && !formValues[field.name]?.trim()) {
      toast.add({ title: `请填写${field.label}`, color: 'error' })
      return false
    }
  }
  return true
}

async function submitForm() {
  const action = formAction.value
  if (!action?.behavior.form) {
    return
  }
  if (!validateForm(action.behavior.form)) {
    return
  }
  formOpen.value = false
  await submitCallback(action, { ...formValues })
}

async function submitCallback(action: CardActionItem, values?: Record<string, string>) {
  if (!action.behavior.actionKey) {
    return
  }
  submittingActionId.value = action.id
  try {
    const result = await postCardAction({
      messageId: props.message.id,
      conversationId: props.message.conversationId,
      actionId: action.id,
      actionKey: action.behavior.actionKey,
      payload: action.behavior.payload,
      formValues: values,
      clientActionId: crypto.randomUUID()
    })
    if (result.toast?.content) {
      const color = result.toast.type === 'error'
        ? 'error'
        : result.toast.type === 'warning'
          ? 'warning'
          : 'success'
      toast.add({ title: result.toast.content, color })
    }
    if (result.message) {
      emit('updated', result.message)
    }
  } catch (error) {
    toast.add({
      title: error instanceof ApiError ? error.message : '操作失败',
      color: 'error'
    })
  } finally {
    submittingActionId.value = null
  }
}
</script>

<template>
  <div class="min-w-[240px] max-w-sm rounded-lg border border-[var(--ws-border-subtle)] bg-[var(--ws-surface)] p-3 text-left shadow-sm">
    <div class="space-y-0.5">
      <p class="text-sm font-semibold text-[var(--ws-text)]">
        {{ card.header?.title ?? '卡片' }}
      </p>
      <p
        v-if="card.header?.subtitle"
        class="text-xs text-[var(--ws-text-muted)]"
      >
        {{ card.header.subtitle }}
      </p>
    </div>

    <dl
      v-if="card.fields?.length"
      class="mt-3 space-y-1.5"
    >
      <div
        v-for="(field, idx) in card.fields"
        :key="idx"
        class="flex gap-2 text-xs"
      >
        <dt class="shrink-0 text-[var(--ws-text-muted)]">
          {{ field.label }}
        </dt>
        <dd class="min-w-0 flex-1 break-words font-medium text-[var(--ws-text)]">
          {{ field.value }}
        </dd>
      </div>
    </dl>

    <p
      v-if="expired"
      class="mt-3 text-xs text-[var(--ws-text-muted)]"
    >
      交互已过期
    </p>

    <div
      v-if="actions.length"
      class="mt-3 flex flex-wrap gap-2"
    >
      <UButton
        v-for="action in actions"
        :key="action.id"
        size="xs"
        :color="buttonColor(action.style)"
        :variant="action.style === 'primary' ? 'solid' : 'soft'"
        :disabled="expired"
        :loading="submittingActionId === action.id"
        @click="handleAction(action)"
      >
        {{ action.label }}
      </UButton>
    </div>

    <UModal v-model:open="formOpen">
      <template #content>
        <div class="space-y-4 p-4">
          <h3 class="font-semibold">
            {{ formAction?.label ?? '填写信息' }}
          </h3>
          <div
            v-for="field in formAction?.behavior.form ?? []"
            :key="field.name"
            class="space-y-1"
          >
            <label class="text-xs text-[var(--ws-text-muted)]">
              {{ field.label }}
              <span v-if="field.required" class="text-error">*</span>
            </label>
            <UTextarea
              v-if="field.control === 'textarea'"
              v-model="formValues[field.name]"
              :rows="3"
              class="w-full"
            />
            <UInput
              v-else
              v-model="formValues[field.name]"
              class="w-full"
            />
          </div>
          <div class="flex justify-end gap-2">
            <UButton
              color="neutral"
              variant="ghost"
              @click="formOpen = false"
            >
              取消
            </UButton>
            <UButton
              color="primary"
              :loading="!!submittingActionId"
              @click="submitForm"
            >
              提交
            </UButton>
          </div>
        </div>
      </template>
    </UModal>
  </div>
</template>
