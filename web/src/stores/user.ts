import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getUserPage, type UserPageItem } from '../api/admin/user'
import { ApiError } from '../api/request'

export interface UserListRecord {
  id: string
  username: string
  nickname: string
  dept: string
  status: string
  createTime: string
}

function normalizeUser(item: UserPageItem): UserListRecord {
  return {
    id: String(item.id),
    username: item.username,
    nickname: item.nickname,
    dept: item.dept ?? '—',
    status: typeof item.status === 'number'
      ? (item.status === 0 ? '启用' : '禁用')
      : (item.status ?? '启用'),
    createTime: item.createTime?.slice(0, 10) ?? '—'
  }
}

export const useUserStore = defineStore('user', () => {
  const list = ref<UserListRecord[]>([])
  const total = ref(0)
  const loading = ref(false)
  const page = ref(1)
  const pageSize = ref(5)
  const keyword = ref('')
  const lastError = ref<string | null>(null)

  async function fetchPage(options?: { page?: number, keyword?: string }) {
    if (options?.page !== undefined) {
      page.value = options.page
    }
    if (options?.keyword !== undefined) {
      keyword.value = options.keyword
    }

    loading.value = true
    lastError.value = null
    try {
      const data = await getUserPage({
        pageNo: page.value,
        pageSize: pageSize.value,
        keyword: keyword.value
      })

      list.value = data.list.map(normalizeUser)
      total.value = data.total
    } catch (error) {
      list.value = []
      total.value = 0
      lastError.value = error instanceof ApiError
        ? error.message
        : '加载用户列表失败，请确认后端服务已启动'
      throw error
    } finally {
      loading.value = false
    }
  }

  function setPage(next: number) {
    page.value = next
  }

  return {
    list,
    total,
    loading,
    page,
    pageSize,
    keyword,
    lastError,
    fetchPage,
    setPage
  }
})
