<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import type { NavigationMenuItem } from '@nuxt/ui'

const route = useRoute()

const props = defineProps<{
  title: string
  description?: string
}>()

const navItems = computed<NavigationMenuItem[]>(() => [
  {
    label: '组织策略',
    icon: 'i-lucide-sliders-horizontal',
    to: '/admin/products/calendar/org',
    active: route.path.startsWith('/admin/products/calendar/org')
  },
  {
    label: '全员日历',
    icon: 'i-lucide-calendar-range',
    to: '/admin/products/calendar/all-staff',
    active: route.path.startsWith('/admin/products/calendar/all-staff')
  }
])
</script>

<template>
  <div class="flex gap-6">
    <aside class="hidden w-52 shrink-0 lg:block">
      <div class="sticky top-0 space-y-3">
        <div class="px-2">
          <p class="text-xs font-medium uppercase tracking-wide text-muted">
            日历
          </p>
          <h2 class="mt-1 text-sm font-semibold text-highlighted">
            组织设置
          </h2>
        </div>
        <UNavigationMenu
          :items="navItems"
          orientation="vertical"
          class="admin-nav-menu"
        />
      </div>
    </aside>

    <div class="min-w-0 flex-1 space-y-6">
      <div class="flex flex-wrap items-start justify-between gap-3 lg:hidden">
        <UNavigationMenu
          :items="navItems"
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
