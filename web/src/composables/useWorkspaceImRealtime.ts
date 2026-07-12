import { onMounted } from 'vue'
import { useImWebSocket } from './useImWebSocket'
import { useImStore } from '../stores/im'

/**
 * 工作台壳层级 IM 实时连接：WebSocket 推送 + 会话列表预加载（供侧栏未读角标）。
 */
export function useWorkspaceImRealtime() {
  const im = useImStore()

  useImWebSocket({
    onMessageNew: message => im.handleMessageNew(message),
    onReadUpdated: payload => im.handleReadUpdated(payload.conversationId, payload.userId, payload.readSeq)
  })

  onMounted(() => {
    void im.fetchConversations().catch(() => {
      // 页面内或侧栏静默失败
    })
  })
}
