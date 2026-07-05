import { createSharedComposable } from '@vueuse/core'
import { useColorMode } from '@vueuse/core'

export const useAdminColorMode = createSharedComposable(() =>
  useColorMode({
    attribute: 'class',
    modes: {
      light: '',
      dark: 'dark',
      auto: ''
    },
    initialValue: 'light',
    storageKey: 'relayflow-color-mode-v2'
  })
)
