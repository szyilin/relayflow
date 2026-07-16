<script setup lang="ts">
import { computed, ref, useTemplateRef, watch } from 'vue'
import { useBusinessCardStore } from '../../stores/businessCard'
import type { ContactRemark } from '../../api/app/contactRemark'
import { avatarTextFromName, resolveAvatarUrl } from '../../utils/avatar'

export type BusinessCardMode = 'self' | 'peer'

const props = withDefaults(defineProps<{
  mode: BusinessCardMode
  userId: string
  nickname: string
  username?: string
  orgLabel?: string
  avatarUrl?: string | null
  avatarText?: string
}>(), {
  username: '',
  orgLabel: '',
  avatarUrl: null,
  avatarText: ''
})

const emit = defineEmits<{
  message: []
}>()

const toast = useToast()
const businessCard = useBusinessCardStore()

const signature = ref('')
const coverFileId = ref<string | null>(null)
const remoteNickname = ref('')
const remoteUsername = ref('')
const remoteOrgLabel = ref('')
const remoteAvatar = ref<string | null>(null)
const remark = ref<ContactRemark>({ targetUserId: '', remarkName: '', description: '' })
const editingSignature = ref(false)
const signatureDraft = ref('')
const remarkOpen = ref(false)
const remarkDraft = ref({ remarkName: '', description: '' })

const coverInput = useTemplateRef<HTMLInputElement>('coverInput')

const isSelf = computed(() => props.mode === 'self')
const isPeer = computed(() => props.mode === 'peer')

const coverUrl = computed(() => resolveAvatarUrl(coverFileId.value))

const resolvedAvatarUrl = computed(() =>
  resolveAvatarUrl(remoteAvatar.value) || props.avatarUrl || undefined)

const displayAvatarText = computed(() =>
  props.avatarText
  || avatarTextFromName(remoteNickname.value || props.nickname || props.username || '?'))

const displayName = computed(() => {
  if (isPeer.value && remark.value.remarkName) {
    return remark.value.remarkName
  }
  return remoteNickname.value || props.nickname || props.username || '成员'
})

const displayUsername = computed(() => remoteUsername.value || props.username)
const displayOrgLabel = computed(() => remoteOrgLabel.value || props.orgLabel)

const signatureDisplay = computed(() => {
  if (signature.value) {
    return signature.value
  }
  return isSelf.value ? '输入你的个性签名…' : '暂无个性签名'
})

const remarkSummary = computed(() => {
  if (remark.value.remarkName || remark.value.description) {
    return remark.value.description || `备注：${remark.value.remarkName}`
  }
  return '添加备注与描述'
})

async function hydrate() {
  editingSignature.value = false
  try {
    if (isSelf.value) {
      const profile = await businessCard.loadSelfCard()
      signature.value = profile.signature ?? ''
      coverFileId.value = profile.coverFileId ?? null
      remoteNickname.value = profile.nickname
      remoteUsername.value = profile.username
      remoteOrgLabel.value = profile.tenantName
      remoteAvatar.value = profile.avatar ?? null
      return
    }
    const [profile, remarkData] = await Promise.all([
      businessCard.loadPeerCard(props.userId),
      businessCard.loadRemark(props.userId)
    ])
    signature.value = profile.signature ?? ''
    coverFileId.value = profile.coverFileId ?? null
    remoteNickname.value = profile.nickname
    remoteUsername.value = profile.username
    remoteOrgLabel.value = props.orgLabel || profile.tenantName
    remoteAvatar.value = profile.avatar ?? null
    remark.value = remarkData
  } catch {
    signature.value = ''
    coverFileId.value = null
    remoteNickname.value = props.nickname
    remoteUsername.value = props.username
    remoteOrgLabel.value = props.orgLabel
    remoteAvatar.value = null
    toast.add({ title: '加载名片失败', description: '请稍后重试', color: 'error' })
  }
}

watch(
  () => [props.userId, props.mode] as const,
  () => {
    void hydrate()
  },
  { immediate: true }
)

function pickCover() {
  if (!isSelf.value) {
    return
  }
  coverInput.value?.click()
}

