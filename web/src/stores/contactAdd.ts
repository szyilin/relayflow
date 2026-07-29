import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  createContactRequest,
  searchContactAccounts,
  type ContactAccountSearchHit
} from '../api/app/contacts'
import { ApiError } from '../api/request'
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
 * Add-external-contact flow — search / create against app-api.
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

  async function search(query: string): Promise<ContactAccountSearchHit[]> {
    searching.value = true
    lastError.value = null
    try {
      const parsed = isExactQuery(query)
      if (!parsed) {
        lastHits.value = []
        lastError.value = '请输入确切的 11 位手机号或邮箱'
        return []
      }
      const hits = await searchContactAccounts(parsed)
      lastHits.value = hits.map(item => ({
        ...item,
        alreadyAdded: item.alreadyAdded || isAlreadyRelated(item.userId)
      }))
      return lastHits.value
    } catch (error) {
      lastHits.value = []
      lastError.value = error instanceof ApiError
        ? error.message
        : (error instanceof Error ? error.message : '搜索失败')
      throw error
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
    try {
      const created = await createContactRequest({
        targetUserId: hit.userId,
        message
      })
      outgoing.value = [
        {
          id: created.id,
          targetUserId: hit.userId,
          displayName: hit.displayName,
          orgLabel: hit.orgLabel,
          avatarText: hit.avatarText,
          createdAt: created.createTime ?? new Date().toISOString(),
          status: 'pending'
        },
        ...outgoing.value
      ]
      lastHits.value = lastHits.value.map(item =>
        item.userId === hit.userId
          ? { ...item, alreadyAdded: true }
          : item
      )
    } catch (error) {
      lastError.value = error instanceof ApiError
        ? error.message
        : (error instanceof Error ? error.message : '发起申请失败')
      throw error
    }
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
