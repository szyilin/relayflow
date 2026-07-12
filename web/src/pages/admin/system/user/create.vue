<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminNavbar from '../../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../../components/admin/AdminPageHeader.vue'
import { getRolePage } from '../../../../api/admin/role'
import { useDeptStore } from '../../../../stores/dept'
import { useUserStore } from '../../../../stores/user'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const userStore = useUserStore()
const deptStore = useDeptStore()

const loading = ref(false)
const optionsLoading = ref(false)
const roleOptions = ref<Array<{ id: string, label: string }>>([])

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  mobile: '',
  email: '',
  deptId: null as string | null,
  roleIds: [] as string[]
})

const deptOptions = computed(() =>
  deptStore.list.map(item => ({
    label: item.name,
    value: item.id
  }))
)

function toggleRole(roleId: string, checked: boolean) {
  if (checked) {
    if (!form.roleIds.includes(roleId)) {
      form.roleIds.push(roleId)
    }
    return
  }
  form.roleIds = form.roleIds.filter(id => id !== roleId)
}

async function loadOptions() {
  optionsLoading.value = true
  try {
    await deptStore.fetchList()
    const queryDeptId = typeof route.query.deptId === 'string' ? route.query.deptId : null
    if (queryDeptId && deptStore.list.some(item => item.id === queryDeptId)) {
      form.deptId = queryDeptId
    } else {
      form.deptId = deptStore.rootDeptId() ?? null
    }
    const roles = await getRolePage({ pageNo: 1, pageSize: 100 })
    roleOptions.value = roles.list.map(item => ({
      id: String(item.id),
      label: item.name
    }))
  } catch {
    toast.add({
      title: '加载选项失败',
      description: '无法获取部门或角色列表',
      color: 'error'
    })
  } finally {
    optionsLoading.value = false
  }
}

async function onSubmit() {
  if (!form.username.trim() || !form.password.trim()) {
    toast.add({
      title: '请完善表单',
      description: '用户名和密码为必填项',
      color: 'error'
    })
    return
  }

  loading.value = true
  try {
    await userStore.create({
      username: form.username.trim(),
      password: form.password,
      nickname: form.nickname.trim() || undefined,
      mobile: form.mobile.trim() || undefined,
      email: form.email.trim() || undefined,
      deptId: form.deptId,
      roleIds: form.roleIds
    })
    toast.add({ title: '用户已创建', color: 'success' })
    await router.push('/admin/system/user')
  } catch (error) {
    toast.add({
      title: '创建失败',
      description: error instanceof Error ? error.message : '请稍后重试',
      color: 'error'
    })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadOptions()
})
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-system-user-create">
    <template #header>
      <AdminNavbar title="新建用户" />
    </template>

    <template #body>
      <div class="mx-auto max-w-2xl space-y-6">
        <AdminPageHeader
          title="新建用户"
          description="填写基本信息并分配部门与角色"
        />

        <form class="space-y-6" @submit.prevent="onSubmit">
          <UCard>
            <template #header>
              <h3 class="font-medium">
                基本信息
              </h3>
            </template>

            <div class="space-y-4">
              <UFormField label="用户名" required>
                <UInput v-model="form.username" placeholder="zhangsan" autocomplete="off" />
              </UFormField>
              <UFormField label="昵称">
                <UInput v-model="form.nickname" placeholder="张三" />
              </UFormField>
              <UFormField label="密码" required>
                <UInput v-model="form.password" type="password" placeholder="••••••••" autocomplete="new-password" />
              </UFormField>
              <UFormField label="手机号">
                <UInput v-model="form.mobile" placeholder="13800138000" />
              </UFormField>
              <UFormField label="邮箱">
                <UInput v-model="form.email" type="email" placeholder="zhangsan@example.com" />
              </UFormField>
            </div>
          </UCard>

          <UCard :ui="{ body: 'space-y-4' }">
            <template #header>
              <h3 class="font-medium">
                归属与权限
              </h3>
            </template>

            <UFormField label="主部门">
              <USelectMenu
                v-model="form.deptId"
                :items="deptOptions"
                value-key="value"
                label-key="label"
                placeholder="选择部门"
                :loading="optionsLoading"
                searchable
              />
            </UFormField>

            <UFormField label="角色">
              <div v-if="optionsLoading" class="text-sm text-muted">
                加载中…
              </div>
              <div v-else-if="roleOptions.length === 0" class="text-sm text-muted">
                暂无可用角色
              </div>
              <div v-else class="space-y-2">
                <label
                  v-for="role in roleOptions"
                  :key="role.id"
                  class="flex items-center gap-2 text-sm"
                >
                  <UCheckbox
                    :model-value="form.roleIds.includes(role.id)"
                    @update:model-value="toggleRole(role.id, $event === true)"
                  />
                  <span>{{ role.label }}</span>
                </label>
              </div>
            </UFormField>
          </UCard>

          <div class="flex gap-2">
            <UButton type="submit" :loading="loading">
              保存
            </UButton>
            <UButton
              color="neutral"
              variant="ghost"
              to="/admin/system/user"
            >
              取消
            </UButton>
          </div>
        </form>
      </div>
    </template>
  </UDashboardPanel>
</template>
