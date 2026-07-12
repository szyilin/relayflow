import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  createUser,
  getUser,
  getUserPage,
  updateUser,
  updateUserDept,
  updateUserRole,
  updateUserStatus,
  type UserCreatePayload,
  type UserDetail,
  type UserId,
  type UserPageItem,
  type UserUpdatePayload
} from '../api/admin/user'
import { ApiError } from '../api/request'

export interface UserListRecord {
  id: string
  username: string
  nickname: string
  dept: string
  status: string
  statusCode: number
  createTime: string
}

function normalizeUser(item: UserPageItem): UserListRecord {
  const statusCode = typeof item.status === 'number' ? item.status : 0
  return {
    id: String(item.id),
    username: item.username,
    nickname: item.nickname,
    dept: item.dept ?? '—',
    status: statusCode === 0 ? '启用' : '禁用',
    statusCode,
    createTime: item.createTime?.slice(0, 10) ?? '—'
  }
}

function normalizeDetail(item: UserDetail): UserDetail {
  return {
    ...item,
    id: String(item.id),
    deptId: item.deptId != null ? String(item.deptId) : null,
    roleIds: (item.roleIds ?? []).map(id => String(id))
  }
}

export const useUserStore = defineStore('user', () => {
  const list = ref<UserListRecord[]>([])
  const total = ref(0)
  const loading = ref(false)
  const page = ref(1)
  const pageSize = ref(5)
  const keyword = ref('')
  const deptId = ref<string | undefined>()
  const lastError = ref<string | null>(null)

  async function fetchPage(options?: { page?: number, keyword?: string, deptId?: string }) {
    if (options?.page !== undefined) {
      page.value = options.page
    }
    if (options?.keyword !== undefined) {
      keyword.value = options.keyword
    }
    if (options?.deptId !== undefined) {
      deptId.value = options.deptId
    }

    loading.value = true
    lastError.value = null
    try {
      const data = await getUserPage({
        pageNo: page.value,
        pageSize: pageSize.value,
        keyword: keyword.value,
        deptId: deptId.value
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

  async function fetchDetail(id: UserId): Promise<UserDetail> {
    const detail = await getUser(id)
    return normalizeDetail(detail)
  }

  async function create(payload: UserCreatePayload) {
    const id = await createUser(payload)
    await fetchPage()
    return String(id)
  }

  async function saveProfile(payload: UserUpdatePayload & {
    deptId?: UserId | null
    roleIds?: UserId[]
  }) {
    await updateUser({
      id: payload.id,
      nickname: payload.nickname,
      mobile: payload.mobile,
      email: payload.email
    })
    await updateUserDept({
      id: payload.id,
      deptId: payload.deptId ?? null
    })
    await updateUserRole({
      id: payload.id,
      roleIds: payload.roleIds ?? []
    })
    await fetchPage()
  }

  async function toggleStatus(id: UserId, status: number) {
    await updateUserStatus({ id, status })
    await fetchPage()
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
    deptId,
    lastError,
    fetchPage,
    fetchDetail,
    create,
    saveProfile,
    toggleStatus,
    setPage
  }
})
