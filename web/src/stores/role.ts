import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  createRole,
  deleteRole,
  getPermissionList,
  getRole,
  getRolePage,
  updateRole,
  type DataScope,
  type PermissionNode,
  type RoleDetail,
  type RolePageItem,
  type RoleSavePayload,
  type RoleType
} from '../api/admin/role'
import { ApiError } from '../api/request'

export interface RoleListRecord {
  id: string
  parentId: string
  name: string
  code: string
  roleType: RoleType
  roleTypeLabel: string
  dataScope: DataScope
  dataScopeLabel: string
  status: string
  createTime: string
  isSystem: boolean
}

const DATA_SCOPE_LABELS: Record<DataScope, string> = {
  ALL: '全部数据',
  DEPT: '本部门',
  DEPT_AND_CHILD: '本部门及下级',
  SELF: '仅本人',
  CUSTOM: '自定义'
}

function normalizeRole(item: RolePageItem): RoleListRecord {
  return {
    id: String(item.id),
    parentId: String(item.parentId ?? 0),
    name: item.name,
    code: item.code ?? '—',
    roleType: item.roleType,
    roleTypeLabel: item.roleType === 'SYSTEM' ? '系统内置' : '自定义',
    dataScope: item.dataScope,
    dataScopeLabel: DATA_SCOPE_LABELS[item.dataScope] ?? item.dataScope,
    status: typeof item.status === 'number'
      ? (item.status === 0 ? '启用' : '停用')
      : '启用',
    createTime: item.createTime?.slice(0, 10) ?? '—',
    isSystem: item.roleType === 'SYSTEM'
  }
}

export const useRoleStore = defineStore('role', () => {
  const list = ref<RoleListRecord[]>([])
  const total = ref(0)
  const loading = ref(false)
  const page = ref(1)
  const pageSize = ref(10)
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
    fetchDetail,
    fetchPermissions,
    saveRole,
    removeRole,
    setPage
  }
})
