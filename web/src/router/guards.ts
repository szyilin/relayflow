import type { Router } from 'vue-router'
import { useAuthStore } from '../stores/auth'

/** 产品唯一登录页 */
export const LOGIN_PATH = '/app/login'

const PUBLIC_PATHS = new Set([LOGIN_PATH])

export function setupAdminGuards(router: Router) {
  router.beforeEach((to) => {
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
        return { path: '/app/messages' }
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

    return true
  })
}
