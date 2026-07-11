<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { TreeItem } from '@nuxt/ui'
import AdminNavbar from '../../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../../components/admin/AdminPageHeader.vue'
import { usePermission } from '../../../../composables/usePermission'
import { useDeptStore, type DeptTreeNode } from '../../../../stores/dept'
import type { DeptItem } from '../../../../api/admin/dept'

const toast = useToast()
const deptStore = useDeptStore()
const { hasPermission } = usePermission()

const formOpen = ref(false)
const deleteOpen = ref(false)
const submitting = ref(false)
const editingDept = ref<DeptItem | null>(null)
const deletingDept = ref<DeptItem | null>(null)

const form = reactive({
  parentId: '0',
  name: '',
  sort: 0,
  status: 0
})

const canCreate = computed(() => hasPermission('system:dept:create'))
const canUpdate = computed(() => hasPermission('system:dept:update'))
const canDelete = computed(() => hasPermission('system:dept:delete'))

const parentSelectItems = computed(() => deptStore.parentOptions(editingDept.value?.id))

const modalTitle = computed(() => (editingDept.value ? '编辑部门' : '新建部门'))

function resetForm(parentId = '0') {
  form.parentId = parentId
  form.name = ''
  form.sort = 0
  form.status = 0
}

async function loadList() {
  try {
    await deptStore.fetchList()
  } catch {
    toast.add({
      title: '加载失败',
      description: deptStore.lastError ?? '无法获取部门列表',
      color: 'error'
    })
  }
}

function openCreate(parentId = '0') {
  editingDept.value = null
  resetForm(parentId)
  formOpen.value = true
}

function openEdit(dept: DeptItem) {
  editingDept.value = dept
  form.parentId = dept.parentId
  form.name = dept.name
  form.sort = dept.sort ?? 0
  form.status = dept.status ?? 0
  formOpen.value = true
}

function openDelete(dept: DeptItem) {
  deletingDept.value = dept
  deleteOpen.value = true
}

async function submitForm() {
  const name = form.name.trim()
  if (!name) {
    toast.add({ title: '请填写部门名称', color: 'warning' })
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

function getDeptFromNode(item: TreeItem): DeptItem | null {
  return (item as DeptTreeNode).dept ?? null
}

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
          description="组织架构与部门层级"
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
          <div v-if="deptStore.loading" class="py-8">
            <USkeleton class="h-8 w-full mb-2" />
            <USkeleton class="h-8 w-full mb-2" />
            <USkeleton class="h-8 w-2/3" />
          </div>

          <UEmpty
            v-else-if="deptStore.list.length === 0"
            icon="i-lucide-building-2"
            title="暂无部门数据"
            description="创建第一个部门以搭建组织架构"
          >
            <UButton v-if="canCreate" icon="i-lucide-plus" @click="openCreate()">
              新建部门
            </UButton>
          </UEmpty>

          <UTree
            v-else
            :items="deptStore.tree"
            :get-key="item => item.id"
            class="max-w-3xl"
          >
            <template #item-trailing="{ item }">
              <div
                v-if="getDeptFromNode(item)"
                class="flex items-center gap-1"
              >
                <UButton
                  v-if="canCreate"
                  icon="i-lucide-plus"
                  color="neutral"
                  variant="ghost"
                  size="xs"
                  aria-label="添加子部门"
                  @click.stop="openCreate(getDeptFromNode(item)!.id)"
                />
                <UButton
                  v-if="canUpdate"
                  icon="i-lucide-pencil"
                  color="neutral"
                  variant="ghost"
                  size="xs"
                  aria-label="编辑部门"
                  @click.stop="openEdit(getDeptFromNode(item)!)"
                />
                <UButton
                  v-if="canDelete"
                  icon="i-lucide-trash-2"
                  color="error"
                  variant="ghost"
                  size="xs"
                  aria-label="删除部门"
                  @click.stop="openDelete(getDeptFromNode(item)!)"
                />
              </div>
            </template>
          </UTree>
        </UCard>
      </div>

      <UModal v-model:open="formOpen" :title="modalTitle">
        <template #body>
          <div class="space-y-4">
            <UFormField label="上级部门">
              <USelectMenu
                v-model="form.parentId"
                :items="parentSelectItems"
                value-key="value"
                class="w-full"
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
