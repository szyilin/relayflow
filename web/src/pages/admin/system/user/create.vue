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
  nickname: '',
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
  const mobile = form.mobile.trim()
  if (!mobile) {
    toast.add({
      title: '请完善表单',
      description: '手机号为必填项',
      color: 'error'
    })
    return
  }

  loading.value = true
  try {
    await userStore.invite({
      mobile,
      nickname: form.nickname.trim() || undefined,
      email: form.email.trim() || undefined,
      deptId: form.deptId,
      roleIds: form.roleIds
    })
    toast.add({
      title: '邀请已发送',
      description: '对方同意后将加入本组织',
      color: 'success'
    })
    await router.push('/admin/system/user')
  } catch (error) {
    toast.add({
      title: '邀请失败',
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
  <UDashboardPanel id="admin-system-user-invite">
    <template #header>
      <AdminNavbar title="邀请成员" />
    </template>

    <template #body>
      <div class="mx-auto w-full max-w-4xl space-y-6">
        <AdminPageHeader
          title="邀请成员"
          description="通过手机号邀请成员加入本组织。账号与密码由成员自行注册管理，组织仅可设置其在本企业内的信息。"
        />

        <form class="space-y-6" @submit.prevent="onSubmit">
          <UCard :ui="{ body: 'space-y-6' }">
            <template #header>
              <h3 class="font-medium">
                基础信息
              </h3>
            </template>

            <div class="grid grid-cols-1 gap-6 md:grid-cols-2">
              <UFormField label="姓名" class="md:col-span-1">
                <UInput v-model="form.nickname" placeholder="张三" size="lg" />
              </UFormField>

              <UFormField label="手机号" required class="md:col-span-1">
                <UInput
                  v-model="form.mobile"
                  placeholder="13800138000"
                  autocomplete="off"
                  size="lg"
                />
                <template #hint>
                  <span class="text-muted">成员可用手机号登录平台；邀请将发送至该手机号</span>
                </template>
              </UFormField>

              <UFormField label="工作邮箱" class="md:col-span-2">
                <UInput
                  v-model="form.email"
                  type="email"
                  placeholder="zhangsan@example.com"
                  size="lg"
                />
                <template #hint>
                  <span class="text-muted">请为成员分配本企业内使用的工作邮箱（可选）</span>
                </template>
              </UFormField>
            </div>
          </UCard>

          <UCard :ui="{ body: 'space-y-6' }">
            <template #header>
              <h3 class="font-medium">
                归属与权限
              </h3>
            </template>

            <div class="grid grid-cols-1 gap-6 md:grid-cols-2">
              <UFormField label="主部门" required class="md:col-span-1">
                <USelectMenu
                  v-model="form.deptId"
                  :items="deptOptions"
                  value-key="value"
                  label-key="label"
                  placeholder="选择部门"
                  :loading="optionsLoading"
                  searchable
                  size="lg"
                />
              </UFormField>

              <UFormField label="角色" class="md:col-span-2">
                <div v-if="optionsLoading" class="text-sm text-muted">
                  加载中…
                </div>
                <div v-else-if="roleOptions.length === 0" class="text-sm text-muted">
                  暂无可用角色
                </div>
                <div v-else class="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <label
                    v-for="role in roleOptions"
                    :key="role.id"
                    class="flex items-center gap-2 rounded-lg border border-default px-3 py-2 text-sm"
                  >
                    <UCheckbox
                      :model-value="form.roleIds.includes(role.id)"
                      @update:model-value="toggleRole(role.id, $event === true)"
                    />
                    <span>{{ role.label }}</span>
                  </label>
                </div>
              </UFormField>
            </div>
          </UCard>

          <div class="flex gap-3 border-t border-default pt-4">
            <UButton type="submit" size="lg" :loading="loading">
              发送邀请
            </UButton>
            <UButton
              color="neutral"
              variant="ghost"
              size="lg"
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
