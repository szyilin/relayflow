import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { useAuthStore } from './auth'
import { useInviteSettingsStore } from './inviteSettings'

export const useCreateHubStore = defineStore('createHub', () => {
  const createGroupOpen = ref(false)
  const orgInviteOpen = ref(false)
  const addExternalOpen = ref(false)

  /**
   * `-web`: derive from inviteSettings store (admin page) + isAdmin.
   * Later replaced by GET /app-api/system/org-invite/capability.
   */
  const canInvite = computed(() => {
    const mode = useInviteSettingsStore().settings.inviteMode
    if (mode === 'OFF') {
      return false
    }
    if (mode === 'ALL_MEMBERS') {
      return true
    }
    return useAuthStore().isAdmin
  })

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
