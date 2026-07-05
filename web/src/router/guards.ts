import type { Router } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const PUBLIC_ADMIN_PATHS = new Set(['/admin/login'])

export function setupAdminGuards(router: Router) {
  router.beforeEach((to) => {
    if (to.path === '/') {
      return { path: '/admin/login' }
    }

    if (!to.path.startsWith('/admin')) {
      return true
    }

    const authStore = useAuthStore()
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