async function onCoverSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !isSelf.value) {
    return
  }
  if (!file.type.startsWith('image/')) {
    toast.add({ title: '请选择图片文件', color: 'warning' })
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    toast.add({ title: '封面图片不能超过 2MB', color: 'warning' })
    return
  }
  try {
    const profile = await businessCard.uploadCover(file)
    coverFileId.value = profile.coverFileId ?? null
    toast.add({ title: '封面已更新', color: 'success' })
  } catch {
    toast.add({ title: '封面上传失败', description: '请确认存储服务可用', color: 'error' })
  }
}

function startEditSignature() {
  if (!isSelf.value) {
    return
  }
  signatureDraft.value = signature.value
  editingSignature.value = true
}

async function commitSignature() {
  if (!isSelf.value) {
    return
  }
  try {
    const profile = await businessCard.saveSignature(signatureDraft.value.trim())
    signature.value = profile.signature ?? ''
    editingSignature.value = false
    toast.add({ title: '签名已保存', color: 'success' })
  } catch {
    toast.add({ title: '保存失败', description: '请稍后重试', color: 'error' })
  }
}

function cancelEditSignature() {
  editingSignature.value = false
}

function onSignatureKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    void commitSignature()
  }
  if (event.key === 'Escape') {
    cancelEditSignature()
  }
}

function openRemarkEditor() {
  if (!isPeer.value) {
    return
  }
  remarkDraft.value = {
    remarkName: remark.value.remarkName || props.nickname,
    description: remark.value.description
  }
  remarkOpen.value = true
}

async function saveRemark() {
  try {
    remark.value = await businessCard.saveRemark(props.userId, {
      remarkName: remarkDraft.value.remarkName,
      description: remarkDraft.value.description
    })
    remarkOpen.value = false
    toast.add({ title: '备注已保存', color: 'success' })
  } catch {
    toast.add({ title: '保存失败', description: '请稍后重试', color: 'error' })
  }
}

function onPlaceholder(label: string) {
  toast.add({
    title: '功能即将推出',
    description: `${label}（占位）`,
    color: 'neutral'
  })
}

function onMessage() {
  emit('message')
}
</script>

