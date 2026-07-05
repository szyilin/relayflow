<script setup lang="ts">
import { onMounted, watch } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import AdminNavbar from '../../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../../components/admin/AdminPageHeader.vue'
import { useUserStore, type UserListRecord } from '../../../../stores/user'

const toast = useToast()
const userStore = useUserStore()

const columns: TableColumn<UserListRecord>[] = [{
  accessorKey: 'username',
  header: '用户名'
}, {
  accessorKey: 'nickname',
  header: '昵称'
}, {
  accessorKey: 'dept',
  header: '部门'
}, {
  accessorKey: 'status',
  header: '状态'
}, {
  accessorKey: 'createTime',
  header: '创建时间'
}]

async function loadPage(options?: { page?: number, keyword?: string }) {
  try {
    await userStore.fetchPage(options)
  } catch {
    toast.add({
      title: '加载失败',
      description: userStore.lastError ?? '无法获取用户列表',
      color: 'error'
    })
  }
}

function onSearch() {
  loadPage({ page: 1, keyword: userStore.keyword })
}

function showPrototypeToast(action: string) {
  toast.add({
    title: '原型未实现',
    description: `${action} 将在后续版本启用`,
    color: 'neutral'
  })
}

onMounted(() => {
  loadPage()
})

watch(() => userStore.page, (page) => {
  loadPage({ page })
})
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-system-user">
    <template #header>
      <AdminNavbar title="用户管理" />
    </template>

    <template #body>
      <div class="space-y-4 p-4 sm:p-6">
        <AdminPageHeader
          title="用户列表"
          description="管理系统用户账号与基本信息"
        >
          <template #actions>
            <UButton to="/admin/system/user/create" icon="i-lucide-plus">
              新建用户
            </UButton>
          </template>
        </AdminPageHeader>

        <UCard>
          <div class="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
            <UInput
              v-model="userStore.keyword"
              placeholder="搜索用户名、昵称或部门"
              icon="i-lucide-search"
              class="sm:max-w-xs"
              @keyup.enter="onSearch"
            />
            <UButton color="neutral" variant="soft" :loading="userStore.loading" @click="onSearch">
              搜索
            </UButton>
          </div>

          <UTable :data="userStore.list" :columns="columns" :loading="userStore.loading" />

          <div class="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <p class="text-sm text-muted">
              共 {{ userStore.total }} 条
            </p>
            <UPagination
              v-model:page="userStore.page"
              :total="userStore.total"
              :items-per-page="userStore.pageSize"
            />
          </div>
        </UCard>

        <div class="flex gap-2">
          <UButton color="neutral" variant="soft" @click="showPrototypeToast('编辑')">
            编辑（演示）
          </UButton>
          <UButton color="error" variant="soft" @click="showPrototypeToast('禁用')">
            禁用（演示）
          </UButton>
        </div>
      </div>
    </template>
  </UDashboardPanel>
</template>
