<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AdminNavbar from '../../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../../components/admin/AdminPageHeader.vue'

const router = useRouter()
const toast = useToast()
const loading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  dept: '总部',
  password: ''
})

const deptOptions = ['总部', '研发部', '产品部', '运营部', '人事部', '财务部']

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
  await new Promise(resolve => setTimeout(resolve, 400))
  loading.value = false

  toast.add({
    title: '保存成功',
    description: '用户已创建（Mock）',
    color: 'success'
  })
  await router.push('/admin/system/user')
}
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
      <div class="mx-auto max-w-2xl space-y-6 p-4 sm:p-6">
        <AdminPageHeader
          title="新建用户"
          description="填写基本信息并分配部门"
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
                <UInput v-model="form.username" placeholder="zhangsan" />
              </UFormField>
              <UFormField label="昵称">
                <UInput v-model="form.nickname" placeholder="张三" />
              </UFormField>
              <UFormField label="密码" required>
                <UInput v-model="form.password" type="password" placeholder="••••••••" />
              </UFormField>
            </div>
          </UCard>

          <UCard>
            <template #header>
              <h3 class="font-medium">
                归属部门
              </h3>
            </template>

            <UFormField label="部门">
              <USelectMenu
                v-model="form.dept"
                :items="deptOptions"
              />
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
