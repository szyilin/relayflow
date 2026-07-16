import { onMounted } from 'vue'
import { useImWebSocket } from './useImWebSocket'
import { useImStore } from '../stores/im'

/**
 * 工作台壳层级实时连接：IM 推送 + 会话列表预加载。
 * 业务触达走 bot_dm / 会话未读，不再订阅 domain=notify。
 */
export function useWorkspaceImRealtime() {
  const im = useImStore()

  useImWebSocket({
    onMessageNew: message => im.handleMessageNew(message),
    onMessageUpdated: message => im.handleMessageUpdated(message),
    onReadUpdated: payload => im.handleReadUpdated(payload.conversationId, payload.userId, payload.readSeq)
  })

  onMounted(() => {
    void im.fetchConversations().catch(() => {
      // 页面内或侧栏静默失败
    })
  })
}
