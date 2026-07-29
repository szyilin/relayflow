<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import type { TableColumn } from '@nuxt/ui'
import AdminNavbar from '../../../../../components/admin/AdminNavbar.vue'
import AdminDocsProductShell from '../../../../../components/admin/products/AdminDocsProductShell.vue'
import AdminMemberSearchField, {
  type AdminMemberSelection
} from '../../../../../components/admin/AdminMemberSearchField.vue'
import WorkspaceOwnerChip from '../../../../../components/workspace/WorkspaceOwnerChip.vue'
import { usePermission } from '../../../../../composables/usePermission'
import {
  docsWorkspaceLink,
  type DocsAdminFileItem,
  type DocsFileLifecycle,
  type DocsFileType
} from '../../../../../api/admin/docsFiles'
import { useDocsFilesStore } from '../../../../../stores/docsFiles'

const toast = useToast()
const route = useRoute()
const store = useDocsFilesStore()
const { hasPermission } = usePermission()

const canQuery = computed(() =>
  hasPermission('docs:files:query')
  || hasPermission('docs:product-settings:query')
  || hasPermission('im:product-settings:query'))
const canTransfer = computed(() =>
  hasPermission('docs:files:transfer') || hasPermission('docs:product-settings:update') || hasPermission('im:product-settings:update'))
const canDelete = computed(() =>
  hasPermission('docs:files:delete') || hasPermission('docs:product-settings:update') || hasPermission('im:product-settings:update'))
const canRestore = computed(() =>
  hasPermission('docs:files:restore') || hasPermission('docs:product-settings:update') || hasPermission('im:product-settings:update'))
const canPurge = computed(() =>
  hasPermission('docs:files:purge') || hasPermission('docs:product-settings:update') || hasPermission('im:product-settings:update'))

const keywordInput = ref('')
const ownerFilter = ref<AdminMemberSelection | null>(null)

const typeOptions: Array<{ label: string, value: string }> = [
  { label: '全部类型', value: '' },
  { label: '文档', value: 'RICH_DOC' },
  { label: '文件', value: 'FILE' }
]

const lifecycleOptions: Array<{ label: string, value: string }> = [
  { label: '全部状态', value: '' },
  { label: '正常', value: 'ACTIVE' },
  { label: '已删除', value: 'DELETED' }
]

const typeFilter = ref(typeOptions[0]!)
const lifecycleFilter = ref(lifecycleOptions[0]!)

const transferOpen = ref(false)
const restoreOpen = ref(false)
const deleteOpen = ref(false)
const purgeOpen = ref(false)
const actionIds = ref<string[]>([])

const transferOwner = ref<AdminMemberSelection | null>(null)
const restoreForm = reactive({
  useNewOwner: false
})
const restoreOwner = ref<AdminMemberSelection | null>(null)

const allSelected = computed({
  get: () => store.list.length > 0 && store.selectedIds.length === store.list.length,
  set: (v: boolean) => store.toggleSelectAll(v)
})

const columns: TableColumn<DocsAdminFileItem>[] = [
  { id: 'select', header: '' },
  { accessorKey: 'title', header: '标题' },
  { accessorKey: 'type', header: '类型' },
  { accessorKey: 'ownerDisplayName', header: '所有者' },
  { accessorKey: 'lifecycle', header: '状态' },
  { accessorKey: 'updatedAt', header: '更新时间' },
  { accessorKey: 'sizeBytes', header: '大小' },
  { id: 'actions', header: '操作' }
]

