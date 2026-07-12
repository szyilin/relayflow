import { watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useContactsStore } from '../stores/contacts'
import { useImStore } from '../stores/im'

/**
 * 企业切换后清空并重新加载当前工作台页的数据；WebSocket 由 useImWebSocket 监听 token 自动重连。
 */
export function useTenantSwitchReload() {
  const auth = useAuthStore()
  const route = useRoute()
  const im = useImStore()
  const contacts = useContactsStore()

  watch(() => auth.tenantId, async (next, prev) => {
    if (!next || !prev || next === prev) {
      return
    }

    im.resetForTenantSwitch()
    contacts.resetForTenantSwitch()

    if (route.path.startsWith('/app/messages')) {
      try {
        await im.fetchConversations()
      } catch {
        // 页面内已有错误提示
      }
      return
    }

    if (route.path.startsWith('/app/contacts')) {
      try {
        await contacts.fetchDepts()
        const rootId = contacts.rootDeptId()
        if (rootId) {
          await contacts.fetchMembers({ deptId: rootId, keyword: '' })
        }
      } catch {
        // 页面内已有错误提示
      }
    }
  })
}
