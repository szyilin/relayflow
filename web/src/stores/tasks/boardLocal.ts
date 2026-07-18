/**
 * Board column constants for /app/tasks list-context kanban.
 */
export const BOARD_STATUSES = ['TODO', 'IN_PROGRESS', 'DONE'] as const

export type BoardStatus = (typeof BOARD_STATUSES)[number]

export const BOARD_COLUMN_LABELS: Record<BoardStatus, string> = {
  TODO: '待办',
  IN_PROGRESS: '进行中',
  DONE: '已完成'
}

/** Rank step for inserting between / at end of a column. */
export const BOARD_RANK_STEP = 1000

export function isBoardStatus(value: string): value is BoardStatus {
  return (BOARD_STATUSES as readonly string[]).includes(value)
}
