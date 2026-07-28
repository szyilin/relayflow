<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { appPromptState, settleAppPrompt } from '../composables/appPrompt'

const inputRef = ref<{ $el?: HTMLElement } | null>(null)

const open = computed({
  get: () => appPromptState.open,
  set: (value: boolean) => {
    if (!value && appPromptState.open) {
      settleAppPrompt(null)
    }
  }
})

watch(() => appPromptState.open, async (isOpen) => {
  if (!isOpen) {
    return
  }
  await nextTick()
  const el = inputRef.value?.$el?.querySelector?.('input') as HTMLInputElement | undefined
  el?.focus()
  el?.select()
})

function onConfirm() {
  const trimmed = appPromptState.value.trim()
  settleAppPrompt(trimmed.length ? trimmed : null)
}
</script>

<template>
  <UModal v-model:open="open" :title="appPromptState.title">
    <template #body>
      <div class="space-y-3">
        <p v-if="appPromptState.description" class="text-sm text-muted whitespace-pre-wrap">
          {{ appPromptState.description }}
        </p>
        <UInput
          ref="inputRef"
          v-model="appPromptState.value"
          :placeholder="appPromptState.placeholder"
          @keyup.enter="onConfirm"
        />
      </div>
    </template>
    <template #footer>
      <div class="flex justify-end gap-2">
        <UButton color="neutral" variant="ghost" @click="settleAppPrompt(null)">
          {{ appPromptState.cancelLabel }}
        </UButton>
        <UButton color="primary" @click="onConfirm">
          {{ appPromptState.confirmLabel }}
        </UButton>
      </div>
    </template>
  </UModal>
</template>
