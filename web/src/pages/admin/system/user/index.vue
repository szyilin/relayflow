<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import AdminNavbar from '../../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../../components/admin/AdminPageHeader.vue'
import { usePermission } from '../../../../composables/usePermission'
import { getRolePage } from '../../../../api/admin/role'
import { useDeptStore, type DeptTreeNode } from '../../../../stores/dept'
import { useUserStore, type UserListRecord } from '../../../../stores/user'

const toast = useToast()
const userStore = useUserStore()
const deptStore = useDeptStore()
const { hasPermission } = usePermission()

const editOpen = ref(false)
const saving = ref(false)
const detailLoading = ref(false)
const roleOptions = ref<Array<{ id: string, label: string }>>([])
const selectedDeptNode = ref<DeptTreeNode | undefined>()

const editForm = reactive({
  id: '',
  username: '',
  nickname: '',
  mobile: '',
  email: '',
  deptId: null as string | null,
  roleIds: [] as string[]
})

const columns: TableColumn<UserListRecord>[] = [{
  accessorKey: 'nickname',
  header: '姓名'
}, {
  accessorKey: 'memberStatus',
  header: '账号状态'
}, {
  accessorKey: 'mobile',
  header: '手机号'
}, {
  accessorKey: 'dept',
  header: '部门'
}, {
  id: 'actions',
  header: '操作'
}]

const selectedDeptId = computed(() => selectedDeptNode.value?.id ?? userStore.deptId)

const createUserLink = computed(() => ({
  path: '/admin/system/user/create',
  query: selectedDeptId.value ? { deptId: selectedDeptId.value } : undefined
}))

function canToggleStatus(record: UserListRecord) {
  return record.memberStatus === 'ACTIVE' || record.memberStatus === 'SUSPENDED'
}

const deptOptions = computed(() =>
  deptStore.list.map(item => ({
    label: item.name,
    value: item.id
  }))
)

function toggleRole(roleId: string, checked: boolean) {
  if (checked) {
    if (!editForm.roleIds.includes(roleId)) {
      editForm.roleIds.push(roleId)
    }
    return
  }
  editForm.roleIds = editForm.roleIds.filter(id => id !== roleId)
}

async function loadPage(options?: { page?: number, keyword?: string, deptId?: string }) {
  try {
    await userStore.fetchPage({
      deptId: options?.deptId ?? selectedDeptId.value,
      page: options?.page,
      keyword: options?.keyword
    })
  } catch {
    toast.add({
      title: '加载失败',
      description: userStore.lastError ?? '无法获取用户列表',
      color: 'error'
    })
  }
}

async function loadOptions() {
  await Promise.all([
    deptStore.fetchList(),
    getRolePage({ pageNo: 1, pageSize: 100 }).then((data) => {
      roleOptions.value = data.list.map(item => ({
        id: String(item.id),
        label: item.name
      }))
    })
  ])
}

function selectDefaultDept() {
  const rootId = deptStore.rootDeptId()
  if (!rootId) {
    return undefined
  }
  const node = deptStore.findTreeNode(deptStore.tree, rootId)
  if (node) {
    selectedDeptNode.value = node
  }
  return rootId
}

function onDeptSelect(_event: unknown, item: DeptTreeNode) {
  if (item.id === userStore.deptId) {
    return
  }
  selectedDeptNode.value = item
  void loadPage({ page: 1, deptId: item.id })
}

async function initDeptAndPage() {
  await loadOptions()
  const rootId = selectDefaultDept()
  await loadPage({ page: 1, deptId: rootId })
}

async function openEdit(record: UserListRecord) {
  editOpen.value = true
  detailLoading.value = true
  try {
    const detail = await userStore.fetchDetail(record.id)
    editForm.id = detail.id
    editForm.username = detail.username
    editForm.nickname = detail.nickname ?? ''
    editForm.mobile = detail.mobile ?? ''
    editForm.email = detail.email ?? ''
    editForm.deptId = detail.deptId ?? null
    editForm.roleIds = [...detail.roleIds]
  } catch (error) {
    editOpen.value = false
    toast.add({
      title: '加载用户失败',
      description: error instanceof Error ? error.message : '请稍后重试',
      color: 'error'
    })
  } finally {
    detailLoading.value = false
  }
}

