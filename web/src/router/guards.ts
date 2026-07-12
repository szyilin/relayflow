import type { Router } from 'vue-router'
import { useAuthStore } from '../stores/auth'

/** 产品唯一登录页 */
export const LOGIN_PATH = '/app/login'

/** 开放注册页（V2；须 permitAll + 路由放行） */
export const REGISTER_PATH = '/app/register'

/** 已登录但无管理身份时的引导页 */
export const NO_ADMIN_ACCESS_PATH = '/app/no-admin-access'

const PUBLIC_PATHS = new Set([
  LOGIN_PATH,
  REGISTER_PATH,
  '/app/invite/accept'
])

async function ensurePermissionInfoLoaded(authStore: ReturnType<typeof useAuthStore>) {
  if (!authStore.isAuthenticated || authStore.permissionInfoLoaded) {
    return
  }

  await authStore.fetchPermissionInfo()
}

export function setupAdminGuards(router: Router) {
  router.beforeEach(async (to) => {
    const authStore = useAuthStore()

    if (to.path === '/admin/login') {
      return {
        path: LOGIN_PATH,
        query: to.query
      }
    }

    if (to.path === '/') {
      if (authStore.isAuthenticated) {
        return { path: '/app/messages' }
      }

      return { path: LOGIN_PATH }
    }

    if (to.path.startsWith('/app')) {
      const isPublic = PUBLIC_PATHS.has(to.path)

      if (!authStore.isAuthenticated && !isPublic) {
        return {
          path: LOGIN_PATH,
          query: { redirect: to.fullPath }
        }
      }

      if (authStore.isAuthenticated && to.path === LOGIN_PATH) {
        if (to.query.addAccount === '1') {
          return true
        }
        return { path: '/app/messages' }
      }

      if (authStore.isAuthenticated && to.path === REGISTER_PATH) {
        return { path: '/app/messages' }
      }

      if (authStore.isAuthenticated && !isPublic) {
        try {
          await ensurePermissionInfoLoaded(authStore)
          if (authStore.tenants.length === 0) {
            await authStore.fetchMyTenants()
          }
        } catch {
          await authStore.logout()
          return {
            path: LOGIN_PATH,
            query: { redirect: to.fullPath }
          }
        }
      }

      return true
    }

    if (!to.path.startsWith('/admin')) {
      return true
    }

    if (!authStore.isAuthenticated) {
      return {
        path: LOGIN_PATH,
        query: { redirect: to.fullPath }
      }
    }

    try {
      await ensurePermissionInfoLoaded(authStore)
    } catch {
      await authStore.logout()
      return {
        path: LOGIN_PATH,
        query: { redirect: to.fullPath }
      }
    }

    if (!authStore.isAdmin) {
      return { path: NO_ADMIN_ACCESS_PATH }
    }

    return true
  })
}
