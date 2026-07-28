<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import AdminNavbar from '../../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../../components/admin/AdminPageHeader.vue'
import AppTreeSelect from '../../../../components/AppTreeSelect.vue'
import { usePermission } from '../../../../composables/usePermission'
import { useAuthStore } from '../../../../stores/auth'
import { useDeptStore, type DeptTreeNode } from '../../../../stores/dept'
import type { DeptItem } from '../../../../api/admin/dept'

interface DeptTableRow {
  id: string
  dept: DeptItem
  depth: number
  hasChildren: boolean
  expanded: boolean
  isRoot: boolean
  displayName: string
}

const toast = useToast()
const authStore = useAuthStore()
const deptStore = useDeptStore()
const { hasPermission } = usePermission()

const formOpen = ref(false)
const deleteOpen = ref(false)
const submitting = ref(false)
const editingDept = ref<DeptItem | null>(null)
const deletingDept = ref<DeptItem | null>(null)
const keyword = ref('')
const expandedIds = ref<Set<string>>(new Set())

const form = reactive({
  parentId: '' as string,
  name: '',
  sort: 0,
  status: 0
})

const canCreate = computed(() => hasPermission('system:dept:create'))
const canUpdate = computed(() => hasPermission('system:dept:update'))
const canDelete = computed(() => hasPermission('system:dept:delete'))

const orgName = computed(() => authStore.activeTenantName || '当前企业')

const parentTreeItems = computed(() =>
  deptStore.parentTreeItems(editingDept.value?.id, orgName.value))

const modalTitle = computed(() => (editingDept.value ? '编辑部门' : '新建部门'))

const columns: TableColumn<DeptTableRow>[] = [{
  accessorKey: 'displayName',
  header: '部门名称'
}, {
  accessorKey: 'dept.status',
  header: '状态'
}, {
  id: 'actions',
  header: '操作'
}]

function displayNameOf(dept: DeptItem) {
  return deptStore.isRootDept(dept) ? orgName.value : dept.name
}

function collectExpandedDefaults(nodes: DeptTreeNode[], into: Set<string>) {
  for (const node of nodes) {
    if (node.children?.length) {
      into.add(node.id)
      collectExpandedDefaults(node.children, into)
    }
  }
}

function syncExpandedDefaults() {
  const next = new Set<string>()
  collectExpandedDefaults(deptStore.tree, next)
  expandedIds.value = next
}

function matchesKeyword(node: DeptTreeNode, q: string): boolean {
  if (displayNameOf(node.dept).toLowerCase().includes(q)) {
    return true
  }
  return (node.children ?? []).some(child => matchesKeyword(child, q))
}

function flattenVisible(
  nodes: DeptTreeNode[],
  depth: number,
  q: string | null,
  rows: DeptTableRow[]
) {
  for (const node of nodes) {
    const childMatch = q ? (node.children ?? []).some(child => matchesKeyword(child, q)) : false
    const selfMatch = q ? displayNameOf(node.dept).toLowerCase().includes(q) : true
    if (q && !selfMatch && !childMatch) {
      continue
    }

    const hasChildren = (node.children?.length ?? 0) > 0
    const forceExpand = Boolean(q && childMatch)
    const expanded = hasChildren && (forceExpand || expandedIds.value.has(node.id))
    rows.push({
      id: node.id,
      dept: node.dept,
      depth,
      hasChildren,
      expanded,
      isRoot: deptStore.isRootDept(node.dept),
      displayName: displayNameOf(node.dept)
    })

    if (expanded) {
      flattenVisible(node.children!, depth + 1, q, rows)
    }
  }
}

const tableRows = computed(() => {
  const q = keyword.value.trim().toLowerCase() || null
  const rows: DeptTableRow[] = []
  flattenVisible(deptStore.tree, 0, q, rows)
  return rows
})

function toggleExpand(row: DeptTableRow) {
  if (!row.hasChildren) {
    return
  }
  const next = new Set(expandedIds.value)
  if (next.has(row.id)) {
    next.delete(row.id)
  } else {
    next.add(row.id)
  }
  expandedIds.value = next
}

function resetForm(parentId: string) {
  form.parentId = parentId
  form.name = ''
  form.sort = 0
  form.status = 0
}

