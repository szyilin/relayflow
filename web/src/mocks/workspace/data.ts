export interface WorkspaceThread {
  id: string
  name: string
  preview: string
  time: string
  avatarText: string
  tag?: string
  tagColor?: 'bot' | 'official' | 'external'
  unread?: number
}

export const mockThreads: WorkspaceThread[] = [{
  id: '1',
  name: '产品讨论组',
  preview: '张三：下周一评审会材料已上传到云文档',
  time: '14:35',
  avatarText: '产',
  unread: 2
}, {
  id: '2',
  name: 'RelayFlow 助手',
  preview: '你有一条待办任务即将到期',
  time: '13:20',
  avatarText: '助',
  tag: '机器人',
  tagColor: 'bot'
}, {
  id: '3',
  name: '研发一部',
  preview: '李四：接口联调已完成，可以开始前端对接',
  time: '昨天',
  avatarText: '研'
}, {
  id: '4',
  name: '人事通知',
  preview: '2026 年度体检预约已开放',
  time: '昨天',
  avatarText: '人',
  tag: '官方',
  tagColor: 'official'
}, {
  id: '5',
  name: '外部协作 · Acme',
  preview: '王五：合同版本 v3 请确认',
  time: '7/3',
  avatarText: 'A',
  tag: '外部',
  tagColor: 'external'
}]

export interface WorkspaceTask {
  id: string
  title: string
  due?: string
  list: string
  done: boolean
}

export const mockTasks: WorkspaceTask[] = [{
  id: '1',
  title: '完成登录页 UI 走查反馈',
  due: '今天 18:00',
  list: '我负责的',
  done: false
}, {
  id: '2',
  title: '整理 IM 会话列表接口字段',
  due: '明天',
  list: '我负责的',
  done: false
}, {
  id: '3',
  title: '更新团队 onboarding 文档',
  list: '我负责的',
  done: true
}]

export const mockContacts = [{
  id: '1',
  name: '张三',
  dept: '研发部',
  avatarText: '张'
}, {
  id: '2',
  name: '李四',
  dept: '产品部',
  avatarText: '李'
}, {
  id: '3',
  name: '王五',
  dept: '运营部',
  avatarText: '王'
}]
