<script setup lang="ts">
import AdminNavbar from '../../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../../components/admin/AdminPageHeader.vue'
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
          description="浏览与管理已上传文件"
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
          <UEmpty
            icon="i-lucide-folder-open"
            title="暂无文件"
            description="文件列表将在接入 API 后展示"
          />
        </UCard>
      </div>
    </template>
  </UDashboardPanel>
</template>