async function loadList() {
  try {
    await deptStore.fetchList()
    syncExpandedDefaults()
  } catch {
    toast.add({
      title: '加载失败',
      description: deptStore.lastError ?? '无法获取部门列表',
      color: 'error'
    })
  }
}

function openCreate(parentId?: string) {
  const rootId = deptStore.rootDeptId()
  const resolvedParent = parentId ?? rootId
  if (!resolvedParent) {
    toast.add({
      title: '无法新建',
      description: '组织根部门尚未就绪，请稍后重试',
      color: 'warning'
    })
    return
  }
  editingDept.value = null
  resetForm(resolvedParent)
  formOpen.value = true
}

function openEdit(dept: DeptItem) {
  if (deptStore.isRootDept(dept)) {
    toast.add({
      title: '根部门不可编辑',
      description: '组织名称跟随企业名称，无法在此修改',
      color: 'warning'
    })
    return
  }
  editingDept.value = dept
  form.parentId = dept.parentId
  form.name = dept.name
  form.sort = dept.sort ?? 0
  form.status = dept.status ?? 0
  formOpen.value = true
}

function openDelete(dept: DeptItem) {
  if (deptStore.isRootDept(dept)) {
    toast.add({
      title: '根部门不可删除',
      description: '组织根节点与企业绑定，无法删除',
      color: 'warning'
    })
    return
  }
  deletingDept.value = dept
  deleteOpen.value = true
}

async function submitForm() {
  const name = form.name.trim()
  if (!name) {
    toast.add({ title: '请填写部门名称', color: 'warning' })
    return
  }
  if (!form.parentId || form.parentId === '0') {
    toast.add({
      title: '请选择上级部门',
      description: '新部门须挂在组织或其子部门下',
      color: 'warning'
    })
    return
  }

  submitting.value = true
  try {
    if (editingDept.value) {
      await deptStore.update({
        id: editingDept.value.id,
        parentId: form.parentId,
        name,
        sort: form.sort,
        status: form.status
      })
      toast.add({ title: '部门已更新', color: 'success' })
    } else {
      await deptStore.create({
        parentId: form.parentId,
        name,
        sort: form.sort,
        status: form.status
      })
      toast.add({ title: '部门已创建', color: 'success' })
    }
    formOpen.value = false
    syncExpandedDefaults()
  } catch (error) {
    toast.add({
      title: editingDept.value ? '更新失败' : '创建失败',
      description: error instanceof Error ? error.message : '操作失败',
      color: 'error'
    })
  } finally {
    submitting.value = false
  }
}

async function confirmDelete() {
  if (!deletingDept.value) {
    return
  }

  submitting.value = true
  try {
    await deptStore.remove(deletingDept.value.id)
    toast.add({ title: '部门已删除', color: 'success' })
    deleteOpen.value = false
    deletingDept.value = null
    syncExpandedDefaults()
  } catch (error) {
    toast.add({
      title: '删除失败',
      description: error instanceof Error ? error.message : '操作失败',
      color: 'error'
    })
  } finally {
    submitting.value = false
  }
}

watch(() => deptStore.list.length, (len, prev) => {
  if (len > 0 && prev === 0) {
    syncExpandedDefaults()
  }
})