function formatTime(value?: string | null) {
  if (!value) {
    return '—'
  }
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) {
    return '—'
  }
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function formatSize(bytes: number | null) {
  if (bytes == null) {
    return '—'
  }
  if (bytes < 1024) {
    return `${bytes} B`
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`
  }
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}

function typeLabel(type: DocsFileType) {
  return type === 'FILE' ? '文件' : '文档'
}

function lifecycleLabel(lifecycle: DocsFileLifecycle) {
  if (lifecycle === 'DELETED') {
    return '已删除'
  }
  if (lifecycle === 'PURGED') {
    return '已销毁'
  }
  return '正常'
}

function lifecycleColor(lifecycle: DocsFileLifecycle): 'success' | 'warning' | 'neutral' {
  if (lifecycle === 'DELETED') {
    return 'warning'
  }
  if (lifecycle === 'PURGED') {
    return 'neutral'
  }
  return 'success'
}

async function loadPage(page = 1) {
  if (!canQuery.value) {
    return
  }
  try {
    await store.fetchPage({
      pageNo: page,
      pageSize: store.pageSize,
      keyword: keywordInput.value.trim(),
      type: (typeFilter.value.value || '') as DocsFileType | '',
      lifecycle: (lifecycleFilter.value.value || '') as DocsFileLifecycle | '',
      ownerUserId: ownerFilter.value?.userId?.trim() || ''
    })
  } catch {
    // lastError already set on store
  }
}

function handleSearch() {
  void loadPage(1)
}

async function copyLink(row: DocsAdminFileItem) {
  try {
    await navigator.clipboard.writeText(docsWorkspaceLink(row.objectId))
    toast.add({ title: '已复制工作台链接', color: 'success' })
  } catch {
    toast.add({ title: '复制失败', color: 'error' })
  }
}

function openTransfer(ids?: string[]) {
  actionIds.value = ids?.length ? ids : [...store.selectedIds]
  if (!actionIds.value.length) {
    toast.add({ title: '请先选择文件', color: 'warning' })
    return
  }
  const invalid = store.list.filter(i => actionIds.value.includes(i.objectId) && i.lifecycle !== 'ACTIVE')
  if (invalid.length) {
    toast.add({ title: '仅可转移状态为「正常」的文件', color: 'warning' })
    return
  }
  transferOwner.value = null
  transferOpen.value = true
}

function openDelete(ids?: string[]) {
  actionIds.value = ids?.length ? ids : [...store.selectedIds]
  if (!actionIds.value.length) {
    toast.add({ title: '请先选择文件', color: 'warning' })
    return
  }
  deleteOpen.value = true
}

function openRestore(ids?: string[]) {
  actionIds.value = ids?.length ? ids : [...store.selectedIds]
  if (!actionIds.value.length) {
    toast.add({ title: '请先选择文件', color: 'warning' })
    return
  }
  const invalid = store.list.filter(i =>
    actionIds.value.includes(i.objectId)
    && (i.lifecycle !== 'DELETED' || (i.daysUntilPurge != null && i.daysUntilPurge <= 0)))
  if (invalid.length) {
    toast.add({ title: '仅可恢复 90 天内的已删除文件', color: 'warning' })
    return
  }
  restoreForm.useNewOwner = false
  restoreOwner.value = null
  restoreOpen.value = true
}

function openPurge(ids?: string[]) {
  actionIds.value = ids?.length ? ids : [...store.selectedIds]
  if (!actionIds.value.length) {
    toast.add({ title: '请先选择文件', color: 'warning' })
    return
  }
  purgeOpen.value = true
}

async function confirmTransfer() {
  if (!transferOwner.value?.userId) {
    toast.add({ title: '请选择新所有者', color: 'warning' })
    return
  }
  try {
    await store.transferOwner(
      actionIds.value,
      transferOwner.value.userId
    )
    transferOpen.value = false
    toast.add({ title: '已转移所有者', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '转移失败', color: 'error' })
  }
}

async function confirmDelete() {
  try {
    await store.softDelete(actionIds.value)
    deleteOpen.value = false
    toast.add({ title: '已删除', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '删除失败', color: 'error' })
  }
}

async function confirmRestore() {
  try {
    const newId = restoreForm.useNewOwner ? restoreOwner.value?.userId ?? null : null
    if (restoreForm.useNewOwner && !newId) {
      toast.add({ title: '请选择新所有者', color: 'warning' })
      return
    }
    await store.restore(actionIds.value, newId)
    restoreOpen.value = false
    toast.add({ title: '已恢复', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '恢复失败', color: 'error' })
  }
}

async function confirmPurge() {
  try {
    await store.purge(actionIds.value)
    purgeOpen.value = false
    toast.add({ title: '已永久销毁', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '销毁失败', color: 'error' })
  }
}

watch(typeFilter, () => {
  store.setTypeFilter((typeFilter.value.value || '') as DocsFileType | '')
})
watch(lifecycleFilter, () => {
  store.setLifecycleFilter((lifecycleFilter.value.value || '') as DocsFileLifecycle | '')
})

function applyOwnerQuery(raw: unknown) {
  const next = typeof raw === 'string' ? raw.trim() : ''
  if (!next) {
    if (ownerFilter.value) {
      ownerFilter.value = null
    }
    return
  }
  if (ownerFilter.value?.userId === next) {
    return
  }
  ownerFilter.value = {
    userId: next,
    displayName: ownerFilter.value?.userId === next
      ? ownerFilter.value.displayName
      : next
  }
}

onMounted(() => {
  applyOwnerQuery(route.query.ownerUserId)
  void loadPage(1)
})

watch(() => route.query.ownerUserId, (value) => {
  applyOwnerQuery(value)
  void loadPage(1)
})
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-docs-files">
    <template #header>
      <AdminNavbar title="云文档 · 文件管理" />
    </template>

    <template #body>
      <div class="flex flex-col gap-6 p-6">
        <UAlert
          v-if="!canQuery"
          color="warning"
          variant="subtle"
          title="无权限"
          description="需要 docs:files:query 权限。"
        />

        <AdminDocsProductShell
          v-else
          title="文件管理"
          description="检索企业云文档元数据，转移所有者、删除、恢复或销毁。管理员无法凭身份直接阅读正文。"
        >
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
                label="标题"
                class="w-48"
              >
                <UInput
                  v-model="keywordInput"
                  placeholder="标题关键词"
                  @keyup.enter="handleSearch"
                />
              </UFormField>
              <UFormField
                label="类型"
                class="w-36"
              >
                <USelectMenu
                  v-model="typeFilter"
                  :items="typeOptions"
                />
              </UFormField>
              <UFormField
                label="状态"
                class="w-36"
              >
                <USelectMenu
                  v-model="lifecycleFilter"
                  :items="lifecycleOptions"
                />
              </UFormField>
              <AdminMemberSearchField
                v-model="ownerFilter"
                label="所有者"
                class="w-56"
              />
              <UButton
                color="primary"
                :loading="store.loading"
                @click="handleSearch"
              >
                查询
              </UButton>
            </div>

            <div class="flex flex-wrap gap-2">
              <UButton
                color="neutral"
                variant="soft"
                size="sm"
                :disabled="!canTransfer || !store.selectedIds.length || store.acting"
                @click="openTransfer()"
              >
                转移所有者
              </UButton>
              <UButton
                color="neutral"
                variant="soft"
                size="sm"
                :disabled="!canDelete || !store.selectedIds.length || store.acting"
                @click="openDelete()"
              >
                删除
              </UButton>
              <UButton
                color="neutral"
                variant="soft"
                size="sm"
                :disabled="!canRestore || !store.selectedIds.length || store.acting"
                @click="openRestore()"
              >
                恢复
              </UButton>
              <UButton
                color="error"
                variant="soft"
                size="sm"
                :disabled="!canPurge || !store.selectedIds.length || store.acting"
                @click="openPurge()"
              >
                销毁
              </UButton>
            </div>

            <UTable
              :data="store.list"
              :columns="columns"
              :loading="store.loading"
              class="w-full"
            >
              <template #select-header>
                <UCheckbox v-model="allSelected" />
              </template>
              <template #select-cell="{ row }">
                <UCheckbox
                  :model-value="store.selectedIds.includes(row.original.objectId)"
                  @update:model-value="(v: boolean | 'indeterminate') => store.toggleSelect(row.original.objectId, v === true)"
                />
              </template>
              <template #title-cell="{ row }">
                <p class="truncate font-medium">
                  {{ row.original.title }}
                </p>
              </template>
              <template #type-cell="{ row }">
                {{ typeLabel(row.original.type) }}
              </template>
              <template #ownerDisplayName-cell="{ row }">
                <WorkspaceOwnerChip
                  :user-id="String(row.original.ownerUserId)"
                  :display-name="row.original.ownerDisplayName"
                />
              </template>
              <template #lifecycle-cell="{ row }">
                <div class="flex flex-col gap-0.5">
                  <UBadge
                    :color="lifecycleColor(row.original.lifecycle)"
                    variant="subtle"
                    size="sm"
                    class="w-fit"
                  >
                    {{ lifecycleLabel(row.original.lifecycle) }}
                  </UBadge>
                  <span
                    v-if="row.original.lifecycle === 'DELETED' && row.original.daysUntilPurge != null"
                    class="text-xs text-muted"
                  >
                    {{ row.original.daysUntilPurge }} 天后可彻底过期
                  </span>
                </div>
              </template>
              <template #updatedAt-cell="{ row }">
                {{ formatTime(row.original.updatedAt) }}
              </template>
              <template #sizeBytes-cell="{ row }">
                {{ formatSize(row.original.sizeBytes) }}
              </template>
              <template #actions-cell="{ row }">
                <div class="flex flex-wrap gap-1">
                  <UButton
                    size="xs"
                    color="neutral"
                    variant="ghost"
                    @click="copyLink(row.original)"
                  >
                    复制链接
                  </UButton>
                  <UButton
                    v-if="row.original.lifecycle === 'ACTIVE' && canTransfer"
                    size="xs"
                    color="neutral"
                    variant="ghost"
                    @click="openTransfer([row.original.objectId])"
                  >
                    转移
                  </UButton>
                  <UButton
                    v-if="row.original.lifecycle === 'ACTIVE' && canDelete"
                    size="xs"
                    color="neutral"
                    variant="ghost"
                    @click="openDelete([row.original.objectId])"
                  >
                    删除
                  </UButton>
                  <UButton
                    v-if="row.original.lifecycle === 'DELETED' && canRestore"
                    size="xs"
                    color="neutral"
                    variant="ghost"
                    @click="openRestore([row.original.objectId])"
                  >
                    恢复
                  </UButton>
                  <UButton
                    v-if="row.original.lifecycle === 'DELETED' && canPurge"
                    size="xs"
                    color="error"
                    variant="ghost"
                    @click="openPurge([row.original.objectId])"
                  >
                    销毁
                  </UButton>
                </div>
              </template>
            </UTable>

            <div class="flex items-center justify-between gap-3">
              <p class="text-sm text-muted">
                共 {{ store.total }} 条
              </p>
              <UPagination
                :page="store.pageNo"
                :items-per-page="store.pageSize"
                :total="store.total"
                @update:page="(p: number) => loadPage(p)"
              />
            </div>
          </div>
        </AdminDocsProductShell>

        <UModal v-model:open="transferOpen">
          <template #content>
            <div class="space-y-4 p-6">
              <h3 class="text-lg font-semibold">
                转移所有者
              </h3>
              <p class="text-sm text-muted">
                将 {{ actionIds.length }} 个对象转移给新所有者（仅正常状态）。
              </p>
              <AdminMemberSearchField
                v-model="transferOwner"
                label="新所有者"
                placeholder="姓名、邮箱或手机号"
              />
              <div class="flex justify-end gap-2">
                <UButton
                  color="neutral"
                  variant="ghost"
                  @click="transferOpen = false"
                >
                  取消
                </UButton>
                <UButton
                  color="primary"
                  :loading="store.acting"
                  @click="confirmTransfer"
                >
                  确认转移
                </UButton>
              </div>
            </div>
          </template>
        </UModal>

        <UModal v-model:open="deleteOpen">
          <template #content>
            <div class="space-y-4 p-6">
              <h3 class="text-lg font-semibold">
                删除文件
              </h3>
              <p class="text-sm text-muted">
                将 {{ actionIds.length }} 个对象标记为已删除。90 天内可在本页恢复。
              </p>
              <div class="flex justify-end gap-2">
                <UButton
                  color="neutral"
                  variant="ghost"
                  @click="deleteOpen = false"
                >
                  取消
                </UButton>
                <UButton
                  color="primary"
                  :loading="store.acting"
                  @click="confirmDelete"
                >
                  确认删除
                </UButton>
              </div>
            </div>
          </template>
        </UModal>

        <UModal v-model:open="restoreOpen">
          <template #content>
            <div class="space-y-4 p-6">
              <h3 class="text-lg font-semibold">
                恢复文件
              </h3>
              <label class="flex items-center gap-2 text-sm">
                <UCheckbox v-model="restoreForm.useNewOwner" />
                恢复到新所有者
              </label>
              <template v-if="restoreForm.useNewOwner">
                <AdminMemberSearchField
                  v-model="restoreOwner"
                  label="新所有者"
                  placeholder="姓名、邮箱或手机号"
                />
              </template>
              <div class="flex justify-end gap-2">
                <UButton
                  color="neutral"
                  variant="ghost"
                  @click="restoreOpen = false"
                >
                  取消
                </UButton>
                <UButton
                  color="primary"
                  :loading="store.acting"
                  @click="confirmRestore"
                >
                  确认恢复
                </UButton>
              </div>
            </div>
          </template>
        </UModal>

        <UModal v-model:open="purgeOpen">
          <template #content>
            <div class="space-y-4 p-6">
              <h3 class="text-lg font-semibold text-error">
                永久销毁
              </h3>
              <p class="text-sm text-muted">
                销毁后不可恢复。确认销毁 {{ actionIds.length }} 个对象？
              </p>
              <div class="flex justify-end gap-2">
                <UButton
                  color="neutral"
                  variant="ghost"
                  @click="purgeOpen = false"
                >
                  取消
                </UButton>
                <UButton
                  color="error"
                  :loading="store.acting"
                  @click="confirmPurge"
                >
                  确认销毁
                </UButton>
              </div>
            </div>
          </template>
        </UModal>
      </div>
    </template>
  </UDashboardPanel>
</template>
