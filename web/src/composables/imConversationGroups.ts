/** Built-in message sidebar group ids (this product slice). */
export const IM_SIDEBAR_GROUP_IDS = [
  'all',
  'unread',
  'flagged',
  'mention_me',
  'tags',
  'direct',
  'group',
  'muted',
  'bot'
] as const

export type ImSidebarGroupId = (typeof IM_SIDEBAR_GROUP_IDS)[number]

export const IM_SIDEBAR_GROUP_LABELS: Record<ImSidebarGroupId, string> = {
  all: '消息',
  unread: '未读',
  flagged: '标记',
  mention_me: '@我',
  tags: '标签',
  direct: '单聊',
  group: '群聊',
  muted: '免打扰',
  bot: '机器人'
}

export const IM_SIDEBAR_GROUP_ICONS: Record<ImSidebarGroupId, string> = {
  all: 'i-lucide-message-square',
  unread: 'i-lucide-circle-dot',
  flagged: 'i-lucide-flag',
  mention_me: 'i-lucide-at-sign',
  tags: 'i-lucide-tags',
  direct: 'i-lucide-user',
  group: 'i-lucide-users',
  muted: 'i-lucide-bell-off',
  bot: 'i-lucide-bot'
}

export type ImSidebarSelection =
  | { kind: 'group', groupId: Exclude<ImSidebarGroupId, 'tags'> }
  | { kind: 'tag', tagId: string }

export function isImSidebarGroupId(value: string): value is ImSidebarGroupId {
  return (IM_SIDEBAR_GROUP_IDS as readonly string[]).includes(value)
}

/**
 * Resolve visible order + hidden list from preference patches.
 * Empty visible list falls back to all nine groups in product default order.
 */
export function resolveSidebarGroupPreferences(
  order: string[] | null | undefined,
  hidden: string[] | null | undefined
): { order: ImSidebarGroupId[], hidden: ImSidebarGroupId[] } {
  const hiddenSet = new Set<ImSidebarGroupId>()
  for (const raw of hidden ?? []) {
    if (isImSidebarGroupId(raw)) {
      hiddenSet.add(raw)
    }
  }

  const orderedVisible: ImSidebarGroupId[] = []
  const seen = new Set<ImSidebarGroupId>()
  for (const raw of order ?? []) {
    if (!isImSidebarGroupId(raw) || hiddenSet.has(raw) || seen.has(raw)) {
      continue
    }
    seen.add(raw)
    orderedVisible.push(raw)
  }

  for (const id of IM_SIDEBAR_GROUP_IDS) {
    if (hiddenSet.has(id) || seen.has(id)) {
      continue
    }
    seen.add(id)
    orderedVisible.push(id)
  }

  if (orderedVisible.length === 0) {
    return {
      order: [...IM_SIDEBAR_GROUP_IDS],
      hidden: []
    }
  }

  const hiddenList = IM_SIDEBAR_GROUP_IDS.filter(id => hiddenSet.has(id))
  return { order: orderedVisible, hidden: hiddenList }
}

export function sanitizeSidebarGroupOrder(order: string[] | null | undefined): ImSidebarGroupId[] {
  return resolveSidebarGroupPreferences(order, undefined).order
}

export function sanitizeSidebarGroupHidden(
  hidden: string[] | null | undefined,
  visibleOrder: ImSidebarGroupId[]
): ImSidebarGroupId[] {
  return resolveSidebarGroupPreferences(visibleOrder, hidden).hidden
}

export type ConversationGroupFields = {
  type: string
  unreadCount: number
  mentionedMe?: boolean
  flagged?: boolean
  muted?: boolean
  tagIds?: string[]
}

export function conversationMatchesSidebarSelection(
  item: ConversationGroupFields,
  selection: ImSidebarSelection
): boolean {
  if (selection.kind === 'tag') {
    return (item.tagIds ?? []).includes(selection.tagId)
  }
  switch (selection.groupId) {
    case 'all':
      return true
    case 'unread':
      return item.unreadCount > 0
    case 'flagged':
      return !!item.flagged
    case 'mention_me':
      return item.unreadCount > 0 && !!item.mentionedMe
    case 'direct':
      return item.type === 'direct'
    case 'group':
      return item.type === 'group'
    case 'muted':
      return !!item.muted
    case 'bot':
      return item.type === 'bot_dm'
    default:
      return true
  }
}

export function sidebarEmptyCopy(selection: ImSidebarSelection): { title: string, description: string } {
  if (selection.kind === 'tag') {
    return {
      title: '该标签下暂无会话',
      description: '可在会话列表右键「标签」打标后在此筛选'
    }
  }
  switch (selection.groupId) {
    case 'unread':
      return { title: '没有未读会话', description: '新消息到达后会出现在这里' }
    case 'flagged':
      return { title: '暂无旗标会话', description: '右键会话可添加旗标' }
    case 'mention_me':
      return { title: '暂无 @我 未读', description: '有人在群聊 @你 且仍未读时会出现在这里' }
    case 'direct':
      return { title: '暂无单聊', description: '从通讯录发起私信后会出现在这里' }
    case 'group':
      return { title: '暂无群聊', description: '创建或加入群聊后会出现在这里' }
    case 'muted':
      return { title: '暂无免打扰会话', description: '右键会话可开启消息免打扰' }
    case 'bot':
      return { title: '暂无机器人会话', description: '与助手的对话会出现在这里' }
    default:
      return {
        title: '暂无会话',
        description: '开始与同事发起单聊、建群，或查看来自助手的消息'
      }
  }
}
