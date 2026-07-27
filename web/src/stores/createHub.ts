import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { useOrgInviteStore } from './orgInvite'

export const useCreateHubStore = defineStore('createHub', () => {
  const createGroupOpen = ref(false)
  const orgInviteOpen = ref(false)
  const addExternalOpen = ref(false)

  /** From GET /app-api/system/org-invite/capability via orgInvite store. */
  const canInvite = computed(() => useOrgInviteStore().canInvite)

  function openCreateGroup() {
    createGroupOpen.value = true
  }

  function openOrgInvite() {
    if (!canInvite.value) {
      return
    }
    orgInviteOpen.value = true
  }

  function openAddExternal() {
    addExternalOpen.value = true
  }

  function resetForTenantSwitch() {
    createGroupOpen.value = false
    orgInviteOpen.value = false
    addExternalOpen.value = false
  }

  return {
    createGroupOpen,
    orgInviteOpen,
    addExternalOpen,
    canInvite,
    openCreateGroup,
    openOrgInvite,
    openAddExternal,
    resetForTenantSwitch
  }
})
