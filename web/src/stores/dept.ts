import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  createDept,
  deleteDept,
  getDeptList,
  updateDept,
  type DeptCreatePayload,
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

function buildTree(items: DeptItem[], parentId = 0): DeptTreeNode[] {
  return items
    .filter(item => item.parentId === parentId)
    .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0) || a.id - b.id)
    .map((item) => {
      const children = buildTree(items, item.id)
      return {
        id: String(item.id),
        label: item.name,
        dept: item,
        ...(children.length > 0 ? { children } : {})
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
      list.value = await getDeptList()
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

  async function remove(id: number) {
    await deleteDept(id)
    await fetchList()
  }

  function parentOptions(excludeId?: number) {
    const options = [{ label: '顶级部门', value: 0 }]
    for (const item of list.value) {
      if (excludeId != null && item.id === excludeId) {
        continue
      }
      options.push({ label: item.name, value: item.id })
    }
    return options
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
    parentOptions
  }
})
