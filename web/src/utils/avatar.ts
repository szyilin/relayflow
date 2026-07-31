import type { CSSProperties } from 'vue'
import { hashId } from './id'

const API_BASE = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') ?? ''

/**
 * Letter-avatar fills inspired by common enterprise collab apps:
 * luminous pastels, small lightness delta, no dark “sink” at the bottom
 * (dark→muddy bases read as dated). White glyph stays readable on all stops.
 *
 * `lift` → `mid` → `base`：左上偏亮、右下略深但仍保持中亮。
 */
const AVATAR_TONES: ReadonlyArray<{
  readonly lift: string
  readonly mid: string
  readonly base: string
}> = [
  { lift: '#FFB4A4', mid: '#FF8F8A', base: '#F06B7A' }, // peach → rose
  { lift: '#FFC98A', mid: '#FFA85C', base: '#F08C45' }, // soft amber
  { lift: '#A8E8CF', mid: '#6FD4B0', base: '#4DBFA0' }, // mint → teal
  { lift: '#A8CEFF', mid: '#7AADFF', base: '#5B93F5' }, // sky（勿藏青）
  { lift: '#D4C2FF', mid: '#B398F5', base: '#9A78F0' }, // lilac
  { lift: '#FFB0CC', mid: '#FF7AA8', base: '#E85A95' }, // pink
  { lift: '#9EE4EE', mid: '#5FCBD8', base: '#3FBACB' }, // aqua
  { lift: '#BEEF98', mid: '#8FD466', base: '#6EBF52' }, // soft lime
  { lift: '#B8C4FF', mid: '#8C9AF5', base: '#6F82F0' }, // periwinkle
  { lift: '#F5D89A', mid: '#E8B85C', base: '#E0A03A' } // warm gold
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
 * Deterministic soft avatar fill from a stable id (prefer userId, else name).
 * Same seed → same tone across pages.
 *
 * Soft 3-stop diagonal. No white radial sheen, no dark navy/charcoal sinks.
 */
export function avatarGradientFromId(seed?: string | number | null): string {
  const key = seed == null || String(seed).trim() === '' ? '0' : String(seed)
  const tone = AVATAR_TONES[hashId(key) % AVATAR_TONES.length]!
  return `linear-gradient(135deg, ${tone.lift} 0%, ${tone.mid} 46%, ${tone.base} 100%)`
}

/** Style for letter/fallback avatars; omit when a photo `src` is present. */
export function letterAvatarStyle(
  seed?: string | number | null,
  hasSrc?: boolean
): CSSProperties | undefined {
  if (hasSrc) {
    return undefined
  }
  const key = seed == null || String(seed).trim() === '' ? '0' : String(seed)
  const tone = AVATAR_TONES[hashId(key) % AVATAR_TONES.length]!
  return {
    backgroundImage: avatarGradientFromId(seed),
    backgroundColor: tone.base,
    color: '#ffffff'
  }
}

/** CSS vars for `.workspace-letter-avatar` (beats theme `bg-*` shorthand). */
export function letterAvatarCssVars(seed?: string | number | null): CSSProperties {
  const key = seed == null || String(seed).trim() === '' ? '0' : String(seed)
  const tone = AVATAR_TONES[hashId(key) % AVATAR_TONES.length]!
  return {
    '--ws-avatar-lift': tone.lift,
    '--ws-avatar-mid': tone.mid,
    '--ws-avatar-base': tone.base
  } as CSSProperties
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
