/**
 * Wraps Nuxt UI useToast: compact workspace notices get semantic icons by color.
 * Resolved via vite alias over `@nuxt/ui` useToast so existing call sites stay unchanged.
 */
import { useToast as useNuxtUiToast } from 'relayflow-nuxt-use-toast'

type ToastColor = 'error' | 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'neutral'

const ICONS: Partial<Record<ToastColor, string>> = {
  success: 'i-lucide-circle-check',
  warning: 'i-lucide-triangle-alert',
  error: 'i-lucide-circle-x',
  info: 'i-lucide-info',
  primary: 'i-lucide-info',
  secondary: 'i-lucide-info'
}

function enrich<T extends Record<string, unknown>>(toast: T): T {
  const color = (toast.color as ToastColor | undefined) ?? 'neutral'
  const icon = (toast.icon as string | undefined) ?? ICONS[color]
  return {
    close: false,
    progress: false,
    ...toast,
    color,
    ...(icon ? { icon } : {})
  }
}

export function useToast() {
  const toast = useNuxtUiToast()
  return {
    ...toast,
    add: (input: Parameters<typeof toast.add>[0]) => toast.add(enrich(input)),
    update: (id: string, input: Parameters<typeof toast.update>[1]) =>
      toast.update(id, enrich(input))
  }
}
