<script setup lang="ts">
import AdminNavbar from '../../../../../components/admin/AdminNavbar.vue'
import AdminDocsProductShell from '../../../../../components/admin/products/AdminDocsProductShell.vue'
import { usePermission } from '../../../../../composables/usePermission'
import { computed } from 'vue'

const { hasPermission } = usePermission()
const canQuery = computed(() =>
  hasPermission('docs:product-settings:query') || hasPermission('docs:files:query'))
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-docs-files">
    <template #header>
      <AdminNavbar title="云文档 · 文件管理" />
    </template>

    <template #body>
      <div class="flex flex-col gap-6 p-6">
        <UAlert
          v-if="!canQuery"
          color="warning"
          variant="subtle"
          title="无权限"
          description="需要 docs:product-settings:query 或 docs:files:query。"
        />

        <AdminDocsProductShell
          v-else
          title="文件管理"
          description="全企业云文档检索、转移所有者、删除与恢复（A1 波次）。"
        >
          <UCard class="mx-auto w-full max-w-2xl">
            <UEmpty
              icon="i-lucide-files"
              title="文件管理即将开放"
              description="本页为壳层占位。列表、转移所有者、软删/恢复/销毁将在 A1 波次交付，不会改用基础设施「文件管理」页冒充。"
            />
          </UCard>
        </AdminDocsProductShell>
      </div>
    </template>
  </UDashboardPanel>
</template>
