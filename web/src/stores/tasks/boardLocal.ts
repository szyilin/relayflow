/**
 * Board column constants for /app/tasks list-context kanban (binary status).
 */
export const BOARD_STATUSES = ['TODO', 'DONE'] as const

export type BoardStatus = (typeof BOARD_STATUSES)[number]

export const BOARD_COLUMN_LABELS: Record<BoardStatus, string> = {
  TODO: '未完成',
  DONE: '已完成'
}

/** Rank step for inserting between / at end of a column. */
export const BOARD_RANK_STEP = 1000

export function isBoardStatus(value: string): value is BoardStatus {
  return (BOARD_STATUSES as readonly string[]).includes(value)
}
