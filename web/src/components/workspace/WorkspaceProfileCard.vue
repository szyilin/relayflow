<script setup lang="ts">
import { computed, nextTick, ref, useTemplateRef, watch } from 'vue'
import { useRouter } from 'vue-router'
import WorkspaceMoreAccountsPanel from './WorkspaceMoreAccountsPanel.vue'
import WorkspaceSettingsPanel from './WorkspaceSettingsPanel.vue'
import { useAuthStore } from '../../stores/auth'
import { useProfileStore } from '../../stores/profile'
import { avatarTextFromName, resolveAvatarUrl } from '../../utils/avatar'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
}>()

const router = useRouter()
const authStore = useAuthStore()
const profileStore = useProfileStore()
const toast = useToast()

type PanelView = 'main' | 'settings' | 'more-accounts'
const panelView = ref<PanelView>('main')

const editingNickname = ref(false)
const nicknameDraft = ref('')
const nicknameInput = useTemplateRef<{ inputRef?: HTMLInputElement }>('nicknameInput')
const avatarInput = useTemplateRef<HTMLInputElement>('avatarInput')

const displayNickname = computed(() =>
  profileStore.profile?.nickname
  ?? authStore.user?.nickname
  ?? '员工')

const displayTenantName = computed(() =>
  profileStore.profile?.tenantName ?? authStore.activeTenantName)

const avatarUrl = computed(() =>
  resolveAvatarUrl(profileStore.profile?.avatar ?? authStore.user?.avatar))

const avatarText = computed(() => avatarTextFromName(displayNickname.value))

watch(() => props.open, (next) => {
  if (next) {
    panelView.value = 'main'
    void profileStore.fetchProfile().catch(() => {
      toast.add({ title: '加载资料失败', description: '请稍后重试', color: 'error' })
    })
    editingNickname.value = false
  }
})

function close() {
  emit('update:open', false)
}

function onPlaceholder(label: string) {
  toast.add({
    title: '功能即将推出',
    description: `${label}（占位）`,
    color: 'neutral'
  })
}

async function startEditNickname() {
  nicknameDraft.value = displayNickname.value
  editingNickname.value = true
  await nextTick()
  nicknameInput.value?.inputRef?.focus()
  nicknameInput.value?.inputRef?.select()
}

async function commitNickname() {
  const next = nicknameDraft.value.trim()
  if (!next) {
    toast.add({ title: '昵称不能为空', color: 'warning' })
    return
  }
  if (next === displayNickname.value) {
    editingNickname.value = false
    return
  }

  try {
    await profileStore.saveNickname(next)
    editingNickname.value = false
    toast.add({ title: '昵称已更新', color: 'success' })
  } catch {
    toast.add({ title: '保存失败', description: '请稍后重试', color: 'error' })
  }
}

function cancelEditNickname() {
  editingNickname.value = false
}

function onNicknameKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter') {
    void commitNickname()
  }
  if (event.key === 'Escape') {
    cancelEditNickname()
  }
}

function pickAvatar() {
  avatarInput.value?.click()
}

async function onAvatarSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }
  if (!file.type.startsWith('image/')) {
    toast.add({ title: '请选择图片文件', color: 'warning' })
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    toast.add({ title: '图片不能超过 5MB', color: 'warning' })
    return
  }

  try {
    await profileStore.uploadAvatar(file)
    toast.add({ title: '头像已更新', color: 'success' })
  } catch {
    toast.add({ title: '头像上传失败', description: '请确认存储服务可用', color: 'error' })
  }
}

async function logout() {
  close()
  await authStore.logout({ preserveOtherAccounts: true })
  if (authStore.isAuthenticated) {
    await router.replace('/app/messages')
    return
  }
  await router.replace('/app/login')
}

function openAdmin() {
  close()
  void router.push('/admin')
}
</script>

