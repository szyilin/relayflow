import { reactive } from 'vue'

export type AppConfirmOptions = {
  title: string
  description: string
  confirmLabel?: string
  cancelLabel?: string
  /** Destructive actions default to error */
  confirmColor?: 'error' | 'primary' | 'neutral'
}

type AppConfirmState = {
  open: boolean
  title: string
  description: string
  confirmLabel: string
  cancelLabel: string
  confirmColor: 'error' | 'primary' | 'neutral'
  resolve: ((ok: boolean) => void) | null
}

export const appConfirmState = reactive<AppConfirmState>({
  open: false,
  title: '',
  description: '',
  confirmLabel: '确定',
  cancelLabel: '取消',
  confirmColor: 'error',
  resolve: null
})

/** Promise-based confirm using Nuxt UI modal (replaces window.confirm). */
export function appConfirm(options: AppConfirmOptions): Promise<boolean> {
  if (appConfirmState.open && appConfirmState.resolve) {
    appConfirmState.resolve(false)
  }
  return new Promise((resolve) => {
    appConfirmState.title = options.title
    appConfirmState.description = options.description
    appConfirmState.confirmLabel = options.confirmLabel ?? '确定'
    appConfirmState.cancelLabel = options.cancelLabel ?? '取消'
    appConfirmState.confirmColor = options.confirmColor ?? 'error'
    appConfirmState.resolve = resolve
    appConfirmState.open = true
  })
}

export function settleAppConfirm(ok: boolean) {
  const resolve = appConfirmState.resolve
  appConfirmState.open = false
  appConfirmState.resolve = null
  resolve?.(ok)
}
