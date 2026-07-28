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

  function isRootDept(dept: Pick<DeptItem, 'parentId'> | null | undefined) {
    if (!dept) {
      return false
    }
    return dept.parentId === ROOT_PARENT_ID
  }

  function parentOptions(excludeId?: DeptId) {
    const options: Array<{ label: string, value: string }> = []
    for (const item of list.value) {
      if (excludeId != null && item.id === excludeId) {
        continue
      }
      options.push({ label: item.name, value: item.id })
    }
    return options
  }

  /** Tree items for parent pickers: org root + descendants (no synthetic「顶级」). */
  function parentTreeItems(excludeId?: DeptId, rootLabel?: string) {
    const blocked = new Set<string>()
    if (excludeId != null) {
      const seed = String(excludeId)
      blocked.add(seed)
      let grew = true
      while (grew) {
        grew = false
        for (const item of list.value) {
          if (blocked.has(item.id)) {
            continue
          }
          if (blocked.has(item.parentId)) {
            blocked.add(item.id)
            grew = true
          }
        }
      }
    }

    function convert(nodes: DeptTreeNode[]): Array<{
      id: string
      label: string
      value: string | null
      icon?: string
      children?: ReturnType<typeof convert>
      defaultExpanded?: boolean
    }> {
      return nodes
        .filter(n => !blocked.has(n.id))
        .map((n) => {
          const children = n.children?.length ? convert(n.children) : undefined
          const label = isRootDept(n.dept) && rootLabel?.trim()
            ? rootLabel.trim()
            : n.label
          return {
            id: n.id,
            label,
            value: n.id,
            icon: isRootDept(n.dept) ? 'i-lucide-building-2' : 'i-lucide-folder',
            ...(children?.length
              ? { children, defaultExpanded: true }
              : {})
          }
        })
    }

    return convert(tree.value)
  }

  /** Flat-list-compatible dept tree for assigning a user's main department. */
  function deptTreeItems(rootLabel?: string) {
    function convert(nodes: DeptTreeNode[]): Array<{
      id: string
      label: string
      value: string | null
      icon?: string
      children?: ReturnType<typeof convert>
      defaultExpanded?: boolean
    }> {
      return nodes.map((n) => {
        const children = n.children?.length ? convert(n.children) : undefined
        const label = isRootDept(n.dept) && rootLabel?.trim()
          ? rootLabel.trim()
          : n.label
        return {
          id: n.id,
          label,
          value: n.id,
          icon: isRootDept(n.dept) ? 'i-lucide-building-2' : 'i-lucide-folder',
          ...(children?.length
            ? { children, defaultExpanded: true }
            : {})
        }
      })
    }
    return convert(tree.value)
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
    isRootDept,
    parentOptions,
    parentTreeItems,
    deptTreeItems,
    rootDeptId,
    findTreeNode
  }
})
