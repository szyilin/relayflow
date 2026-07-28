<script setup lang="ts">
import AdminNavbar from '../../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../../components/admin/AdminPageHeader.vue'
import { getProductById } from '../../../../products/registry'

const product = getProductById('calendar')!
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-products-calendar">
    <template #header>
      <AdminNavbar title="日历" />
    </template>

    <template #body>
      <div class="flex flex-col gap-6 p-6">
        <AdminPageHeader
          :title="product.label"
          description="组织级日历产品设置"
        />

        <UCard class="mx-auto w-full max-w-2xl">
          <UEmpty
            :icon="product.icon"
            title="组织级设置将在后续版本开放"
            description="此处仅作产品设置目录占位。个人日历偏好（如周起始日）仍在工作台头像设置中配置，与本页无关。"
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
                  打开工作台日历
                </UButton>
              </div>
            </template>
          </UEmpty>
        </UCard>
      </div>
    </template>
  </UDashboardPanel>
</template>
