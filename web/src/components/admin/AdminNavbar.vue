<script setup lang="ts">
import { useTenantStore } from '../../stores/tenant'
import AdminUserMenu from './AdminUserMenu.vue'

defineProps<{
  title: string
}>()

const tenantStore = useTenantStore()
</script>

<template>
  <UDashboardNavbar :title="title" :ui="{ right: 'gap-2 sm:gap-3' }">
    <template #leading>
      <UDashboardSidebarCollapse />
    </template>

    <template #right>
      <div class="hidden sm:flex items-center gap-2 text-sm text-muted">
        <UIcon name="i-lucide-building-2" class="size-4 shrink-0" />
        <span>{{ tenantStore.tenantName }}</span>
      </div>

      <AdminUserMenu />
    </template>
  </UDashboardNavbar>
</template>
