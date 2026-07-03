<script setup lang="ts">
import { ref } from 'vue'
import type { NavigationMenuItem } from '@nuxt/ui'

const open = ref(false)

const links = [[{
  label: '概览',
  icon: 'i-lucide-layout-dashboard',
  to: '/admin',
  exact: true,
  onSelect: () => {
    open.value = false
  }
}]] satisfies NavigationMenuItem[][]
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
        <div class="flex items-center gap-2 px-2 py-1 font-semibold" :class="collapsed ? 'justify-center' : ''">
          <UIcon name="i-lucide-workflow" class="size-5 shrink-0 text-primary" />
          <span v-if="!collapsed">RelayFlow</span>
        </div>
      </template>

      <template #default="{ collapsed }">
        <UNavigationMenu
          :collapsed="collapsed"
          :items="links[0]"
          orientation="vertical"
          tooltip
        />
      </template>
    </UDashboardSidebar>

    <RouterView />
  </UDashboardGroup>
</template>
