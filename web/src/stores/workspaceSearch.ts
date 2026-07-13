import { ref } from 'vue'
import { defineStore } from 'pinia'
import axios from 'axios'
import { searchWorkspace, type WorkspaceSearchGroup } from '../api/app/workspace-search'
import { ApiError } from '../api/request'

const MOCK_GROUPS: WorkspaceSearchGroup[] = [
  {
    type: 'member',
    label: '联系人',
    items: [{
      id: '1001',
      title: '张三',
      subtitle: '研发部',
      route: '/app/contacts?memberId=1001',
      entityType: 'member',
      entityId: '1001'
    }]
  },
  {
    type: 'conversation',
    label: '消息',
    items: [{
      id: '2001',
      title: '张三',
      subtitle: '你好，在吗？',
      route: '/app/messages?conversationId=2001',
      entityType: 'conversation',
      entityId: '2001'
    }]
  },
  {
    type: 'task',
    label: '任务',
    items: [{
      id: '3001',
      title: '整理周报',
      subtitle: 'TODO',
      route: '/app/tasks?taskId=3001',
      entityType: 'task',
      entityId: '3001'
    }]
  }
]

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
      groups.value = await fetchGroups(trimmed)
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

async function fetchGroups(keyword: string): Promise<WorkspaceSearchGroup[]> {
  try {
    const data = await searchWorkspace(keyword)
    return data.groups
  } catch (error) {
    if (shouldUseSearchMock(error)) {
      const lower = keyword.toLowerCase()
      return MOCK_GROUPS
        .map(group => ({
          ...group,
          items: group.items.filter(item =>
            item.title.toLowerCase().includes(lower)
            || item.subtitle?.toLowerCase().includes(lower))
        }))
        .filter(group => group.items.length > 0)
    }
    throw error
  }
}

function shouldUseSearchMock(error: unknown): boolean {
  if (error instanceof ApiError && (error.code === 404 || error.code === 0)) {
    return true
  }
  return axios.isAxiosError(error) && (error.response?.status === 404 || !error.response)
}