<template>
  <div class="workspace-business-card w-72 overflow-hidden">
    <button
      type="button"
      class="workspace-business-card__cover group relative block w-full"
      :class="{ 'cursor-pointer': isSelf, 'cursor-default': isPeer }"
      :disabled="isPeer || businessCard.saving"
      :aria-label="isSelf ? '更换封面' : undefined"
      @click="pickCover"
    >
      <div
        class="h-24 w-full bg-gradient-to-br from-sky-600 via-blue-700 to-indigo-800"
        :style="coverUrl
          ? { backgroundImage: `url(${coverUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' }
          : undefined"
      />
      <span
        v-if="isSelf"
        class="absolute inset-0 flex items-center justify-center bg-black/35 opacity-0 transition-opacity group-hover:opacity-100"
      >
        <span class="inline-flex items-center gap-1.5 rounded-full bg-black/50 px-2.5 py-1 text-xs text-white">
          <UIcon name="i-lucide-image-plus" class="size-3.5" />
          更换封面
        </span>
      </span>
    </button>
    <input
      ref="coverInput"
      type="file"
      accept="image/*"
      class="hidden"
      @change="onCoverSelected"
    >

    <div class="relative px-4 pb-4 pt-0">
      <div class="flex justify-center">
        <UAvatar
          :src="resolvedAvatarUrl"
          :alt="displayName"
          :text="displayAvatarText"
          size="3xl"
          class="workspace-business-card__avatar -mt-10 ring-4 ring-[var(--ws-panel-bg)]"
        />
      </div>

      <div class="mt-3 text-center">
        <h3 class="truncate text-lg font-semibold leading-tight">
          {{ displayName }}
        </h3>
        <p
          v-if="displayUsername"
          class="mt-0.5 truncate text-sm text-[var(--ws-text-muted)]"
        >
          @{{ displayUsername }}
        </p>
        <p
          v-if="displayOrgLabel"
          class="mt-1 truncate text-sm text-[var(--ws-text-muted)]"
        >
          {{ displayOrgLabel }}
        </p>
      </div>

      <div v-if="isSelf" class="mt-3">
        <div v-if="editingSignature" class="space-y-2">
          <UTextarea
            v-model="signatureDraft"
            :rows="2"
            autoresize
            maxlength="120"
            placeholder="输入你的个性签名…"
            @keydown="onSignatureKeydown"
          />
          <div class="flex justify-end gap-2">
            <UButton size="xs" color="neutral" variant="ghost" @click="cancelEditSignature">
              取消
            </UButton>
            <UButton size="xs" :loading="businessCard.saving" @click="commitSignature">
              保存
            </UButton>
          </div>
        </div>
        <button
          v-else
          type="button"
          class="w-full rounded-lg px-2 py-1.5 text-center text-sm text-[var(--ws-text-muted)] hover:bg-[var(--ws-rail-hover)]"
          @click="startEditSignature"
        >
          <span :class="{ 'italic': !signature }">{{ signatureDisplay }}</span>
        </button>
      </div>

      <p
        v-else
        class="mt-3 px-1 text-center text-sm text-[var(--ws-text-muted)]"
        :class="{ 'italic': !signature }"
      >
        {{ signatureDisplay }}
      </p>

      <div
        v-if="isPeer"
        class="mt-4 flex items-start justify-center gap-5"
      >
        <button
          type="button"
          class="workspace-business-card__action"
          @click="onMessage"
        >
          <span class="workspace-business-card__action-icon">
            <UIcon name="i-lucide-message-circle" class="size-5" />
          </span>
          <span>消息</span>
        </button>
        <button
          type="button"
          class="workspace-business-card__action"
          @click="onPlaceholder('语音通话')"
        >
          <span class="workspace-business-card__action-icon">
            <UIcon name="i-lucide-phone" class="size-5" />
          </span>
          <span>语音</span>
        </button>
        <button
          type="button"
          class="workspace-business-card__action"
          @click="onPlaceholder('视频通话')"
        >
          <span class="workspace-business-card__action-icon">
            <UIcon name="i-lucide-video" class="size-5" />
          </span>
          <span>视频</span>
        </button>
      </div>

      <button
        v-if="isPeer"
        type="button"
        class="mt-4 flex w-full items-center justify-between gap-2 rounded-lg border border-[var(--ws-border-subtle)] px-3 py-2.5 text-left hover:bg-[var(--ws-rail-hover)]"
        @click="openRemarkEditor"
      >
        <div class="min-w-0">
          <p class="text-xs text-[var(--ws-text-muted)]">
            备注与描述
          </p>
          <p class="mt-0.5 truncate text-sm">
            {{ remarkSummary }}
          </p>
        </div>
        <UIcon name="i-lucide-chevron-right" class="size-4 shrink-0 text-[var(--ws-text-muted)]" />
      </button>
    </div>

    <UModal v-model:open="remarkOpen" title="备注与描述">
      <template #body>
        <div class="space-y-4">
          <UFormField label="备注名">
            <UInput v-model="remarkDraft.remarkName" maxlength="64" placeholder="备注名" />
          </UFormField>
          <UFormField label="描述">
            <UTextarea
              v-model="remarkDraft.description"
              :rows="3"
              autoresize
              maxlength="500"
              placeholder="添加描述"
            />
          </UFormField>
          <div class="flex justify-end gap-2 pt-2">
            <UButton color="neutral" variant="ghost" @click="remarkOpen = false">
              取消
            </UButton>
            <UButton :loading="businessCard.saving" @click="saveRemark">
              保存
            </UButton>
          </div>
        </div>
      </template>
    </UModal>
  </div>
</template>

<style scoped>
.workspace-business-card {
  background: var(--ws-panel-bg);
  color: var(--ws-text);
}

.workspace-business-card__avatar {
  box-shadow: 0 0 0 1px var(--ws-border-subtle);
}

.workspace-business-card__action {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  color: var(--ws-text);
}

.workspace-business-card__action-icon {
  display: flex;
  width: 2.5rem;
  height: 2.5rem;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  background: color-mix(in oklab, var(--color-primary) 14%, transparent);
  color: var(--color-primary);
  transition: background-color 0.15s ease;
}

.workspace-business-card__action:hover .workspace-business-card__action-icon {
  background: color-mix(in oklab, var(--color-primary) 24%, transparent);
}
</style>
