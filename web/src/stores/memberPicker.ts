import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getGroupMembers } from '../api/app/im'
import { useAuthStore } from './auth'
import { useImStore } from './im'

export interface ManagedGroupMember {
  userId: string
  displayName: string
  avatarText: string
}

export interface ManagedGroupItem {
  id: string
  title: string
  memberCount: number
  members: ManagedGroupMember[]
}

const MAX_GROUPS = 30

/**
 * Shared member-picker data. Managed groups aggregated from IM
 * until GET /app-api/im/group/managed exists.
 */
export const useMemberPickerStore = defineStore('memberPicker', () => {
  const managedGroups = ref<ManagedGroupItem[]>([])
  const loadingManaged = ref(false)
  const lastError = ref<string | null>(null)
  const loadedOnce = ref(false)

  async function loadManagedGroups(options?: { force?: boolean }) {
    if (loadingManaged.value) {
      return
    }
    if (loadedOnce.value && !options?.force) {
      return
    }

    loadingManaged.value = true
    lastError.value = null
    const auth = useAuthStore()
    const im = useImStore()
    const selfId = auth.userId

    try {
      if (!im.conversations.length) {
        await im.fetchConversations()
      }
      const groupConvs = im.conversations
        .filter(item => item.type === 'group')
        .slice(0, MAX_GROUPS)

      const results: ManagedGroupItem[] = []
      await Promise.all(groupConvs.map(async (conv) => {
        try {
          const members = await getGroupMembers(conv.id)
          const me = members.find(
            m => m.subjectType === 'user' && m.userId === selfId
          )
          if (!me || (me.role !== 'owner' && me.role !== 'admin')) {
            return
          }
          const users: ManagedGroupMember[] = members
            .filter(m => m.subjectType === 'user' && m.userId)
            .map(m => ({
              userId: m.userId!,
              displayName: m.nickname,
              avatarText: m.avatarText || m.nickname.slice(0, 1) || '?'
            }))
          results.push({
            id: conv.id,
            title: conv.title,
            memberCount: users.length,
            members: users
          })
        } catch {
          // skip failed group
        }
      }))

      results.sort((a, b) => a.title.localeCompare(b.title, 'zh-CN'))
      managedGroups.value = results
      loadedOnce.value = true
    } catch (error) {
      managedGroups.value = []
      lastError.value = error instanceof Error ? error.message : '加载我管理的群组失败'
      throw error
    } finally {
      loadingManaged.value = false
    }
  }

  function resetForTenantSwitch() {
    managedGroups.value = []
    loadingManaged.value = false
    lastError.value = null
    loadedOnce.value = false
  }

  return {
    managedGroups,
    loadingManaged,
    lastError,
    loadedOnce,
    loadManagedGroups,
    resetForTenantSwitch
  }
})
