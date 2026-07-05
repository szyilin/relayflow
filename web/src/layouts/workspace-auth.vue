<template>
  <div class="workspace-auth-bg flex min-h-svh items-center justify-center p-6">
    <div class="absolute right-4 top-4 flex items-center gap-1">
      <AdminColorModeToggle />
      <AdminThemeMenu />
    </div>
    <RouterView />
  </div>
</template>

<script setup lang="ts">
import AdminColorModeToggle from '../components/admin/AdminColorModeToggle.vue'
import AdminThemeMenu from '../components/admin/AdminThemeMenu.vue'
</script>
