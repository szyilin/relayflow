<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import type { NavigationMenuItem } from '@nuxt/ui'

const route = useRoute()

const props = defineProps<{
  title: string
  description?: string
}>()

const imNavItems = computed<NavigationMenuItem[]>(() => [
  {
    label: '群规模管控',
    icon: 'i-lucide-users',
    to: '/admin/products/im/group-size',
    active: route.path.startsWith('/admin/products/im/group-size')
  },
  {
    label: '群权限管控',
    icon: 'i-lucide-shield',
    to: '/admin/products/im/group-permission',
    active: route.path.startsWith('/admin/products/im/group-permission')
  },
  {
    label: '消息权限管控',
    icon: 'i-lucide-message-square',
    to: '/admin/products/im/message-permission',
    active: route.path.startsWith('/admin/products/im/message-permission')
  }
])
</script>

<template>
  <div class="flex min-h-0 flex-1 gap-6">
    <aside class="hidden w-52 shrink-0 lg:block">
      <div class="sticky top-0 space-y-3">
        <div class="px-2">
          <p class="text-xs font-medium uppercase tracking-wide text-muted">
            即时消息
          </p>
          <h2 class="mt-1 text-sm font-semibold text-highlighted">
            组织设置
          </h2>
        </div>
        <UNavigationMenu
          :items="imNavItems"
          orientation="vertical"
          class="admin-nav-menu"
        />
      </div>
    </aside>

    <div class="min-w-0 flex-1 space-y-6">
      <div class="flex flex-wrap items-start justify-between gap-3 lg:hidden">
        <UNavigationMenu
          :items="imNavItems"
          orientation="horizontal"
          class="admin-nav-menu"
        />
      </div>

      <div class="flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0 space-y-1">
          <h1 class="text-xl font-semibold text-highlighted">
            {{ props.title }}
          </h1>
          <p
            v-if="props.description"
            class="text-sm text-muted"
          >
            {{ props.description }}
          </p>
        </div>
        <div
          v-if="$slots.actions"
          class="flex shrink-0 items-center gap-2"
        >
          <slot name="actions" />
        </div>
      </div>

      <slot />
    </div>
  </div>
</template>
