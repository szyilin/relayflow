import type { MessageContent, MessageItem } from '../../api/app/im'
import type { MessageViewItem } from './types'

/** 同发送者连续消息合并窗口（对齐飞书约 5 分钟） */
export const MESSAGE_GROUP_GAP_MS = 5 * 60_000

export type MessageTimelineItem = {
  message: MessageViewItem
  /** 分段上方居中时间头（跨日日期分隔 / 同日间隔时钟） */
  showTimeHeader: boolean
  timeHeader: string
  isGroupStart: boolean
  isGroupEnd: boolean
}

function startOfDay(date: Date): number {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
}

function pad2(n: number): string {
  return n < 10 ? `0${n}` : String(n)
}

function calendarDayDiff(from: Date, to: Date): number {
  return Math.round((startOfDay(to) - startOfDay(from)) / 86_400_000)
}

/** HH:mm */
export function formatClockTime(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  return `${pad2(date.getHours())}:${pad2(date.getMinutes())}`
}

/**
 * 会话列表时间（飞书风格）：
 * 今天 → HH:mm；昨天 → 昨天；本年 → M月D日；更早 → YYYY年M月D日
 */
export function formatRelativeTime(iso?: string): string {
  if (!iso) {
    return ''
  }
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  const dayDiff = calendarDayDiff(date, new Date())
  if (dayDiff === 0) {
    return formatClockTime(iso)
  }
  if (dayDiff === 1) {
    return '昨天'
  }
  if (date.getFullYear() === new Date().getFullYear()) {
    return `${date.getMonth() + 1}月${date.getDate()}日`
  }
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

/**
 * 消息区跨日日期分隔（飞书风格，不含时钟）：
 * 今天 → 今天；昨天 → 昨天；本年 → M月D日；更早 → YYYY年M月D日
 */
export function formatDayLabel(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  const dayDiff = calendarDayDiff(date, new Date())
  if (dayDiff === 0) {
    return '今天'
  }
  if (dayDiff === 1) {
    return '昨天'
  }
  if (date.getFullYear() === new Date().getFullYear()) {
    return `${date.getMonth() + 1}月${date.getDate()}日`
  }
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

/**
 * 消息区分段时间头。
 * - day：跨日日期分隔（昨天 / 7月14日…）
 * - time：同日间隔的开始时钟（HH:mm）
 */
export function formatMessageTimeHeader(iso: string, kind: 'day' | 'time' = 'time'): string {
  if (kind === 'day') {
    return formatDayLabel(iso)
  }
  return formatClockTime(iso)
}

function isSameCalendarDay(a: Date, b: Date): boolean {
  return startOfDay(a) === startOfDay(b)
}

function canContinueMessageGroup(
  prev: MessageViewItem,
  curr: MessageViewItem,
  isSystem: (m: Pick<MessageItem, 'senderType' | 'type'>) => boolean
): boolean {
  if (isSystem(prev) || isSystem(curr)) {
    return false
  }
  if (prev.senderId !== curr.senderId) {
    return false
  }
  const prevAt = new Date(prev.createTime)
  const currAt = new Date(curr.createTime)
  if (!isSameCalendarDay(prevAt, currAt)) {
    return false
  }
  return currAt.getTime() - prevAt.getTime() < MESSAGE_GROUP_GAP_MS
}

/**
 * 为消息列表标注时间头与连续分组边界（纯前端展示适配）。
 *
 * 规则（对齐飞书）：
 * - 同发送者、同日、间隔不足 5 分钟 → 连续成组（紧凑气泡，组末显示 HH:mm）
 * - 跨日 → 居中日期分隔（昨天 / M月D日…），组末仍显示时钟
 * - 同日间隔 ≥ 5 分钟 → 居中 HH:mm 作为该段开始时间
 * - 会话首条若为今天 → 不额外插时间头（组末时钟即可）
 */
export function buildMessageTimeline(
  messages: MessageViewItem[],
  isSystem: (m: Pick<MessageItem, 'senderType' | 'type'>) => boolean
): MessageTimelineItem[] {
  return messages.map((message, index) => {
    const prev = index > 0 ? messages[index - 1] : undefined
    const next = index < messages.length - 1 ? messages[index + 1] : undefined

    if (isSystem(message)) {
      return {
        message,
        showTimeHeader: false,
        timeHeader: '',
        isGroupStart: true,
        isGroupEnd: true
      }
    }

    const continuesPrev = prev != null && canContinueMessageGroup(prev, message, isSystem)
    const continuesNext = next != null && canContinueMessageGroup(message, next, isSystem)

    let showTimeHeader = false
    let timeHeader = ''

    if (!continuesPrev) {
      const currAt = new Date(message.createTime)
      if (!prev || isSystem(prev)) {
        // 会话首条，或系统消息之后的新段
        const dayDiff = calendarDayDiff(currAt, new Date())
        if (dayDiff !== 0) {
          showTimeHeader = true
          timeHeader = formatMessageTimeHeader(message.createTime, 'day')
        } else if (prev && isSystem(prev)) {
          const prevAt = new Date(prev.createTime)
          if (!isSameCalendarDay(prevAt, currAt)
            || currAt.getTime() - prevAt.getTime() >= MESSAGE_GROUP_GAP_MS) {
            showTimeHeader = true
            timeHeader = !isSameCalendarDay(prevAt, currAt)
              ? formatMessageTimeHeader(message.createTime, 'day')
              : formatMessageTimeHeader(message.createTime, 'time')
          }
        }
        // 今天会话首条：不插时间头，组末时钟足够
      } else {
        const prevAt = new Date(prev.createTime)
        if (!isSameCalendarDay(prevAt, currAt)) {
          showTimeHeader = true
          timeHeader = formatMessageTimeHeader(message.createTime, 'day')
        } else if (currAt.getTime() - prevAt.getTime() >= MESSAGE_GROUP_GAP_MS) {
          showTimeHeader = true
          timeHeader = formatMessageTimeHeader(message.createTime, 'time')
        }
        // 仅发送者切换且未跨间隔：无时间头，靠分组间距区分
      }
    }

    return {
      message,
      showTimeHeader,
      timeHeader,
      isGroupStart: !continuesPrev,
      isGroupEnd: !continuesNext
    }
  })
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
