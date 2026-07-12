import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getContactsByDept,
  getDeptTree,
  type ContactItem,
  type DeptTreeItem
} from '../api/app/contacts'
import { ApiError } from '../api/request'

export interface ContactDeptTreeNode {
  id: string
  label: string
  dept: DeptTreeItem
  children?: ContactDeptTreeNode[]
  defaultExpanded?: boolean
}

const ROOT_PARENT_ID = '0'

function buildTree(items: DeptTreeItem[], parentId = ROOT_PARENT_ID): ContactDeptTreeNode[] {
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

export const useContactsStore = defineStore('contacts', () => {
  const depts = ref<DeptTreeItem[]>([])
  const members = ref<ContactItem[]>([])
  const selectedDeptId = ref<string>()
  const selectedContactId = ref<string>()
  const keyword = ref('')
  const loadingDepts = ref(false)
  const loadingMembers = ref(false)
  const lastError = ref<string | null>(null)

  const tree = computed(() => buildTree(depts.value))

  const selectedContact = computed(() =>
    members.value.find(item => item.id === selectedContactId.value))

  function rootDeptId(): string | undefined {
    const roots = depts.value
      .filter(item => item.parentId === ROOT_PARENT_ID)
      .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0) || a.id.localeCompare(b.id))
    return roots[0]?.id
  }

  function findTreeNode(nodes: ContactDeptTreeNode[], id: string): ContactDeptTreeNode | undefined {
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

  async function fetchDepts() {
    loadingDepts.value = true
    lastError.value = null
    try {
      depts.value = await getDeptTree()
    } catch (error) {
      depts.value = []
      lastError.value = error instanceof ApiError ? error.message : '加载部门失败'
      throw error
    } finally {
      loadingDepts.value = false
    }
  }

  async function fetchMembers(options?: { deptId?: string, keyword?: string }) {
    if (options?.deptId !== undefined) {
      selectedDeptId.value = options.deptId
    }
    if (options?.keyword !== undefined) {
      keyword.value = options.keyword
    }
    const deptId = selectedDeptId.value
    if (!deptId) {
      members.value = []
      return
    }

    loadingMembers.value = true
    lastError.value = null
    try {
      members.value = await getContactsByDept(deptId, keyword.value)
      if (selectedContactId.value && !members.value.some(item => item.id === selectedContactId.value)) {
        selectedContactId.value = undefined
      }
    } catch (error) {
      members.value = []
      lastError.value = error instanceof ApiError ? error.message : '加载成员失败'
      throw error
    } finally {
      loadingMembers.value = false
    }
  }

  function selectContact(contactId?: string) {
    selectedContactId.value = contactId
  }

  return {
    depts,
    members,
    tree,
    selectedDeptId,
    selectedContactId,
    selectedContact,
    keyword,
    loadingDepts,
    loadingMembers,
    lastError,
    rootDeptId,
    findTreeNode,
    fetchDepts,
    fetchMembers,
    selectContact
  }
})
