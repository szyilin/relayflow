<script setup lang="ts">
import AdminNavbar from '../../components/admin/AdminNavbar.vue'

const quickLinks = [{
  label: '用户管理',
  description: '查看与管理系统用户',
  icon: 'i-lucide-users',
  to: '/admin/system/user'
}, {
  label: '文件管理',
  description: '浏览已上传的文件',
  icon: 'i-lucide-folder-open',
  to: '/admin/infra/file'
}, {
  label: '部门管理',
  description: '组织架构与部门',
  icon: 'i-lucide-building-2',
  to: '/admin/system/dept'
}]
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-home">
    <template #header>
      <AdminNavbar title="概览" />
    </template>

    <template #body>
      <div class="space-y-6">
        <UCard>
          <UEmpty
            icon="i-lucide-layout-dashboard"
            title="概览统计"
            description="统计数据将在接入 API 后展示"
          />
        </UCard>

        <UCard>
          <template #header>
            <h3 class="font-semibold">
              快捷入口
            </h3>
          </template>

          <div class="grid gap-3 sm:grid-cols-3">
            <UButton
              v-for="link in quickLinks"
              :key="link.to"
              :to="link.to"
              color="neutral"
              variant="soft"
              class="h-auto flex-col items-start gap-2 p-4"
            >
              <div class="flex items-center gap-2 font-medium">
                <UIcon :name="link.icon" class="size-4 text-primary" />
                {{ link.label }}
              </div>
              <span class="text-xs font-normal text-muted">
                {{ link.description }}
              </span>
            </UButton>
          </div>
        </UCard>
      </div>
    </template>
  </UDashboardPanel>
</template>
