import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  addRoleMembers,
  createRole,
  deleteRole,
  getPermissionList,
  getRole,
  getRoleMembers,
  getRolePage,
  removeRoleMember,
  updateRole,
  type PermissionNode,
  type RoleAdminSimple,
  type RoleDetail,
  type RolePageItem,
  type RoleSavePayload,
  type RoleType
} from '../api/admin/role'
import { ApiError } from '../api/request'
import { DEFAULT_LIST_PAGE_SIZE } from '../constants/pagination'

export interface RoleListRecord {
  id: string
  parentId: string
  name: string
  roleType: RoleType
  isSystem: boolean
  permissionSummary: string
  admins: RoleAdminSimple[]
  status: string
  createTime: string
}

function normalizeRole(item: RolePageItem): RoleListRecord {
  return {
    id: String(item.id),
    parentId: String(item.parentId ?? 0),
    name: item.name,
    roleType: item.roleType,
    isSystem: item.roleType === 'SYSTEM',
    permissionSummary: item.permissionSummary?.trim() || '—',
    admins: item.admins ?? [],
    status: typeof item.status === 'number'
      ? (item.status === 0 ? '启用' : '停用')
      : '启用',
    createTime: item.createTime?.slice(0, 10) ?? '—'
  }
}

export const useRoleStore = defineStore('role', () => {
  const list = ref<RoleListRecord[]>([])
  const total = ref(0)
  const loading = ref(false)
  const page = ref(1)
  const pageSize = ref(DEFAULT_LIST_PAGE_SIZE)
  const keyword = ref('')
  const lastError = ref<string | null>(null)

  const permissionTree = ref<PermissionNode[]>([])
  const permissionsLoading = ref(false)

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
      const data = await getRolePage({
        pageNo: page.value,
        pageSize: pageSize.value,
        keyword: keyword.value
      })

      list.value = data.list.map(normalizeRole)
      total.value = data.total
    } catch (error) {
      list.value = []
      total.value = 0
      lastError.value = error instanceof ApiError
        ? error.message
        : '加载角色列表失败，请确认后端服务已启动'
      throw error
    } finally {
      loading.value = false
    }
  }

  /** Load all roles for pickers (parent / assign); paginates — never silent single pageSize:100. */
  async function fetchAllForOptions(): Promise<RolePageItem[]> {
    const collected: RolePageItem[] = []
    let pageNo = 1
    let totalCount = Infinity
    const size = DEFAULT_LIST_PAGE_SIZE
    while ((pageNo - 1) * size < totalCount) {
      const data = await getRolePage({ pageNo, pageSize: size })
      collected.push(...data.list)
      totalCount = data.total
      if (data.list.length === 0) {
        break
      }
      pageNo += 1
      if (pageNo > 50) {
        break
      }
    }
    return collected
  }

  async function fetchDetail(id: number): Promise<RoleDetail> {
    return getRole(id)
  }

  async function fetchPermissions() {
    if (permissionTree.value.length > 0) {
      return permissionTree.value
    }

    permissionsLoading.value = true
    try {
      permissionTree.value = await getPermissionList()
      return permissionTree.value
    } finally {
      permissionsLoading.value = false
    }
  }

  async function saveRole(payload: RoleSavePayload & { id?: number }) {
    if (payload.id != null) {
      await updateRole({ ...payload, id: payload.id })
      return payload.id
    }
    return createRole(payload)
  }

  async function removeRole(id: number) {
    await deleteRole(id)
  }

  async function fetchMembers(roleId: number) {
    return getRoleMembers(roleId)
  }

  async function addMembers(roleId: number, userIds: number[]) {
    await addRoleMembers(roleId, userIds)
  }

  async function removeMember(roleId: number, userId: number) {
    await removeRoleMember(roleId, userId)
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
    permissionTree,
    permissionsLoading,
    fetchPage,
    fetchAllForOptions,
    fetchDetail,
    fetchPermissions,
    saveRole,
    removeRole,
    fetchMembers,
    addMembers,
    removeMember,
    setPage
  }
})
