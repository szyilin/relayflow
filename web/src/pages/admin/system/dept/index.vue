<script setup lang="ts">
import AdminNavbar from '../../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../../components/admin/AdminPageHeader.vue'
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-system-dept">
    <template #header>
      <AdminNavbar title="部门管理" />
    </template>

    <template #body>
      <div class="space-y-4 p-4 sm:p-6">
        <AdminPageHeader
          title="部门架构"
          description="组织架构与部门层级"
        />

        <UCard>
          <UEmpty
            icon="i-lucide-building-2"
            title="暂无部门数据"
            description="部门列表将在接入 API 后展示"
          />
        </UCard>
      </div>
    </template>
  </UDashboardPanel>
</template>
