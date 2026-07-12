import type { ContactItem, DeptTreeItem } from '../api/app/contacts'

const mockDepts: DeptTreeItem[] = [
  { id: '1', parentId: '0', name: '默认企业', sort: 0 },
  { id: '2', parentId: '1', name: '研发部', sort: 1 },
  { id: '3', parentId: '1', name: '产品部', sort: 2 }
]

const mockContacts: ContactItem[] = [
  { id: '102', nickname: '李晓明', username: 'liming', deptId: '2', deptName: '研发部', avatarText: '李' },
  { id: '103', nickname: '王芳', username: 'wangfang', deptId: '2', deptName: '研发部', avatarText: '王' },
  { id: '104', nickname: '赵强', username: 'zhaoqiang', deptId: '3', deptName: '产品部', avatarText: '赵' },
  { id: '105', nickname: '陈静', username: 'chenjing', deptId: '3', deptName: '产品部', avatarText: '陈' },
  { id: '106', nickname: '刘洋', username: 'liuyang', deptId: '1', deptName: '默认企业', avatarText: '刘' }
]

export function mockGetDeptTree(): DeptTreeItem[] {
  return mockDepts.map(item => ({ ...item }))
}

export function mockGetContactsByDept(deptId: string, keyword?: string): ContactItem[] {
  const q = keyword?.trim().toLowerCase()
  let list = mockContacts.filter(item => item.deptId === deptId)
  if (q) {
    list = list.filter(item =>
      item.nickname.toLowerCase().includes(q) || item.username.toLowerCase().includes(q))
  }
  return list.map(item => ({ ...item }))
}
