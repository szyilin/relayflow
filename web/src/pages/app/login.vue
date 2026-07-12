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

async function completeLogin(mobile: string, password: string, selectedTenantId?: number) {
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

  toast.add({ title: '登录失败', description: result.message, color: 'error' })
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
  <div class="w-full max-w-md space-y-6">
    <div class="space-y-2 text-center">
      <div class="mx-auto flex size-14 items-center justify-center rounded-2xl bg-primary text-white shadow-lg">
        <UIcon name="i-lucide-workflow" class="size-7" />
      </div>
      <h1 class="text-2xl font-semibold">
        {{ isAddAccount ? '登录更多账号' : 'RelayFlow 工作台' }}
      </h1>
      <p class="text-sm text-muted">
        {{ isAddAccount
          ? '使用另一个账号登录，将添加到左下角账号列表，方便随时切换'
          : '员工协作入口 —— 消息、任务、文档一处搞定' }}
      </p>
    </div>

    <UCard v-if="tenantSelection" class="ring-1 ring-default">
      <div class="space-y-4">
        <div class="space-y-1">
          <h2 class="text-lg font-semibold">
            选择要进入的企业
          </h2>
          <p class="text-sm text-muted">
            你的账号关联了多个企业，请选择本次要进入的工作台
          </p>
        </div>

        <div class="space-y-2">
          <UButton
            v-for="tenant in tenantSelection"
            :key="tenant.tenantId"
            block
            color="neutral"
            variant="soft"
            class="justify-start"
            :loading="loading"
            @click="onSelectTenant(tenant)"
          >
            <UIcon name="i-lucide-building-2" class="size-4" />
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
    </UCard>

    <UCard v-else class="ring-1 ring-default">
      <form class="space-y-4" @submit.prevent="onSubmit">
        <UFormField label="手机号">
          <UInput
            v-model="form.mobile"
            placeholder="11 位手机号，可分段输入"
            icon="i-lucide-smartphone"
            inputmode="tel"
            autocomplete="tel"
          />
        </UFormField>
        <UFormField label="密码">
          <UInput
            v-model="form.password"
            type="password"
            placeholder="••••••••"
            icon="i-lucide-lock"
            autocomplete="current-password"
          />
        </UFormField>
        <UButton type="submit" block :loading="loading">
          进入工作台
        </UButton>
      </form>
    </UCard>

    <p v-if="!tenantSelection" class="text-center text-xs text-muted">
      使用手机号登录；管理员与普通员工同一入口，权限由系统分配
    </p>
    <p v-if="!tenantSelection" class="text-center text-sm text-muted">
      <RouterLink :to="registerTo" class="text-primary hover:underline">
        没有账号？注册
      </RouterLink>
    </p>
  </div>
</template>
