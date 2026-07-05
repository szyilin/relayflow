import type { Router } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const PUBLIC_ADMIN_PATHS = new Set(['/admin/login'])
const PUBLIC_APP_PATHS = new Set(['/app/login'])

export function setupAdminGuards(router: Router) {
  router.beforeEach((to) => {
    const authStore = useAuthStore()

    if (to.path === '/') {
      return true
    }

    if (to.path.startsWith('/app')) {
      const isPublic = PUBLIC_APP_PATHS.has(to.path)

      if (!authStore.isAuthenticated && !isPublic) {
        return {
          path: '/app/login',
          query: { redirect: to.fullPath }
        }
      }

      if (authStore.isAuthenticated && to.path === '/app/login') {
        return { path: '/app/messages' }
      }

      return true
    }

    if (!to.path.startsWith('/admin')) {
      return true
    }

    const isPublic = PUBLIC_ADMIN_PATHS.has(to.path)

    if (!authStore.isAuthenticated && !isPublic) {
      return {
        path: '/admin/login',
        query: { redirect: to.fullPath }
      }
    }

    if (authStore.isAuthenticated && to.path === '/admin/login') {
      return { path: '/admin' }
    }

    return true
  })
}
