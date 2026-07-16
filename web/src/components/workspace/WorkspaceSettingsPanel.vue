<script setup lang="ts">
import { computed } from 'vue'
import { useAdminColorMode } from '../../composables/useAdminColorMode'

defineEmits<{
  back: []
}>()

const colorMode = useAdminColorMode()
const toast = useToast()

const themeOptions = [
  { value: 'light' as const, label: '浅色', icon: 'i-lucide-sun' },
  { value: 'dark' as const, label: '深色', icon: 'i-lucide-moon' },
  { value: 'auto' as const, label: '跟随系统', icon: 'i-lucide-laptop' }
]

const currentPref = computed(() => colorMode.store.value)

function setTheme(mode: 'light' | 'dark' | 'auto') {
  colorMode.value = mode
}

function onPlaceholder(label: string) {
  toast.add({
    title: '功能即将推出',
    description: `${label}（占位）`,
    color: 'neutral'
  })
}
</script>

<template>
  <div class="workspace-settings-panel w-72 p-3">
    <div class="mb-2 flex items-center gap-1">
      <button
        type="button"
        class="workspace-profile-action !w-auto !gap-1 !px-2"
        aria-label="返回"
        @click="$emit('back')"
      >
        <UIcon name="i-lucide-chevron-left" class="size-4" />
        <span class="text-sm font-medium">设置</span>
      </button>
    </div>

    <div class="space-y-3 px-1">
      <div>
        <p class="mb-1.5 px-1 text-xs font-medium text-[var(--ws-text-muted)]">
          外观
        </p>
        <div class="flex gap-1 rounded-xl bg-[var(--ws-input-bar-bg)] p-1">
          <button
            v-for="option in themeOptions"
            :key="option.value"
            type="button"
            class="flex flex-1 items-center justify-center gap-1 rounded-lg px-2 py-1.5 text-xs transition-colors"
            :class="currentPref === option.value
              ? 'bg-[var(--ws-panel-bg)] font-medium text-[var(--ws-text)] shadow-sm'
              : 'text-[var(--ws-text-muted)] hover:text-[var(--ws-text)]'"
            @click="setTheme(option.value)"
          >
            <UIcon :name="option.icon" class="size-3.5 shrink-0" />
            <span class="truncate">{{ option.label }}</span>
          </button>
        </div>
      </div>

      <div class="space-y-0.5 border-t border-[var(--ws-border-subtle)] pt-2">
        <button
          type="button"
          class="workspace-profile-action"
          @click="onPlaceholder('通知设置')"
        >
          <UIcon name="i-lucide-bell" class="size-4 shrink-0" />
          <span>通知设置</span>
          <span class="ml-auto text-xs text-[var(--ws-text-muted)]">（占位）</span>
        </button>
        <button
          type="button"
          class="workspace-profile-action"
          @click="onPlaceholder('隐私与安全')"
        >
          <UIcon name="i-lucide-shield-check" class="size-4 shrink-0" />
          <span>隐私与安全</span>
          <span class="ml-auto text-xs text-[var(--ws-text-muted)]">（占位）</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.workspace-profile-action {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 0.625rem;
  border-radius: 0.625rem;
  padding: 0.5rem 0.625rem;
  font-size: 0.875rem;
  line-height: 1.25rem;
  color: var(--ws-text);
  transition: background-color 0.15s ease;
}

.workspace-profile-action:hover {
  background: var(--ws-rail-hover);
}
</style>
