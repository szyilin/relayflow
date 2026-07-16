import { useAuthStore } from '../stores/auth'

export function usePermission() {
  const auth = useAuthStore()

  function hasPermission(code: string) {
    if (auth.permissions.includes('*:*:*')) {
      return true
    }
    return auth.permissions.includes(code)
  }

  return { hasPermission }
}
