<script setup lang="ts">
import WorkspaceAvatar from './WorkspaceAvatar.vue'
import { computed, ref, watch } from 'vue'
import type { GroupMemberItem } from '../../api/app/im'

const props = defineProps<{
  open: boolean
  members: GroupMemberItem[]
  query: string
  activeIndex: number
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  select: [member: GroupMemberItem]
  'update:activeIndex': [value: number]
}>()

const listRef = ref<HTMLElement | null>(null)

const filtered = computed(() => {
  const q = props.query.trim().toLowerCase()
  if (!q) {
    return props.members
  }
  return props.members.filter((member) => {
    const nick = member.nickname.toLowerCase()
    const code = (member.botCode ?? '').toLowerCase()
    return nick.includes(q) || code.includes(q)
  })
})

watch(filtered, (list) => {
  if (props.activeIndex >= list.length) {
    emit('update:activeIndex', Math.max(0, list.length - 1))
  }
})

watch(
  () => props.activeIndex,
  async (index) => {
    await Promise.resolve()
    const el = listRef.value?.querySelector<HTMLElement>(`[data-mention-index="${index}"]`)
    el?.scrollIntoView({ block: 'nearest' })
  }
)

function memberKey(member: GroupMemberItem) {
  if (member.subjectType === 'bot') {
    return `bot:${member.botId ?? member.botCode}`
  }
  return `user:${member.userId}`
}

function subtitle(member: GroupMemberItem) {
  if (member.subjectType === 'bot') {
    return member.botCode ? `机器人 · ${member.botCode}` : '机器人'
  }
  if (member.role === 'owner') {
    return '群主'
  }
  return '成员'
}

function onSelect(member: GroupMemberItem) {
  emit('select', member)
  emit('update:open', false)
}
</script>

<template>
  <div
    v-show="open"
    class="absolute inset-x-0 bottom-full z-20 mb-2 overflow-hidden rounded-xl border border-[var(--ws-border)] bg-[var(--ws-panel-bg)] shadow-lg"
    role="listbox"
    aria-label="选择要 @ 的成员"
  >
    <div class="border-b border-[var(--ws-border-subtle)] px-3 py-2 text-xs text-[var(--ws-text-muted)]">
      {{ query ? `匹配「${query}」` : '选择要 @ 的成员' }}
    </div>
    <div
      ref="listRef"
      class="max-h-56 overflow-y-auto py-1"
    >
      <button
        v-for="(member, index) in filtered"
        :key="memberKey(member)"
        type="button"
        role="option"
        :aria-selected="index === activeIndex"
        :data-mention-index="index"
        class="flex w-full items-center gap-2.5 px-3 py-2 text-left text-sm transition-colors"
        :class="index === activeIndex
          ? 'bg-primary/15 text-[var(--ws-text)]'
          : 'text-[var(--ws-text)] hover:bg-[var(--ws-rail-hover)]'"
        @mousedown.prevent="onSelect(member)"
        @mouseenter="emit('update:activeIndex', index)"
      >
        <WorkspaceAvatar
          :text="member.avatarText || member.nickname.slice(0, 1)"
          :seed="member.userId || member.botId || member.nickname"
          size="xs"
        />
        <span class="min-w-0 flex-1 truncate font-medium">
          {{ member.nickname }}
        </span>
        <span class="shrink-0 text-xs text-[var(--ws-text-muted)]">
          {{ subtitle(member) }}
        </span>
      </button>
      <div
        v-if="filtered.length === 0"
        class="px-3 py-6 text-center text-sm text-[var(--ws-text-muted)]"
      >
        无匹配成员
      </div>
    </div>
  </div>
</template>
