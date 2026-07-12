import { defineStore } from 'pinia'
import { ref } from 'vue'
import { batchPresence } from '../api/app/presence'

export const usePresenceStore = defineStore('presence', () => {
  const onlineByUserId = ref<Record<string, boolean>>({})
  let pollTimer: ReturnType<typeof setInterval> | undefined
  let trackedUserIds: string[] = []

  function isOnline(userId?: string): boolean {
    if (!userId) {
      return false
    }
    return onlineByUserId.value[userId] === true
  }

  async function refreshPresence(userIds: string[]) {
    const uniqueIds = [...new Set(userIds.filter(Boolean))]
    if (uniqueIds.length === 0) {
      return
    }
    const result = await batchPresence(uniqueIds)
    for (const item of result.items) {
      onlineByUserId.value[item.userId] = item.online
    }
  }

  function startPolling(userIds: string[], intervalMs = 30_000) {
    stopPolling()
    trackedUserIds = [...new Set(userIds.filter(Boolean))]
    if (trackedUserIds.length === 0) {
      return
    }
    void refreshPresence(trackedUserIds)
    pollTimer = setInterval(() => {
      void refreshPresence(trackedUserIds)
    }, intervalMs)
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = undefined
    }
    trackedUserIds = []
  }

  return {
    onlineByUserId,
    isOnline,
    refreshPresence,
    startPolling,
    stopPolling
  }
})
