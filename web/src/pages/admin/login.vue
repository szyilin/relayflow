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
  username: '',
  password: ''
})

async function onSubmit() {
  loading.value = true
  try {
    const result = await authStore.login(form.username, form.password)
    if (!result.ok) {
      toast.add({
        title: '登录失败',
        description: result.message,
        color: 'error'
      })
      return
    }

    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/admin'
    await router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<route lang="yaml">
meta:
  layout: auth
</route>

<template>
  <div class="w-full max-w-sm space-y-6">
    <div class="space-y-2 lg:hidden">
      <div class="flex items-center gap-2 text-primary">
        <UIcon name="i-lucide-workflow" class="size-6" />
        <span class="font-semibold">RelayFlow</span>
      </div>
      <h1 class="text-2xl font-semibold">
        管理控制台
      </h1>
    </div>

    <UCard>
      <template #header>
        <div class="space-y-1">
          <h2 class="text-lg font-semibold">
            登录
          </h2>
          <p class="text-sm text-muted">
            使用管理员账号进入控制台
          </p>
        </div>
      </template>

      <form class="space-y-4" @submit.prevent="onSubmit">
        <UFormField label="用户名" required>
          <UInput
            v-model="form.username"
            placeholder="admin"
            autocomplete="username"
            icon="i-lucide-user"
          />
        </UFormField>

        <UFormField label="密码" required>
          <UInput
            v-model="form.password"
            type="password"
            placeholder="••••••••"
            autocomplete="current-password"
            icon="i-lucide-lock"
          />
        </UFormField>

        <UButton
          type="submit"
          block
          :loading="loading"
        >
          登录
        </UButton>
      </form>
    </UCard>

    <p class="text-center text-xs text-muted">
      使用管理员账号登录 RelayFlow 控制台
    </p>
  </div>
</template>
