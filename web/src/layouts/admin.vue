<script setup lang="ts">
import { onMounted } from 'vue'
import { useAdminNav } from '../composables/useAdminNav'
import { useTenantStore } from '../stores/tenant'
import AdminUserMenu from '../components/admin/AdminUserMenu.vue'

const {
  open,
  primaryLinks,
  systemLinks,
  infraLinks,
  devLinks
} = useAdminNav()

const tenantStore = useTenantStore()

onMounted(() => {
  tenantStore.fetchDefaultTenant()
})
</script>

<template>
  <UDashboardGroup unit="rem" storage="local" class="admin-shell">
    <UDashboardSidebar
      id="admin"
      v-model:open="open"
      collapsible
      resizable
      class="admin-sidebar"
      :ui="{ footer: 'lg:border-t lg:border-default' }"
    >
      <template #header="{ collapsed }">
        <div
          class="flex items-center gap-2 px-2 py-1 font-semibold"
          :class="collapsed ? 'justify-center' : ''"
        >
          <div class="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary/15 text-primary">
            <UIcon name="i-lucide-workflow" class="size-5" />
          </div>
          <span v-if="!collapsed" class="text-highlighted">RelayFlow</span>
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

    <div class="admin-main min-w-0 flex-1">
      <RouterView />
    </div>
  </UDashboardGroup>
</template>
