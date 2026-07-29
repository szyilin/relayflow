import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const USER_CLIENT_TTL_MS = 6_000
/** Align with server BOT_TYPING_TTL (60s); renew keeps indicator alive. */
const BOT_CLIENT_TTL_MS = 65_000

type TypingEntry = {
  /** userId or `bot:{botId}` */
  key: string
  kind: 'user' | 'bot'
  expireAt: number
}

export type TypingUpdatedPayload = {
  conversationId: string
  typing: boolean
  senderType?: 'user' | 'bot' | string
  userId?: string
  botId?: string
}

/**
 * Ephemeral typing indicators per conversation (WS presence domain).
 * Supports human users and bots (Telegram-style「对方正在输入…」).
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

  function entryKey(payload: TypingUpdatedPayload): string | null {
    if (payload.senderType === 'bot' || (payload.botId && !payload.userId)) {
      return payload.botId ? `bot:${payload.botId}` : null
    }
    return payload.userId || null
  }

  function applyTypingUpdated(payload: TypingUpdatedPayload) {
    const conversationId = payload.conversationId
    const key = entryKey(payload)
    if (!conversationId || !key) {
      return
    }
    const kind: 'user' | 'bot' = key.startsWith('bot:') ? 'bot' : 'user'
    const ttl = kind === 'bot' ? BOT_CLIENT_TTL_MS : USER_CLIENT_TTL_MS
    ensureSweep()
    const current = [...(byConversation.value[conversationId] ?? [])]
    const without = current.filter(e => e.key !== key)
    if (payload.typing) {
      without.push({ key, kind, expireAt: Date.now() + ttl })
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

  function activeEntries(conversationId: string | null | undefined): TypingEntry[] {
    if (!conversationId) {
      return []
    }
    const now = Date.now()
    return (byConversation.value[conversationId] ?? []).filter(e => e.expireAt > now)
  }

  function typingUserIds(conversationId: string | null | undefined): string[] {
    return activeEntries(conversationId)
      .filter(e => e.kind === 'user')
      .map(e => e.key)
  }

  function hasBotTyping(conversationId: string | null | undefined): boolean {
    return activeEntries(conversationId).some(e => e.kind === 'bot')
  }

  function typingLabel(
    conversationId: string | null | undefined,
    options: {
      selfUserId?: string | null
      resolveName?: (userId: string) => string
      isDirect?: boolean
      isBotDm?: boolean
    } = {}
  ): string {
    const entries = activeEntries(conversationId)
    if (!entries.length) {
      return ''
    }
    if (options.isBotDm || (options.isDirect && entries.some(e => e.kind === 'bot'))) {
      return '对方正在输入…'
    }
    if (entries.some(e => e.kind === 'bot') && !entries.some(e => e.kind === 'user')) {
      return '对方正在输入…'
    }
    const ids = entries
      .filter(e => e.kind === 'user')
      .map(e => e.key)
      .filter(id => id !== options.selfUserId)
    if (!ids.length) {
      return entries.some(e => e.kind === 'bot') ? '对方正在输入…' : ''
    }
    if (options.isDirect) {
      return '对方正在输入…'
    }
    const names = ids.slice(0, 3).map(id => options.resolveName?.(id) || '成员')
    const botExtra = entries.some(e => e.kind === 'bot')
    if (ids.length > 3 || botExtra) {
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
    hasBotTyping,
    typingLabel
  }
})
