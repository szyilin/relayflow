import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getMyProfile, updateMyProfile, type UserProfile } from '../api/app/profile'
import { uploadPublicFile } from '../api/app/file'
import { useAccountDockStore } from './accountDock'
import { useAuthStore } from './auth'

export const useProfileStore = defineStore('profile', () => {
  const profile = ref<UserProfile | null>(null)
  const loading = ref(false)
  const saving = ref(false)
  const uploading = ref(false)

  async function fetchProfile() {
    loading.value = true
    try {
      profile.value = await getMyProfile()
      return profile.value
    } finally {
      loading.value = false
    }
  }

  async function saveNickname(nickname: string) {
    saving.value = true
    try {
      const updated = await updateMyProfile({ nickname: nickname.trim() })
      profile.value = updated
      applyToAuth(updated)
      return updated
    } finally {
      saving.value = false
    }
  }

  async function uploadAvatar(file: File) {
    uploading.value = true
    try {
      const fileId = await uploadPublicFile(file)
      const updated = await updateMyProfile({ avatar: String(fileId) })
      profile.value = updated
      applyToAuth(updated)
      return updated
    } finally {
      uploading.value = false
    }
  }

  function applyToAuth(updated: UserProfile) {
    const auth = useAuthStore()
    if (auth.user) {
      auth.user.nickname = updated.nickname
      auth.user.avatar = updated.avatar
      localStorage.setItem('relayflow:admin:user', JSON.stringify(auth.user))
    }
    useAccountDockStore().updateCurrentProfile(updated.nickname, updated.avatar)
  }

  return {
    profile,
    loading,
    saving,
    uploading,
    fetchProfile,
    saveNickname,
    uploadAvatar
  }
})