async function saveEdit() {
  saving.value = true
  try {
    await userStore.saveProfile({
      id: editForm.id,
      nickname: editForm.nickname.trim() || undefined,
      email: editForm.email.trim() || undefined,
      deptId: editForm.deptId,
      roleIds: editForm.roleIds
    })
    editOpen.value = false
    toast.add({ title: '成员已更新', color: 'success' })
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

async function toggleStatus(record: UserListRecord) {
  const nextStatus = record.statusCode === 0 ? 1 : 0
  const action = nextStatus === 1 ? '暂停' : '恢复'
  try {
    await userStore.toggleStatus(record.id, nextStatus)
    toast.add({ title: `成员已${action}`, color: 'success' })
  } catch (error) {
    toast.add({
      title: `${action}失败`,
      description: error instanceof Error ? error.message : '请稍后重试',
      color: 'error'
    })
  }
}

onMounted(() => {
  void initDeptAndPage()
})

watch(() => userStore.page, (page) => {
  void loadPage({ page })
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
      <div class="space-y-4">
        <AdminPageHeader
          title="成员列表"
          description="按部门浏览与管理组织成员"
        >
          <template #actions>
            <UButton
              v-if="hasPermission('system:user:create')"
              :to="createUserLink"
              icon="i-lucide-user-plus"
            >
              邀请成员
            </UButton>
          </template>
        </AdminPageHeader>

        <div class="flex flex-col gap-4 lg:flex-row lg:items-start">
          <UCard class="w-full shrink-0 lg:w-64">
            <template #header>
              <h3 class="text-sm font-medium">
                组织架构
              </h3>
            </template>

            <div v-if="deptStore.loading" class="space-y-2 py-2">
              <USkeleton v-for="i in 4" :key="i" class="h-7 w-full" />
            </div>

            <UEmpty
              v-else-if="deptStore.tree.length === 0"
              icon="i-lucide-building-2"
              title="暂无部门"
              description="请先在部门管理中创建组织架构"
              class="py-6"
            />

            <UTree
              v-else
              v-model="selectedDeptNode"
              :items="deptStore.tree"
              :get-key="item => item.id"
              @select="onDeptSelect"
            />
          </UCard>

          <UCard class="min-w-0 flex-1">
            <div class="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
              <UInput
                v-model="userStore.keyword"
                placeholder="搜索姓名或手机号"
                icon="i-lucide-search"
                class="sm:max-w-xs"
                @keyup.enter="loadPage({ page: 1, keyword: userStore.keyword })"
              />
              <UButton
                color="neutral"
                variant="soft"
                :loading="userStore.loading"
                @click="loadPage({ page: 1, keyword: userStore.keyword })"
              >
                搜索
              </UButton>
            </div>

            <UTable :data="userStore.list" :columns="columns" :loading="userStore.loading">
              <template #memberStatus-cell="{ row }">
                <UBadge :color="row.original.statusColor" variant="subtle">
                  {{ row.original.statusLabel }}
                </UBadge>
              </template>

              <template #actions-cell="{ row }">
                <div class="flex gap-1">
                  <UButton
                    v-if="hasPermission('system:user:update')"
                    size="xs"
                    color="neutral"
                    variant="soft"
                    @click="openEdit(row.original)"
                  >
                    编辑
                  </UButton>
                  <UButton
                    v-if="hasPermission('system:user:update') && canToggleStatus(row.original)"
                    size="xs"
                    :color="row.original.statusCode === 0 ? 'error' : 'primary'"
                    variant="soft"
                    @click="toggleStatus(row.original)"
                  >
                    {{ row.original.statusCode === 0 ? '暂停' : '恢复' }}
                  </UButton>
                </div>
              </template>
            </UTable>

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
        </div>
      </div>

      <UModal v-model:open="editOpen" title="编辑成员">
        <template #body>
          <div v-if="detailLoading" class="py-8 text-center text-sm text-muted">
            加载中…
          </div>
          <form v-else class="space-y-4" @submit.prevent="saveEdit">
            <UFormField label="姓名">
              <UInput v-model="editForm.nickname" placeholder="张三" />
            </UFormField>
            <UFormField label="手机号">
              <UInput :model-value="editForm.mobile" disabled />
              <template #hint>
                <span class="text-muted">手机号为账号标识，组织不可修改</span>
              </template>
            </UFormField>
            <UFormField label="工作邮箱">
              <UInput v-model="editForm.email" type="email" placeholder="zhangsan@example.com" />
            </UFormField>
            <UFormField label="主部门">
              <USelectMenu
                v-model="editForm.deptId"
                :items="deptOptions"
                value-key="value"
                label-key="label"
                placeholder="选择部门"
                searchable
              />
            </UFormField>
            <UFormField label="角色">
              <div v-if="roleOptions.length === 0" class="text-sm text-muted">
                暂无可用角色
              </div>
              <div v-else class="max-h-40 space-y-2 overflow-y-auto">
                <label
                  v-for="role in roleOptions"
                  :key="role.id"
                  class="flex items-center gap-2 text-sm"
                >
                  <UCheckbox
                    :model-value="editForm.roleIds.includes(role.id)"
                    @update:model-value="toggleRole(role.id, $event === true)"
                  />
                  <span>{{ role.label }}</span>
                </label>
              </div>
            </UFormField>
            <div class="flex justify-end gap-2 pt-2">
              <UButton color="neutral" variant="ghost" @click="editOpen = false">
                取消
              </UButton>
              <UButton type="submit" :loading="saving">
                保存
              </UButton>
            </div>
          </form>
        </template>
      </UModal>
    </template>
  </UDashboardPanel>
</template>
