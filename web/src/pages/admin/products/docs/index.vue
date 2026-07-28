<script setup lang="ts">
import AdminNavbar from '../../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../../components/admin/AdminPageHeader.vue'
import { getProductById } from '../../../../products/registry'

const product = getProductById('docs')!
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-products-docs">
    <template #header>
      <AdminNavbar title="云文档" />
    </template>

    <template #body>
      <div class="flex flex-col gap-6 p-6">
        <AdminPageHeader
          :title="product.label"
          description="组织级云文档产品设置"
        />

        <UCard class="mx-auto w-full max-w-2xl">
          <UEmpty
            :icon="product.icon"
            title="组织级设置将在后续版本开放"
            description="此处仅作产品设置目录占位。个人云文档使用习惯仍在工作台侧配置；本页不调用云文档策略 API。"
          >
            <template #actions>
              <div class="flex flex-wrap justify-center gap-2">
                <UButton
                  to="/admin/products"
                  color="neutral"
                  variant="soft"
                >
                  返回产品设置
                </UButton>
                <UButton
                  :to="product.workspacePath"
                  color="primary"
                  variant="soft"
                >
                  打开工作台云文档
                </UButton>
              </div>
            </template>
          </UEmpty>
        </UCard>
      </div>
    </template>
  </UDashboardPanel>
</template>
