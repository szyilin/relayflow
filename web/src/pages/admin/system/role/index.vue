<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, resolveComponent, watch } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import AdminNavbar from '../../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../../components/admin/AdminPageHeader.vue'
import AppTreeSelect from '../../../../components/AppTreeSelect.vue'
import WorkspaceOwnerChip from '../../../../components/workspace/WorkspaceOwnerChip.vue'
import { getUserPage, type UserPageItem } from '../../../../api/admin/user'
import { appConfirm } from '../../../../composables/appConfirm'
import { usePermission } from '../../../../composables/usePermission'
import {
  type PermissionNode,
  type RoleAdminSimple,
  type RolePageItem
} from '../../../../api/admin/role'
import { useRoleStore, type RoleListRecord } from '../../../../stores/role'
import type { AppTreeSelectItem } from '../../../../utils/treeSelect'

const toast = useToast()
const UBadge = resolveComponent('UBadge')
const roleStore = useRoleStore()
const { hasPermission } = usePermission()

const modalOpen = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const saving = ref(false)
const parentOptions = ref<RolePageItem[]>([])

const detailOpen = ref(false)
const detailTab = ref<'members' | 'permissions'>('members')
const detailRole = ref<RoleListRecord | null>(null)
const detailMembers = ref<RoleAdminSimple[]>([])
const detailPermissionIds = ref<number[]>([])
const detailLoading = ref(false)
const memberSaving = ref(false)

const addMemberOpen = ref(false)
const userSearchKeyword = ref('')
const userCandidates = ref<UserPageItem[]>([])
const userSearchLoading = ref(false)
const selectedUserIds = ref<string[]>([])

const form = reactive({
  id: null as string | null,
  parentId: '0',
  name: '',
  canDelegate: 0,
  sort: 0,
  status: 0,
  remark: '',
  permissionIds: [] as number[]
})

const expandedPermissionIds = ref<Set<number>>(new Set())

const parentRoleTreeItems = computed(() => {
  const excludeId = form.id != null ? String(form.id) : null
  const items = parentOptions.value.map(item => ({
    id: String(item.id),
    parentId: String(item.parentId ?? 0),
    name: item.name
  }))
  const blocked = new Set<string>()
  if (excludeId) {
    blocked.add(excludeId)
    let grew = true
    while (grew) {
      grew = false
      for (const item of items) {
        if (blocked.has(item.id)) {
          continue
        }
        if (blocked.has(item.parentId)) {
          blocked.add(item.id)
          grew = true
        }
      }
    }
  }

  function build(parentId: string): AppTreeSelectItem[] {
    return items
      .filter(item => item.parentId === parentId && !blocked.has(item.id))
      .map((item) => {
        const children = build(item.id)
        return {
          id: item.id,
          label: item.name,
          value: item.id,
          icon: 'i-lucide-shield',
          ...(children.length ? { children, defaultExpanded: true } : {})
        }
      })
  }

  const roots = build('0')
  if (roots.length) {
    return roots
  }
  const ids = new Set(items.map(i => i.id))
  return items
    .filter(item => !blocked.has(item.id) && !ids.has(item.parentId))
    .map((item) => {
      const children = build(item.id)
      return {
        id: item.id,
        label: item.name,
        value: item.id,
        icon: 'i-lucide-shield' as const,
        ...(children.length ? { children, defaultExpanded: true } : {})
      }
    })
})

const parentRoleIdModel = computed({
  get: () => (form.parentId && form.parentId !== '0' ? form.parentId : ''),
  set: (value: string | null | undefined) => {
    const next = value?.trim()
    form.parentId = next && next !== '0' ? next : '0'
  }
})

const columns: TableColumn<RoleListRecord>[] = [{
  accessorKey: 'name',
  header: '角色名称',
  cell: ({ row }) => {
    const record = row.original
    return h('div', { class: 'flex items-center gap-2' }, [
      h('span', { class: 'font-medium' }, record.name),
      record.isSystem
        ? h(UBadge, { color: 'primary', variant: 'subtle', size: 'sm' }, () => '系统角色')
        : null
    ])
  }
}, {
  id: 'admins',
  header: '管理员'
}, {
  accessorKey: 'permissionSummary',
  header: '权限范围'
}, {
  id: 'actions',
  header: '操作'
}]

const parentPermissionIds = ref<Set<number>>(new Set())

