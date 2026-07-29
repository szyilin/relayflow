import { onMounted } from 'vue'
import { useImWebSocket } from './useImWebSocket'
import { useImStore } from '../stores/im'
import { useTypingStore } from '../stores/typing'

/**
 * 工作台壳层级实时连接：IM 推送 + 会话列表预加载。
 * 业务触达走 bot_dm / 会话未读，不再订阅 domain=notify。
 */
export function useWorkspaceImRealtime() {
  const im = useImStore()
  const typing = useTypingStore()

  useImWebSocket({
    onMessageNew: message => im.handleMessageNew(message),
    onMessageUpdated: message => im.handleMessageUpdated(message),
    onReactionUpdated: payload => im.applyReactionUpdated(payload),
    onReadUpdated: payload => im.handleReadUpdated(payload.conversationId, payload.userId, payload.readSeq),
    onTypingUpdated: payload => typing.applyTypingUpdated(payload)
  })

  onMounted(() => {
    void im.fetchConversations().catch(() => {
      // 页面内或侧栏静默失败
    })
  })
}
