<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import type { NavigationMenuItem } from '@nuxt/ui'

const route = useRoute()

const props = defineProps<{
  title: string
  description?: string
}>()

const docsNavItems = computed<NavigationMenuItem[]>(() => [
  {
    label: '权限与分享',
    icon: 'i-lucide-shield',
    to: '/admin/products/docs/permissions',
    active: route.path.startsWith('/admin/products/docs/permissions')
  },
  {
    label: '文件管理',
    icon: 'i-lucide-files',
    to: '/admin/products/docs/files',
    active: route.path.startsWith('/admin/products/docs/files')
  },
  {
    label: '存储空间',
    icon: 'i-lucide-hard-drive',
    to: '/admin/products/docs/storage',
    active: route.path.startsWith('/admin/products/docs/storage')
  }
])
</script>

<template>
  <div class="flex min-h-0 flex-1 gap-6">
    <aside class="hidden w-52 shrink-0 lg:block">
      <div class="sticky top-0 space-y-3">
        <div class="px-2">
          <p class="text-xs font-medium uppercase tracking-wide text-muted">
            云文档
          </p>
          <h2 class="mt-1 text-sm font-semibold text-highlighted">
            组织设置
          </h2>
        </div>
        <UNavigationMenu
          :items="docsNavItems"
          orientation="vertical"
          class="admin-nav-menu"
        />
      </div>
    </aside>

    <div class="min-w-0 flex-1 space-y-6">
      <div class="flex flex-wrap items-start justify-between gap-3 lg:hidden">
        <UNavigationMenu
          :items="docsNavItems"
          orientation="horizontal"
          class="admin-nav-menu"
        />
      </div>

      <div class="flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0 space-y-1">
          <nav
            class="flex flex-wrap items-center gap-1.5 text-sm text-muted"
            aria-label="面包屑"
          >
            <RouterLink
              to="/admin/products"
              class="inline-flex items-center gap-1 rounded-md transition-colors hover:text-highlighted"
            >
              <UIcon
                name="i-lucide-arrow-left"
                class="size-3.5 shrink-0"
              />
              产品设置
            </RouterLink>
            <span
              class="text-muted/50"
              aria-hidden="true"
            >/</span>
            <span class="text-default">云文档</span>
          </nav>
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
