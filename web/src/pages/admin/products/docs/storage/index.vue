<script setup lang="ts">
import { computed } from 'vue'
import AdminNavbar from '../../../../../components/admin/AdminNavbar.vue'
import AdminDocsProductShell from '../../../../../components/admin/products/AdminDocsProductShell.vue'
import { usePermission } from '../../../../../composables/usePermission'

const { hasPermission } = usePermission()
const canQuery = computed(() =>
  hasPermission('docs:product-settings:query') || hasPermission('docs:storage-quota:query'))
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-docs-storage">
    <template #header>
      <AdminNavbar title="云文档 · 存储空间" />
    </template>

    <template #body>
      <div class="flex flex-col gap-6 p-6">
        <UAlert
          v-if="!canQuery"
          color="warning"
          variant="subtle"
          title="无权限"
          description="需要 docs:product-settings:query 或 docs:storage-quota:query。"
        />

        <AdminDocsProductShell
          v-else
          title="存储空间"
          description="按成员查看云文档用量与上限（A2 波次）。与基础设施对象存储配置页相互独立。"
        >
          <UCard class="mx-auto w-full max-w-2xl">
            <UEmpty
              icon="i-lucide-hard-drive"
              title="存储空间管理即将开放"
              description="本页为壳层占位。成员用量列表与配额将在 A2 波次交付，不会使用 /admin/infra/storage 冒充。"
            />
          </UCard>
        </AdminDocsProductShell>
      </div>
    </template>
  </UDashboardPanel>
</template>
