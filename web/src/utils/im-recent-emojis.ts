import { RECENT_EMOJI_MAX } from '../constants/im-system-emojis'

function storageKey(userId: string) {
  return `rf:im:recent-emojis:${userId || 'anon'}`
}

export function loadRecentEmojis(userId: string): string[] {
  try {
    const raw = localStorage.getItem(storageKey(userId))
    if (!raw) {
      return []
    }
    const parsed = JSON.parse(raw) as unknown
    if (!Array.isArray(parsed)) {
      return []
    }
    return parsed.filter(item => typeof item === 'string' && item.length > 0).slice(0, RECENT_EMOJI_MAX)
  } catch {
    return []
  }
}

export function pushRecentEmoji(userId: string, emoji: string): string[] {
  const normalized = emoji.trim()
  if (!normalized) {
    return loadRecentEmojis(userId)
  }
  const next = [normalized, ...loadRecentEmojis(userId).filter(item => item !== normalized)]
    .slice(0, RECENT_EMOJI_MAX)
  try {
    localStorage.setItem(storageKey(userId), JSON.stringify(next))
  } catch {
    // ignore quota
  }
  return next
}
