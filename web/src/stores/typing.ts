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
      resolveBotName?: (botId: string) => string
      isDirect?: boolean
      isBotDm?: boolean
      isGroup?: boolean
    } = {}
  ): string {
    const entries = activeEntries(conversationId)
    if (!entries.length) {
      return ''
    }
    // 1:1 / bot_dm keep WeChat-style「对方」
    if (options.isBotDm || options.isDirect) {
      return '对方正在输入…'
    }

    const names: string[] = []
    for (const entry of entries) {
      if (entry.kind === 'user') {
        if (entry.key === options.selfUserId) {
          continue
        }
        names.push(options.resolveName?.(entry.key) || '成员')
      } else {
        const botId = entry.key.startsWith('bot:') ? entry.key.slice(4) : entry.key
        names.push(options.resolveBotName?.(botId) || '助手')
      }
    }
    if (!names.length) {
      return ''
    }
    if (names.length === 1) {
      return `${names[0]}正在输入…`
    }
    if (names.length === 2) {
      return `${names[0]}、${names[1]}正在输入…`
    }
    return `${names[0]}等 ${names.length} 人正在输入…`
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
