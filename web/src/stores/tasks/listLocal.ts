/**
 * Temporary in-store task-list data for `-web` until `-api` / integrate.
 * Delete usage when wiring real `/app-api/task/list/*` (keep flag false).
 */
import type { TaskItem } from '../../api/app/task'
import type { TaskList, TaskListMember, TaskListRole } from '../../api/app/taskList'

/** Flip to false in integrate when list APIs are ready. */
export const USE_LOCAL_TASK_LISTS = true

let localIdSeq = 900_000

export function nextLocalId(prefix: string): string {
  localIdSeq += 1
  return `${prefix}-${localIdSeq}`
}

export function seedLocalLists(ownerId: string, ownerNickname: string): {
  lists: TaskList[]
  membersByList: Record<string, TaskListMember[]>
  tasksByList: Record<string, TaskItem[]>
} {
  const listId = 'local-list-seed-1'
  const now = new Date().toISOString()
  const lists: TaskList[] = [
    {
      id: listId,
      name: '产品发布',
      description: '示例清单（本地临时，联调后删除）',
      ownerId: ownerId || '1',
      archived: false,
      myRole: 'OWNER',
      createTime: now
    }
  ]
  const membersByList: Record<string, TaskListMember[]> = {
    [listId]: [
      {
        userId: ownerId || '1',
        nickname: ownerNickname || '我',
        avatarText: (ownerNickname || '我').slice(0, 1),
        role: 'OWNER',
        joinTime: now
      }
    ]
  }
  const tasksByList: Record<string, TaskItem[]> = {
    [listId]: [
      {
        id: 'local-task-seed-1',
        title: '整理发布检查单',
        status: 'TODO',
        listId,
        assigneeId: ownerId || '1',
        creatorId: ownerId || '1',
        dueTime: null,
        createTime: now,
        subtaskDoneCount: 0,
        subtaskTotal: 0
      }
    ]
  }
  return { lists, membersByList, tasksByList }
}

export function canEditListMeta(role: TaskListRole | null | undefined): boolean {
  return role === 'OWNER'
}

export function canMutateListTasks(role: TaskListRole | null | undefined): boolean {
  return role === 'OWNER' || role === 'EDITOR'
}
