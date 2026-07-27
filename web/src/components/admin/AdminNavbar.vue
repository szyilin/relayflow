<script setup lang="ts">
defineProps<{
  title: string
}>()
</script>

<template>
  <UDashboardNavbar
    :title="title"
    class="admin-navbar"
  >
    <template #leading>
      <UDashboardSidebarCollapse />
    </template>
  </UDashboardNavbar>
</template>
