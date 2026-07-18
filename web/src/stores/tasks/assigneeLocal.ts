import type { TaskItem } from '../../api/app/task'

export function resolveAssigneeIds(task: Pick<TaskItem, 'assigneeId' | 'assigneeIds'>): string[] {
  if (task.assigneeIds && task.assigneeIds.length > 0) {
    return [...task.assigneeIds]
  }
  if (task.assigneeId) {
    return [String(task.assigneeId)]
  }
  return []
}

export function withAssigneeIds(task: TaskItem, assigneeIds: string[]): TaskItem {
  const unique = Array.from(new Set(assigneeIds.map(String).filter(Boolean)))
  return {
    ...task,
    assigneeIds: unique,
    assigneeId: unique[0] ?? null
  }
}

export function isUserAssignee(
  task: Pick<TaskItem, 'assigneeId' | 'assigneeIds'>,
  userId: string | number | null | undefined
): boolean {
  if (userId == null || userId === '') {
    return false
  }
  const id = String(userId)
  return resolveAssigneeIds(task).includes(id)
}
