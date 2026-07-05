import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getUserPage, type UserPageItem } from '../api/admin/user'
import { isApiUnavailable } from '../api/request'
import { mockUserPage } from '../mocks/system/users'

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

  async function fetchPage(options?: { page?: number, keyword?: string }) {
    if (options?.page !== undefined) {
      page.value = options.page
    }
    if (options?.keyword !== undefined) {
      keyword.value = options.keyword
    }

    loading.value = true
    try {
      const data = await getUserPage({
        pageNo: page.value,
        pageSize: pageSize.value,
        keyword: keyword.value
      })

      list.value = data.list.map(normalizeUser)
      total.value = data.total
    } catch (error) {
      if (isApiUnavailable(error)) {
        const mock = mockUserPage({
          page: page.value,
          pageSize: pageSize.value,
          keyword: keyword.value
        })
        list.value = mock.list
        total.value = mock.total
        return
      }

      list.value = []
      total.value = 0
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
    fetchPage,
    setPage
  }
})
