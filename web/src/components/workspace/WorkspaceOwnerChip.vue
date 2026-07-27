<script setup lang="ts">
import { computed } from 'vue'
import WorkspaceBusinessCard from './WorkspaceBusinessCard.vue'
import { useClickAnchoredPopover } from '../../composables/useClickAnchoredPopover'
import { useAuthStore } from '../../stores/auth'
import { useContactsStore } from '../../stores/contacts'
import { useRouter } from 'vue-router'
import { useImStore } from '../../stores/im'
import { avatarTextFromName, resolveAvatarUrl } from '../../utils/avatar'

const router = useRouter()

const props = withDefaults(defineProps<{
  userId: string
  /** Hint from API; ignored when it is bare「我」and a real name can be resolved */
  displayName?: string | null
  avatarUrl?: string | null
  size?: 'xs' | 'sm'
}>(), {
  displayName: null,
  avatarUrl: null,
  size: 'xs'
})

const auth = useAuthStore()
const contacts = useContactsStore()
const im = useImStore()
const toast = useToast()

const {
  open: cardOpen,
  reference: cardReference,
  content: cardContent,
  show: showCard,
  close: closeCard
} = useClickAnchoredPopover<null>()

const isSelf = computed(() => Boolean(auth.userId && String(auth.userId) === String(props.userId)))

const contact = computed(() =>
  contacts.members.find(m => String(m.id) === String(props.userId)))

const resolvedName = computed(() => {
  if (isSelf.value) {
    return auth.user?.nickname || auth.user?.username || contact.value?.nickname || '用户'
  }
  const hint = props.displayName?.trim()
  if (hint && hint !== '我') {
    return hint
  }
  return contact.value?.nickname || contact.value?.username || hint || String(props.userId)
})

const resolvedAvatarUrl = computed(() => {
  // Prefer current session avatar for self — list payloads may carry a stale file id.
  if (isSelf.value) {
    const fromAuth = resolveAvatarUrl(auth.user?.avatar)
    if (fromAuth) {
      return fromAuth
    }
  }
  if (props.avatarUrl) {
    return resolveAvatarUrl(props.avatarUrl) || undefined
  }
  return undefined
})

const avatarText = computed(() =>
  contact.value?.avatarText || avatarTextFromName(resolvedName.value))

const avatarClass = computed(() =>
  props.size === 'sm' ? 'size-6 text-[10px]' : 'size-5 text-[9px]')

function onChipClick(event: MouseEvent) {
  event.preventDefault()
  event.stopPropagation()
  if (!props.userId) {
    return
  }
  showCard(null, event)
}

function handleOpenChange(next: boolean) {
  if (!next) {
    closeCard()
  }
}

async function openMessage() {
  closeCard()
  try {
    await im.openDirectChat(props.userId, {
      title: resolvedName.value,
      avatarText: avatarText.value
    })
    await router.push('/app/messages')
  } catch (e: unknown) {
    toast.add({
      title: '无法打开会话',
      description: e instanceof Error ? e.message : '请稍后重试',
      color: 'error'
    })
  }
}
</script>

<template>
  <UPopover
    :open="cardOpen"
    :reference="cardReference"
    :content="cardContent"
    :ui="{ content: 'p-0 overflow-hidden' }"
    @update:open="handleOpenChange"
  >
    <button
      type="button"
      class="inline-flex max-w-full min-w-0 items-center gap-1.5 rounded-md px-0.5 py-0.5 text-left hover:bg-[var(--ws-bg-muted,var(--ui-bg-elevated))]"
      :title="resolvedName"
      @click="onChipClick"
    >
      <UAvatar
        :src="resolvedAvatarUrl"
        :alt="resolvedName"
        :text="avatarText"
        :class="avatarClass"
      />
      <span class="min-w-0 truncate text-xs text-[var(--ws-text-secondary,var(--ui-text-muted))]">
        {{ resolvedName }}
      </span>
    </button>
    <template #content>
      <WorkspaceBusinessCard
        :mode="isSelf ? 'self' : 'peer'"
        :user-id="props.userId"
        :nickname="resolvedName"
        :username="contact?.username || (isSelf ? auth.user?.username : '') || ''"
        :avatar-url="resolvedAvatarUrl"
        :avatar-text="avatarText"
        @message="openMessage"
      />
    </template>
  </UPopover>
</template>
