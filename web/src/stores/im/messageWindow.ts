/** Aligned with server `ImMessageListWindows` hard-cap / around defaults. */
export const MESSAGE_WINDOW_LIMIT = 50
export const MESSAGE_AROUND_BEFORE = 25
export const MESSAGE_AROUND_AFTER = 25

export type MessageSeqItem = {
  seq: number
}

export function mergeMessagesBySeq<T extends MessageSeqItem & { id: string }>(
  existing: T[],
  incoming: T[]
): T[] {
  const byId = new Map<string, T>()
  for (const item of existing) {
    byId.set(item.id, item)
  }
  for (const item of incoming) {
    byId.set(item.id, item)
  }
  return [...byId.values()].sort((a, b) => (a.seq ?? 0) - (b.seq ?? 0))
}
