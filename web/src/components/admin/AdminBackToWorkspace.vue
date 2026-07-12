<script setup lang="ts">
defineProps<{
  collapsed?: boolean
}>()
</script>

<template>
  <RouterLink
    to="/app/messages"
    class="admin-back-workspace flex items-center gap-2 rounded-lg px-2.5 py-2 text-sm text-muted transition-colors hover:bg-elevated hover:text-highlighted"
    :class="collapsed ? 'justify-center' : ''"
    :title="collapsed ? '返回工作台' : undefined"
  >
    <UIcon name="i-lucide-arrow-left" class="size-4 shrink-0" />
    <span v-if="!collapsed" class="truncate">返回工作台</span>
  </RouterLink>
</template>

<style scoped>
.admin-back-workspace {
  margin-bottom: 0.25rem;
}
</style>
