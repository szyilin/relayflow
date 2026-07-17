import { useRouter } from 'vue-router'
import type { useImStore } from '../../stores/im'

/**
 * Keep messages conversation selection in sync with `?conversationId=`.
 */
export function useMessageConversationRoute(im: ReturnType<typeof useImStore>) {
  const router = useRouter()

  async function selectConversation(conversationId: string) {
    await im.selectConversation(conversationId)
    await router.replace({
      query: {
        ...router.currentRoute.value.query,
        conversationId
      }
    })
  }

  return { selectConversation }
}
