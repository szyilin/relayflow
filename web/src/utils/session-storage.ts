/**
 * 统一会话 localStorage 键。
 * 同一 JWT 服务 `/app` 与 `/admin`；历史键曾带 `admin:` 前缀，读取时自动迁移。
 */

export const SESSION_KEYS = {
  accessToken: 'relayflow:access-token',
  tenantId: 'relayflow:tenant-id',
  user: 'relayflow:user',
  tenants: 'relayflow:tenants'
} as const

const LEGACY_KEYS: Record<keyof typeof SESSION_KEYS, string> = {
  accessToken: 'relayflow:admin:access-token',
  tenantId: 'relayflow:admin:tenant-id',
  user: 'relayflow:admin:user',
  tenants: 'relayflow:admin:tenants'
}

function migrateKey(current: string, legacy: string): string | null {
  const value = localStorage.getItem(current)
  if (value != null) {
    return value
  }
  const legacyValue = localStorage.getItem(legacy)
  if (legacyValue == null) {
    return null
  }
  localStorage.setItem(current, legacyValue)
  localStorage.removeItem(legacy)
  return legacyValue
}

/** 应用启动时把旧 `relayflow:admin:*` 迁到新键（幂等）。 */
export function migrateLegacySessionKeys(): void {
  for (const key of Object.keys(SESSION_KEYS) as Array<keyof typeof SESSION_KEYS>) {
    migrateKey(SESSION_KEYS[key], LEGACY_KEYS[key])
  }
}

export function getSessionItem(key: keyof typeof SESSION_KEYS): string | null {
  return migrateKey(SESSION_KEYS[key], LEGACY_KEYS[key])
}

export function setSessionItem(key: keyof typeof SESSION_KEYS, value: string): void {
  localStorage.setItem(SESSION_KEYS[key], value)
  localStorage.removeItem(LEGACY_KEYS[key])
}

export function removeSessionItem(key: keyof typeof SESSION_KEYS): void {
  localStorage.removeItem(SESSION_KEYS[key])
  localStorage.removeItem(LEGACY_KEYS[key])
}

export function clearSessionStorage(): void {
  for (const key of Object.keys(SESSION_KEYS) as Array<keyof typeof SESSION_KEYS>) {
    removeSessionItem(key)
  }
}

export function getAccessToken(): string | null {
  return getSessionItem('accessToken')
}
