<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { TableColumn } from '@nuxt/ui'
import AdminNavbar from '../../../../../components/admin/AdminNavbar.vue'
import AdminDocsProductShell from '../../../../../components/admin/products/AdminDocsProductShell.vue'
import WorkspaceOwnerChip from '../../../../../components/workspace/WorkspaceOwnerChip.vue'
import { usePermission } from '../../../../../composables/usePermission'
import type { DocsStorageUsageItem } from '../../../../../api/admin/docsStorage'
import { useDocsStorageStore } from '../../../../../stores/docsStorage'

const router = useRouter()
const store = useDocsStorageStore()
const { hasPermission } = usePermission()

const canQuery = computed(() =>
  hasPermission('docs:storage-quota:query')
  || hasPermission('docs:product-settings:query')
  || hasPermission('im:product-settings:query'))
const canUpdate = computed(() =>
  hasPermission('docs:storage-quota:update')
  || hasPermission('docs:product-settings:update')
  || hasPermission('im:product-settings:update'))

const keywordInput = ref('')

const columns: TableColumn<DocsStorageUsageItem>[] = [
  { accessorKey: 'displayName', header: '姓名' },
  { accessorKey: 'deptName', header: '部门' },
  { accessorKey: 'usedBytes', header: '已用存储' },
  { accessorKey: 'quotaBytes', header: '存储上限' },
  { accessorKey: 'usageRatio', header: '已用占比' },
  { id: 'actions', header: '操作' }
]

function formatBytes(bytes: number | null | undefined, unlimitedLabel = '不限制') {
  if (bytes == null) {
    return unlimitedLabel
  }
  if (bytes < 1024) {
    return `${bytes} B`
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(2)} KB`
  }
  if (bytes < 1024 * 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
  }
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`
}

function formatRatio(ratio: number | null) {
  if (ratio == null) {
    return '—'
  }
  return `${Math.min(999, Math.round(ratio * 10000) / 100).toFixed(2)}%`
}

async function loadPage(page = 1) {
  if (!canQuery.value) {
    return
  }
  try {
    await store.fetchUsage({
      pageNo: page,
      pageSize: store.pageSize,
      keyword: keywordInput.value.trim()
    })
  } catch {
    // lastError on store
  }
}

function handleSearch() {
  void loadPage(1)
}

function openFiles(userId: string) {
  void router.push({
    path: '/admin/products/docs/files',
    query: { ownerUserId: userId }
  })
}

function openQuotaManagement() {
  void router.push('/admin/products/docs/storage/quota')
}

onMounted(() => {
  void loadPage(1)
})
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-docs-storage">
    <template #header>
      <AdminNavbar title="云文档 · 存储用量详情" />
    </template>

    <template #body>
      <div class="flex flex-col gap-6 p-6">
        <UAlert
          v-if="!canQuery"
          color="warning"
          variant="subtle"
          title="无权限"
          description="需要 docs:storage-quota:query 权限。"
        />

        <AdminDocsProductShell
          v-else
          title="存储用量详情"
          description="按成员查看云文档 FILE 用量与生效上限。仅统计 ACTIVE 云盘文件；与基础设施对象存储配置无关。"
        >
          <template #actions>
            <UButton
              v-if="canUpdate"
              color="primary"
              icon="i-lucide-sliders-horizontal"
              @click="openQuotaManagement"
            >
              存储上限管理
            </UButton>
          </template>

          <div class="space-y-4">
            <UAlert
              v-if="store.lastError"
              color="error"
              variant="subtle"
              title="加载失败"
              :description="store.lastError"
            />

            <div class="flex flex-wrap items-end gap-3">
              <UFormField
                label="成员"
                class="min-w-[20rem] flex-1"
              >
                <UInput
                  v-model="keywordInput"
                  icon="i-lucide-search"
                  placeholder="请输入姓名、邮箱或手机号"
                  @keyup.enter="handleSearch"
                />
              </UFormField>
              <UButton
                color="primary"
                :loading="store.loading"
                @click="handleSearch"
              >
                搜索
              </UButton>
            </div>

            <UTable
              :data="store.list"
              :columns="columns"
              :loading="store.loading"
              class="w-full"
            >
              <template #displayName-cell="{ row }">
                <WorkspaceOwnerChip
                  :user-id="String(row.original.userId)"
                  :display-name="row.original.displayName"
                  :avatar-url="row.original.avatarUrl"
                  size="sm"
                />
              </template>
              <template #deptName-cell="{ row }">
                <span class="text-sm text-muted">
                  {{ row.original.deptName || '—' }}
                </span>
              </template>
              <template #usedBytes-cell="{ row }">
                {{ formatBytes(row.original.usedBytes, '0 B') }}
              </template>
              <template #quotaBytes-cell="{ row }">
                {{ formatBytes(row.original.quotaBytes) }}
              </template>
              <template #usageRatio-cell="{ row }">
                <span
                  :class="row.original.usageRatio != null && row.original.usageRatio >= 1
                    ? 'text-error'
                    : ''"
                >
                  {{ formatRatio(row.original.usageRatio) }}
                </span>
              </template>
              <template #actions-cell="{ row }">
                <UButton
                  size="xs"
                  color="primary"
                  variant="link"
                  @click="openFiles(row.original.userId)"
                >
                  查看云文档
                </UButton>
              </template>
            </UTable>

            <div class="flex justify-end">
              <UPagination
                :page="store.pageNo"
                :items-per-page="store.pageSize"
                :total="store.total"
                @update:page="(p: number) => loadPage(p)"
              />
            </div>
          </div>
        </AdminDocsProductShell>
      </div>
    </template>
  </UDashboardPanel>
</template>
