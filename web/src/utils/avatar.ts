import type { CSSProperties } from 'vue'
import { hashId } from './id'

const API_BASE = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') ?? ''

/** Stable dual-tone pairs for letter avatars (readable white glyph on both themes). */
const AVATAR_GRADIENT_PAIRS: ReadonlyArray<readonly [string, string]> = [
  ['#3b82f6', '#06b6d4'],
  ['#14b8a6', '#22c55e'],
  ['#8b5cf6', '#ec4899'],
  ['#f59e0b', '#ef4444'],
  ['#6366f1', '#3b82f6'],
  ['#0ea5e9', '#14b8a6'],
  ['#f97316', '#eab308'],
  ['#a855f7', '#6366f1'],
  ['#10b981', '#0ea5e9'],
  ['#e11d48', '#f97316']
]

export function avatarTextFromName(name?: string | null): string {
  const trimmed = name?.trim()
  if (!trimmed) {
    return '员'
  }
  return trimmed.slice(0, 1)
}

export function resolveAvatarUrl(avatar?: string | number | null): string | undefined {
  if (avatar == null) {
    return undefined
  }
  const value = String(avatar).trim()
  if (!value) {
    return undefined
  }
  if (value.startsWith('http://') || value.startsWith('https://') || value.startsWith('/')) {
    return value.startsWith('/') ? `${API_BASE}${value}` : value
  }
  return `${API_BASE}/app-api/infra/file/public/${value}`
}

/**
 * Deterministic CSS linear-gradient from a stable id (prefer userId, else name).
 * Same seed → same pair across pages.
 */
export function avatarGradientFromId(seed?: string | number | null): string {
  const key = seed == null || String(seed).trim() === '' ? '0' : String(seed)
  const pair = AVATAR_GRADIENT_PAIRS[hashId(key) % AVATAR_GRADIENT_PAIRS.length]
  return `linear-gradient(135deg, ${pair[0]} 0%, ${pair[1]} 100%)`
}

/** Style for letter/fallback avatars; omit when a photo `src` is present. */
export function letterAvatarStyle(
  seed?: string | number | null,
  hasSrc?: boolean
): CSSProperties | undefined {
  if (hasSrc) {
    return undefined
  }
  return {
    backgroundImage: avatarGradientFromId(seed),
    backgroundColor: 'transparent',
    color: '#ffffff'
  }
}

export function tenantTileColor(tenantId: string | number): string {
  const palette = [
    '#3370ff',
    '#34c724',
    '#ff8800',
    '#f54a45',
    '#7f3bf5',
    '#14c0ff'
  ]
  const key = typeof tenantId === 'number' ? String(tenantId) : tenantId
  return palette[hashId(key) % palette.length]
}