const permissionChildrenMap = computed(() => {
  const map = new Map<number, number[]>()
  function walk(nodes: PermissionNode[]) {
    for (const node of nodes) {
      const childIds = (node.children ?? []).map(child => child.id)
      map.set(node.id, childIds)
      if (node.children?.length) {
        walk(node.children)
      }
    }
  }
  walk(roleStore.permissionTree)
  return map
})

function collectDescendants(id: number): number[] {
  const result: number[] = []
  const queue = [...(permissionChildrenMap.value.get(id) ?? [])]
  while (queue.length) {
    const current = queue.shift()!
    result.push(current)
    queue.push(...(permissionChildrenMap.value.get(current) ?? []))
  }
  return result
}

function visiblePermissionRows(): Array<PermissionNode & { depth: number }> {
  const rows: Array<PermissionNode & { depth: number }> = []
  function walk(nodes: PermissionNode[], depth: number, parentExpanded: boolean) {
    for (const node of nodes) {
      if (!parentExpanded) {
        continue
      }
      if (parentPermissionIds.value.size > 0 && !parentPermissionIds.value.has(node.id)) {
        continue
      }
      rows.push({ ...node, depth })
      const expanded = expandedPermissionIds.value.has(node.id)
      if (node.children?.length && expanded) {
        walk(node.children, depth + 1, true)
      }
    }
  }
  walk(roleStore.permissionTree, 0, true)
  return rows
}

const visiblePermissions = computed(() => visiblePermissionRows())

const detailPermissionRows = computed(() => {
  const selected = new Set(detailPermissionIds.value)
  const rows: Array<PermissionNode & { depth: number }> = []
  function walk(nodes: PermissionNode[], depth: number) {
    for (const node of nodes) {
      const selfOrChildSelected = selected.has(node.id)
        || (node.children ?? []).some(function hasSelected(child): boolean {
          return selected.has(child.id) || (child.children ?? []).some(hasSelected)
        })
      if (!selfOrChildSelected && depth > 0) {
        // still show roots that have selected descendants via recursion below
      }
      if (selected.has(node.id) || (node.children ?? []).some(function hasSelected(child): boolean {
        return selected.has(child.id) || (child.children ?? []).some(hasSelected)
      })) {
        rows.push({ ...node, depth })
        if (node.children?.length) {
          walk(node.children, depth + 1)
        }
      }
    }
  }
  walk(roleStore.permissionTree, 0)
  return rows.filter(row => selected.has(row.id))
})

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
  parentOptions.value = await roleStore.fetchAllForOptions()
}

async function refreshParentPermissions(parentId: string | number) {
  const id = String(parentId ?? '0')
  if (!id || id === '0') {
    parentPermissionIds.value = new Set()
    return
  }
  try {
    const parent = await roleStore.fetchDetail(id)
    parentPermissionIds.value = new Set(parent.permissionIds)
    form.permissionIds = form.permissionIds.filter(pid => parentPermissionIds.value.has(pid))
  } catch {
    parentPermissionIds.value = new Set()
  }
}

