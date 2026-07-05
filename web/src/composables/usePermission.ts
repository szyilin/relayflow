import { useAuthStore } from '../stores/auth'

export function usePermission() {
  const auth = useAuthStore()

  function hasPermission(code: string) {
    return auth.permissions.includes(code)
  }

  return { hasPermission }
}
