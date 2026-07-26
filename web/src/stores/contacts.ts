import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getContactsByDept,
  getDeptTree,
  type ContactItem,
  type DeptTreeItem
} from '../api/app/contacts'
import { ApiError } from '../api/request'
import { groupBySortLetter } from '../utils/contactSort'

export type ContactsCategory =
  | 'org'
  | 'external'
  | 'requests'
  | 'starred'
  | 'groups'

export interface ContactDeptTreeNode {
  id: string
  label: string
  dept: DeptTreeItem
  children?: ContactDeptTreeNode[]
  defaultExpanded?: boolean
}

export interface ExternalContactItem {
  id: string
  userId: string
  displayName: string
  orgLabel: string
  avatarText: string
}

export type ContactRequestStatus = 'pending' | 'accepted' | 'rejected'

export interface ContactRequestItem {
  id: string
  fromUserId: string
  displayName: string
  subtitle: string
  avatarText: string
  status: ContactRequestStatus
}

export interface StarredContactItem {
  id: string
  userId: string
  displayName: string
  orgLabel: string
  avatarText: string
  username?: string
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

function seedExternal(): ExternalContactItem[] {
  return [
    {
      id: 'ext-1',
      userId: '9001',
      displayName: 'Audrey',
      orgLabel: '用户 687476 的组织 · 未认证',
      avatarText: 'A'
    },
    {
      id: 'ext-2',
      userId: '9002',
      displayName: '陈可',
      orgLabel: '外部协作方',
      avatarText: '陈'
    },
    {
      id: 'ext-3',
      userId: '9003',
      displayName: 'Blake',
      orgLabel: 'Partner Co.',
      avatarText: 'B'
    }
  ]
}

function seedRequests(): ContactRequestItem[] {
  return [
    {
      id: 'req-1',
      fromUserId: '9001',
      displayName: 'Audrey',
      subtitle: '想添加你为联系人',
      avatarText: 'A',
      status: 'pending'
    },
    {
      id: 'req-2',
      fromUserId: '9004',
      displayName: '周明',
      subtitle: '通过群聊添加',
      avatarText: '周',
      status: 'accepted'
    }
  ]
}

function seedStarred(): StarredContactItem[] {
  return []
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
  const lastError = ref<string | null>(null)

  /** Temporary -web data; remove on integrate. */
  const externalContacts = ref<ExternalContactItem[]>(seedExternal())
  const contactRequests = ref<ContactRequestItem[]>(seedRequests())
  const starredContacts = ref<StarredContactItem[]>(seedStarred())

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

  function acceptRequest(requestId: string) {
    const item = contactRequests.value.find(r => r.id === requestId)
    if (!item || item.status !== 'pending') {
      return
    }
    item.status = 'accepted'
    if (!externalContacts.value.some(c => c.userId === item.fromUserId)) {
      externalContacts.value.push({
        id: `ext-${item.fromUserId}`,
        userId: item.fromUserId,
        displayName: item.displayName,
        orgLabel: '外部联系人',
        avatarText: item.avatarText
      })
    }
  }

  function unstarContact(userId: string) {
    starredContacts.value = starredContacts.value.filter(item => item.userId !== userId)
  }

  function toggleStarFromMember(member: ContactItem) {
    toggleStar({
      userId: member.id,
      displayName: member.nickname,
      orgLabel: member.deptName,
      avatarText: member.avatarText,
      username: member.username
    })
  }

  /** Returns true when the contact is starred after the toggle. */
  function toggleStar(input: {
    userId: string
    displayName: string
    orgLabel?: string
    avatarText: string
    username?: string
  }): boolean {
    if (isStarred(input.userId)) {
      unstarContact(input.userId)
      return false
    }
    starredContacts.value.push({
      id: `star-${input.userId}`,
      userId: input.userId,
      displayName: input.displayName,
      orgLabel: input.orgLabel || '',
      avatarText: input.avatarText,
      username: input.username
    })
    return true
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
    externalContacts.value = seedExternal()
    contactRequests.value = seedRequests()
    starredContacts.value = seedStarred()
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
    acceptRequest,
    unstarContact,
    toggleStarFromMember,
    toggleStar,
    isStarred,
    resetForTenantSwitch
  }
})
