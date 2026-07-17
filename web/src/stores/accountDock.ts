/**
 * Multi-account dock (本机已登录会话列表).
 *
 * V1 persists bearer tokens in localStorage (`relayflow:account-dock`) so users can
 * one-click switch accounts. XSS can read every docked JWT — threat model and target
 * hardening (httpOnly / opaque session) are documented in
 * docs/dev/workspace-ui-patterns.md § Account Dock.
 *
 * Same-user cross-tenant switch MUST go through auth.switchTenant (API), not by
 * trusting a copied token alone — see auth.switchToDockEntry.
 */
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { TenantSummary } from '../api/app/tenant'
import { idsEqual } from '../utils/id'
import { useAuthStore } from './auth'

export interface AccountDockEntry {
  key: string
  userId: string
  username: string
  nickname: string
  avatar?: string
  tenantId: string
  tenantName: string
  /** V1: bearer for cross-user restore; same-user tenant switch refreshes via API. */
  token: string
  isAdmin?: boolean
}

const DOCK_KEY = 'relayflow:account-dock'

function buildKey(userId: string, tenantId: string) {
  return `${userId}:${tenantId}`
}

function readStoredEntries(): AccountDockEntry[] {
  const raw = localStorage.getItem(DOCK_KEY)
  if (!raw) {
    return []
  }

  try {
    const parsed = JSON.parse(raw) as AccountDockEntry[]
    if (!Array.isArray(parsed)) {
      return []
    }
    return parsed.map(entry => ({
      ...entry,
      userId: String(entry.userId),
      tenantId: String(entry.tenantId)
    }))
  } catch {
    return []
  }
}

function persistEntries(entries: AccountDockEntry[]) {
  localStorage.setItem(DOCK_KEY, JSON.stringify(entries))
}

export const useAccountDockStore = defineStore('accountDock', () => {
  const entries = ref<AccountDockEntry[]>(readStoredEntries())

  const currentKey = computed(() => {
    const auth = useAuthStore()
    if (auth.userId == null || auth.tenantId == null) {
      return null
    }
    return buildKey(auth.userId, auth.tenantId)
  })

  const otherEntries = computed(() =>
    entries.value.filter(entry => entry.key !== currentKey.value))

  function upsertEntry(entry: AccountDockEntry) {
    const next = entries.value.filter(item => item.key !== entry.key)
    next.unshift(entry)
    entries.value = next
    persistEntries(next)
  }

  function removeByUserId(userId: string) {
    const next = entries.value.filter(entry => entry.userId !== userId)
    entries.value = next
    persistEntries(next)
    return next
  }

  function removeEntry(key: string) {
    const next = entries.value.filter(entry => entry.key !== key)
    entries.value = next
    persistEntries(next)
    return next
  }

  function syncCurrentSession(options?: { isAdmin?: boolean }) {
    const auth = useAuthStore()
    if (!auth.isAuthenticated || auth.userId == null || auth.tenantId == null || !auth.token) {
      return
    }

    upsertEntry({
      key: buildKey(auth.userId, auth.tenantId),
      userId: auth.userId,
      username: auth.user?.username ?? '',
      nickname: auth.user?.nickname ?? auth.user?.username ?? '',
      avatar: auth.user?.avatar,
      tenantId: auth.tenantId,
      tenantName: auth.activeTenantName,
      token: auth.token,
      isAdmin: options?.isAdmin ?? auth.isAdmin
    })
  }

  async function syncAllTenantsForCurrentAccount() {
    const auth = useAuthStore()
    if (!auth.isAuthenticated || auth.userId == null || !auth.token) {
      return
    }

    syncCurrentSession()

    let tenants: TenantSummary[] = auth.tenants
    if (tenants.length === 0) {
      try {
        tenants = await auth.fetchMyTenants()
      } catch {
        return
      }
    }

    for (const tenant of tenants) {
      if (idsEqual(tenant.tenantId, auth.tenantId)) {
        continue
      }
      const key = buildKey(auth.userId, tenant.tenantId)
      const existing = entries.value.find(item => item.key === key)
      upsertEntry({
        key,
        userId: auth.userId,
        username: auth.user?.username ?? existing?.username ?? '',
        nickname: auth.user?.nickname ?? existing?.nickname ?? '',
        avatar: auth.user?.avatar ?? existing?.avatar,
        tenantId: tenant.tenantId,
        tenantName: tenant.tenantName,
        token: auth.token,
        isAdmin: existing?.isAdmin
      })
    }
  }

  function updateCurrentProfile(nickname: string, avatar?: string) {
    const auth = useAuthStore()
    if (auth.userId == null || auth.tenantId == null || !auth.token) {
      return
    }

    const key = buildKey(auth.userId, auth.tenantId)
    entries.value = entries.value.map((entry) => {
      if (entry.userId !== auth.userId) {
        return entry
      }
      return {
        ...entry,
        nickname: entry.key === key ? nickname : entry.nickname,
        avatar: avatar ?? entry.avatar,
        token: entry.key === key ? auth.token! : entry.token,
        tenantName: entry.key === key ? auth.activeTenantName : entry.tenantName
      }
    })
    persistEntries(entries.value)
  }

  function clearAll() {
    entries.value = []
    localStorage.removeItem(DOCK_KEY)
  }

  return {
    entries,
    currentKey,
    otherEntries,
    upsertEntry,
    removeByUserId,
    removeEntry,
    syncCurrentSession,
    syncAllTenantsForCurrentAccount,
    updateCurrentProfile,
    clearAll
  }
})
