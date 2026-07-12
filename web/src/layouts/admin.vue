<script setup lang="ts">
import { onMounted } from 'vue'
import { useAdminNav } from '../composables/useAdminNav'
import { useAuthStore } from '../stores/auth'
import { ApiError } from '../api/request'
import AdminBackToWorkspace from '../components/admin/AdminBackToWorkspace.vue'
import AdminUserMenu from '../components/admin/AdminUserMenu.vue'

const toast = useToast()

const {
  open,
  primaryLinks,
  systemLinks,
  infraLinks,
  devLinks
} = useAdminNav()

const authStore = useAuthStore()

onMounted(async () => {
  if (authStore.isAuthenticated) {
    try {
      await authStore.fetchPermissionInfo()
      if (authStore.tenants.length === 0) {
        await authStore.fetchMyTenants()
      }
    } catch (error) {
      const message = error instanceof ApiError
        ? error.message
        : '权限信息加载失败，请刷新页面重试'
      toast.add({
        title: '权限信息加载失败',
        description: message,
        color: 'warning'
      })
    }
  }
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

        <template v-if="systemLinks.length">
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
        </template>

        <template v-if="infraLinks.length">
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
        </template>

        <UNavigationMenu
          :collapsed="collapsed"
          :items="devLinks"
          orientation="vertical"
          tooltip
          class="mt-auto admin-nav-menu"
        />

        <AdminBackToWorkspace :collapsed="collapsed" />
      </template>

      <template #footer="{ collapsed }">
        <AdminUserMenu :collapsed="collapsed" />
      </template>
    </UDashboardSidebar>

    <div class="admin-main flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden">
      <RouterView />
    </div>
  </UDashboardGroup>
</template>
