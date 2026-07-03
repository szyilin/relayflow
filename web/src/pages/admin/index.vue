<script setup lang="ts">
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-home">
    <template #header>
      <UDashboardNavbar title="管理后台">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>
      </UDashboardNavbar>
    </template>

    <template #body>
      <UCard>
        <template #header>
          <h2 class="text-lg font-semibold">
            RelayFlow 管理端
          </h2>
        </template>
        <p class="text-muted">
          管理端脚手架已就绪。业务页面将挂载在 <code class="text-sm">/admin/*</code> 路由下。
        </p>
      </UCard>
    </template>
  </UDashboardPanel>
</template>
