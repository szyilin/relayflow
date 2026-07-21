<script setup lang="ts">
import { computed, onBeforeMount, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { isValidMobile, normalizeMobile } from '../../utils/mobile'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
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

onBeforeMount(() => {
  if (typeof route.query.mobile === 'string' && !form.mobile) {
    form.mobile = route.query.mobile
  }
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
    const invitedNames = result.invitedTenantNames
    const description = invitedNames.length
      ? `已创建 ${tenantName}，并加入 ${invitedNames.join('、')}`
      : `欢迎加入 ${tenantName}`
    toast.add({ title: '注册成功', description, color: 'success' })
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
  <div class="w-full max-w-[22rem]">
    <div class="workspace-auth-mobile-brand">
      <span class="workspace-auth-mobile-icon">
        <UIcon name="i-lucide-workflow" class="size-4" />
      </span>
      <span class="workspace-auth-mobile-name">RelayFlow</span>
    </div>

    <header class="workspace-auth-heading">
      <h2>注册</h2>
      <p>创建账号与企业，开始协作</p>
    </header>

    <form class="workspace-auth-form" @submit.prevent="onSubmit">
      <UFormField label="手机号">
        <UInput
          v-model="form.mobile"
          size="lg"
          placeholder="11 位手机号"
          icon="i-lucide-smartphone"
          inputmode="tel"
          class="w-full"
        />
      </UFormField>
      <UFormField label="昵称">
        <UInput
          v-model="form.nickname"
          size="lg"
          placeholder="组织内展示名称"
          icon="i-lucide-user"
          class="w-full"
        />
      </UFormField>
      <UFormField label="企业名称">
        <UInput
          v-model="form.tenantName"
          size="lg"
          placeholder="企业或团队名称"
          icon="i-lucide-building-2"
          class="w-full"
        />
      </UFormField>
      <UFormField label="密码">
        <UInput
          v-model="form.password"
          size="lg"
          type="password"
          placeholder="至少 6 位"
          icon="i-lucide-lock"
          class="w-full"
        />
      </UFormField>
      <UFormField label="确认密码">
        <UInput
          v-model="form.confirmPassword"
          size="lg"
          type="password"
          placeholder="再次输入密码"
          icon="i-lucide-lock-keyhole"
          class="w-full"
        />
      </UFormField>
      <UButton type="submit" block size="lg" class="mt-1" :loading="loading">
        注册并进入工作台
      </UButton>
    </form>

    <p class="workspace-auth-footer">
      已有账号？
      <RouterLink :to="loginTo">
        返回登录
      </RouterLink>
    </p>
  </div>
</template>
