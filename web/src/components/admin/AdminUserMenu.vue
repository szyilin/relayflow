<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { DropdownMenuItem } from '@nuxt/ui'
import { useAuthStore } from '../../stores/auth'

defineProps<{
  collapsed?: boolean
}>()

const router = useRouter()
const authStore = useAuthStore()

const displayName = computed(() => authStore.user?.nickname ?? '管理员')

const items = computed<DropdownMenuItem[][]>(() => [[{
  type: 'label',
  label: displayName.value,
  avatar: {
    alt: displayName.value,
    text: displayName.value.slice(0, 1)
  }
}], [{
  label: '退出登录',
  icon: 'i-lucide-log-out',
  onSelect: () => {
    authStore.logout()
    router.push('/admin/login')
  }
}]])
</script>

<template>
  <UDropdownMenu
    :items="items"
    :content="{ align: 'center', collisionPadding: 12 }"
    :ui="{ content: collapsed ? 'w-48' : 'w-(--reka-dropdown-menu-trigger-width)' }"
  >
    <UButton
      :label="collapsed ? undefined : displayName"
      :avatar="{ alt: displayName, text: displayName.slice(0, 1) }"
      :trailing-icon="collapsed ? undefined : 'i-lucide-chevrons-up-down'"
      color="neutral"
      variant="ghost"
      block
      :square="collapsed"
      class="data-[state=open]:bg-elevated"
      :ui="{ trailingIcon: 'text-dimmed' }"
    />
  </UDropdownMenu>
</template>
