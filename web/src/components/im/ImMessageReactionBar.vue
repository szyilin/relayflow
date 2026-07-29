<script setup lang="ts">
import { ref, watch } from 'vue'
import type { MessageReactionAgg } from '../../api/app/im'

const QUICK_EMOJIS = ['👍', '❤️', '😄', '🎉', '👀', '🙏'] as const

const props = defineProps<{
  reactions?: MessageReactionAgg[]
  disabled?: boolean
  forceOpen?: boolean
}>()

const emit = defineEmits<{
  toggle: [emoji: string]
  'update:forceOpen': [value: boolean]
}>()

const moreOpen = ref(false)

watch(() => props.forceOpen, (open) => {
  if (open) {
    moreOpen.value = true
  }
})

function onChip(emoji: string) {
  if (props.disabled) {
    return
  }
  emit('toggle', emoji)
}

function onQuick(emoji: string) {
  if (props.disabled) {
    return
  }
  moreOpen.value = false
  emit('update:forceOpen', false)
  emit('toggle', emoji)
}

function toggleMore() {
  moreOpen.value = !moreOpen.value
  if (!moreOpen.value) {
    emit('update:forceOpen', false)
  }
}
</script>

<template>
  <div class="mt-1 flex flex-wrap items-center gap-1">
    <button
      v-for="item in (reactions ?? [])"
      :key="item.emoji"
      type="button"
      class="inline-flex items-center gap-0.5 rounded-full border px-1.5 py-0.5 text-xs transition-colors"
      :class="item.me
        ? 'border-primary/40 bg-primary/10 text-[var(--ws-text)]'
        : 'border-[var(--ws-border-subtle)] bg-[var(--ws-input-bar-bg)] text-[var(--ws-text)] hover:bg-black/5'"
      :disabled="disabled"
      @click.stop="onChip(item.emoji)"
    >
      <span>{{ item.emoji }}</span>
      <span class="tabular-nums text-[10px] opacity-70">{{ item.count }}</span>
    </button>

    <div
      v-if="!disabled"
      class="relative"
    >
      <button
        type="button"
        class="inline-flex size-6 items-center justify-center rounded-full border border-dashed border-[var(--ws-border-subtle)] text-[var(--ws-text-muted)] transition-opacity"
        :class="moreOpen || forceOpen ? 'opacity-100' : 'opacity-0 group-hover:opacity-100'"
        title="表情回应"
        aria-label="表情回应"
        @click.stop="toggleMore"
      >
        <UIcon
          name="i-lucide-smile-plus"
          class="size-3.5"
        />
      </button>
      <div
        v-if="moreOpen"
        class="absolute bottom-full left-0 z-20 mb-1 flex gap-0.5 rounded-md border border-[var(--ws-border-subtle)] bg-[var(--ui-bg)] p-1 shadow-md"
        @click.stop
      >
        <button
          v-for="emoji in QUICK_EMOJIS"
          :key="emoji"
          type="button"
          class="size-7 rounded text-base hover:bg-[var(--ws-input-bar-bg)]"
          @click="onQuick(emoji)"
        >
          {{ emoji }}
        </button>
      </div>
    </div>
  </div>
</template>
