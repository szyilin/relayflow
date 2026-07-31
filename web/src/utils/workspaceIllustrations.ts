/**
 * Workspace Storyset illustration catalog.
 * Source / workflow: docs/dev/workspace-illustrations.md
 * Attribution: illustrations by Storyset (https://storyset.com/)
 */

import celebrationUrl from '../assets/illustrations/messages-idle-celebration.svg'
import chattingUrl from '../assets/illustrations/messages-idle-chatting.svg'
import coffeeBreakUrl from '../assets/illustrations/messages-idle-coffee-break.svg'
import conversationUrl from '../assets/illustrations/messages-idle-conversation.svg'
import groupChatUrl from '../assets/illustrations/messages-idle-group-chat.svg'
import mailUrl from '../assets/illustrations/messages-idle-mail.svg'
import messagingUrl from '../assets/illustrations/messages-idle-messaging.svg'
import natureUrl from '../assets/illustrations/messages-idle-nature.svg'
import onlineWorldUrl from '../assets/illustrations/messages-idle-online-world.svg'
import socialDashboardUrl from '../assets/illustrations/messages-idle-social-dashboard.svg'
import takingNotesUrl from '../assets/illustrations/messages-idle-taking-notes.svg'

export type WorkspaceIllustrationId =
  | 'messages-idle-celebration'
  | 'messages-idle-chatting'
  | 'messages-idle-coffee-break'
  | 'messages-idle-conversation'
  | 'messages-idle-group-chat'
  | 'messages-idle-mail'
  | 'messages-idle-messaging'
  | 'messages-idle-nature'
  | 'messages-idle-online-world'
  | 'messages-idle-social-dashboard'
  | 'messages-idle-taking-notes'

export interface WorkspaceIllustration {
  id: WorkspaceIllustrationId
  /** Vite-resolved asset URL */
  src: string
  /** Soft caption under the art (idle main pane) */
  caption: string
  /** Storyset concept slug (for re-download / recolor on storyset.com) */
  storysetSlug: string
  /** Locked product style */
  style: 'rafiki'
}

/** Pool for messages main pane when no conversation is selected. */
export const MESSAGES_IDLE_ILLUSTRATIONS: readonly WorkspaceIllustration[] = [
  {
    id: 'messages-idle-chatting',
    src: chattingUrl,
    caption: '慢慢来，好消息总会到',
    storysetSlug: 'chatting',
    style: 'rafiki'
  },
  {
    id: 'messages-idle-messaging',
    src: messagingUrl,
    caption: '选一条会话，继续刚才的对话',
    storysetSlug: 'messaging',
    style: 'rafiki'
  },
  {
    id: 'messages-idle-conversation',
    src: conversationUrl,
    caption: '聊一聊，灵感就来了',
    storysetSlug: 'conversation',
    style: 'rafiki'
  },
  {
    id: 'messages-idle-group-chat',
    src: groupChatUrl,
    caption: '团队在一起，事情更好办',
    storysetSlug: 'group-chat',
    style: 'rafiki'
  },
  {
    id: 'messages-idle-mail',
    src: mailUrl,
    caption: '新消息会在这儿等你',
    storysetSlug: 'mail',
    style: 'rafiki'
  },
  {
    id: 'messages-idle-coffee-break',
    src: coffeeBreakUrl,
    caption: '喝杯水再开工也不迟',
    storysetSlug: 'coffee-break',
    style: 'rafiki'
  },
  {
    id: 'messages-idle-celebration',
    src: celebrationUrl,
    caption: '今天也值得被认真对待',
    storysetSlug: 'celebration',
    style: 'rafiki'
  },
  {
    id: 'messages-idle-nature',
    src: natureUrl,
    caption: '喘口气，再挑一个会话',
    storysetSlug: 'nature',
    style: 'rafiki'
  },
  {
    id: 'messages-idle-online-world',
    src: onlineWorldUrl,
    caption: '连接就在左侧列表里',
    storysetSlug: 'online-world',
    style: 'rafiki'
  },
  {
    id: 'messages-idle-social-dashboard',
    src: socialDashboardUrl,
    caption: '先看看谁在找你',
    storysetSlug: 'social-dashboard',
    style: 'rafiki'
  },
  {
    id: 'messages-idle-taking-notes',
    src: takingNotesUrl,
    caption: '记下来的，总会有用',
    storysetSlug: 'taking-notes',
    style: 'rafiki'
  }
] as const

export function pickRandomIllustration(
  pool: readonly WorkspaceIllustration[]
): WorkspaceIllustration {
  if (pool.length === 0) {
    throw new Error('illustration pool is empty')
  }
  const index = Math.floor(Math.random() * pool.length)
  return pool[index]!
}