<template>
  <WorkspaceSettingsPanel
    v-if="panelView === 'settings'"
    @back="panelView = 'main'"
  />

  <WorkspaceMoreAccountsPanel
    v-else-if="panelView === 'more-accounts'"
    @back="panelView = 'main'"
    @close="close"
  />

  <div v-else class="workspace-profile-card w-72 p-4">
    <div class="flex items-center gap-3 border-b border-[var(--ws-border-subtle)] pb-4">
      <button
        type="button"
        class="group relative shrink-0"
        :disabled="profileStore.uploading"
        @click="pickAvatar"
      >
        <UAvatar
          :src="avatarUrl"
          :alt="displayNickname"
          :text="avatarText"
          size="xl"
          class="ring-2 ring-primary/20"
        />
        <span
          class="absolute inset-0 flex items-center justify-center rounded-full bg-black/45 opacity-0 transition-opacity group-hover:opacity-100"
        >
          <UIcon name="i-lucide-camera" class="size-4 text-white" />
        </span>
        <span
          v-if="profileStore.uploading"
          class="absolute inset-0 flex items-center justify-center rounded-full bg-black/50"
        >
          <UIcon name="i-lucide-loader-circle" class="size-4 animate-spin text-white" />
        </span>
      </button>
      <input
        ref="avatarInput"
        type="file"
        accept="image/*"
        class="hidden"
        @change="onAvatarSelected"
      >

      <div class="min-w-0 flex-1">
        <div v-if="editingNickname" class="flex items-center gap-1">
          <UInput
            ref="nicknameInput"
            v-model="nicknameDraft"
            size="sm"
            class="flex-1"
            :disabled="profileStore.saving"
            @keydown="onNicknameKeydown"
            @blur="commitNickname"
          />
        </div>
        <button
          v-else
          type="button"
          class="group inline-flex max-w-full items-center gap-1.5 rounded-md px-0.5 py-0.5 hover:bg-[var(--ws-rail-hover)]"
          @click="startEditNickname"
        >
          <span class="truncate text-base font-semibold leading-tight">{{ displayNickname }}</span>
          <UIcon
            name="i-lucide-pencil"
            class="size-3.5 shrink-0 text-[var(--ws-text-muted)] opacity-0 transition-opacity group-hover:opacity-100"
          />
        </button>
        <p class="mt-0.5 truncate text-sm text-[var(--ws-text-muted)]">
          {{ displayTenantName }}
        </p>
      </div>
    </div>

    <div class="space-y-0.5 pt-2">
      <button
        type="button"
        class="workspace-profile-action"
        @click="onPlaceholder('个性签名')"
      >
        <UIcon name="i-lucide-pen-line" class="size-4 shrink-0" />
        <span>个性签名</span>
        <span class="ml-auto text-xs text-[var(--ws-text-muted)]">（占位）</span>
      </button>
      <button
        type="button"
        class="workspace-profile-action"
        @click="onPlaceholder('我的个人名片')"
      >
        <UIcon name="i-lucide-contact" class="size-4 shrink-0" />
        <span>我的个人名片</span>
        <span class="ml-auto text-xs text-[var(--ws-text-muted)]">（占位）</span>
      </button>
      <button
        type="button"
        class="workspace-profile-action"
        @click="panelView = 'more-accounts'"
      >
        <UIcon name="i-lucide-users" class="size-4 shrink-0" />
        <span>登录更多账号</span>
        <UIcon name="i-lucide-chevron-right" class="ml-auto size-3.5 text-[var(--ws-text-muted)]" />
      </button>
      <button
        type="button"
        class="workspace-profile-action"
        @click="panelView = 'settings'"
      >
        <UIcon name="i-lucide-settings" class="size-4 shrink-0" />
        <span>设置</span>
        <UIcon name="i-lucide-chevron-right" class="ml-auto size-3.5 text-[var(--ws-text-muted)]" />
      </button>
      <button
        type="button"
        class="workspace-profile-action text-error"
        @click="logout"
      >
        <UIcon name="i-lucide-log-out" class="size-4 shrink-0" />
        <span>退出登录</span>
      </button>
    </div>

    <div
      v-if="authStore.isAdmin"
      class="mt-1 space-y-0.5 border-t border-[var(--ws-border-subtle)] pt-2"
    >
      <button
        type="button"
        class="workspace-profile-action"
        @click="openAdmin"
      >
        <UIcon name="i-lucide-shield" class="size-4 shrink-0" />
        <span>管理后台</span>
        <UIcon name="i-lucide-external-link" class="ml-auto size-3.5 text-[var(--ws-text-muted)]" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.workspace-profile-action {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 0.625rem;
  border-radius: 0.625rem;
  padding: 0.5rem 0.625rem;
  font-size: 0.875rem;
  line-height: 1.25rem;
  color: var(--ws-text);
  transition: background-color 0.15s ease;
}

.workspace-profile-action:hover {
  background: var(--ws-rail-hover);
}
</style>
