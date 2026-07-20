import { watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useCalendarStore } from '../stores/calendar'
import { useContactsStore } from '../stores/contacts'
import { useDocsStore } from '../stores/docs'
import { useImStore } from '../stores/im'
import { useProfileStore } from '../stores/profile'
import { useTasksStore } from '../stores/tasks'
import { useUserPreferenceStore } from '../stores/userPreference'

/**
 * 账号/企业切换后清空并重新加载当前工作台页的数据；WebSocket 由 useImWebSocket 监听 token 自动重连。
 * 威胁模型与约定见 docs/dev/workspace-ui-patterns.md § 企业/账号切换。
 */
export function useTenantSwitchReload() {
  const auth = useAuthStore()
  const route = useRoute()
  const im = useImStore()
  const contacts = useContactsStore()
  const profile = useProfileStore()
    const tasks = useTasksStore()
    const calendar = useCalendarStore()
    const docs = useDocsStore()
    const preference = useUserPreferenceStore()

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
    tasks.resetForTenantSwitch()
    calendar.resetForTenantSwitch()
    docs.resetLocal()
    preference.resetForTenantSwitch()

    try {
      await preference.fetchFromServer()
    } catch {
      // 外观回退到默认；设置窗可再拉
    }

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

    if (route.path.startsWith('/app/tasks')) {
      try {
        await tasks.setNavView(tasks.navView)
      } catch {
        // 页面内已有错误提示
      }
    }

    if (route.path.startsWith('/app/calendar')) {
      try {
        await calendar.fetchCalendars()
        await calendar.fetchShares()
      } catch {
        // 页面 onMounted / watch 会再拉
      }
    }

    if (route.path.startsWith('/app/docs')) {
      try {
        await docs.ensureHydrated()
      } catch {
        // 页面 onMounted 会再拉
      }
    }
  })
}
