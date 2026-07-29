<script setup lang="ts">
import type { StickerItem, StickerPackItem } from '../../api/app/im'

defineProps<{
  open: boolean
  packs: StickerPackItem[]
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  select: [sticker: StickerItem]
}>()

function pick(sticker: StickerItem) {
  emit('select', sticker)
  emit('update:open', false)
}
</script>

<template>
  <div
    v-if="open"
    class="absolute bottom-full left-0 z-30 mb-2 w-72 rounded-lg border border-[var(--ws-border-subtle)] bg-[var(--ui-bg)] p-2 shadow-lg"
  >
    <div class="mb-2 flex items-center justify-between px-1">
      <p class="text-xs font-medium text-[var(--ws-text)]">
        表情包
      </p>
      <UButton
        icon="i-lucide-x"
        size="xs"
        color="neutral"
        variant="ghost"
        square
        @click="emit('update:open', false)"
      />
    </div>
    <div
      v-for="pack in packs"
      :key="pack.id"
      class="mb-2 last:mb-0"
    >
      <p class="mb-1 px-1 text-[10px] uppercase tracking-wide text-[var(--ws-text-muted)]">
        {{ pack.name }}
      </p>
      <div class="grid grid-cols-6 gap-1">
        <button
          v-for="sticker in pack.stickers"
          :key="sticker.id"
          type="button"
          class="flex aspect-square items-center justify-center rounded-md text-2xl hover:bg-[var(--ws-input-bar-bg)]"
          :title="sticker.name"
          @click="pick(sticker)"
        >
          <img
            v-if="sticker.downloadUrl"
            :src="sticker.downloadUrl"
            :alt="sticker.name"
            class="size-8 object-contain"
          >
          <span v-else>{{ sticker.emoji || '🙂' }}</span>
        </button>
      </div>
    </div>
  </div>
</template>
