<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { UserStickerItem } from '../../api/app/im'
import { SYSTEM_EMOJIS } from '../../constants/im-system-emojis'
import { loadRecentEmojis, pushRecentEmoji } from '../../utils/im-recent-emojis'
import ImAuthenticatedImage from '../workspace/ImAuthenticatedImage.vue'

type PickerTab = 'emoji' | 'custom' | 'settings'

const props = defineProps<{
  open: boolean
  userId: string
  customStickers: UserStickerItem[]
  uploading?: boolean
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  'insert-emoji': [emoji: string]
  'send-custom': [sticker: UserStickerItem]
  'add-custom': []
  'delete-custom': [ids: string[]]
  'reorder-custom': [ids: string[]]
}>()

const tab = ref<PickerTab>('emoji')
const recent = ref<string[]>([])
const selectedIds = ref<string[]>([])

const settingsList = computed(() => [...props.customStickers])

watch(() => [props.open, props.userId] as const, ([open]) => {
  if (!open) {
    return
  }
  recent.value = loadRecentEmojis(props.userId)
  tab.value = 'emoji'
  selectedIds.value = []
})

function close() {
  emit('update:open', false)
}

function pickEmoji(emoji: string) {
  recent.value = pushRecentEmoji(props.userId, emoji)
  emit('insert-emoji', emoji)
}

function pickCustom(sticker: UserStickerItem) {
  emit('send-custom', sticker)
  close()
}

function toggleSelect(id: string) {
  if (selectedIds.value.includes(id)) {
    selectedIds.value = selectedIds.value.filter(item => item !== id)
  } else {
    selectedIds.value = [...selectedIds.value, id]
  }
}

function selectAll() {
  selectedIds.value = settingsList.value.map(item => item.id)
}

function clearSelection() {
  selectedIds.value = []
}

async function deleteSelected() {
  if (selectedIds.value.length === 0) {
    return
  }
  emit('delete-custom', [...selectedIds.value])
  selectedIds.value = []
}

function moveSelected(direction: 'up' | 'down') {
  if (selectedIds.value.length !== 1) {
    return
  }
  const id = selectedIds.value[0]!
  const ids = settingsList.value.map(item => item.id)
  const index = ids.indexOf(id)
  if (index < 0) {
    return
  }
  const swapWith = direction === 'up' ? index - 1 : index + 1
  if (swapWith < 0 || swapWith >= ids.length) {
    return
  }
  const next = [...ids]
  const tmp = next[index]!
  next[index] = next[swapWith]!
  next[swapWith] = tmp
  emit('reorder-custom', next)
}
</script>

