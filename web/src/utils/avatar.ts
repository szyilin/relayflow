const API_BASE = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') ?? ''

export function avatarTextFromName(name?: string | null): string {
  const trimmed = name?.trim()
  if (!trimmed) {
    return '员'
  }
  return trimmed.slice(0, 1)
}

export function resolveAvatarUrl(avatar?: string | null): string | undefined {
  if (!avatar?.trim()) {
    return undefined
  }
  const value = avatar.trim()
  if (value.startsWith('http://') || value.startsWith('https://') || value.startsWith('/')) {
    return value.startsWith('/') ? `${API_BASE}${value}` : value
  }
  return `${API_BASE}/app-api/infra/file/public/${value}`
}

export function tenantTileColor(tenantId: number): string {
  const palette = [
    '#3370ff',
    '#34c724',
    '#ff8800',
    '#f54a45',
    '#7f3bf5',
    '#14c0ff'
  ]
  return palette[Math.abs(tenantId) % palette.length]
}
