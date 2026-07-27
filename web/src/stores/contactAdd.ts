import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { ContactAccountSearchHit } from '../api/app/contacts'
import { useContactsStore } from './contacts'

export type { ContactAccountSearchHit }

export interface OutgoingContactRequest {
  id: string
  targetUserId: string
  displayName: string
  orgLabel: string
  avatarText: string
  createdAt: string
  status: 'pending' | 'accepted' | 'cancelled'
}

/**
 * Add-external-contact flow for `-web`.
 * Search/create are local drafts until contact-add-external-api.
 */
export const useContactAddStore = defineStore('contactAdd', () => {
  const lastHits = ref<ContactAccountSearchHit[]>([])
  const outgoing = ref<OutgoingContactRequest[]>([])
  const lastError = ref<string | null>(null)
  const searching = ref(false)

  function isExactQuery(query: string): { mobile?: string, email?: string } | null {
    const q = query.trim()
    if (/^\d{11}$/.test(q)) {
      return { mobile: q }
    }
    if (q.includes('@') && q.length >= 5) {
      return { email: q }
    }
    return null
  }

  function isAlreadyRelated(userId: string): boolean {
    const contacts = useContactsStore()
    if (contacts.externalContacts.some(item => item.userId === userId)) {
      return true
    }
    return outgoing.value.some(
      item => item.targetUserId === userId && item.status === 'pending'
    )
  }

  /** Local exact-match demo results（integrate 删除）. */
  function buildDraftHits(query: string): ContactAccountSearchHit[] {
    const parsed = isExactQuery(query)
    if (!parsed) {
      return []
    }
    const suffix = parsed.mobile?.slice(-6) ?? parsed.email?.split('@')[0]?.slice(0, 6) ?? '000000'
    const candidates: ContactAccountSearchHit[] = [
      {
        userId: `draft-ext-${suffix}-a`,
        displayName: '样依琳',
        orgLabel: '合作方演示企业',
        avatarText: '样',
        alreadyAdded: false
      },
      {
        userId: `draft-ext-${suffix}-b`,
        displayName: `用户 ${suffix}`,
        orgLabel: '另一组织',
        avatarText: '用',
        alreadyAdded: false
      }
    ]
    return candidates.map(item => ({
      ...item,
      alreadyAdded: isAlreadyRelated(item.userId)
    }))
  }

  async function search(query: string): Promise<ContactAccountSearchHit[]> {
    searching.value = true
    lastError.value = null
    try {
      if (!isExactQuery(query)) {
        lastHits.value = []
        lastError.value = '请输入确切的 11 位手机号或邮箱'
        return []
      }
      // Prefer API when ready; `-web` uses draft hits.
      lastHits.value = buildDraftHits(query)
      return lastHits.value
    } finally {
      searching.value = false
    }
  }

  async function sendRequest(hit: ContactAccountSearchHit, message?: string) {
    lastError.value = null
    if (hit.alreadyAdded || isAlreadyRelated(hit.userId)) {
      lastError.value = '已添加或已有待处理申请'
      throw new Error(lastError.value)
    }
    outgoing.value = [
      {
        id: `out_${Date.now()}`,
        targetUserId: hit.userId,
        displayName: hit.displayName,
        orgLabel: hit.orgLabel,
        avatarText: hit.avatarText,
        createdAt: new Date().toISOString(),
        status: 'pending'
      },
      ...outgoing.value
    ]
    lastHits.value = lastHits.value.map(item =>
      item.userId === hit.userId
        ? { ...item, alreadyAdded: true }
        : item
    )
    void message
  }

  function resetForTenantSwitch() {
    lastHits.value = []
    outgoing.value = []
    lastError.value = null
  }

  return {
    lastHits,
    outgoing,
    lastError,
    searching,
    isExactQuery,
    isAlreadyRelated,
    search,
    sendRequest,
    resetForTenantSwitch
  }
})