<template>
  <div
    v-if="open"
    class="absolute bottom-full left-0 z-30 mb-2 flex h-80 w-80 flex-col overflow-hidden rounded-lg border border-[var(--ws-border-subtle)] bg-[var(--ui-bg)] shadow-lg"
  >
    <div class="min-h-0 flex-1 overflow-y-auto p-2">
      <template v-if="tab === 'emoji'">
        <div v-if="recent.length > 0" class="mb-3">
          <p class="mb-1 px-1 text-[10px] font-medium tracking-wide text-[var(--ws-text-muted)]">
            最近使用
          </p>
          <div class="grid grid-cols-6 gap-0.5">
            <button
              v-for="emoji in recent"
              :key="`recent-${emoji}`"
              type="button"
              class="flex aspect-square items-center justify-center rounded-md text-xl hover:bg-[var(--ws-input-bar-bg)]"
              :title="emoji"
              @click="pickEmoji(emoji)"
            >
              {{ emoji }}
            </button>
          </div>
        </div>
        <p class="mb-1 px-1 text-[10px] font-medium tracking-wide text-[var(--ws-text-muted)]">
          默认表情
        </p>
        <div class="grid grid-cols-6 gap-0.5">
          <button
            v-for="emoji in SYSTEM_EMOJIS"
            :key="emoji"
            type="button"
            class="flex aspect-square items-center justify-center rounded-md text-xl hover:bg-[var(--ws-input-bar-bg)]"
            :title="emoji"
            @click="pickEmoji(emoji)"
          >
            {{ emoji }}
          </button>
        </div>
      </template>

      <template v-else-if="tab === 'custom'">
        <div
          v-if="customStickers.length === 0"
          class="flex h-full flex-col items-center justify-center gap-2 px-4 py-8"
        >
          <button
            type="button"
            class="flex size-20 items-center justify-center rounded-lg border border-dashed border-[var(--ws-border-subtle)] text-[var(--ws-text-muted)] hover:bg-[var(--ws-input-bar-bg)]"
            :disabled="uploading"
            aria-label="添加自定义表情"
            @click="emit('add-custom')"
          >
            <UIcon
              name="i-lucide-plus"
              class="size-8"
            />
          </button>
          <p class="text-xs text-[var(--ws-text-muted)]">
            点击添加自定义表情
          </p>
        </div>
        <div
          v-else
          class="grid grid-cols-4 gap-2"
        >
          <button
            type="button"
            class="flex aspect-square items-center justify-center rounded-lg border border-dashed border-[var(--ws-border-subtle)] text-[var(--ws-text-muted)] hover:bg-[var(--ws-input-bar-bg)]"
            :disabled="uploading"
            title="添加"
            @click="emit('add-custom')"
          >
            <UIcon
              name="i-lucide-plus"
              class="size-5"
            />
          </button>
          <button
            v-for="sticker in customStickers"
            :key="sticker.id"
            type="button"
            class="flex aspect-square items-center justify-center overflow-hidden rounded-lg hover:bg-[var(--ws-input-bar-bg)]"
            :title="sticker.name || '表情'"
            @click="pickCustom(sticker)"
          >
            <ImAuthenticatedImage
              v-if="sticker.downloadUrl"
              :src="sticker.downloadUrl"
              :alt="sticker.name || '表情'"
              @preview="pickCustom(sticker)"
            />
          </button>
        </div>
      </template>

      <template v-else>
        <div class="mb-2 flex flex-wrap items-center gap-1 px-1">
          <UButton
            size="xs"
            color="neutral"
            variant="soft"
            :disabled="settingsList.length === 0"
            @click="selectAll"
          >
            全选
          </UButton>
          <UButton
            size="xs"
            color="neutral"
            variant="ghost"
            :disabled="selectedIds.length === 0"
            @click="clearSelection"
          >
            取消
          </UButton>
          <UButton
            size="xs"
            color="neutral"
            variant="ghost"
            :disabled="selectedIds.length !== 1"
            @click="moveSelected('up')"
          >
            上移
          </UButton>
          <UButton
            size="xs"
            color="neutral"
            variant="ghost"
            :disabled="selectedIds.length !== 1"
            @click="moveSelected('down')"
          >
            下移
          </UButton>
          <UButton
            size="xs"
            color="error"
            variant="soft"
            :disabled="selectedIds.length === 0"
            @click="deleteSelected"
          >
            删除
          </UButton>
        </div>
        <div
          v-if="settingsList.length === 0"
          class="px-2 py-10 text-center text-xs text-[var(--ws-text-muted)]"
        >
          暂无自定义表情
        </div>
        <div
          v-else
          class="grid grid-cols-4 gap-2"
        >
          <button
            v-for="sticker in settingsList"
            :key="sticker.id"
            type="button"
            class="relative flex aspect-square items-center justify-center overflow-hidden rounded-lg border hover:bg-[var(--ws-input-bar-bg)]"
            :class="selectedIds.includes(sticker.id)
              ? 'border-[var(--ui-primary)]'
              : 'border-transparent'"
            @click="toggleSelect(sticker.id)"
          >
            <ImAuthenticatedImage
              v-if="sticker.downloadUrl"
              :src="sticker.downloadUrl"
              :alt="sticker.name || '表情'"
              @preview="toggleSelect(sticker.id)"
            />
            <span
              v-if="selectedIds.includes(sticker.id)"
              class="absolute right-1 top-1 flex size-4 items-center justify-center rounded-full bg-[var(--ui-primary)] text-[10px] text-white"
            >
              ✓
            </span>
          </button>
        </div>
      </template>
    </div>

    <div class="flex items-center gap-1 border-t border-[var(--ws-border-subtle)] px-2 py-1.5">
      <UButton
        icon="i-lucide-smile"
        size="xs"
        :color="tab === 'emoji' ? 'primary' : 'neutral'"
        :variant="tab === 'emoji' ? 'soft' : 'ghost'"
        square
        aria-label="系统表情"
        @click="tab = 'emoji'"
      />
      <UButton
        icon="i-lucide-heart"
        size="xs"
        :color="tab === 'custom' ? 'primary' : 'neutral'"
        :variant="tab === 'custom' ? 'soft' : 'ghost'"
        square
        aria-label="自定义表情"
        @click="tab = 'custom'"
      />
      <div class="flex-1" />
      <UButton
        icon="i-lucide-settings"
        size="xs"
        :color="tab === 'settings' ? 'primary' : 'neutral'"
        :variant="tab === 'settings' ? 'soft' : 'ghost'"
        square
        aria-label="表情设置"
        @click="tab = 'settings'"
      />
      <UButton
        icon="i-lucide-x"
        size="xs"
        color="neutral"
        variant="ghost"
        square
        aria-label="关闭"
        @click="close"
      />
    </div>
  </div>
</template>
