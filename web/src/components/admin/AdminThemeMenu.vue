<script setup lang="ts">
import { computed } from 'vue'
import type { DropdownMenuItem } from '@nuxt/ui'
import { useAdminColorMode } from '../../composables/useAdminColorMode'

const colorMode = useAdminColorMode()
const appConfig = useAppConfig()

const primaryColors = ['teal', 'green', 'cyan', 'blue', 'emerald', 'sky'] as const

const appearanceIcon = computed(() => {
  if (colorMode.value === 'dark') {
    return 'i-lucide-moon'
  }
  return 'i-lucide-sun'
})

function setAppearance(mode: 'light' | 'dark' | 'auto') {
  // VueUse 14：用 .value 写入 store，不存在 .preference
  colorMode.value = mode
}

function setPrimary(color: string) {
  appConfig.ui.colors.primary = color
}

const items = computed<DropdownMenuItem[][]>(() => {
  const pref = colorMode.store.value

  return [[{
    label: '浅色模式',
    icon: 'i-lucide-sun',
    type: pref === 'light' ? 'checkbox' : undefined,
    checked: pref === 'light',
    onSelect(e: Event) {
      e.preventDefault()
      setAppearance('light')
    }
  }, {
    label: '深色模式',
    icon: 'i-lucide-moon',
    type: pref === 'dark' ? 'checkbox' : undefined,
    checked: pref === 'dark',
    onSelect(e: Event) {
      e.preventDefault()
      setAppearance('dark')
    }
  }, {
    label: '跟随系统',
    icon: 'i-lucide-laptop',
    type: pref === 'auto' ? 'checkbox' : undefined,
    checked: pref === 'auto',
    onSelect(e: Event) {
      e.preventDefault()
      setAppearance('auto')
    }
  }], primaryColors.map(color => ({
    label: `主题色 · ${color}`,
    slot: 'chip',
    chip: color,
    type: appConfig.ui.colors.primary === color ? 'checkbox' : undefined,
    checked: appConfig.ui.colors.primary === color,
    onSelect(e: Event) {
      e.preventDefault()
      setPrimary(color)
    }
  }))]
})
</script>

<template>
  <UDropdownMenu
    :items="items"
    :content="{ align: 'end', collisionPadding: 12 }"
  >
    <UButton
      :icon="appearanceIcon"
      color="neutral"
      variant="ghost"
      square
      aria-label="外观与主题色"
    />

    <template #chip-leading="{ item }">
      <div class="inline-flex size-5 shrink-0 items-center justify-center">
        <span
          class="size-2 rounded-full ring ring-bg bg-(--chip-light) dark:bg-(--chip-dark)"
          :style="{
            '--chip-light': `var(--color-${(item as { chip?: string }).chip}-500)`,
            '--chip-dark': `var(--color-${(item as { chip?: string }).chip}-400)`
          }"
        />
      </div>
    </template>
  </UDropdownMenu>
</template>
