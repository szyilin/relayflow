<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '../../stores/auth'
import AdminThemeMenu from './AdminThemeMenu.vue'
import AdminUserMenu from './AdminUserMenu.vue'

defineProps<{
  title: string
}>()

const authStore = useAuthStore()

onMounted(() => {
  if (authStore.isAuthenticated && authStore.tenants.length === 0) {
    void authStore.fetchMyTenants()
  }
})
</script>

<template>
  <UDashboardNavbar
    :title="title"
    class="admin-navbar"
    :ui="{ right: 'flex items-center gap-1.5 sm:gap-2 shrink-0' }"
  >
    <template #leading>
      <UDashboardSidebarCollapse />
    </template>

    <template #right>
      <div
        class="hidden min-w-0 max-w-[11rem] items-center gap-1.5 rounded-lg border border-default bg-elevated/60 px-2.5 py-1.5 text-sm text-muted sm:flex"
        :title="authStore.activeTenantName"
      >
        <UIcon name="i-lucide-building-2" class="size-4 shrink-0 text-primary" />
        <span class="truncate">{{ authStore.activeTenantName }}</span>
      </div>

      <AdminThemeMenu />
      <AdminUserMenu />
    </template>
  </UDashboardNavbar>
</template>
