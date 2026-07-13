import { onMounted } from 'vue'
import { useImWebSocket } from './useImWebSocket'
import { useImStore } from '../stores/im'
import { useNotifyStore } from '../stores/notify'

/**
 * 工作台壳层级实时连接：IM 推送 + 通知角标 + 会话列表预加载。
 */
export function useWorkspaceImRealtime() {
  const im = useImStore()
  const notify = useNotifyStore()

  useImWebSocket({
    onMessageNew: message => im.handleMessageNew(message),
    onReadUpdated: payload => im.handleReadUpdated(payload.conversationId, payload.userId, payload.readSeq),
    onNotifyNew: () => notify.handleNotifyNew()
  })

  onMounted(() => {
    void im.fetchConversations().catch(() => {
      // 页面内或侧栏静默失败
    })
  })
}
