import { ref } from 'vue'
import { defineStore } from 'pinia'
import { searchWorkspace, type WorkspaceSearchGroup } from '../api/app/workspace-search'

export const useWorkspaceSearchStore = defineStore('workspaceSearch', () => {
  const keyword = ref('')
  const groups = ref<WorkspaceSearchGroup[]>([])
  const loading = ref(false)
  const searched = ref(false)

  async function search(value: string) {
    const trimmed = value.trim()
    keyword.value = trimmed
    if (!trimmed) {
      groups.value = []
      searched.value = false
      return
    }
    loading.value = true
    searched.value = true
    try {
      const data = await searchWorkspace(trimmed)
      groups.value = data.groups
    } finally {
      loading.value = false
    }
  }

  function clear() {
    keyword.value = ''
    groups.value = []
    searched.value = false
  }

  return {
    keyword,
    groups,
    loading,
    searched,
    search,
    clear
  }
})
