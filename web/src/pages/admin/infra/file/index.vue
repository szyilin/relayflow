<script setup lang="ts">
import type { TableColumn } from '@nuxt/ui'
import AdminNavbar from '../../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../../components/admin/AdminPageHeader.vue'
import { mockFiles } from '../../../../mocks/system/users'

type MockFile = typeof mockFiles[number]

const columns: TableColumn<MockFile>[] = [{
  accessorKey: 'name',
  header: '文件名'
}, {
  accessorKey: 'size',
  header: '大小'
}, {
  accessorKey: 'type',
  header: '类型'
}, {
  accessorKey: 'createTime',
  header: '上传时间'
}]
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-infra-file">
    <template #header>
      <AdminNavbar title="文件管理" />
    </template>

    <template #body>
      <div class="space-y-4 p-4 sm:p-6">
        <AdminPageHeader
          title="文件列表"
          description="浏览与管理已上传文件（原型占位）"
        >
          <template #actions>
            <UTooltip text="接 API 后启用">
              <UButton icon="i-lucide-upload" disabled>
                上传文件
              </UButton>
            </UTooltip>
          </template>
        </AdminPageHeader>

        <UCard>
          <UTable :data="mockFiles" :columns="columns" />
        </UCard>
      </div>
    </template>
  </UDashboardPanel>
</template>
