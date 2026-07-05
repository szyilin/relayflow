<script setup lang="ts">
import { useAdminNav } from '../composables/useAdminNav'
import AdminUserMenu from '../components/admin/AdminUserMenu.vue'

const {
  open,
  primaryLinks,
  systemLinks,
  infraLinks,
  devLinks
} = useAdminNav()
</script>

<template>
  <UDashboardGroup unit="rem" storage="local">
    <UDashboardSidebar
      id="admin"
      v-model:open="open"
      collapsible
      resizable
      class="bg-elevated/25"
      :ui="{ footer: 'lg:border-t lg:border-default' }"
    >
      <template #header="{ collapsed }">
        <div
          class="flex items-center gap-2 px-2 py-1 font-semibold"
          :class="collapsed ? 'justify-center' : ''"
        >
          <UIcon name="i-lucide-workflow" class="size-5 shrink-0 text-primary" />
          <span v-if="!collapsed">RelayFlow</span>
        </div>
      </template>

      <template #default="{ collapsed }">
        <UNavigationMenu
          :collapsed="collapsed"
          :items="primaryLinks"
          orientation="vertical"
          tooltip
          class="admin-nav-menu"
        />

        <div
          v-if="!collapsed"
          class="px-3 pt-4 pb-1 text-xs font-medium uppercase tracking-wide text-muted"
        >
          系统管理
        </div>
        <UNavigationMenu
          :collapsed="collapsed"
          :items="systemLinks"
          orientation="vertical"
          tooltip
          class="admin-nav-menu"
        />

        <div
          v-if="!collapsed"
          class="px-3 pt-4 pb-1 text-xs font-medium uppercase tracking-wide text-muted"
        >
          基础设施
        </div>
        <UNavigationMenu
          :collapsed="collapsed"
          :items="infraLinks"
          orientation="vertical"
          tooltip
          class="admin-nav-menu"
        />

        <UNavigationMenu
          :collapsed="collapsed"
          :items="devLinks"
          orientation="vertical"
          tooltip
          class="mt-auto admin-nav-menu"
        />
      </template>

      <template #footer="{ collapsed }">
        <AdminUserMenu :collapsed="collapsed" />
      </template>
    </UDashboardSidebar>

    <RouterView />
  </UDashboardGroup>
</template>

<style scoped>
.admin-nav-menu :deep([data-active="true"]) {
  border-left: 2px solid var(--ui-primary);
  background-color: color-mix(in oklab, var(--ui-primary) 12%, transparent);
}
</style>
