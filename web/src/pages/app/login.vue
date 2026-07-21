<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { TenantSummary } from '../../api/app/tenant'
import { useAuthStore } from '../../stores/auth'
import { isValidMobile, normalizeMobile } from '../../utils/mobile'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const toast = useToast()
const loading = ref(false)
const isAddAccount = computed(() => route.query.addAccount === '1')
const registerTo = computed(() =>
  isAddAccount.value ? { path: '/app/register', query: { addAccount: '1' } } : '/app/register'
)
const tenantSelection = ref<TenantSummary[] | null>(null)
const pendingCredentials = ref<{ mobile: string, password: string } | null>(null)

const form = reactive({
  mobile: import.meta.env.DEV ? '19988888888' : '',
  password: import.meta.env.DEV ? '123456' : ''
})

const pageTitle = computed(() => {
  if (tenantSelection.value) {
    return '选择企业'
  }
  return isAddAccount.value ? '登录更多账号' : '登录'
})

const pageSubline = computed(() => {
  if (tenantSelection.value) {
    return '你的账号关联了多个企业，请选择本次要进入的工作台'
  }
  return isAddAccount.value
    ? '使用另一账号登录，将添加到本机账号列表'
    : '使用手机号进入 RelayFlow 工作台'
})

async function completeLogin(mobile: string, password: string, selectedTenantId?: string) {
  const result = await authStore.login(mobile, password, selectedTenantId)
  if (result.ok) {
    tenantSelection.value = null
    pendingCredentials.value = null
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/app/messages'
    await router.replace(redirect)
    return
  }

  if ('needTenantSelection' in result && result.needTenantSelection) {
    tenantSelection.value = result.tenants
    pendingCredentials.value = result.credentials
    return
  }

  if ('message' in result) {
    toast.add({ title: '登录失败', description: result.message, color: 'error' })
  }
}

async function onSubmit() {
  const mobile = normalizeMobile(form.mobile.trim())
  const password = form.password.trim()

  if (!mobile || !password) {
    toast.add({ title: '请输入手机号和密码', color: 'error' })
    return
  }
  if (!isValidMobile(form.mobile)) {
    toast.add({ title: '请输入 11 位手机号', color: 'error' })
    return
  }

  loading.value = true
  try {
    await completeLogin(mobile, password)
  } finally {
    loading.value = false
  }
}

async function onSelectTenant(tenant: TenantSummary) {
  if (!pendingCredentials.value) {
    return
  }

  loading.value = true
  try {
    await completeLogin(
      pendingCredentials.value.mobile,
      pendingCredentials.value.password,
      tenant.tenantId
    )
  } finally {
    loading.value = false
  }
}

function cancelTenantSelection() {
  tenantSelection.value = null
  pendingCredentials.value = null
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
      <h2>{{ pageTitle }}</h2>
      <p>{{ pageSubline }}</p>
    </header>

    <div v-if="tenantSelection" class="space-y-4">
      <div class="workspace-auth-tenant-list">
        <UButton
          v-for="tenant in tenantSelection"
          :key="tenant.tenantId"
          block
          color="neutral"
          variant="soft"
          size="lg"
          class="justify-start"
          :loading="loading"
          @click="onSelectTenant(tenant)"
        >
          <UIcon name="i-lucide-building-2" class="size-4 shrink-0" />
          <span class="truncate">{{ tenant.tenantName }}</span>
          <UBadge v-if="tenant.owner" color="primary" variant="subtle" class="ml-auto">
            我创建的
          </UBadge>
        </UButton>
      </div>

      <UButton block color="neutral" variant="ghost" :disabled="loading" @click="cancelTenantSelection">
        返回重新登录
      </UButton>
    </div>

    <form v-else class="workspace-auth-form" @submit.prevent="onSubmit">
      <UFormField label="手机号">
        <UInput
          v-model="form.mobile"
          size="lg"
          placeholder="11 位手机号"
          icon="i-lucide-smartphone"
          inputmode="tel"
          autocomplete="tel"
          class="w-full"
        />
      </UFormField>
      <UFormField label="密码">
        <UInput
          v-model="form.password"
          size="lg"
          type="password"
          placeholder="请输入密码"
          icon="i-lucide-lock"
          autocomplete="current-password"
          class="w-full"
        />
      </UFormField>
      <UButton type="submit" block size="lg" class="mt-1" :loading="loading">
        进入工作台
      </UButton>
    </form>

    <p v-if="!tenantSelection" class="workspace-auth-footer">
      没有账号？
      <RouterLink :to="registerTo">
        注册
      </RouterLink>
    </p>
  </div>
</template>
