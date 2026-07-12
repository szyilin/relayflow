import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  createDept,
  deleteDept,
  getDeptList,
  updateDept,
  type DeptCreatePayload,
  type DeptId,
  type DeptItem,
  type DeptUpdatePayload
} from '../api/admin/dept'
import { ApiError } from '../api/request'

export interface DeptTreeNode {
  id: string
  label: string
  dept: DeptItem
  children?: DeptTreeNode[]
}

const ROOT_PARENT_ID = '0'

function normalizeDept(item: DeptItem): DeptItem {
  return {
    ...item,
    id: String(item.id),
    parentId: String(item.parentId ?? ROOT_PARENT_ID),
    leaderUserId: item.leaderUserId != null ? String(item.leaderUserId) : null
  }
}

function buildTree(items: DeptItem[], parentId = ROOT_PARENT_ID): DeptTreeNode[] {
  return items
    .filter(item => item.parentId === parentId)
    .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0) || a.id.localeCompare(b.id))
    .map((item) => {
      const children = buildTree(items, item.id)
      return {
        id: item.id,
        label: item.name,
        dept: item,
        ...(children.length > 0
          ? { children, defaultExpanded: true }
          : {})
      }
    })
}

export const useDeptStore = defineStore('dept', () => {
  const list = ref<DeptItem[]>([])
  const loading = ref(false)
  const lastError = ref<string | null>(null)

  const tree = computed(() => buildTree(list.value))

  async function fetchList() {
    loading.value = true
    lastError.value = null
    try {
      const items = await getDeptList()
      list.value = items.map(normalizeDept)
    } catch (error) {
      list.value = []
      lastError.value = error instanceof ApiError
        ? error.message
        : '加载部门列表失败，请确认后端服务已启动'
      throw error
    } finally {
      loading.value = false
    }
  }

  async function create(payload: DeptCreatePayload) {
    const id = await createDept(payload)
    await fetchList()
    return id
  }

  async function update(payload: DeptUpdatePayload) {
    await updateDept(payload)
    await fetchList()
  }

  async function remove(id: DeptId) {
    await deleteDept(id)
    await fetchList()
  }

  function parentOptions(excludeId?: DeptId) {
    const options = [{ label: '顶级部门', value: ROOT_PARENT_ID }]
    for (const item of list.value) {
      if (excludeId != null && item.id === excludeId) {
        continue
      }
      options.push({ label: item.name, value: item.id })
    }
    return options
  }

  function rootDeptId(): string | undefined {
    const roots = list.value
      .filter(item => item.parentId === ROOT_PARENT_ID)
      .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0) || a.id.localeCompare(b.id))
    return roots[0]?.id
  }

  function findTreeNode(nodes: DeptTreeNode[], id: string): DeptTreeNode | undefined {
    for (const node of nodes) {
      if (node.id === id) {
        return node
      }
      if (node.children?.length) {
        const found = findTreeNode(node.children, id)
        if (found) {
          return found
        }
      }
    }
    return undefined
  }

  return {
    list,
    tree,
    loading,
    lastError,
    fetchList,
    create,
    update,
    remove,
    parentOptions,
    rootDeptId,
    findTreeNode
  }
})
