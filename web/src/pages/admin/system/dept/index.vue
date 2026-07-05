<script setup lang="ts">
import AdminNavbar from '../../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../../components/admin/AdminPageHeader.vue'
import { mockDeptTree } from '../../../../mocks/system/users'
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
          description="组织架构与部门层级（原型占位）"
        />

        <UCard>
          <UTree :items="mockDeptTree" />
        </UCard>
      </div>
    </template>
  </UDashboardPanel>
</template>
