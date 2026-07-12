import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getMemberInvitePending, type MemberInvitePendingItem } from '../api/app/member-invite-pending'
import {
  getNotifyPage,
  getNotifyUnreadCount,
  markNotifyRead,
  type NotifyItem
} from '../api/app/notify'

export const useNotifyStore = defineStore('notify', () => {
  const pendingItems = ref<MemberInvitePendingItem[]>([])
  const pendingLoading = ref(false)

  const items = ref<NotifyItem[]>([])
  const unreadCount = ref(0)
  const total = ref(0)
  const inboxLoading = ref(false)

  const hasUnread = computed(() => unreadCount.value > 0)

  async function fetchPendingByMobile(mobile: string) {
    const trimmed = mobile.trim()
    if (!trimmed) {
      pendingItems.value = []
      return
    }
    pendingLoading.value = true
    try {
      const data = await getMemberInvitePending(trimmed)
      pendingItems.value = data.items
    } finally {
      pendingLoading.value = false
    }
  }

  function clearPending() {
    pendingItems.value = []
  }

  async function fetchUnreadCount() {
    const data = await getNotifyUnreadCount()
    unreadCount.value = data.unreadCount
  }

  async function fetchInbox(pageNo = 1, pageSize = 20) {
    inboxLoading.value = true
    try {
      const data = await getNotifyPage(pageNo, pageSize)
      items.value = data.list
      total.value = data.total
    } finally {
      inboxLoading.value = false
    }
  }

  async function markItemsRead(ids: string[]) {
    if (!ids.length) {
      return
    }
    await markNotifyRead(ids)
    const idSet = new Set(ids)
    for (const item of items.value) {
      if (idSet.has(item.id)) {
        item.read = true
      }
    }
    await fetchUnreadCount()
  }

  async function refreshInbox() {
    await Promise.all([fetchUnreadCount(), fetchInbox()])
  }

  return {
    pendingItems,
    pendingLoading,
    items,
    unreadCount,
    total,
    inboxLoading,
    hasUnread,
    fetchPendingByMobile,
    clearPending,
    fetchUnreadCount,
    fetchInbox,
    markItemsRead,
    refreshInbox
  }
})
