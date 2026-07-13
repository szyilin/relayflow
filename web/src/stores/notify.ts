import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getNotifyPage,
  getNotifyUnreadCount,
  markNotifyRead,
  markNotifyReadAll,
  type NotifyItem
} from '../api/app/notify'
import type { NotifyFilterKey } from '../utils/notify'

export const useNotifyStore = defineStore('notify', () => {
  const items = ref<NotifyItem[]>([])
  const unreadCount = ref(0)
  const total = ref(0)
  const inboxLoading = ref(false)
  const filterType = ref<NotifyFilterKey>('all')
  const inboxModalOpen = ref(false)

  const hasUnread = computed(() => unreadCount.value > 0)

  const activeTypeFilter = computed(() =>
    filterType.value === 'all' ? undefined : filterType.value)

  async function fetchUnreadCount() {
    const data = await getNotifyUnreadCount()
    unreadCount.value = data.unreadCount
  }

  async function fetchInbox(pageNo = 1, pageSize = 20) {
    inboxLoading.value = true
    try {
      const data = await getNotifyPage(pageNo, pageSize, activeTypeFilter.value)
      items.value = data.list
      total.value = data.total
    } finally {
      inboxLoading.value = false
    }
  }

  async function setFilterType(next: NotifyFilterKey) {
    filterType.value = next
    await fetchInbox()
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

  async function markAllRead() {
    await markNotifyReadAll(activeTypeFilter.value)
    for (const item of items.value) {
      item.read = true
    }
    await fetchUnreadCount()
  }

  async function refreshInbox() {
    await Promise.all([fetchUnreadCount(), fetchInbox()])
  }

  function handleNotifyNew() {
    void fetchUnreadCount()
    if (inboxModalOpen.value) {
      void fetchInbox()
    }
  }

  return {
    items,
    unreadCount,
    total,
    inboxLoading,
    filterType,
    inboxModalOpen,
    hasUnread,
    fetchUnreadCount,
    fetchInbox,
    setFilterType,
    markItemsRead,
    markAllRead,
    refreshInbox,
    handleNotifyNew
  }
})
