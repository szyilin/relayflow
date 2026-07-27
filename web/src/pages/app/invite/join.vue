<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../../stores/auth'
import { useOrgInviteStore } from '../../../stores/orgInvite'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const orgInvite = useOrgInviteStore()
const toast = useToast()

const codeInput = ref('')
const resultMessage = ref<string | null>(null)
const resultOk = ref(false)
const busy = ref(false)

const tokenFromQuery = computed(() =>
  typeof route.query.t === 'string' ? route.query.t : '')

const codeFromQuery = computed(() =>
  typeof route.query.code === 'string' ? route.query.code : '')

async function tryConsume(kind: 'token' | 'code', value: string) {
  busy.value = true
  resultMessage.value = null
  try {
    if (!auth.isAuthenticated) {
      await router.replace({
        path: '/app/login',
        query: { redirect: route.fullPath }
      })
      return
    }
    const result = orgInvite.consumeDraft(kind, value)
    resultOk.value = result.ok
    resultMessage.value = result.message
    toast.add({
      title: result.ok ? '加入结果' : '无法加入',
      description: result.message,
      color: result.ok ? 'success' : 'error'
    })
  } finally {
    busy.value = false
  }
}

function submitCode() {
  void tryConsume('code', codeInput.value)
}

onMounted(() => {
  if (tokenFromQuery.value) {
    void tryConsume('token', tokenFromQuery.value)
    return
  }
  if (codeFromQuery.value) {
    codeInput.value = codeFromQuery.value
    void tryConsume('code', codeFromQuery.value)
  }
})
</script>

<route lang="yaml">
meta:
  layout: workspace
</route>

<template>
  <div class="flex min-h-[70vh] items-center justify-center p-6">
    <UCard class="w-full max-w-md">
      <template #header>
        <div>
          <h1 class="text-base font-semibold">
            加入企业
          </h1>
          <p class="mt-1 text-sm text-[var(--ws-text-muted)]">
            通过邀请链接或邀请码加入当前会话中的企业（`-web` 草稿校验）
          </p>
        </div>
      </template>

      <div class="space-y-4">
        <div
          v-if="tokenFromQuery"
          class="rounded-lg bg-[var(--ws-input-bar-bg)]/50 p-3 text-xs text-[var(--ws-text-muted)]"
        >
          链接 token：
          <span class="font-mono break-all text-[var(--ws-text)]">{{ tokenFromQuery }}</span>
        </div>

        <UFormField label="企业邀请码">
          <UInput
            v-model="codeInput"
            placeholder="输入邀请码"
            maxlength="16"
            @keydown.enter.prevent="submitCode"
          />
        </UFormField>

        <p
          v-if="resultMessage"
          class="text-sm"
          :class="resultOk ? 'text-success' : 'text-error'"
        >
          {{ resultMessage }}
        </p>

        <div class="flex justify-end gap-2">
          <UButton
            color="neutral"
            variant="ghost"
            @click="router.push('/app/messages')"
          >
            返回工作台
          </UButton>
          <UButton
            :loading="busy"
            @click="submitCode"
          >
            加入
          </UButton>
        </div>
      </div>
    </UCard>
  </div>
</template>
