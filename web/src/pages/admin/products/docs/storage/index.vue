<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { TableColumn } from '@nuxt/ui'
import AdminNavbar from '../../../../../components/admin/AdminNavbar.vue'
import AdminDocsProductShell from '../../../../../components/admin/products/AdminDocsProductShell.vue'
import { usePermission } from '../../../../../composables/usePermission'
import type { DocsStorageUsageItem } from '../../../../../api/admin/docsStorage'
import { useDocsStorageStore } from '../../../../../stores/docsStorage'

const toast = useToast()
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
const quotaOpen = ref(false)

/** UI unit for editing: MB; empty string = unlimited */
const defaultQuotaMb = ref('')
const ruleDrafts = ref<Array<{ userId: string, displayName: string, quotaMb: string }>>([])

const columns: TableColumn<DocsStorageUsageItem>[] = [
  { accessorKey: 'displayName', header: '成员' },
  { accessorKey: 'usedBytes', header: '已用' },
  { accessorKey: 'quotaBytes', header: '上限' },
  { accessorKey: 'usageRatio', header: '占比' },
  { id: 'actions', header: '操作' }
]

function formatBytes(bytes: number | null | undefined) {
  if (bytes == null) {
    return '不限制'
  }
  if (bytes < 1024) {
    return `${bytes} B`
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`
  }
  if (bytes < 1024 * 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  }
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`
}

function formatRatio(ratio: number | null) {
  if (ratio == null) {
    return '—'
  }
  return `${Math.min(999, Math.round(ratio * 1000) / 10)}%`
}

function bytesToMbInput(bytes: number | null): string {
  if (bytes == null) {
    return ''
  }
  return String(Math.round(bytes / (1024 * 1024) * 100) / 100)
}

function mbInputToBytes(mb: string): number | null {
  const trimmed = mb.trim()
  if (!trimmed) {
    return null
  }
  const n = Number(trimmed)
  if (!Number.isFinite(n) || n < 0) {
    throw new Error('配额须为非负数字（留空表示不限制）')
  }
  return Math.round(n * 1024 * 1024)
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

async function openQuotaEditor() {
  if (!canUpdate.value) {
    return
  }
  try {
    await store.loadQuota()
    defaultQuotaMb.value = bytesToMbInput(store.quota.defaultQuotaBytes)
    ruleDrafts.value = store.quota.userRules.map(r => ({
      userId: String(r.userId),
      displayName: r.displayName || String(r.userId),
      quotaMb: bytesToMbInput(r.quotaBytes)
    }))
    quotaOpen.value = true
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '加载配额失败', color: 'error' })
  }
}

function addRuleRow() {
  ruleDrafts.value = [
    ...ruleDrafts.value,
    { userId: '', displayName: '', quotaMb: '' }
  ]
}

function removeRuleRow(index: number) {
  ruleDrafts.value = ruleDrafts.value.filter((_, i) => i !== index)
}

async function confirmSaveQuota() {
  try {
    const payload = {
      defaultQuotaBytes: mbInputToBytes(defaultQuotaMb.value),
      userRules: ruleDrafts.value
        .filter(r => r.userId.trim())
        .map(r => ({
          userId: r.userId.trim(),
          quotaBytes: mbInputToBytes(r.quotaMb)
        }))
    }
    await store.saveQuota(payload)
    quotaOpen.value = false
    toast.add({ title: '配额已保存', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '保存失败', color: 'error' })
  }
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
      <AdminNavbar title="云文档 · 存储空间" />
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
          title="存储空间"
          description="按成员查看云文档 FILE 用量与上限。仅统计 ACTIVE 云盘文件；与 /admin/infra/storage 无关。"
        >
          <template #actions>
            <UButton
              v-if="canUpdate"
              color="primary"
              variant="soft"
              icon="i-lucide-sliders-horizontal"
              :loading="store.quotaLoading"
              @click="openQuotaEditor"
            >
              配额设置
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
                class="w-56"
              >
                <UInput
                  v-model="keywordInput"
                  placeholder="姓名 / 用户名 / 手机"
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
                <div class="min-w-0">
                  <p class="truncate font-medium">
                    {{ row.original.displayName }}
                  </p>
                  <p class="truncate text-xs text-muted">
                    {{ row.original.userId }}
                  </p>
                </div>
              </template>
              <template #usedBytes-cell="{ row }">
                {{ formatBytes(row.original.usedBytes) }}
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
                  color="neutral"
                  variant="ghost"
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

        <UModal v-model:open="quotaOpen">
          <template #content>
            <div class="max-h-[80vh] space-y-4 overflow-y-auto p-6">
              <h3 class="text-lg font-semibold">
                存储配额
              </h3>
              <p class="text-sm text-muted">
                单位：MB。留空表示不限制。用户规则优先于全员默认。
              </p>
              <UFormField label="全员默认上限（MB）">
                <UInput
                  v-model="defaultQuotaMb"
                  placeholder="留空 = 不限制"
                />
              </UFormField>

              <div class="space-y-2">
                <div class="flex items-center justify-between">
                  <p class="text-sm font-medium">
                    用户覆盖
                  </p>
                  <UButton
                    size="xs"
                    variant="soft"
                    @click="addRuleRow"
                  >
                    添加
                  </UButton>
                </div>
                <div
                  v-for="(rule, index) in ruleDrafts"
                  :key="index"
                  class="flex flex-wrap items-end gap-2 rounded-md border border-default p-3"
                >
                  <UFormField
                    label="用户 ID"
                    class="w-40"
                  >
                    <UInput v-model="rule.userId" />
                  </UFormField>
                  <UFormField
                    label="上限（MB）"
                    class="w-32"
                  >
                    <UInput
                      v-model="rule.quotaMb"
                      placeholder="不限制"
                    />
                  </UFormField>
                  <UButton
                    size="xs"
                    color="error"
                    variant="ghost"
                    @click="removeRuleRow(index)"
                  >
                    删除
                  </UButton>
                </div>
              </div>

              <div class="flex justify-end gap-2">
                <UButton
                  color="neutral"
                  variant="ghost"
                  @click="quotaOpen = false"
                >
                  取消
                </UButton>
                <UButton
                  color="primary"
                  :loading="store.saving"
                  @click="confirmSaveQuota"
                >
                  保存
                </UButton>
              </div>
            </div>
          </template>
        </UModal>
      </div>
    </template>
  </UDashboardPanel>
</template>
