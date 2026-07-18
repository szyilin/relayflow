import type { TaskItem } from '../../api/app/task'

/**
 * Temporary until workspace-task-quick-views-api (+ assigner).
 * Integrate MUST set false and delete mock helpers.
 */
export const USE_LOCAL_QUICK_VIEWS = true

export function mergeTasksById(...lists: TaskItem[][]): TaskItem[] {
  const map = new Map<string, TaskItem>()
  for (const list of lists) {
    for (const item of list) {
      if (!map.has(item.id)) {
        map.set(item.id, item)
      }
    }
  }
  return Array.from(map.values()).sort((a, b) =>
    (b.createTime || '').localeCompare(a.createTime || ''))
}

/** Demo rows for「我分配的」when assigner API is not ready. */
export function buildAssignedByMeMock(currentUserId: string): TaskItem[] {
  if (!currentUserId) {
    return []
  }
  const now = new Date().toISOString()
  return [
    {
      id: `local-assigned-1`,
      title: '（演示）已分配给同事的任务',
      status: 'TODO',
      startTime: null,
      dueTime: null,
      remindBeforeMinutes: null,
      description: '临时 Mock：分配人=我且负责人不含我；联调后删除',
      parentId: null,
      listId: null,
      assigneeId: '0',
      creatorId: currentUserId,
      assignerId: currentUserId,
      createTime: now,
      subtaskDoneCount: 0,
      subtaskTotal: 0
    }
  ]
}

export function filterOpenTasks(items: TaskItem[]): TaskItem[] {
  return items.filter(item => item.status !== 'DONE')
}
