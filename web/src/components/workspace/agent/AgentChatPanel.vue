<script setup lang="ts">
import { computed } from 'vue'
import { Comark } from '@comark/vue'
import type { UIMessage } from 'ai'
import type { AgentMessagePart, AgentSessionMessage } from '../../../api/app/agent'
import type { AgentStreamStatus } from '../../../stores/agent'

const props = defineProps<{
  messages: AgentSessionMessage[]
  status: AgentStreamStatus
  draft: string
  loadingMessages?: boolean
  error?: Error | null
}>()

const emit = defineEmits<{
  'update:draft': [value: string]
  submit: []
  stop: []
}>()

/** Local parts → UIMessage shape for UChatMessages (subset; not full AI SDK tool parts). */
const chatMessages = computed(() =>
  props.messages
    .filter(m => m.role === 'user' || m.role === 'assistant')
    .map(m => ({
      id: m.id,
      role: m.role as 'user' | 'assistant',
      parts: (m.parts?.length ? m.parts : [{ type: 'text' as const, text: m.content }]) as UIMessage['parts']
    })) as UIMessage[]
)

const input = computed({
  get: () => props.draft,
  set: (value: string) => emit('update:draft', value)
})

function isTextPart(part: AgentMessagePart): part is Extract<AgentMessagePart, { type: 'text' }> {
  return part.type === 'text'
}

function isReasoningPart(part: AgentMessagePart): part is Extract<AgentMessagePart, { type: 'reasoning' }> {
  return part.type === 'reasoning'
}

function isToolPart(part: AgentMessagePart): part is Extract<AgentMessagePart, { type: 'tool' }> {
  return part.type === 'tool'
}

function messageParts(message: UIMessage): AgentMessagePart[] {
  return (message.parts ?? []) as unknown as AgentMessagePart[]
}

function partStreaming(part: AgentMessagePart) {
  if (part.type === 'tool') {
    return part.state === 'running'
  }
  return part.state === 'streaming'
}

function onSubmit() {
  if (!props.draft.trim()) {
    return
  }
  if (props.status === 'streaming' || props.status === 'submitted') {
    return
  }
  emit('submit')
}
</script>

<template>
  <div class="flex min-h-0 flex-1 flex-col">
    <div
      v-if="loadingMessages"
      class="flex-1 space-y-3 overflow-y-auto px-5 py-4"
    >
      <USkeleton
        v-for="i in 3"
        :key="i"
        class="h-16 w-2/3"
      />
    </div>

    <div
      v-else
      class="min-h-0 flex-1 overflow-y-auto px-5 py-4"
    >
      <UChatMessages
        :messages="chatMessages"
        :status="status"
        :should-auto-scroll="true"
        class="w-full max-w-3xl mx-auto"
      >
        <template #content="{ message }">
          <template
            v-for="(part, index) in messageParts(message)"
            :key="`${message.id}-${part.type}-${index}`"
          >
            <UChatReasoning
              v-if="isReasoningPart(part)"
              :text="part.text"
              :streaming="partStreaming(part)"
              class="mb-2"
            >
              <Comark
                :markdown="part.text"
                :streaming="partStreaming(part)"
                class="*:first:mt-0 *:last:mb-0 text-sm"
              />
            </UChatReasoning>

            <UChatTool
              v-else-if="isToolPart(part)"
              :text="part.state === 'running' ? `正在调用 ${part.toolName}` : `已调用 ${part.toolName}`"
              :suffix="part.input"
              :streaming="partStreaming(part)"
              icon="i-lucide-wrench"
              variant="card"
              chevron="leading"
              class="mb-2 max-w-md"
            >
              <pre
                v-if="part.output || part.input"
                class="whitespace-pre-wrap text-xs"
              >{{ part.output || part.input }}</pre>
            </UChatTool>

            <template v-else-if="isTextPart(part)">
              <Comark
                v-if="message.role === 'assistant'"
                :markdown="part.text"
                :streaming="partStreaming(part)"
                class="*:first:mt-0 *:last:mb-0"
              />
              <p
                v-else
                class="whitespace-pre-wrap"
              >
                {{ part.text }}
              </p>
            </template>
          </template>
        </template>
      </UChatMessages>

      <p
        v-if="!chatMessages.length && status === 'ready'"
        class="py-12 text-center text-sm text-[var(--ws-text-muted)]"
      >
        发送第一条消息开始对话
      </p>
    </div>

    <div class="border-t border-[var(--ws-border-subtle)] p-4">
      <UChatPrompt
        v-model="input"
        variant="subtle"
        :error="error ?? undefined"
        class="mx-auto max-w-3xl"
        placeholder="继续输入…"
        @submit="onSubmit"
      >
        <UChatPromptSubmit
          :status="status"
          @stop="emit('stop')"
        />
      </UChatPrompt>
    </div>
  </div>
</template>
