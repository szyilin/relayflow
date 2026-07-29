import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const CLIENT_TTL_MS = 6_000

type TypingEntry = {
  userId: string
  expireAt: number
}

/**
 * Ephemeral typing indicators per conversation (WS presence domain).
 */
export const useTypingStore = defineStore('im-typing', () => {
  /** conversationId → active typers */
  const byConversation = ref<Record<string, TypingEntry[]>>({})

  let sweepTimer: ReturnType<typeof setInterval> | null = null

  function ensureSweep() {
    if (sweepTimer) {
      return
    }
    sweepTimer = setInterval(() => {
      const now = Date.now()
      const next: Record<string, TypingEntry[]> = {}
      let changed = false
      for (const [conversationId, entries] of Object.entries(byConversation.value)) {
        const kept = entries.filter(e => e.expireAt > now)
        if (kept.length !== entries.length) {
          changed = true
        }
        if (kept.length) {
          next[conversationId] = kept
        } else if (entries.length) {
          changed = true
        }
      }
      if (changed) {
        byConversation.value = next
      }
    }, 1_000)
  }

  function applyTypingUpdated(payload: { conversationId: string, userId: string, typing: boolean }) {
    const conversationId = payload.conversationId
    const userId = payload.userId
    if (!conversationId || !userId) {
      return
    }
    ensureSweep()
    const current = [...(byConversation.value[conversationId] ?? [])]
    const without = current.filter(e => e.userId !== userId)
    if (payload.typing) {
      without.push({ userId, expireAt: Date.now() + CLIENT_TTL_MS })
      byConversation.value = {
        ...byConversation.value,
        [conversationId]: without
      }
      return
    }
    if (without.length === current.length) {
      return
    }
    if (!without.length) {
      const { [conversationId]: _, ...rest } = byConversation.value
      byConversation.value = rest
      return
    }
    byConversation.value = {
      ...byConversation.value,
      [conversationId]: without
    }
  }

  function clearConversation(conversationId: string) {
    if (!byConversation.value[conversationId]) {
      return
    }
    const { [conversationId]: _, ...rest } = byConversation.value
    byConversation.value = rest
  }

  function typingUserIds(conversationId: string | null | undefined): string[] {
    if (!conversationId) {
      return []
    }
    const now = Date.now()
    return (byConversation.value[conversationId] ?? [])
      .filter(e => e.expireAt > now)
      .map(e => e.userId)
  }

  function typingLabel(
    conversationId: string | null | undefined,
    options: {
      selfUserId?: string | null
      resolveName?: (userId: string) => string
      isDirect?: boolean
    } = {}
  ): string {
    const ids = typingUserIds(conversationId).filter(id => id !== options.selfUserId)
    if (!ids.length) {
      return ''
    }
    if (options.isDirect) {
      return '对方正在输入…'
    }
    const names = ids.slice(0, 3).map(id => options.resolveName?.(id) || '成员')
    if (ids.length > 3) {
      return `${names.join('、')}等正在输入…`
    }
    return `${names.join('、')}正在输入…`
  }

  const activeCount = computed(() =>
    Object.values(byConversation.value).reduce((sum, list) => sum + list.length, 0))

  return {
    byConversation,
    activeCount,
    applyTypingUpdated,
    clearConversation,
    typingUserIds,
    typingLabel
  }
})
