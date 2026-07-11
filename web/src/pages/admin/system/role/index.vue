<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import AdminNavbar from '../../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../../components/admin/AdminPageHeader.vue'
import { usePermission } from '../../../../composables/usePermission'
import {
  getRolePage,
  type DataScope,
  type PermissionNode,
  type RolePageItem
} from '../../../../api/admin/role'
import { useRoleStore, type RoleListRecord } from '../../../../stores/role'

const toast = useToast()
const roleStore = useRoleStore()
const { hasPermission } = usePermission()

const modalOpen = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const saving = ref(false)
const parentOptions = ref<RolePageItem[]>([])

const form = reactive({
  id: null as number | null,
  parentId: 100,
  name: '',
  code: '',
  dataScope: 'DEPT' as DataScope,
  canDelegate: 0,
  sort: 0,
  status: 0,
  remark: '',
  permissionIds: [] as number[],
  deptIdsText: ''
})

const dataScopeOptions: { label: string, value: DataScope }[] = [{
  label: '全部数据',
  value: 'ALL'
}, {
  label: '本部门',
  value: 'DEPT'
}, {
  label: '本部门及下级',
  value: 'DEPT_AND_CHILD'
}, {
  label: '仅本人',
  value: 'SELF'
}, {
  label: '自定义',
  value: 'CUSTOM'
}]

const columns: TableColumn<RoleListRecord>[] = [{
  accessorKey: 'name',
  header: '角色名称'
}, {
  accessorKey: 'code',
  header: '编码'
}, {
  accessorKey: 'roleTypeLabel',
  header: '类型'
}, {
  accessorKey: 'dataScopeLabel',
  header: '数据范围'
}, {
  accessorKey: 'status',
  header: '状态'
}, {
  accessorKey: 'createTime',
  header: '创建时间'
}, {
  id: 'actions',
  header: '操作'
}]

const parentPermissionIds = ref<Set<number>>(new Set())

const flatPermissions = computed(() => flattenPermissions(roleStore.permissionTree))

const visiblePermissions = computed(() => {
  if (parentPermissionIds.value.size === 0) {
    return flatPermissions.value
  }
  return flatPermissions.value.filter(item => parentPermissionIds.value.has(item.id))
})

function flattenPermissions(nodes: PermissionNode[], depth = 0): Array<PermissionNode & { depth: number }> {
  const result: Array<PermissionNode & { depth: number }> = []
  for (const node of nodes) {
    result.push({ ...node, depth })
    if (node.children?.length) {
      result.push(...flattenPermissions(node.children, depth + 1))
    }
  }
  return result
}

function parseDeptIds(text: string): number[] {
  return text
    .split(/[,，\s]+/)
    .map(part => Number(part.trim()))
    .filter(id => Number.isFinite(id) && id > 0)
}

async function loadPage(options?: { page?: number, keyword?: string }) {
  try {
    await roleStore.fetchPage(options)
  } catch {
    toast.add({
      title: '加载失败',
      description: roleStore.lastError ?? '无法获取角色列表',
      color: 'error'
    })
  }
}

async function loadParentOptions() {
  const data = await getRolePage({ pageNo: 1, pageSize: 100 })
  parentOptions.value = data.list
}

async function refreshParentPermissions(parentId: number) {
  if (!parentId || parentId === 0) {
    parentPermissionIds.value = new Set()
    return
  }
  try {
    const parent = await roleStore.fetchDetail(parentId)
    parentPermissionIds.value = new Set(parent.permissionIds)
    form.permissionIds = form.permissionIds.filter(id => parentPermissionIds.value.has(id))
  } catch {
    parentPermissionIds.value = new Set()
  }
}

function resetForm() {
  form.id = null
  form.parentId = parentOptions.value[0]?.id ?? 100
  form.name = ''
  form.code = ''
  form.dataScope = 'DEPT'
  form.canDelegate = 0
  form.sort = 0
  form.status = 0
  form.remark = ''
  form.permissionIds = []
  form.deptIdsText = ''
}

