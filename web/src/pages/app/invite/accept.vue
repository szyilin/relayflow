<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { acceptMemberInvite, previewMemberInvite } from '../../../api/app/member-invite'
import { ApiError } from '../../../api/request'
import { useAuthStore } from '../../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const toast = useToast()

const loading = ref(false)
const previewLoading = ref(false)
const preview = ref<{ tenantName: string, nickname: string } | null>(null)

const form = reactive({
  mobile: '',
  password: '',
  confirmPassword: ''
})

async function loadPreview() {
  const mobile = form.mobile.trim()
  if (!mobile) {
    preview.value = null
    return
  }

  previewLoading.value = true
  try {
    const data = await previewMemberInvite(mobile)
    preview.value = {
      tenantName: data.tenantName,
      nickname: data.nickname
    }
  } catch (error) {
    preview.value = null
    if (error instanceof ApiError) {
      toast.add({ title: '未找到邀请', description: error.message, color: 'warning' })
    }
  } finally {
    previewLoading.value = false
  }
}

async function onSubmit() {
  const mobile = form.mobile.trim()
  const password = form.password.trim()
  const confirmPassword = form.confirmPassword.trim()

  if (!mobile) {
    toast.add({ title: '请输入手机号', color: 'error' })
    return
  }
  if (password.length < 6) {
    toast.add({ title: '密码至少 6 位', color: 'error' })
    return
  }
  if (password !== confirmPassword) {
    toast.add({ title: '两次密码不一致', color: 'error' })
    return
  }

  loading.value = true
  try {
    const data = await acceptMemberInvite({ mobile, password })
    await authStore.establishSession(data.accessToken, data.tenantId, mobile)
    toast.add({ title: '已加入组织', description: preview.value?.tenantName ?? '欢迎加入', color: 'success' })
    await router.replace('/app/messages')
  } catch (error) {
    const message = error instanceof ApiError
      ? error.message
      : '加入失败，请稍后重试'
    toast.add({ title: '加入失败', description: message, color: 'error' })
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
        <UIcon name="i-lucide-user-plus" class="size-7" />
      </div>
      <h1 class="text-2xl font-semibold">
        接受组织邀请
      </h1>
      <p class="text-sm text-muted">
        使用邀请手机号设置密码，加入企业并开始协作
      </p>
    </div>

    <UCard class="ring-1 ring-default">
      <form class="space-y-4" @submit.prevent="onSubmit">
        <UFormField label="手机号">
          <UInput
            v-model="form.mobile"
            placeholder="邀请时填写的手机号"
            icon="i-lucide-smartphone"
            inputmode="tel"
            @blur="loadPreview"
          />
        </UFormField>

        <div
          v-if="preview"
          class="rounded-lg border border-default bg-elevated/50 px-4 py-3 text-sm"
        >
          <p class="font-medium text-highlighted">
            {{ preview.tenantName }}
          </p>
          <p class="mt-1 text-muted">
            邀请你以「{{ preview.nickname }}」身份加入
          </p>
        </div>

        <UFormField label="设置密码">
          <UInput
            v-model="form.password"
            type="password"
            placeholder="至少 6 位"
            icon="i-lucide-lock"
          />
        </UFormField>
        <UFormField label="确认密码">
          <UInput
            v-model="form.confirmPassword"
            type="password"
            placeholder="再次输入密码"
            icon="i-lucide-lock-keyhole"
          />
        </UFormField>

        <UButton type="submit" block :loading="loading || previewLoading">
          加入组织
        </UButton>
      </form>
    </UCard>

    <p class="text-center text-sm text-muted">
      已有账号？
      <RouterLink to="/app/login" class="text-primary hover:underline">
        返回登录
      </RouterLink>
    </p>
  </div>
</template>
