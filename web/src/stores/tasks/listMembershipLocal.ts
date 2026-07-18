import type { TaskItem } from '../../api/app/task'

export function resolveListIds(task: Pick<TaskItem, 'listId' | 'listIds'>): string[] {
  if (task.listIds && task.listIds.length > 0) {
    return [...task.listIds]
  }
  if (task.listId) {
    return [String(task.listId)]
  }
  return []
}

export function withListIds(task: TaskItem, listIds: string[]): TaskItem {
  const unique = Array.from(new Set(listIds.map(String).filter(Boolean)))
  return {
    ...task,
    listIds: unique,
    listId: unique[0] ?? null
  }
}