async function openCreateModal() {
  modalMode.value = 'create'
  resetForm()
  await roleStore.fetchPermissions()
  await loadParentOptions()
  await refreshParentPermissions(form.parentId)
  modalOpen.value = true
}

async function openEditModal(record: RoleListRecord) {
  if (record.isSystem) {
    toast.add({
      title: '不可编辑',
      description: '系统内置角色不可修改',
      color: 'warning'
    })
    return
  }

  modalMode.value = 'edit'
  await roleStore.fetchPermissions()
  await loadParentOptions()

  try {
    const detail = await roleStore.fetchDetail(Number(record.id))
    form.id = detail.id
    form.parentId = detail.parentId
    form.name = detail.name
    form.code = detail.code
    form.dataScope = detail.dataScope
    form.canDelegate = detail.canDelegate ?? 0
    form.sort = detail.sort ?? 0
    form.status = detail.status ?? 0
    form.remark = detail.remark ?? ''
    form.permissionIds = [...detail.permissionIds]
    form.deptIdsText = detail.deptIds.join(', ')
    await refreshParentPermissions(form.parentId)
    modalOpen.value = true
  } catch {
    toast.add({
      title: '加载失败',
      description: '无法获取角色详情',
      color: 'error'
    })
  }
}

async function onSubmit() {
  if (!form.name.trim() || !form.code.trim()) {
    toast.add({
      title: '请完善表单',
      description: '角色名称和编码为必填项',
      color: 'error'
    })
    return
  }

  saving.value = true
  try {
    const payload = {
      id: form.id ?? undefined,
      parentId: form.parentId,
      name: form.name.trim(),
      code: form.code.trim(),
      dataScope: form.dataScope,
      canDelegate: form.canDelegate,
      sort: form.sort,
      status: form.status,
      remark: form.remark.trim() || undefined,
      permissionIds: form.permissionIds,
      deptIds: form.dataScope === 'CUSTOM' ? parseDeptIds(form.deptIdsText) : undefined
    }

    await roleStore.saveRole(payload)
    toast.add({
      title: modalMode.value === 'create' ? '创建成功' : '更新成功',
      color: 'success'
    })
    modalOpen.value = false
    await loadPage()
  } catch (error) {
    toast.add({
      title: '保存失败',
      description: error instanceof Error ? error.message : '请稍后重试',
      color: 'error'
    })
  } finally {
    saving.value = false
  }
}

async function onDelete(record: RoleListRecord) {
  if (record.isSystem) {
    toast.add({
      title: '不可删除',
      description: '系统内置角色不可删除',
      color: 'warning'
    })
    return
  }

  if (!window.confirm(`确定删除角色「${record.name}」？`)) {
    return
  }

  try {
    await roleStore.removeRole(Number(record.id))
    toast.add({ title: '删除成功', color: 'success' })
    await loadPage()
  } catch (error) {
    toast.add({
      title: '删除失败',
      description: error instanceof Error ? error.message : '请稍后重试',
      color: 'error'
    })
  }
}

function togglePermission(id: number, checked: boolean) {
  if (checked) {
    if (!form.permissionIds.includes(id)) {
      form.permissionIds.push(id)
    }
  } else {
    form.permissionIds = form.permissionIds.filter(item => item !== id)
  }
}

function onSearch() {
  loadPage({ page: 1, keyword: roleStore.keyword })
}

onMounted(() => {
  loadPage()
})

watch(() => roleStore.page, (page) => {
  loadPage({ page })
})

