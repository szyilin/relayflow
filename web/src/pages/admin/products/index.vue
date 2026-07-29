<script setup lang="ts">
import { computed } from 'vue'
import AdminNavbar from '../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../components/admin/AdminPageHeader.vue'
import { usePermission } from '../../../composables/usePermission'
import { ADMIN_PRODUCT_CATALOG } from '../../../products/registry'

const { hasPermission } = usePermission()
/** OR：具备任一产品设置读权限即可进入目录（与 useAdminNav 侧栏一致）。 */
const canQuery = computed(() =>
  hasPermission('im:product-settings:query')
  || hasPermission('calendar:product-settings:query')
)
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-products">
    <template #header>
      <AdminNavbar title="产品设置" />
    </template>

    <template #body>
      <div class="flex flex-col gap-6 p-6">
        <AdminPageHeader
          title="产品设置"
          description="配置当前企业对各基础产品的组织策略与管理能力。个人使用习惯仍在工作台头像菜单中设置。"
        />

        <UAlert
          v-if="!canQuery"
          color="warning"
          variant="subtle"
          title="无权限"
          description="需要 im:product-settings:query 或 calendar:product-settings:query 才能查看产品设置。"
        />

        <div
          v-else
          class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3"
        >
          <UCard
            v-for="product in ADMIN_PRODUCT_CATALOG"
            :key="product.id"
            class="transition-colors hover:bg-elevated/50"
          >
            <div class="flex items-start gap-3">
              <div class="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/15 text-primary">
                <UIcon
                  :name="product.icon"
                  class="size-5"
                />
              </div>
              <div class="min-w-0 flex-1 space-y-2">
                <div class="flex items-center gap-2">
                  <h3 class="font-semibold text-highlighted">
                    {{ product.label }}
                  </h3>
                  <UBadge
                    v-if="product.adminStatus === 'placeholder'"
                    color="neutral"
                    variant="subtle"
                    size="sm"
                  >
                    即将开放
                  </UBadge>
                </div>
                <p class="text-sm text-muted">
                  {{ product.description }}
                </p>
                <UButton
                  :to="product.adminSettingsPath"
                  color="primary"
                  variant="soft"
                  size="sm"
                  trailing-icon="i-lucide-chevron-right"
                >
                  {{ product.adminStatus === 'enabled' ? '进入设置' : '查看占位' }}
                </UButton>
              </div>
            </div>
          </UCard>
        </div>
      </div>
    </template>
  </UDashboardPanel>
</template>