onMounted(() => {
  loadList()
})
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-system-dept">
    <template #header>
      <AdminNavbar title="部门管理" />
    </template>

    <template #body>
      <div class="space-y-4">
        <AdminPageHeader
          title="部门架构"
          description="组织为根节点，其余部门为其子部门；组织名称跟随企业名称"
        >
          <template #actions>
            <UButton
              v-if="canCreate"
              icon="i-lucide-plus"
              @click="openCreate()"
            >
              新建部门
            </UButton>
          </template>
        </AdminPageHeader>

        <UCard>
          <div class="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <UInput
              v-model="keyword"
              placeholder="搜索部门名"
              icon="i-lucide-search"
              class="sm:max-w-xs"
            />
          </div>

          <div v-if="deptStore.loading && deptStore.list.length === 0" class="space-y-2 py-4">
            <USkeleton class="h-10 w-full" />
            <USkeleton class="h-10 w-full" />
            <USkeleton class="h-10 w-2/3" />
          </div>

          <UEmpty
            v-else-if="deptStore.list.length === 0"
            icon="i-lucide-building-2"
            title="暂无部门数据"
            description="组织根部门将在企业初始化时自动创建"
          />

          <UEmpty
            v-else-if="tableRows.length === 0"
            icon="i-lucide-search"
            title="未找到匹配部门"
            description="试试其他关键词"
          />

          <UTable
            v-else
            :data="tableRows"
            :columns="columns"
            :loading="deptStore.loading"
          >
            <template #displayName-cell="{ row }">
              <div
                class="flex min-w-0 items-center gap-1"
                :style="{ paddingLeft: `${row.original.depth * 1.25}rem` }"
              >
                <button
                  v-if="row.original.hasChildren"
                  type="button"
                  class="inline-flex size-6 shrink-0 items-center justify-center rounded-md text-muted hover:bg-elevated hover:text-highlighted"
                  :aria-label="row.original.expanded ? '收起' : '展开'"
                  @click="toggleExpand(row.original)"
                >
                  <UIcon
                    :name="row.original.expanded
                      ? 'i-lucide-chevron-down'
                      : 'i-lucide-chevron-right'"
                    class="size-4"
                  />
                </button>
                <span
                  v-else
                  class="inline-block size-6 shrink-0"
                  aria-hidden="true"
                />
                <UIcon
                  :name="row.original.isRoot ? 'i-lucide-building-2' : 'i-lucide-folder'"
                  class="size-4 shrink-0 text-muted"
                />
                <span
                  class="truncate font-medium"
                  :class="row.original.isRoot ? 'text-highlighted' : ''"
                >
                  {{ row.original.displayName }}
                </span>
              </div>
            </template>

            <template #dept.status-cell="{ row }">
              <UBadge
                :color="row.original.dept.status === 1 ? 'neutral' : 'success'"
                variant="subtle"
                size="sm"
              >
                {{ row.original.dept.status === 1 ? '停用' : '启用' }}
              </UBadge>
            </template>

            <template #actions-cell="{ row }">
              <div class="flex flex-wrap items-center gap-1">
                <UButton
                  v-if="canCreate"
                  size="xs"
                  color="primary"
                  variant="link"
                  @click="openCreate(row.original.dept.id)"
                >
                  添加子部门
                </UButton>
                <UButton
                  v-if="canUpdate && !row.original.isRoot"
                  size="xs"
                  color="neutral"
                  variant="ghost"
                  @click="openEdit(row.original.dept)"
                >
                  编辑
                </UButton>
                <UButton
                  v-if="canDelete && !row.original.isRoot"
                  size="xs"
                  color="error"
                  variant="ghost"
                  @click="openDelete(row.original.dept)"
                >
                  删除
                </UButton>
              </div>
            </template>
          </UTable>
        </UCard>
      </div>

      <UModal v-model:open="formOpen" :title="modalTitle">
        <template #body>
          <div class="space-y-4">
            <UFormField label="上级部门" required>
              <AppTreeSelect
                v-model="form.parentId"
                :items="parentTreeItems"
                placeholder="选择上级部门（组织或其子部门）"
              />
            </UFormField>

            <UFormField label="部门名称" required>
              <UInput v-model="form.name" placeholder="请输入部门名称" class="w-full" />
            </UFormField>

            <UFormField label="排序">
              <UInput v-model.number="form.sort" type="number" min="0" class="w-full" />
            </UFormField>

            <UFormField label="状态">
              <USelectMenu
                v-model="form.status"
                :items="[
                  { label: '启用', value: 0 },
                  { label: '停用', value: 1 }
                ]"
                value-key="value"
                class="w-full"
              />
            </UFormField>
          </div>
        </template>

        <template #footer>
          <div class="flex justify-end gap-2">
            <UButton color="neutral" variant="ghost" @click="formOpen = false">
              取消
            </UButton>
            <UButton :loading="submitting" @click="submitForm">
              保存
            </UButton>
          </div>
        </template>
      </UModal>

      <UModal v-model:open="deleteOpen" title="删除部门">
        <template #body>
          <p class="text-sm text-muted">
            确定删除部门「{{ deletingDept?.name }}」吗？存在子部门或关联用户时将无法删除。
          </p>
        </template>

        <template #footer>
          <div class="flex justify-end gap-2">
            <UButton color="neutral" variant="ghost" @click="deleteOpen = false">
              取消
            </UButton>
            <UButton color="error" :loading="submitting" @click="confirmDelete">
              删除
            </UButton>
          </div>
        </template>
      </UModal>
    </template>
  </UDashboardPanel>
</template>
