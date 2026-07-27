<script setup lang="ts">
import AdminUserMenu from './AdminUserMenu.vue'

defineProps<{
  title: string
}>()
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
      <AdminUserMenu />
    </template>
  </UDashboardNavbar>
</template>
