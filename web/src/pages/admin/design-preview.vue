<script setup lang="ts">
import AdminNavbar from '../../components/admin/AdminNavbar.vue'
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-design-preview">
    <template #header>
      <AdminNavbar title="设计预览" />
    </template>

    <template #body>
      <div class="space-y-8">
        <section class="space-y-4">
          <h2 class="text-lg font-semibold">
            色板
          </h2>
          <div class="grid grid-cols-2 gap-3 sm:grid-cols-5">
            <div class="space-y-1">
              <div class="h-12 rounded-lg bg-primary" />
              <p class="text-xs text-muted">Primary</p>
            </div>
            <div class="space-y-1">
              <div class="h-12 rounded-lg bg-success" />
              <p class="text-xs text-muted">Success</p>
            </div>
            <div class="space-y-1">
              <div class="h-12 rounded-lg bg-warning" />
              <p class="text-xs text-muted">Warning</p>
            </div>
            <div class="space-y-1">
              <div class="h-12 rounded-lg bg-error" />
              <p class="text-xs text-muted">Error</p>
            </div>
            <div class="space-y-1">
              <div class="h-12 rounded-lg bg-muted" />
              <p class="text-xs text-muted">Muted</p>
            </div>
          </div>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold">
            按钮
          </h2>
          <div class="flex flex-wrap gap-2">
            <UButton>Primary</UButton>
            <UButton color="neutral" variant="soft">
              Soft
            </UButton>
            <UButton color="neutral" variant="outline">
              Outline
            </UButton>
            <UButton color="neutral" variant="ghost">
              Ghost
            </UButton>
            <UButton color="error" variant="soft">
              Danger
            </UButton>
          </div>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold">
            提示
          </h2>
          <div class="space-y-2">
            <UAlert title="信息提示" description="这是一条普通信息。" icon="i-lucide-info" />
            <UAlert title="成功" description="操作已完成。" color="success" icon="i-lucide-check-circle" />
            <UAlert title="警告" description="请注意检查配置。" color="warning" icon="i-lucide-alert-triangle" />
          </div>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold">
            表单
          </h2>
          <UCard class="max-w-md">
            <div class="space-y-4">
              <UFormField label="用户名">
                <UInput placeholder="请输入用户名" icon="i-lucide-user" />
              </UFormField>
              <UFormField label="状态">
                <USelectMenu
                  :items="['启用', '禁用']"
                  placeholder="选择状态"
                />
              </UFormField>
            </div>
          </UCard>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold">
            表格
          </h2>
          <UCard>
            <UTable
              :data="[
                { name: '管理员', dept: '总部', status: '启用' },
                { name: '张三', dept: '研发部', status: '启用' }
              ]"
              :columns="[
                { accessorKey: 'name', header: '姓名' },
                { accessorKey: 'dept', header: '部门' },
                { accessorKey: 'status', header: '状态' }
              ]"
            />
          </UCard>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold">
            空状态与骨架
          </h2>
          <div class="grid gap-4 lg:grid-cols-2">
            <UCard>
              <UEmpty
                icon="i-lucide-inbox"
                title="暂无数据"
                description="当前列表为空，可创建第一条记录。"
              />
            </UCard>
            <UCard>
              <div class="space-y-3">
                <USkeleton class="h-4 w-3/4" />
                <USkeleton class="h-4 w-1/2" />
                <USkeleton class="h-24 w-full" />
              </div>
            </UCard>
          </div>
        </section>
      </div>
    </template>
  </UDashboardPanel>
</template>
