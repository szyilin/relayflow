/**
 * Task list role helpers (shared by store / UI).
 */
import type { TaskListRole } from '../../api/app/taskList'

export function canEditListMeta(role: TaskListRole | null | undefined): boolean {
  return role === 'OWNER'
}

export function canMutateListTasks(role: TaskListRole | null | undefined): boolean {
  return role === 'OWNER' || role === 'EDITOR'
}
