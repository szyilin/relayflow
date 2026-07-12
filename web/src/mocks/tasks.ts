import type { TaskItem, TaskPageResult } from '../api/app/task'

let mockSeq = 3000

const mockTasks: TaskItem[] = [
  {
    id: '3001',
    title: '整理本周周报',
    status: 'TODO',
    dueTime: new Date(Date.now() + 86_400_000).toISOString(),
    createTime: new Date().toISOString()
  },
  {
    id: '3002',
    title: '评审产品原型',
    status: 'DONE',
    dueTime: null,
    createTime: new Date(Date.now() - 86_400_000).toISOString()
  }
]

export function mockTaskPage(): TaskPageResult {
  return {
    list: [...mockTasks],
    total: mockTasks.length
  }
}

export function mockCreateTask(payload: { title: string, dueTime?: string | null }): string {
  mockSeq += 1
  const id = String(mockSeq)
  mockTasks.unshift({
    id,
    title: payload.title,
    status: 'TODO',
    dueTime: payload.dueTime ?? null,
    createTime: new Date().toISOString()
  })
  return id
}

export function mockToggleTaskDone(id: string, done: boolean): void {
  const task = mockTasks.find(item => item.id === id)
  if (task) {
    task.status = done ? 'DONE' : 'TODO'
  }
}

export function mockDeleteTask(id: string): void {
  const index = mockTasks.findIndex(item => item.id === id)
  if (index >= 0) {
    mockTasks.splice(index, 1)
  }
}

export function mockUpdateTask(payload: { id: string, title?: string, dueTime?: string | null }): void {
  const task = mockTasks.find(item => item.id === payload.id)
  if (!task) {
    return
  }
  if (payload.title !== undefined) {
    task.title = payload.title
  }
  if (payload.dueTime !== undefined) {
    task.dueTime = payload.dueTime
  }
}
