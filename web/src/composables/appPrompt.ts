import { reactive } from 'vue'

export type AppPromptOptions = {
  title: string
  description?: string
  defaultValue?: string
  placeholder?: string
  confirmLabel?: string
  cancelLabel?: string
}

type AppPromptState = {
  open: boolean
  title: string
  description: string
  value: string
  placeholder: string
  confirmLabel: string
  cancelLabel: string
  resolve: ((value: string | null) => void) | null
}

export const appPromptState = reactive<AppPromptState>({
  open: false,
  title: '',
  description: '',
  value: '',
  placeholder: '',
  confirmLabel: '确定',
  cancelLabel: '取消',
  resolve: null
})

/** Promise-based text prompt using Nuxt UI modal (replaces window.prompt). */
export function appPrompt(options: AppPromptOptions): Promise<string | null> {
  if (appPromptState.open && appPromptState.resolve) {
    appPromptState.resolve(null)
  }
  return new Promise((resolve) => {
    appPromptState.title = options.title
    appPromptState.description = options.description ?? ''
    appPromptState.value = options.defaultValue ?? ''
    appPromptState.placeholder = options.placeholder ?? ''
    appPromptState.confirmLabel = options.confirmLabel ?? '确定'
    appPromptState.cancelLabel = options.cancelLabel ?? '取消'
    appPromptState.resolve = resolve
    appPromptState.open = true
  })
}

export function settleAppPrompt(value: string | null) {
  const resolve = appPromptState.resolve
  appPromptState.open = false
  appPromptState.resolve = null
  resolve?.(value)
}
