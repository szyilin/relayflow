import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getUser,
  getUserPage,
  inviteUser,
  updateUser,
  updateUserDept,
  updateUserRole,
  updateUserStatus,
  type UserDetail,
  type UserId,
  type UserInvitePayload,
  type UserPageItem,
  type UserUpdatePayload
} from '../api/admin/user'
import { ApiError } from '../api/request'

export type MemberStatusCode = 'ACTIVE' | 'NOT_JOINED' | 'SUSPENDED' | 'PENDING_ACTIVATION' | 'PENDING_LEAVE' | 'LEFT'

export interface UserListRecord {
  id: string
  username: string
  nickname: string
  mobile: string
  dept: string
  memberStatus: MemberStatusCode
  statusLabel: string
  statusColor: 'success' | 'warning' | 'error' | 'neutral'
  statusCode: number
  createTime: string
}

const MEMBER_STATUS_LABELS: Record<string, string> = {
  ACTIVE: '正常',
  NOT_JOINED: '待同意',
  SUSPENDED: '已暂停',
  PENDING_ACTIVATION: '待激活',
  PENDING_LEAVE: '待离职',
  LEFT: '已离职'
}

const MEMBER_STATUS_COLORS: Record<string, UserListRecord['statusColor']> = {
  ACTIVE: 'success',
  NOT_JOINED: 'warning',
  SUSPENDED: 'error',
  PENDING_ACTIVATION: 'warning',
  PENDING_LEAVE: 'neutral',
  LEFT: 'neutral'
}

function resolveMemberStatus(item: UserPageItem): MemberStatusCode {
  if (item.memberStatus && item.memberStatus in MEMBER_STATUS_LABELS) {
    return item.memberStatus as MemberStatusCode
  }
  const statusCode = typeof item.status === 'number' ? item.status : 0
  return statusCode === 0 ? 'ACTIVE' : 'SUSPENDED'
}

function normalizeUser(item: UserPageItem): UserListRecord {
  const memberStatus = resolveMemberStatus(item)
  const statusCode = typeof item.status === 'number' ? item.status : 0
  return {
    id: String(item.id),
    username: item.username,
    nickname: item.nickname || item.username,
    mobile: item.mobile ?? '—',
    dept: item.dept ?? '—',
    memberStatus,
    statusLabel: MEMBER_STATUS_LABELS[memberStatus] ?? memberStatus,
    statusColor: MEMBER_STATUS_COLORS[memberStatus] ?? 'neutral',
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

  async function invite(payload: UserInvitePayload) {
    const id = await inviteUser(payload)
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
    invite,
    saveProfile,
    toggleStatus,
    setPage
  }
})