watch(() => form.parentId, (parentId) => {
  if (modalOpen.value) {
    refreshParentPermissions(parentId)
  }
})
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-system-role">
    <template #header>
      <AdminNavbar title="角色管理" />
    </template>

    <template #body>
      <div class="space-y-4">
        <AdminPageHeader
          title="角色列表"
          description="管理功能权限与数据范围"
        >
          <template #actions>
            <UButton
              v-if="hasPermission('system:role:create')"
              icon="i-lucide-plus"
              @click="openCreateModal"
            >
              新建角色
            </UButton>
          </template>
        </AdminPageHeader>

        <UCard>
          <div class="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
            <UInput
              v-model="roleStore.keyword"
              placeholder="搜索角色名称或编码"
              icon="i-lucide-search"
              class="sm:max-w-xs"
              @keyup.enter="onSearch"
            />
            <UButton color="neutral" variant="soft" :loading="roleStore.loading" @click="onSearch">
              搜索
            </UButton>
          </div>

          <UTable :data="roleStore.list" :columns="columns" :loading="roleStore.loading">
            <template #actions-cell="{ row }">
              <div class="flex gap-1">
                <UButton
                  v-if="hasPermission('system:role:update')"
                  size="xs"
                  color="neutral"
                  variant="ghost"
                  :disabled="row.original.isSystem"
                  @click="openEditModal(row.original)"
                >
                  编辑
                </UButton>
                <UButton
                  v-if="hasPermission('system:role:delete')"
                  size="xs"
                  color="error"
                  variant="ghost"
                  :disabled="row.original.isSystem"
                  @click="onDelete(row.original)"
                >
                  删除
                </UButton>
              </div>
            </template>
          </UTable>

          <div class="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <p class="text-sm text-muted">
              共 {{ roleStore.total }} 条
            </p>
            <UPagination
              v-model:page="roleStore.page"
              :total="roleStore.total"
              :items-per-page="roleStore.pageSize"
            />
          </div>
        </UCard>
      </div>

      <UModal v-model:open="modalOpen" :title="modalMode === 'create' ? '新建角色' : '编辑角色'">
        <template #body>
          <form class="space-y-4" @submit.prevent="onSubmit">
            <UFormField label="上级角色" required>
              <USelectMenu
                v-model="form.parentId"
                :items="parentOptions.map(item => ({ label: item.name, value: item.id }))"
                value-key="value"
              />
            </UFormField>

            <UFormField label="角色名称" required>
              <UInput v-model="form.name" placeholder="如：部门管理员" />
            </UFormField>

            <UFormField label="角色编码" required>
              <UInput v-model="form.code" placeholder="如：dept_admin" />
            </UFormField>

            <UFormField label="数据范围" required>
              <USelectMenu
                v-model="form.dataScope"
                :items="dataScopeOptions"
                value-key="value"
              />
            </UFormField>

            <UFormField v-if="form.dataScope === 'CUSTOM'" label="自定义部门 ID">
              <UInput
                v-model="form.deptIdsText"
                placeholder="多个 ID 用逗号分隔，如：1001, 1002"
              />
              <template #hint>
                部门选择器将在部门管理接入后提供；当前可手动填写部门 ID
              </template>
            </UFormField>

            <UFormField label="功能权限">
              <div v-if="roleStore.permissionsLoading" class="text-sm text-muted">
                加载权限树…
              </div>
              <div v-else class="max-h-60 space-y-1 overflow-y-auto rounded-md border border-default p-3">
                <label
                  v-for="perm in visiblePermissions"
                  :key="perm.id"
                  class="flex items-center gap-2 text-sm"
                  :style="{ paddingLeft: `${perm.depth * 12}px` }"
                >
                  <UCheckbox
                    :model-value="form.permissionIds.includes(perm.id)"
                    @update:model-value="togglePermission(perm.id, $event === true)"
                  />
                  <span>{{ perm.name }}</span>
                  <span class="text-xs text-muted">{{ perm.code }}</span>
                </label>
              </div>
            </UFormField>

            <UFormField label="备注">
              <UTextarea v-model="form.remark" :rows="2" />
            </UFormField>
          </form>
        </template>

        <template #footer>
          <div class="flex justify-end gap-2">
            <UButton color="neutral" variant="ghost" @click="modalOpen = false">
              取消
            </UButton>
            <UButton :loading="saving" @click="onSubmit">
              保存
            </UButton>
          </div>
        </template>
      </UModal>
    </template>
  </UDashboardPanel>
</template>
