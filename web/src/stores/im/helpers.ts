import type { MessageContent, MessageItem } from '../../api/app/im'
import type { MessageViewItem } from './types'

export function formatRelativeTime(iso?: string): string {
  if (!iso) {
    return ''
  }
  const date = new Date(iso)
  const diffMs = Date.now() - date.getTime()
  const diffMin = Math.floor(diffMs / 60_000)
  if (diffMin < 1) {
    return '刚刚'
  }
  if (diffMin < 60) {
    return `${diffMin} 分钟前`
  }
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) {
    return `${diffHour} 小时前`
  }
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

export function textFromContent(content: MessageContent): string {
  return content.blocks
    .filter(block => (block.type === 'text' || block.type === 'mention') && block.text)
    .map(block => block.text!.trim())
    .filter(Boolean)
    .join(' ')
}

export function fileBlockFromContent(content: MessageContent) {
  return content.blocks.find(block => block.type === 'file')
}

export function isImageMessage(message: Pick<MessageItem, 'type' | 'content'>): boolean {
  if (message.type === 'image') {
    return true
  }
  const fileBlock = fileBlockFromContent(message.content)
  return fileBlock?.mimeType?.startsWith('image/') ?? false
}

export function isFileMessage(message: Pick<MessageItem, 'type' | 'content'>): boolean {
  return message.type === 'file' || message.type === 'image'
    || fileBlockFromContent(message.content) != null
}

export function messagePreviewFromContent(type: MessageItem['type'], content: MessageContent): string {
  if (type === 'image') {
    return '[图片]'
  }
  if (type === 'file') {
    const fileBlock = fileBlockFromContent(content)
    return fileBlock?.filename ? `[文件] ${fileBlock.filename}` : '[文件]'
  }
  if (type === 'card') {
    const card = content.blocks.find(block => block.type === 'card')
    const title = card?.header?.title?.trim()
    return title ? `[卡片] ${title}` : '[卡片]'
  }
  return textFromContent(content)
}

export function normalizeIncomingMessage(raw: MessageItem): MessageViewItem {
  return {
    ...raw,
    id: String(raw.id),
    conversationId: String(raw.conversationId),
    senderId: String(raw.senderId),
    localStatus: 'sent'
  }
}
