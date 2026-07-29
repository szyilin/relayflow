/**
 * Product default workspace left-rail order (must match backend UserPreferenceDefaults).
 */
export const DEFAULT_RAIL_ORDER = [
  'messages',
  'calendar',
  'meeting',
  'tasks',
  'agents',
  'docs',
  'contacts'
] as const

export type WorkspaceRailId = (typeof DEFAULT_RAIL_ORDER)[number]

/**
 * Keep known ids in preferred relative order; append missing defaults / leftovers.
 */
export function resolveRailOrder(
  preferred: string[] | undefined | null,
  availableIds: string[]
): string[] {
  const known = new Set(availableIds)
  const seen = new Set<string>()
  const out: string[] = []

  for (const id of preferred ?? []) {
    if (known.has(id) && !seen.has(id)) {
      out.push(id)
      seen.add(id)
    }
  }

  for (const id of DEFAULT_RAIL_ORDER) {
    if (known.has(id) && !seen.has(id)) {
      out.push(id)
      seen.add(id)
    }
  }

  for (const id of availableIds) {
    if (!seen.has(id)) {
      out.push(id)
      seen.add(id)
    }
  }

  return out
}

/** Move `fromId` so it sits before `beforeId` (or at end when beforeId is null). */
export function moveRailIdBefore(
  order: string[],
  fromId: string,
  beforeId: string | null
): string[] {
  if (fromId === beforeId) {
    return order
  }
  const next = order.filter(id => id !== fromId)
  if (beforeId == null) {
    next.push(fromId)
    return next
  }
  const idx = next.indexOf(beforeId)
  if (idx < 0) {
    next.push(fromId)
    return next
  }
  next.splice(idx, 0, fromId)
  return next
}
