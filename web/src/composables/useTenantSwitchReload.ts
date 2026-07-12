import { watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useContactsStore } from '../stores/contacts'
import { useImStore } from '../stores/im'
import { useProfileStore } from '../stores/profile'

/**
 * 账号/企业切换后清空并重新加载当前工作台页的数据；WebSocket 由 useImWebSocket 监听 token 自动重连。
 */
export function useTenantSwitchReload() {
  const auth = useAuthStore()
  const route = useRoute()
  const im = useImStore()
  const contacts = useContactsStore()
  const profile = useProfileStore()

  watch(() => [auth.tenantId, auth.userId] as const, async ([nextTenant, nextUser], prev) => {
    const [prevTenant, prevUser] = prev ?? [null, null]
    if (!nextTenant || !nextUser) {
      return
    }
    if (nextTenant === prevTenant && nextUser === prevUser) {
      return
    }

    profile.resetForAccountSwitch()
    im.resetForTenantSwitch()
    contacts.resetForTenantSwitch()

    try {
      await profile.fetchProfile()
    } catch {
      // 侧栏展示回退到 auth store
    }

    if (route.path.startsWith('/app/messages')) {
      try {
        await im.fetchConversations()
      } catch {
        // 页面内已有错误提示
      }
      return
    }

    try {
      await im.fetchConversations()
    } catch {
      // 侧栏未读角标静默失败
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
