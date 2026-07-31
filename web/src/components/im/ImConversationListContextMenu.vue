<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { ConversationItem } from '../../api/app/im'
import { useImStore } from '../../stores/im'

const props = defineProps<{
  open: boolean
  x: number
  y: number
  conversation: ConversationItem | null
}>()

const emit = defineEmits<{
  close: []
}>()

const im = useImStore()
const menuRef = ref<HTMLElement | null>(null)
const tagFlyoutOpen = ref(false)
const creatingTag = ref(false)
const newTagName = ref('')
const newTagInputRef = ref<HTMLInputElement | null>(null)

const hasUnread = computed(() => (props.conversation?.unreadCount ?? 0) > 0)

const assignedTagIds = computed(() => new Set(props.conversation?.tagIds ?? []))

const style = computed(() => ({
  left: `${Math.min(props.x, typeof window !== 'undefined' ? window.innerWidth - 220 : props.x)}px`,
  top: `${Math.min(props.y, typeof window !== 'undefined' ? window.innerHeight - 280 : props.y)}px`
}))

function close() {
  tagFlyoutOpen.value = false
  creatingTag.value = false
  newTagName.value = ''
  emit('close')
}

function run(action: () => void) {
  action()
  close()
}

function onDocPointer(event: MouseEvent) {
  if (!props.open) {
    return
  }
  const el = menuRef.value
  if (el && event.target instanceof Node && el.contains(event.target)) {
    return
  }
  close()
}

function openCreateTag() {
  creatingTag.value = true
  newTagName.value = ''
  void nextTick(() => newTagInputRef.value?.focus())
}

async function submitNewTag() {
  if (!props.conversation) {
    return
  }
  const tag = await im.createAndAssignTag(props.conversation.id, newTagName.value)
  if (!tag) {
    return
  }
  close()
}

async function assignTag(tagId: string) {
  if (!props.conversation) {
    return
  }
  await im.assignTagToConversation(props.conversation.id, tagId)
  close()
}

watch(
  () => props.open,
  open => {
    if (!open) {
      tagFlyoutOpen.value = false
      creatingTag.value = false
      newTagName.value = ''
    }
  }
)

onMounted(() => {
  document.addEventListener('mousedown', onDocPointer, true)
})
onBeforeUnmount(() => {
  document.removeEventListener('mousedown', onDocPointer, true)
})
</script>

<template>
  <div
    v-if="open && conversation"
    ref="menuRef"
    class="fixed z-[80] min-w-44 rounded-lg border border-[var(--ws-border-subtle)] bg-[var(--ui-bg)] py-1 shadow-lg"
    :style="style"
    @contextmenu.prevent
  >
    <button
      type="button"
      class="flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm hover:bg-[var(--ws-list-hover)]"
      @click="run(() => im.togglePin(conversation!.id))"
    >
      <UIcon :name="conversation.pinned ? 'i-lucide-pin-off' : 'i-lucide-pin'" class="size-4" />
      {{ conversation.pinned ? '取消置顶' : '置顶' }}
    </button>

    <button
      v-if="hasUnread"
      type="button"
      class="flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm hover:bg-[var(--ws-list-hover)]"
      @click="run(() => im.markInboxRead(conversation!.id))"
    >
      <UIcon name="i-lucide-mail-open" class="size-4" />
      标记已读
    </button>
    <button
      v-else
      type="button"
      class="flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm hover:bg-[var(--ws-list-hover)]"
      @click="run(() => im.markInboxUnread(conversation!.id))"
    >
      <UIcon name="i-lucide-mail" class="size-4" />
      标记未读
    </button>

    <button
      type="button"
      class="flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm hover:bg-[var(--ws-list-hover)]"
      @click="run(() => im.toggleFlag(conversation!.id))"
    >
      <UIcon name="i-lucide-flag" class="size-4" />
      {{ conversation.flagged ? '取消旗标' : '旗标' }}
    </button>
    <button
      type="button"
      class="flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm hover:bg-[var(--ws-list-hover)]"
      @click="run(() => im.toggleMute(conversation!.id))"
    >
      <UIcon
        :name="conversation.muted ? 'i-lucide-bell' : 'i-lucide-bell-off'"
        class="size-4"
      />
      {{ conversation.muted ? '取消免打扰' : '消息免打扰' }}
    </button>

    <div
      class="relative"
      @mouseenter="tagFlyoutOpen = true"
      @mouseleave="tagFlyoutOpen = false"
    >
      <button
        type="button"
        class="flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm hover:bg-[var(--ws-list-hover)]"
        @click="tagFlyoutOpen = !tagFlyoutOpen"
      >
        <UIcon name="i-lucide-tags" class="size-4" />
        <span class="flex-1">标签</span>
        <UIcon name="i-lucide-chevron-right" class="size-3.5 text-[var(--ws-text-muted)]" />
      </button>

      <div
        v-if="tagFlyoutOpen"
        class="absolute top-0 left-full z-[81] ml-0.5 flex w-48 flex-col overflow-hidden rounded-lg border border-[var(--ws-border-subtle)] bg-[var(--ui-bg)] shadow-lg"
      >
        <div class="max-h-48 overflow-y-auto py-1">
          <p
            v-if="im.userTags.length === 0 && !creatingTag"
            class="px-3 py-2 text-xs text-[var(--ws-text-muted)]"
          >
            暂无标签
          </p>
          <button
            v-for="tag in im.userTags"
            :key="tag.id"
            type="button"
            class="flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm hover:bg-[var(--ws-list-hover)]"
            @click="assignTag(tag.id)"
          >
            <UIcon
              :name="assignedTagIds.has(tag.id) ? 'i-lucide-check' : 'i-lucide-tag'"
              class="size-3.5 shrink-0"
              :class="assignedTagIds.has(tag.id) ? 'text-[var(--ui-primary)]' : ''"
            />
            <span class="truncate">{{ tag.name }}</span>
          </button>
        </div>

        <div class="border-t border-[var(--ws-border-subtle)] p-1.5">
          <div v-if="creatingTag" class="flex flex-col gap-1.5 px-1 py-0.5">
            <input
              ref="newTagInputRef"
              v-model="newTagName"
              type="text"
              maxlength="32"
              placeholder="标签名称"
              class="w-full rounded-md border border-[var(--ws-border-subtle)] bg-transparent px-2 py-1 text-sm outline-none focus:border-[var(--ui-primary)]"
              @keydown.enter.prevent="submitNewTag"
              @keydown.escape.prevent="creatingTag = false"
            >
            <div class="flex justify-end gap-1">
              <button
                type="button"
                class="rounded px-2 py-0.5 text-xs text-[var(--ws-text-muted)] hover:bg-[var(--ws-list-hover)]"
                @click="creatingTag = false"
              >
                取消
              </button>
              <button
                type="button"
                class="rounded px-2 py-0.5 text-xs text-[var(--ui-primary)] hover:bg-[var(--ws-list-hover)]"
                @click="submitNewTag"
              >
                添加
              </button>
            </div>
          </div>
          <button
            v-else
            type="button"
            class="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm hover:bg-[var(--ws-list-hover)]"
            @click="openCreateTag"
          >
            <UIcon name="i-lucide-plus" class="size-3.5" />
            添加新标签
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
