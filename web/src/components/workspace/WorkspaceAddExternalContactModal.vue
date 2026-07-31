<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  useContactAddStore,
  type ContactAccountSearchHit
} from '../../stores/contactAdd'
import { resolveAvatarUrl } from '../../utils/avatar'
import WorkspaceAvatar from './WorkspaceAvatar.vue'

const open = defineModel<boolean>('open', { required: true })
const contactAdd = useContactAddStore()
const toast = useToast()

type ExternalTab = 'search' | 'qr'

const tab = ref<ExternalTab>('search')
const query = ref('')
const hits = ref<ContactAccountSearchHit[]>([])

watch(open, (isOpen) => {
  if (isOpen) {
    tab.value = 'search'
    query.value = ''
    hits.value = []
    contactAdd.lastError = null
  }
})

async function runSearch() {
  const results = await contactAdd.search(query.value)
  hits.value = results
  if (!query.value.trim()) {
    return
  }
  if (contactAdd.lastError && results.length === 0) {
    toast.add({
      title: '无法搜索',
      description: contactAdd.lastError,
      color: 'warning'
    })
    return
  }
  if (results.length === 0) {
    toast.add({
      title: '未找到账号',
      description: '请输入确切的 11 位手机号或邮箱',
      color: 'neutral'
    })
  }
}

async function addHit(hit: ContactAccountSearchHit) {
  if (hit.alreadyAdded) {
    return
  }
  try {
    await contactAdd.sendRequest(hit)
    hits.value = contactAdd.lastHits
    toast.add({
      title: '已发送申请（草稿）',
      description: '对接 API 后将通知对方；可在本机会话 outgoing 中查看',
      color: 'success'
    })
  } catch {
    toast.add({
      title: '无法添加',
      description: contactAdd.lastError ?? '请稍后重试',
      color: 'error'
    })
  }
}

function finish() {
  open.value = false
}
</script>

<template>
  <UModal
    v-model:open="open"
    title="添加外部联系人"
    description="「外部联系人」是你所在企业、部门或组织外的合作伙伴"
    :ui="{ content: 'max-w-lg w-[min(520px,94vw)]' }"
  >
    <template #body>
      <div class="space-y-4">
        <div class="flex gap-1 rounded-lg bg-[var(--ws-input-bar-bg)]/60 p-1">
          <button
            type="button"
            class="flex-1 rounded-md px-2.5 py-1.5 text-sm"
            :class="tab === 'search'
              ? 'bg-[var(--ws-card-bg)] font-medium text-primary shadow-sm'
              : 'text-[var(--ws-text-muted)]'"
            @click="tab = 'search'"
          >
            手机号或邮箱
          </button>
          <button
            type="button"
            class="flex-1 rounded-md px-2.5 py-1.5 text-sm"
            :class="tab === 'qr'
              ? 'bg-[var(--ws-card-bg)] font-medium text-primary shadow-sm'
              : 'text-[var(--ws-text-muted)]'"
            @click="tab = 'qr'"
          >
            我的二维码
          </button>
        </div>

        <template v-if="tab === 'search'">
          <div class="flex gap-2">
            <UInput
              v-model="query"
              class="min-w-0 flex-1"
              icon="i-lucide-search"
              placeholder="输入确切手机号或邮箱"
              @keydown.enter.prevent="runSearch"
            />
            <UButton
              color="neutral"
              variant="soft"
              :loading="contactAdd.searching"
              @click="runSearch"
            >
              搜索
            </UButton>
          </div>

          <div
            v-if="hits.length"
            class="max-h-56 space-y-1 overflow-y-auto"
          >
            <p class="px-1 text-xs text-[var(--ws-text-muted)]">
              搜索结果
              <span class="text-[10px]">（同一手机号可绑定多个企业账号）</span>
            </p>
            <div
              v-for="hit in hits"
              :key="hit.userId"
              class="flex items-center gap-3 rounded-lg px-2 py-2 hover:bg-[var(--ws-rail-hover)]"
            >
              <WorkspaceAvatar
                :src="resolveAvatarUrl(hit.avatar)"
                :text="hit.avatarText"
                :seed="hit.userId"
                size="md"
              />
              <div class="min-w-0 flex-1">
                <p class="truncate text-sm font-medium">
                  {{ hit.displayName }}
                </p>
                <p class="truncate text-xs text-[var(--ws-text-muted)]">
                  {{ hit.orgLabel }}
                </p>
              </div>
              <UButton
                v-if="!hit.alreadyAdded"
                size="sm"
                color="primary"
                variant="soft"
                @click="addHit(hit)"
              >
                添加
              </UButton>
              <span
                v-else
                class="text-sm text-[var(--ws-text-muted)]"
              >
                已添加
              </span>
            </div>
          </div>

          <div
            v-if="contactAdd.outgoing.length"
            class="rounded-lg border border-[var(--ws-border-subtle)] p-2"
          >
            <p class="mb-1 text-xs text-[var(--ws-text-muted)]">
              本会话已发出申请
            </p>
            <p
              v-for="item in contactAdd.outgoing.slice(0, 5)"
              :key="item.id"
              class="truncate text-sm"
            >
              {{ item.displayName }}
              <span class="text-xs text-[var(--ws-text-muted)]">· {{ item.status }}</span>
            </p>
          </div>
        </template>

        <div
          v-else
          class="flex flex-col items-center gap-2 py-8 text-[var(--ws-text-muted)]"
        >
          <UIcon name="i-lucide-qr-code" class="size-16 opacity-50" />
          <p class="text-sm">
            我的二维码（占位，后续可扫码加外部联系人）
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
          <UButton @click="finish">
            完成
          </UButton>
        </div>
      </div>
    </template>
  </UModal>
</template>