function resetForm() {
  form.id = null
  form.parentId = '0'
  form.name = ''
  form.canDelegate = 0
  form.sort = 0
  form.status = 0
  form.remark = ''
  form.permissionIds = []
  expandedPermissionIds.value = new Set()
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
    const detail = await roleStore.fetchDetail(record.id)
    form.id = String(detail.id)
    form.parentId = String(detail.parentId ?? 0)
    form.name = detail.name
    form.canDelegate = detail.canDelegate ?? 0
    form.sort = detail.sort ?? 0
    form.status = detail.status ?? 0
    form.remark = detail.remark ?? ''
    form.permissionIds = [...detail.permissionIds]
    expandedPermissionIds.value = new Set(
      form.permissionIds.filter(id => (permissionChildrenMap.value.get(id) ?? []).length > 0)
    )
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

async function openDetail(record: RoleListRecord, tab: 'members' | 'permissions' = 'members') {
  detailRole.value = record
  detailTab.value = tab
  detailOpen.value = true
  detailLoading.value = true
  try {
    await roleStore.fetchPermissions()
    const detail = await roleStore.fetchDetail(record.id)
    detailPermissionIds.value = [...detail.permissionIds]
    detailMembers.value = (await roleStore.fetchMembers(record.id)).map(member => ({
      ...member,
      userId: String(member.userId)
    }))
  } catch {
    toast.add({
      title: '加载失败',
      description: '无法获取角色详情',
      color: 'error'
    })
  } finally {
    detailLoading.value = false
  }
}

async function openAddMembers() {
  if (!detailRole.value) {
    return
  }
  selectedUserIds.value = []
  userSearchKeyword.value = ''
  addMemberOpen.value = true
  await searchUsers()
}

async function searchUsers() {
  userSearchLoading.value = true
  try {
    const data = await getUserPage({
      pageNo: 1,
      pageSize: 20,
      keyword: userSearchKeyword.value.trim() || undefined
    })
    const existing = new Set(detailMembers.value.map(item => String(item.userId)))
    userCandidates.value = data.list.filter(item => !existing.has(String(item.id)))
  } catch (error) {
    toast.add({
      title: '加载用户失败',
      description: error instanceof Error ? error.message : '请稍后重试',
      color: 'error'
    })
  } finally {
    userSearchLoading.value = false
  }
}

function toggleCandidate(userId: string, checked: boolean) {
  if (checked) {
    if (!selectedUserIds.value.includes(userId)) {
      selectedUserIds.value.push(userId)
    }
  } else {
    selectedUserIds.value = selectedUserIds.value.filter(id => id !== userId)
  }
}

async function confirmAddMembers() {
  if (!detailRole.value || selectedUserIds.value.length === 0) {
    return
  }
  memberSaving.value = true
  try {
    await roleStore.addMembers(detailRole.value.id, selectedUserIds.value)
    toast.add({ title: '已添加管理员', color: 'success' })
    addMemberOpen.value = false
    detailMembers.value = (await roleStore.fetchMembers(detailRole.value.id)).map(member => ({
      ...member,
      userId: String(member.userId)
    }))
    await loadPage()
  } catch (error) {
    toast.add({
      title: '添加失败',
      description: error instanceof Error ? error.message : '请稍后重试',
      color: 'error'
    })
  } finally {
    memberSaving.value = false
  }
}

async function onRemoveMember(member: RoleAdminSimple) {
  if (!detailRole.value) {
    return
  }
  const ok = await appConfirm({
    title: '移除管理员',
    description: `确定移除管理员「${member.nickname || member.username || member.userId}」？`,
    confirmLabel: '移除'
  })
  if (!ok) {
    return
  }
  try {
    await roleStore.removeMember(detailRole.value.id, String(member.userId))
    toast.add({ title: '已移除', color: 'success' })
    detailMembers.value = (await roleStore.fetchMembers(detailRole.value.id)).map(m => ({
      ...m,
      userId: String(m.userId)
    }))
    await loadPage()
  } catch (error) {
    toast.add({
      title: '移除失败',
      description: error instanceof Error ? error.message : '请稍后重试',
      color: 'error'
    })
  }
}

async function onSubmit() {
  if (!form.name.trim()) {
    toast.add({
      title: '请完善表单',
      description: '角色名称为必填项',
      color: 'error'
    })
    return
  }

  saving.value = true
  try {
    const payload = {
      id: form.id ?? undefined,
      parentId: form.parentId !== '0' ? form.parentId : 0,
      name: form.name.trim(),
      canDelegate: form.canDelegate,
      sort: form.sort,
      status: form.status,
      remark: form.remark.trim() || undefined,
      permissionIds: form.permissionIds
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

  const ok = await appConfirm({
    title: '删除角色',
    description: `确定删除角色「${record.name}」？`,
    confirmLabel: '删除'
  })
  if (!ok) {
    return
  }

  try {
    await roleStore.removeRole(record.id)
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

function toggleExpand(id: number) {
  const next = new Set(expandedPermissionIds.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  expandedPermissionIds.value = next
}

function togglePermission(id: number, checked: boolean) {
  const descendants = collectDescendants(id)
  const related = [id, ...descendants]
  if (checked) {
    const next = new Set(form.permissionIds)
    for (const item of related) {
      next.add(item)
    }
    form.permissionIds = [...next]
    if (descendants.length) {
      const expanded = new Set(expandedPermissionIds.value)
      expanded.add(id)
      expandedPermissionIds.value = expanded
    }
  } else {
    const remove = new Set(related)
    form.permissionIds = form.permissionIds.filter(item => !remove.has(item))
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
      <AdminNavbar title="管理员角色" />
    </template>

    <template #body>
      <div class="space-y-4">
        <AdminPageHeader
          title="管理员角色"
          description="配置角色名称、管理员成员与后台功能权限"
        >
          <template #actions>
            <UButton
              v-if="hasPermission('system:role:create')"
              icon="i-lucide-plus"
              @click="openCreateModal"
            >
              创建角色
            </UButton>
          </template>
        </AdminPageHeader>

        <UCard>
          <div class="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
            <UInput
              v-model="roleStore.keyword"
              placeholder="输入角色名称"
              icon="i-lucide-search"
              class="sm:max-w-xs"
              @keyup.enter="onSearch"
            />
            <UButton color="neutral" variant="soft" :loading="roleStore.loading" @click="onSearch">
              搜索
            </UButton>
          </div>

          <UTable :data="roleStore.list" :columns="columns" :loading="roleStore.loading">
            <template #admins-cell="{ row }">
              <div
                v-if="row.original.admins.length"
                class="flex flex-wrap items-center gap-1"
                @click.stop
              >
                <WorkspaceOwnerChip
                  v-for="admin in row.original.admins"
                  :key="admin.userId"
                  :user-id="String(admin.userId)"
                  :display-name="admin.nickname || admin.username"
                  :avatar-url="admin.avatar"
                />
              </div>
              <span v-else class="text-sm text-muted">暂无</span>
            </template>
            <template #actions-cell="{ row }">
              <div class="flex gap-1">
                <UButton
                  size="xs"
                  color="neutral"
                  variant="ghost"
                  @click="openDetail(row.original, 'members')"
                >
                  详情
                </UButton>
                <UButton
                  v-if="hasPermission('system:role:update')"
                  size="xs"
                  color="neutral"
                  variant="ghost"
                  @click="openDetail(row.original, 'members').then(() => openAddMembers())"
                >
                  添加管理员
                </UButton>
                <UButton
                  v-if="hasPermission('system:role:update') && !row.original.isSystem"
                  size="xs"
                  color="neutral"
                  variant="ghost"
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
            <UFormField label="上级管理员角色" hint="如需创建一级角色，则本字段留空，无需选择上级角色">
              <div class="flex gap-2">
                <AppTreeSelect
                  v-model="parentRoleIdModel"
                  class="flex-1"
                  :items="parentRoleTreeItems"
                  placeholder="请选择（可留空）"
                />
                <UButton
                  v-if="form.parentId !== '0'"
                  color="neutral"
                  variant="soft"
                  @click="form.parentId = '0'"
                >
                  清除
                </UButton>
              </div>
            </UFormField>

            <UFormField label="角色名称" required>
              <UInput v-model="form.name" placeholder="如：部门管理员" />
            </UFormField>

            <UFormField label="功能权限">
              <div v-if="roleStore.permissionsLoading" class="text-sm text-muted">
                加载权限树…
              </div>
              <div v-else class="max-h-60 space-y-1 overflow-y-auto rounded-md border border-default p-3">
                <div
                  v-for="perm in visiblePermissions"
                  :key="perm.id"
                  class="flex items-center gap-2 text-sm"
                  :style="{ paddingLeft: `${perm.depth * 12}px` }"
                >
                  <UButton
                    v-if="(perm.children?.length ?? 0) > 0"
                    size="xs"
                    color="neutral"
                    variant="ghost"
                    square
                    :icon="expandedPermissionIds.has(perm.id) ? 'i-lucide-chevron-down' : 'i-lucide-chevron-right'"
                    @click="toggleExpand(perm.id)"
                  />
                  <span v-else class="inline-block w-7" />
                  <UCheckbox
                    :model-value="form.permissionIds.includes(perm.id)"
                    @update:model-value="togglePermission(perm.id, $event === true)"
                  />
                  <span>{{ perm.name }}</span>
                </div>
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

      <USlideover
        v-model:open="detailOpen"
        side="right"
        title="角色详情"
        :ui="{ content: 'w-full max-w-lg sm:max-w-xl' }"
      >
        <template #body>
          <div v-if="detailRole" class="space-y-4">
            <div class="space-y-1">
              <div class="flex items-center gap-2">
                <h3 class="text-base font-semibold">
                  {{ detailRole.name }}
                </h3>
                <UBadge v-if="detailRole.isSystem" color="primary" variant="subtle" size="sm">
                  系统角色
                </UBadge>
              </div>
              <p class="text-sm text-muted">
                {{ detailRole.permissionSummary }}
              </p>
            </div>

            <div class="flex gap-2 border-b border-default pb-2">
              <UButton
                size="sm"
                :variant="detailTab === 'members' ? 'soft' : 'ghost'"
                color="neutral"
                @click="detailTab = 'members'"
              >
                成员
              </UButton>
              <UButton
                size="sm"
                :variant="detailTab === 'permissions' ? 'soft' : 'ghost'"
                color="neutral"
                @click="detailTab = 'permissions'"
              >
                权限
              </UButton>
            </div>

            <div v-if="detailLoading" class="text-sm text-muted">
              加载中…
            </div>

            <div v-else-if="detailTab === 'members'" class="space-y-3">
              <div class="flex justify-end">
                <UButton
                  v-if="hasPermission('system:role:update')"
                  size="sm"
                  icon="i-lucide-user-plus"
                  @click="openAddMembers"
                >
                  添加管理员
                </UButton>
              </div>
              <div v-if="!detailMembers.length" class="text-sm text-muted">
                暂无管理员
              </div>
              <div
                v-for="member in detailMembers"
                :key="member.userId"
                class="flex items-center justify-between gap-3 rounded-md border border-default px-3 py-2"
              >
                <div class="min-w-0 flex-1">
                  <WorkspaceOwnerChip
                    :user-id="String(member.userId)"
                    :display-name="member.nickname || member.username"
                    :avatar-url="member.avatar"
                    size="sm"
                  />
                  <p class="mt-0.5 truncate pl-0.5 text-xs text-muted">
                    {{ member.deptName || '—' }}
                    <span v-if="member.mobile"> · {{ member.mobile }}</span>
                  </p>
                </div>
                <UButton
                  v-if="hasPermission('system:role:update')"
                  size="xs"
                  color="error"
                  variant="ghost"
                  @click="onRemoveMember(member)"
                >
                  移除
                </UButton>
              </div>
            </div>

            <div v-else class="space-y-2">
              <div v-if="!detailPermissionRows.length" class="text-sm text-muted">
                暂无权限
              </div>
              <div
                v-for="perm in detailPermissionRows"
                :key="perm.id"
                class="text-sm"
                :style="{ paddingLeft: `${perm.depth * 12}px` }"
              >
                {{ perm.name }}
              </div>
              <UButton
                v-if="hasPermission('system:role:update') && detailRole && !detailRole.isSystem"
                size="sm"
                color="neutral"
                variant="soft"
                class="mt-2"
                @click="detailOpen = false; openEditModal(detailRole)"
              >
                编辑权限
              </UButton>
            </div>
          </div>
        </template>
      </USlideover>

      <UModal v-model:open="addMemberOpen" title="添加管理员">
        <template #body>
          <div class="space-y-3">
            <div class="flex gap-2">
              <UInput
                v-model="userSearchKeyword"
                placeholder="搜索姓名 / 用户名"
                class="flex-1"
                @keyup.enter="searchUsers"
              />
              <UButton color="neutral" variant="soft" :loading="userSearchLoading" @click="searchUsers">
                搜索
              </UButton>
            </div>
            <div class="max-h-64 space-y-1 overflow-y-auto rounded-md border border-default p-2">
              <div v-if="!userCandidates.length" class="px-2 py-4 text-center text-sm text-muted">
                暂无候选用户
              </div>
              <label
                v-for="user in userCandidates"
                :key="user.id"
                class="flex items-center gap-2 rounded-md px-2 py-1.5 text-sm hover:bg-elevated"
              >
                <UCheckbox
                  :model-value="selectedUserIds.includes(String(user.id))"
                  @update:model-value="toggleCandidate(String(user.id), $event === true)"
                />
                <span>{{ user.nickname || user.username }}</span>
                <span class="text-xs text-muted">{{ user.dept || '' }}</span>
              </label>
            </div>
          </div>
        </template>
        <template #footer>
          <div class="flex justify-end gap-2">
            <UButton color="neutral" variant="ghost" @click="addMemberOpen = false">
              取消
            </UButton>
            <UButton
              :loading="memberSaving"
              :disabled="selectedUserIds.length === 0"
              @click="confirmAddMembers"
            >
              添加
            </UButton>
          </div>
        </template>
      </UModal>
    </template>
  </UDashboardPanel>
</template>
