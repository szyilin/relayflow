import type { MessageItem } from '../../api/app/im'

export type MessageViewItem = MessageItem & {
  localStatus?: 'sending' | 'sent' | 'failed'
}

export type PendingDirectChat = {
  peerUserId: string
  title: string
  avatarText?: string
}

export type GroupMemberCandidate = {
  userId: string
  nickname: string
  avatarText: string
}
