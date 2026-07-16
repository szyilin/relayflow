import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getMemberProfile,
  updateMyProfile,
  type UserProfile
} from '../api/app/profile'
import {
  getContactRemark,
  updateContactRemark,
  type ContactRemark
} from '../api/app/contactRemark'
import { uploadPublicFile } from '../api/app/file'
import { useProfileStore } from './profile'

export const useBusinessCardStore = defineStore('businessCard', () => {
  const loading = ref(false)
  const saving = ref(false)

  async function loadSelfCard(): Promise<UserProfile> {
    loading.value = true
    try {
      return await useProfileStore().fetchProfile()
    } finally {
      loading.value = false
    }
  }

  async function loadPeerCard(userId: string): Promise<UserProfile> {
    loading.value = true
    try {
      return await getMemberProfile(userId)
    } finally {
      loading.value = false
    }
  }

  async function saveSignature(signature: string): Promise<UserProfile> {
    saving.value = true
    try {
      const updated = await updateMyProfile({ signature })
      useProfileStore().profile = updated
      return updated
    } finally {
      saving.value = false
    }
  }

  async function uploadCover(file: File): Promise<UserProfile> {
    saving.value = true
    try {
      const fileId = await uploadPublicFile(file)
      const updated = await updateMyProfile({ coverFileId: fileId })
      useProfileStore().profile = updated
      return updated
    } finally {
      saving.value = false
    }
  }

  async function loadRemark(targetUserId: string): Promise<ContactRemark> {
    return getContactRemark(targetUserId)
  }

  async function saveRemark(
    targetUserId: string,
    remark: { remarkName: string, description: string }
  ): Promise<ContactRemark> {
    saving.value = true
    try {
      return await updateContactRemark(targetUserId, remark)
    } finally {
      saving.value = false
    }
  }

  return {
    loading,
    saving,
    loadSelfCard,
    loadPeerCard,
    saveSignature,
    uploadCover,
    loadRemark,
    saveRemark
  }
})
