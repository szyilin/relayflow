<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { ConversationItem } from '../../api/app/im'

const open = defineModel<boolean>('open', { default: false })

const props = defineProps<{
  conversations: ConversationItem[]
  excludeConversationId?: string
}>()

const emit = defineEmits<{
  select: [conversationId: string]
}>()

const keyword = ref('')

watch(open, (isOpen) => {
  if (isOpen) {
    keyword.value = ''
  }
})

const candidates = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return props.conversations.filter((item) => {
    if (item.id === props.excludeConversationId) {
      return false
    }
    if (item.type === 'channel') {
      return false
    }
    if (!q) {
      return true
    }
    return item.title.toLowerCase().includes(q)
  })
})

function close() {
  open.value = false
}

function pick(id: string) {
  emit('select', id)
  close()
}
</script>

<template>
  <UModal
    v-model:open="open"
    title="转发到"
    description="选择目标会话"
    :ui="{ content: 'max-w-md' }"
  >
    <template #body>
      <UInput
        v-model="keyword"
        icon="i-lucide-search"
        placeholder="搜索会话"
        size="sm"
        class="mb-3"
      />
      <div class="max-h-72 space-y-1 overflow-y-auto">
        <button
          v-for="item in candidates"
          :key="item.id"
          type="button"
          class="flex w-full items-center gap-2 rounded-md px-2 py-2 text-left text-sm hover:bg-[var(--ws-input-bar-bg)]"
          @click="pick(item.id)"
        >
          <span class="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary/15 text-xs font-medium text-primary">
            {{ item.avatarText || item.title.slice(0, 1) }}
          </span>
          <span class="min-w-0 flex-1 truncate text-[var(--ws-text)]">{{ item.title }}</span>
          <span class="shrink-0 text-[10px] text-[var(--ws-text-muted)]">{{ item.type }}</span>
        </button>
        <p
          v-if="candidates.length === 0"
          class="py-6 text-center text-sm text-[var(--ws-text-muted)]"
        >
          没有可转发的会话
        </p>
      </div>
    </template>
    <template #footer>
      <div class="flex justify-end">
        <UButton
          color="neutral"
          variant="ghost"
          label="取消"
          @click="close"
        />
      </div>
    </template>
  </UModal>
</template>
