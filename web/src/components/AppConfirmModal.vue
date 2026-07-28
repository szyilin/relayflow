<script setup lang="ts">
import { computed } from 'vue'
import { appConfirmState, settleAppConfirm } from '../composables/appConfirm'

const open = computed({
  get: () => appConfirmState.open,
  set: (value: boolean) => {
    if (!value && appConfirmState.open) {
      settleAppConfirm(false)
    }
  }
})
</script>

<template>
  <UModal v-model:open="open" :title="appConfirmState.title">
    <template #body>
      <p class="text-sm text-muted whitespace-pre-wrap">
        {{ appConfirmState.description }}
      </p>
    </template>
    <template #footer>
      <div class="flex justify-end gap-2">
        <UButton color="neutral" variant="ghost" @click="settleAppConfirm(false)">
          {{ appConfirmState.cancelLabel }}
        </UButton>
        <UButton
          :color="appConfirmState.confirmColor"
          @click="settleAppConfirm(true)"
        >
          {{ appConfirmState.confirmLabel }}
        </UButton>
      </div>
    </template>
  </UModal>
</template>
