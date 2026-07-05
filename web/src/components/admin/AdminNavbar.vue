<script setup lang="ts">
import { useTenantStore } from '../../stores/tenant'
import AdminColorModeToggle from './AdminColorModeToggle.vue'
import AdminThemeMenu from './AdminThemeMenu.vue'
import AdminUserMenu from './AdminUserMenu.vue'

defineProps<{
  title: string
}>()

const tenantStore = useTenantStore()
</script>

<template>
  <UDashboardNavbar
    :title="title"
    class="admin-navbar"
    :ui="{ right: 'gap-2 sm:gap-3' }"
  >
    <template #leading>
      <UDashboardSidebarCollapse />
    </template>

    <template #right>
      <div class="hidden sm:flex items-center gap-2 text-sm text-muted">
        <UIcon name="i-lucide-building-2" class="size-4 shrink-0 text-primary" />
        <span>{{ tenantStore.tenantName }}</span>
      </div>

      <AdminColorModeToggle />
      <AdminThemeMenu />
      <AdminUserMenu />
    </template>
  </UDashboardNavbar>
</template>
