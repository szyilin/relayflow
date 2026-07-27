<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useOrgInviteStore } from '../../stores/orgInvite'

const open = defineModel<boolean>('open', { required: true })
const toast = useToast()
const orgInvite = useOrgInviteStore()

type InviteTab = 'qr' | 'link' | 'code' | 'mobile'

const tab = ref<InviteTab>('qr')
const mobile = ref('')

const tabs = [
  { value: 'qr' as const, label: '企业二维码' },
  { value: 'link' as const, label: '企业链接' },
  { value: 'code' as const, label: '企业邀请码' },
  { value: 'mobile' as const, label: '手机号' }
]

const inviteLink = computed(() => orgInvite.inviteLink())
const modeHint = computed(() => {
  const mode = orgInvite.inviteMode
  if (mode === 'OFF') {
    return '当前企业已关闭邀请'
  }
  if (mode === 'ALL_MEMBERS') {
    return '全员可发送邀请'
  }
  return '仅管理员可发送邀请'
})

watch(open, (isOpen) => {
  if (!isOpen) {
    return
  }
  tab.value = 'qr'
  mobile.value = ''
  void loadShared()
})

async function loadShared() {
  try {
    await orgInvite.ensureShared()
  } catch {
    toast.add({
      title: '无法加载邀请凭证',
      description: orgInvite.lastError ?? '请确认后端服务已启动',
      color: 'error'
    })
  }
}

function copyText(text: string, title: string) {
  if (!text) {
    toast.add({ title: '暂无可复制内容', color: 'warning' })
    return
  }
  void navigator.clipboard.writeText(text).then(() => {
    toast.add({ title, color: 'success' })
  }).catch(() => {
    toast.add({ title: '复制失败', color: 'error' })
  })
}

async function resetChannel() {
  try {
    await orgInvite.resetShared()
    toast.add({
      title: '已重置',
      description: '旧链接 / 二维码 / 邀请码已失效',
      color: 'neutral'
    })
  } catch {
    toast.add({
      title: '重置失败',
      description: orgInvite.lastError ?? '请稍后重试',
      color: 'error'
    })
  }
}

async function submitMobile() {
  try {
    const data = await orgInvite.inviteMobile(mobile.value)
    const pending = data.status === 'PENDING_APPROVAL'
    toast.add({
      title: pending ? '邀请已提交，待审批' : '邀请已发送',
      description: pending
        ? `已邀请 ${data.mobile}，需管理员审批后方可加入`
        : `已邀请 ${data.mobile}`,
      color: 'success'
    })
    mobile.value = ''
    open.value = false
  } catch {
    toast.add({
      title: '无法发送',
      description: orgInvite.lastError ?? '请检查手机号',
      color: 'error'
    })
  }
}
</script>

