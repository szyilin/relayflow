export interface MockUserRecord {
  id: string
  username: string
  nickname: string
  dept: string
  status: '启用' | '禁用'
  createTime: string
}

export const mockUsers: MockUserRecord[] = [
  { id: '1', username: 'admin', nickname: '管理员', dept: '总部', status: '启用', createTime: '2026-01-01' },
  { id: '2', username: 'zhangsan', nickname: '张三', dept: '研发部', status: '启用', createTime: '2026-02-15' },
  { id: '3', username: 'lisi', nickname: '李四', dept: '研发部', status: '启用', createTime: '2026-02-20' },
  { id: '4', username: 'wangwu', nickname: '王五', dept: '产品部', status: '启用', createTime: '2026-03-01' },
  { id: '5', username: 'zhaoliu', nickname: '赵六', dept: '运营部', status: '禁用', createTime: '2026-03-10' },
  { id: '6', username: 'sunqi', nickname: '孙七', dept: '研发部', status: '启用', createTime: '2026-03-15' },
  { id: '7', username: 'zhouba', nickname: '周八', dept: '人事部', status: '启用', createTime: '2026-04-01' },
  { id: '8', username: 'wujiu', nickname: '吴九', dept: '财务部', status: '启用', createTime: '2026-04-08' },
  { id: '9', username: 'zhengshi', nickname: '郑十', dept: '产品部', status: '启用', createTime: '2026-04-12' },
  { id: '10', username: 'chenyi', nickname: '陈一', dept: '运营部', status: '启用', createTime: '2026-05-01' }
]

export interface MockUserPageResult {
  list: MockUserRecord[]
  total: number
}

export function mockUserPage(params: {
  page: number
  pageSize: number
  keyword?: string
}): MockUserPageResult {
  const keyword = params.keyword?.trim().toLowerCase() ?? ''
  const filtered = keyword
    ? mockUsers.filter(user =>
        user.username.toLowerCase().includes(keyword)
        || user.nickname.toLowerCase().includes(keyword)
        || user.dept.toLowerCase().includes(keyword))
    : mockUsers

  const start = (params.page - 1) * params.pageSize
  const list = filtered.slice(start, start + params.pageSize)

  return {
    list,
    total: filtered.length
  }
}

export const mockDeptTree = [{
  label: '总部',
  icon: 'i-lucide-building-2',
  children: [{
    label: '研发部',
    icon: 'i-lucide-code-2',
    children: [{
      label: '前端组',
      icon: 'i-lucide-laptop'
    }, {
      label: '后端组',
      icon: 'i-lucide-server'
    }]
  }, {
    label: '产品部',
    icon: 'i-lucide-lightbulb'
  }, {
    label: '运营部',
    icon: 'i-lucide-megaphone'
  }]
}]

export const mockFiles = [{
  id: '1',
  name: 'logo.png',
  size: '128 KB',
  type: 'image/png',
  createTime: '2026-05-01 10:00'
}, {
  id: '2',
  name: 'report-2026-q1.pdf',
  size: '2.4 MB',
  type: 'application/pdf',
  createTime: '2026-05-02 14:30'
}, {
  id: '3',
  name: 'backup.zip',
  size: '156 MB',
  type: 'application/zip',
  createTime: '2026-05-03 09:15'
}]
