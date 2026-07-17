import type { TaskItem } from '../../api/app/task'

export type TasksNavView = 'mine' | 'done' | 'created' | 'following' | 'activity'

const LEGACY_DETAIL_LOCAL_KEY = 'relayflow-task-detail-local-v1'
const LEGACY_COLLAB_LOCAL_KEY = 'relayflow-task-collab-local-v1'

export function recomputeTaskProgress(parent: TaskItem, children: TaskItem[]): TaskItem {
  const total = children.length
  const done = children.filter(c => c.status === 'DONE').length
  return {
    ...parent,
    subtaskTotal: total,
    subtaskDoneCount: done
  }
}

export function clearLegacyTaskLocal() {
  try {
    localStorage.removeItem(LEGACY_DETAIL_LOCAL_KEY)
    localStorage.removeItem(LEGACY_COLLAB_LOCAL_KEY)
  } catch {
    // ignore
  }
}

export function isOverdueTask(item: TaskItem): boolean {
  if (item.status === 'DONE' || !item.dueTime) {
    return false
  }
  return new Date(item.dueTime).getTime() < Date.now()
}
