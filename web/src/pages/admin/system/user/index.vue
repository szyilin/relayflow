<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import AdminNavbar from '../../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../../components/admin/AdminPageHeader.vue'
import { usePermission } from '../../../../composables/usePermission'
import { getRolePage } from '../../../../api/admin/role'
import { useDeptStore } from '../../../../stores/dept'
import { useUserStore, type UserListRecord } from '../../../../stores/user'

const toast = useToast()
const userStore = useUserStore()
const deptStore = useDeptStore()
const { hasPermission } = usePermission()

const editOpen = ref(false)
const saving = ref(false)
const detailLoading = ref(false)
const roleOptions = ref<Array<{ id: string, label: string }>>([])

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
}, {
  id: 'actions',
  header: '操作'
}]

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
      mobile: editForm.mobile.trim() || undefined,
      email: editForm.email.trim() || undefined,
      deptId: editForm.deptId,
      roleIds: editForm.roleIds
    })
    editOpen.value = false
    toast.add({ title: '用户已更新', color: 'success' })
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
  const action = nextStatus === 1 ? '禁用' : '启用'
  try {
    await userStore.toggleStatus(record.id, nextStatus)
    toast.add({ title: `用户已${action}`, color: 'success' })
  } catch (error) {
    toast.add({
      title: `${action}失败`,
      description: error instanceof Error ? error.message : '请稍后重试',
      color: 'error'
    })
  }
}

onMounted(() => {
  loadOptions()
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
            <UButton
              v-if="hasPermission('system:user:create')"
              to="/admin/system/user/create"
              icon="i-lucide-plus"
            >
              新建用户
            </UButton>
          </template>
        </AdminPageHeader>

        <UCard>
          <div class="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
            <UInput
              v-model="userStore.keyword"
              placeholder="搜索用户名或昵称"
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
                  v-if="hasPermission('system:user:update')"
                  size="xs"
                  :color="row.original.statusCode === 0 ? 'error' : 'primary'"
                  variant="soft"
                  @click="toggleStatus(row.original)"
                >
                  {{ row.original.statusCode === 0 ? '禁用' : '启用' }}
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

      <UModal v-model:open="editOpen" title="编辑用户">
        <template #body>
          <div v-if="detailLoading" class="py-8 text-center text-sm text-muted">
            加载中…
          </div>
          <form v-else class="space-y-4" @submit.prevent="saveEdit">
            <UFormField label="用户名">
              <UInput :model-value="editForm.username" disabled />
            </UFormField>
            <UFormField label="昵称">
              <UInput v-model="editForm.nickname" placeholder="张三" />
            </UFormField>
            <UFormField label="手机号">
              <UInput v-model="editForm.mobile" placeholder="13800138000" />
            </UFormField>
            <UFormField label="邮箱">
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
