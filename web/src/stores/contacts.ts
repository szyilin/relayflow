import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  acceptContactRequest,
  getContactsByDept,
  getDeptTree,
  listContactRequests,
  listExternalContacts,
  listStarredContacts,
  updateContactStarred,
  type ContactItem,
  type ContactRequestItem,
  type ContactRequestStatus,
  type DeptTreeItem,
  type ExternalContactItem,
  type StarredContactItem
} from '../api/app/contacts'
import { ApiError } from '../api/request'
import { groupBySortLetter } from '../utils/contactSort'

export type ContactsCategory =
  | 'org'
  | 'external'
  | 'requests'
  | 'starred'
  | 'groups'

export type {
  ContactRequestItem,
  ContactRequestStatus,
  ExternalContactItem,
  StarredContactItem
}

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
  const category = ref<ContactsCategory>('org')
  const depts = ref<DeptTreeItem[]>([])
  const members = ref<ContactItem[]>([])
  const selectedDeptId = ref<string>()
  const selectedContactId = ref<string>()
  const keyword = ref('')
  const loadingDepts = ref(false)
  const loadingMembers = ref(false)
  const loadingEnrich = ref(false)
  const lastError = ref<string | null>(null)

  const externalContacts = ref<ExternalContactItem[]>([])
  const contactRequests = ref<ContactRequestItem[]>([])
  const starredContacts = ref<StarredContactItem[]>([])

  const tree = computed(() => buildTree(depts.value))

  const selectedContact = computed(() =>
    members.value.find(item => item.id === selectedContactId.value))

  const childDepts = computed(() => {
    const parentId = selectedDeptId.value
    if (!parentId) {
      return [] as DeptTreeItem[]
    }
    return depts.value
      .filter(item => item.parentId === parentId)
      .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0) || a.id.localeCompare(b.id))
  })

  const breadcrumb = computed(() => {
    const path: DeptTreeItem[] = []
    let currentId = selectedDeptId.value
    const byId = new Map(depts.value.map(d => [d.id, d]))
    while (currentId) {
      const node = byId.get(currentId)
      if (!node) {
        break
      }
      path.unshift(node)
      if (node.parentId === ROOT_PARENT_ID) {
        break
      }
      currentId = node.parentId
    }
    return path
  })

  const externalGrouped = computed(() =>
    groupBySortLetter(externalContacts.value, item => item.displayName))

  const pendingRequestCount = computed(() =>
    contactRequests.value.filter(item => item.status === 'pending').length)

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

  function findDept(id: string): DeptTreeItem | undefined {
    return depts.value.find(item => item.id === id)
  }

  function setCategory(next: ContactsCategory) {
    category.value = next
    selectedContactId.value = undefined
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

  async function enterDept(deptId: string) {
    selectedContactId.value = undefined
    keyword.value = ''
    await fetchMembers({ deptId, keyword: '' })
  }

  function selectContact(contactId?: string) {
    selectedContactId.value = contactId
  }

  async function fetchExternal(options?: { silent?: boolean }) {
    if (!options?.silent) {
      loadingEnrich.value = true
    }
    lastError.value = null
    try {
      externalContacts.value = await listExternalContacts()
    } catch (error) {
      externalContacts.value = []
      lastError.value = error instanceof ApiError ? error.message : '加载外部联系人失败'
      throw error
    } finally {
      if (!options?.silent) {
        loadingEnrich.value = false
      }
    }
  }

  async function fetchRequests(options?: { silent?: boolean }) {
    if (!options?.silent) {
      loadingEnrich.value = true
    }
    lastError.value = null
    try {
      contactRequests.value = await listContactRequests()
    } catch (error) {
      contactRequests.value = []
      lastError.value = error instanceof ApiError ? error.message : '加载联系人申请失败'
      throw error
    } finally {
      if (!options?.silent) {
        loadingEnrich.value = false
      }
    }
  }

  async function fetchStarred(options?: { silent?: boolean }) {
    if (!options?.silent) {
      loadingEnrich.value = true
    }
    lastError.value = null
    try {
      starredContacts.value = await listStarredContacts()
    } catch (error) {
      starredContacts.value = []
      lastError.value = error instanceof ApiError ? error.message : '加载星标联系人失败'
      throw error
    } finally {
      if (!options?.silent) {
        loadingEnrich.value = false
      }
    }
  }

  async function ensureCategoryData(next: ContactsCategory) {
    if (next === 'external') {
      await fetchExternal()
    } else if (next === 'requests') {
      await fetchRequests()
    } else if (next === 'starred') {
      await fetchStarred()
    }
  }

  async function acceptRequest(requestId: string) {
    lastError.value = null
    try {
      await acceptContactRequest(requestId)
      await Promise.all([fetchRequests(), fetchExternal()])
    } catch (error) {
      lastError.value = error instanceof ApiError ? error.message : '同意申请失败'
      throw error
    }
  }

  /**
   * Reject pending request（`-web` 本地改状态；`-api` 就绪后改调 rejectContactRequest）.
   */
  async function rejectRequest(requestId: string) {
    lastError.value = null
    const row = contactRequests.value.find(item => item.id === requestId)
    if (!row) {
      lastError.value = '申请不存在'
      throw new Error(lastError.value)
    }
    if (row.status !== 'pending') {
      lastError.value = '仅待处理申请可拒绝'
      throw new Error(lastError.value)
    }
    contactRequests.value = contactRequests.value.map(item =>
      item.id === requestId
        ? { ...item, status: 'rejected' as const }
        : item
    )
  }

  async function unstarContact(userId: string) {
    lastError.value = null
    try {
      await updateContactStarred(userId, false)
      await fetchStarred()
    } catch (error) {
      lastError.value = error instanceof ApiError ? error.message : '取消星标失败'
      throw error
    }
  }

  async function toggleStarFromMember(member: ContactItem) {
    return toggleStar({
      userId: member.id,
      displayName: member.nickname,
      orgLabel: member.deptName,
      avatarText: member.avatarText,
      username: member.username
    })
  }

  /** Returns true when the contact is starred after the toggle. */
  async function toggleStar(input: {
    userId: string
    displayName: string
    orgLabel?: string
    avatarText: string
    username?: string
  }): Promise<boolean> {
    lastError.value = null
    const nextStarred = !isStarred(input.userId)
    try {
      await updateContactStarred(input.userId, nextStarred)
      await fetchStarred()
      return isStarred(input.userId)
    } catch (error) {
      lastError.value = error instanceof ApiError ? error.message : '更新星标失败'
      throw error
    }
  }

  function isStarred(userId: string) {
    return starredContacts.value.some(item => item.userId === userId)
  }

  function resetForTenantSwitch() {
    category.value = 'org'
    depts.value = []
    members.value = []
    selectedDeptId.value = undefined
    selectedContactId.value = undefined
    keyword.value = ''
    lastError.value = null
    loadingEnrich.value = false
    externalContacts.value = []
    contactRequests.value = []
    starredContacts.value = []
  }

  return {
    category,
    depts,
    members,
    tree,
    childDepts,
    breadcrumb,
    selectedDeptId,
    selectedContactId,
    selectedContact,
    keyword,
    loadingDepts,
    loadingMembers,
    loadingEnrich,
    lastError,
    externalContacts,
    externalGrouped,
    contactRequests,
    pendingRequestCount,
    starredContacts,
    rootDeptId,
    findTreeNode,
    findDept,
    setCategory,
    fetchDepts,
    fetchMembers,
    enterDept,
    selectContact,
    fetchExternal,
    fetchRequests,
    fetchStarred,
    ensureCategoryData,
    acceptRequest,
    rejectRequest,
    unstarContact,
    toggleStarFromMember,
    toggleStar,
    isStarred,
    resetForTenantSwitch
  }
})