<template>
  <UModal
    v-model:open="open"
    title="添加企业成员"
    description="「企业成员」是你在企业、部门或组织内的同事"
    :ui="{ content: 'max-w-lg w-[min(520px,94vw)]' }"
  >
    <template #body>
      <div class="space-y-4">
        <p class="text-xs text-[var(--ws-text-muted)]">
          {{ modeHint }}
        </p>

        <div class="flex flex-wrap gap-1 rounded-lg bg-[var(--ws-input-bar-bg)]/60 p-1">
          <button
            v-for="item in tabs"
            :key="item.value"
            type="button"
            class="rounded-md px-2.5 py-1.5 text-sm transition-colors"
            :class="tab === item.value
              ? 'bg-[var(--ws-card-bg)] font-medium text-primary shadow-sm'
              : 'text-[var(--ws-text-muted)] hover:text-[var(--ws-text)]'"
            @click="tab = item.value"
          >
            {{ item.label }}
          </button>
        </div>

        <div
          v-if="orgInvite.loadingShared && !orgInvite.sharedToken"
          class="flex justify-center py-8"
        >
          <UIcon
            name="i-lucide-loader-circle"
            class="size-6 animate-spin text-[var(--ws-text-muted)]"
          />
        </div>

        <template v-else>
          <div
            v-if="tab === 'qr'"
            class="space-y-3 text-center"
          >
            <p class="text-xs text-[var(--ws-text-muted)]">
              仅支持区号为 +86 的手机号加入当前企业
            </p>
            <div class="mx-auto flex size-44 flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-[var(--ws-border-subtle)] bg-[var(--ws-input-bar-bg)]/40 p-3">
              <UIcon name="i-lucide-qr-code" class="size-12 opacity-60" />
              <p class="break-all font-mono text-[10px] text-[var(--ws-text-muted)]">
                {{ orgInvite.sharedToken || '—' }}
              </p>
            </div>
            <p class="text-xs text-[var(--ws-text-muted)]">
              二维码失效时间：{{ orgInvite.expiresLabel }}
              <button
                type="button"
                class="ml-2 text-primary disabled:opacity-50"
                :disabled="orgInvite.saving"
                @click="resetChannel"
              >
                重置
              </button>
            </p>
            <div class="flex justify-end gap-2">
              <UButton
                color="neutral"
                variant="soft"
                @click="copyText(inviteLink, '已复制邀请链接')"
              >
                保存二维码
              </UButton>
              <UButton @click="copyText(inviteLink, '已复制邀请链接')">
                复制二维码
              </UButton>
            </div>
          </div>

          <div
            v-else-if="tab === 'link'"
            class="space-y-3"
          >
            <UInput
              :model-value="inviteLink"
              readonly
            />
            <p class="text-xs text-[var(--ws-text-muted)]">
              链接失效：{{ orgInvite.expiresLabel }}
              <button
                type="button"
                class="ml-2 text-primary disabled:opacity-50"
                :disabled="orgInvite.saving"
                @click="resetChannel"
              >
                重置
              </button>
            </p>
            <div class="flex justify-end">
              <UButton @click="copyText(inviteLink, '已复制企业链接')">
                复制链接
              </UButton>
            </div>
          </div>

          <div
            v-else-if="tab === 'code'"
            class="space-y-3 text-center"
          >
            <p class="font-mono text-3xl tracking-[0.35em] text-[var(--ws-text)]">
              {{ orgInvite.inviteCode || '—' }}
            </p>
            <p class="text-xs text-[var(--ws-text-muted)]">
              邀请码失效：{{ orgInvite.expiresLabel }}
              <button
                type="button"
                class="ml-2 text-primary disabled:opacity-50"
                :disabled="orgInvite.saving"
                @click="resetChannel"
              >
                重置
              </button>
            </p>
            <div class="flex justify-end">
              <UButton @click="copyText(orgInvite.inviteCode, '已复制邀请码')">
                复制邀请码
              </UButton>
            </div>
          </div>

          <div
            v-else
            class="space-y-3"
          >
            <UFormField label="手机号">
              <UInput
                v-model="mobile"
                placeholder="输入 11 位手机号"
                maxlength="11"
              />
            </UFormField>
            <div
              v-if="orgInvite.mobileInvites.length"
              class="rounded-lg border border-[var(--ws-border-subtle)] p-2"
            >
              <p class="mb-1 text-xs text-[var(--ws-text-muted)]">
                最近发送
              </p>
              <p
                v-for="item in orgInvite.mobileInvites.slice(0, 5)"
                :key="item.id"
                class="text-sm"
              >
                {{ item.mobile }}
                <span class="text-xs text-[var(--ws-text-muted)]">· {{ item.status }}</span>
              </p>
            </div>
            <div class="flex justify-end gap-2">
              <UButton
                color="neutral"
                variant="ghost"
                @click="open = false"
              >
                取消
              </UButton>
              <UButton
                :loading="orgInvite.saving"
                @click="submitMobile"
              >
                发送邀请
              </UButton>
            </div>
          </div>
        </template>
      </div>
    </template>
  </UModal>
</template>
