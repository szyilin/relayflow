import { get } from '../request'

export interface DeptTreeItem {
  id: string
  parentId: string
  name: string
  sort: number
}

export interface ContactItem {
  id: string
  nickname: string
  username: string
  deptId: string
  deptName: string
  avatarText: string
}

type RawDeptTreeItem = Omit<DeptTreeItem, 'id' | 'parentId'> & {
  id: number | string
  parentId: number | string
}

type RawContactItem = Omit<ContactItem, 'id' | 'deptId'> & {
  id: number | string
  deptId: number | string
}

function normalizeDept(item: RawDeptTreeItem): DeptTreeItem {
  return {
    ...item,
    id: String(item.id),
    parentId: String(item.parentId ?? '0')
  }
}

function normalizeContact(item: RawContactItem): ContactItem {
  return {
    ...item,
    id: String(item.id),
    deptId: String(item.deptId)
  }
}

export function getDeptTree(): Promise<DeptTreeItem[]> {
  return get<RawDeptTreeItem[]>('/app-api/system/dept/tree').then(list => list.map(normalizeDept))
}

export function getContactsByDept(deptId: string, keyword?: string): Promise<ContactItem[]> {
  return get<RawContactItem[]>('/app-api/system/user/list-by-dept', {
    params: {
      deptId,
      keyword: keyword?.trim() || undefined
    }
  }).then(list => list.map(normalizeContact))
}
