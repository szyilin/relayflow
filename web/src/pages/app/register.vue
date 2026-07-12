<script setup lang="ts">
import { computed, onBeforeMount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDebounceFn } from '@vueuse/core'
import { useAuthStore } from '../../stores/auth'
import { useNotifyStore } from '../../stores/notify'
import { isValidMobile, normalizeMobile } from '../../utils/mobile'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const notifyStore = useNotifyStore()
const isAddAccount = computed(() => route.query.addAccount === '1')
const loginTo = computed(() =>
  authStore.isAuthenticated || isAddAccount.value
    ? { path: '/app/login', query: { addAccount: '1' } }
    : '/app/login'
)
const toast = useToast()
const loading = ref(false)

const form = reactive({
  mobile: typeof route.query.mobile === 'string' ? route.query.mobile : '',
  password: '',
  confirmPassword: '',
  nickname: '',
  tenantName: ''
})

const pendingInvites = computed(() => notifyStore.pendingItems)

const loadPendingInvites = useDebounceFn(async () => {
  if (!isValidMobile(form.mobile)) {
    notifyStore.clearPending()
    return
  }
  await notifyStore.fetchPendingByMobile(normalizeMobile(form.mobile.trim()))
}, 400)

watch(() => form.mobile, () => {
  void loadPendingInvites()
})

onBeforeMount(() => {
  if (typeof route.query.mobile === 'string' && !form.mobile) {
    form.mobile = route.query.mobile
  }
  void loadPendingInvites()
})

async function onSubmit() {
  const mobile = normalizeMobile(form.mobile.trim())
  const password = form.password.trim()
  const confirmPassword = form.confirmPassword.trim()
  const nickname = form.nickname.trim()
  const tenantName = form.tenantName.trim()

  if (!mobile) {
    toast.add({ title: '请输入手机号', color: 'error' })
    return
  }
  if (!isValidMobile(form.mobile)) {
    toast.add({ title: '请输入 11 位手机号', color: 'error' })
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
  if (!nickname) {
    toast.add({ title: '请输入昵称', color: 'error' })
    return
  }
  if (!tenantName) {
    toast.add({ title: '请输入企业名称', color: 'error' })
    return
  }

  loading.value = true
  try {
    const result = await authStore.register({
      mobile,
      password,
      nickname,
      tenantName
    })
    if (!result.ok) {
      toast.add({ title: '注册失败', description: result.message, color: 'error' })
      return
    }
    toast.add({ title: '注册成功', description: `欢迎加入 ${tenantName}`, color: 'success' })
    await router.replace('/app/messages')
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
        <UIcon name="i-lucide-building-2" class="size-7" />
      </div>
      <h1 class="text-2xl font-semibold">
        注册 RelayFlow
      </h1>
      <p class="text-sm text-muted">
        创建账号与企业，开始协作；被邀请的手机号注册后将一并激活待加入组织
      </p>
    </div>

    <UAlert
      v-if="pendingInvites.length"
      color="primary"
      variant="subtle"
      icon="i-lucide-mail-plus"
      :title="`你收到 ${pendingInvites.length} 个企业邀请`"
      description="注册后将自动加入以下组织（同时可创建自己的企业）"
      class="text-left"
    >
      <ul class="mt-2 space-y-1 text-sm">
        <li v-for="item in pendingInvites" :key="item.tenantId" class="flex items-center gap-2">
          <UIcon name="i-lucide-building-2" class="size-4 shrink-0 opacity-70" />
          <span>{{ item.tenantName }}</span>
        </li>
      </ul>
    </UAlert>

    <UCard class="ring-1 ring-default">
      <form class="space-y-4" @submit.prevent="onSubmit">
        <UFormField label="手机号">
          <UInput
            v-model="form.mobile"
            placeholder="11 位手机号，可分段输入"
            icon="i-lucide-smartphone"
            inputmode="tel"
          />
        </UFormField>
        <UFormField label="昵称">
          <UInput
            v-model="form.nickname"
            placeholder="在组织内的展示名称"
            icon="i-lucide-user"
          />
        </UFormField>
        <UFormField label="企业名称">
          <UInput
            v-model="form.tenantName"
            placeholder="你创建的企业或团队名称"
            icon="i-lucide-building-2"
          />
        </UFormField>
        <UFormField label="密码">
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
        <UButton type="submit" block :loading="loading">
          注册并进入工作台
        </UButton>
      </form>
    </UCard>

    <p class="text-center text-sm text-muted">
      已有账号？
      <RouterLink :to="loginTo" class="text-primary hover:underline">
        返回登录
      </RouterLink>
    </p>
  </div>
</template>
