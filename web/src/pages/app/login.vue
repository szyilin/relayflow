<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const toast = useToast()
const loading = ref(false)
const form = reactive({
  username: import.meta.env.DEV ? 'admin' : '',
  password: import.meta.env.DEV ? 'admin123' : ''
})

async function onSubmit() {
  loading.value = true
  try {
    const result = await authStore.login(form.username, form.password)
    if (!result.ok) {
      toast.add({ title: '登录失败', description: result.message, color: 'error' })
      return
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/app/messages'
    await router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<route lang="yaml">
meta:
  layout: workspace-auth
</route>

<template>
  <div class="w-full max-w-md space-y-6">
    <div class="space-y-2 text-center">
      <div class="mx-auto flex size-14 items-center justify-center rounded-2xl bg-primary text-white shadow-lg">
        <UIcon name="i-lucide-workflow" class="size-7" />
      </div>
      <h1 class="text-2xl font-semibold">
        RelayFlow 工作台
      </h1>
      <p class="text-sm text-muted">
        员工协作入口 —— 消息、任务、文档一处搞定
      </p>
    </div>

    <UCard class="ring-1 ring-default">
      <form class="space-y-4" @submit.prevent="onSubmit">
        <UFormField label="账号">
          <UInput v-model="form.username" placeholder="yourname" icon="i-lucide-user" />
        </UFormField>
        <UFormField label="密码">
          <UInput v-model="form.password" type="password" placeholder="••••••••" icon="i-lucide-lock" />
        </UFormField>
        <UButton type="submit" block :loading="loading">
          进入工作台
        </UButton>
      </form>
    </UCard>

    <p class="text-center text-xs text-muted">
      使用企业账号登录；管理员与普通员工同一入口，权限由系统分配
    </p>
    <p class="text-center text-sm text-muted">
      <RouterLink to="/app/invite/accept" class="text-primary hover:underline">
        收到邀请？设置密码加入
      </RouterLink>
    </p>
  </div>
</template>
