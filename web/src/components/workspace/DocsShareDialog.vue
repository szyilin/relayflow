<script setup lang="ts">
import { ref, watch } from 'vue'
import { useDebounceFn } from '@vueuse/core'
import type { DocsShareItem, DocsSharePermission } from '../../api/app/docs'
import { searchMembers, type MemberSearchItem } from '../../api/app/member-search'

const open = defineModel<boolean>('open', { required: true })

const props = defineProps<{
  objectId: string | null
  title?: string
  shares: DocsShareItem[]
}>()

const emit = defineEmits<{
  grant: [payload: { granteeUserId: string, granteeDisplayName: string, permission: DocsSharePermission }]
  revoke: [shareId: string]
  updatePermission: [payload: { shareId: string, permission: DocsSharePermission }]
}>()

const keyword = ref('')
const results = ref<MemberSearchItem[]>([])
const searching = ref(false)
const permission = ref<DocsSharePermission>('EDIT')

const permItems = [
  { label: '可阅读', value: 'VIEW' satisfies DocsSharePermission },
  { label: '可编辑', value: 'EDIT' satisfies DocsSharePermission }
]

const runSearch = useDebounceFn(async () => {
  const q = keyword.value.trim()
  if (!q) {
    results.value = []
    return
  }
  searching.value = true
  try {
    results.value = await searchMembers(q, 8)
  } catch {
    results.value = []
  } finally {
    searching.value = false
  }
}, 250)

watch(keyword, () => {
  void runSearch()
})

watch(open, (v) => {
  if (!v) {
    keyword.value = ''
    results.value = []
    permission.value = 'EDIT'
  }
})

function onPick(member: MemberSearchItem) {
  emit('grant', {
    granteeUserId: member.entityId || member.id,
    granteeDisplayName: member.title,
    permission: permission.value
  })
  keyword.value = ''
  results.value = []
}
</script>

<template>
  <UModal
    v-model:open="open"
    title="分享文档"
    :description="props.title || '选择企业成员并设置权限'"
  >
    <template #body>
      <div class="space-y-4">
        <div class="flex gap-2">
          <UInput
            v-model="keyword"
            class="flex-1"
            placeholder="搜索成员姓名 / 手机号"
            icon="i-lucide-search"
            :loading="searching"
          />
          <USelectMenu
            v-model="permission"
            :items="permItems"
            value-key="value"
            class="w-28"
          />
        </div>
        <div
          v-if="results.length"
          class="max-h-40 space-y-1 overflow-y-auto rounded-md border border-[var(--ws-border-subtle)] p-1"
        >
          <button
            v-for="m in results"
            :key="m.id"
            type="button"
            class="workspace-list-item flex w-full items-center gap-2 px-2 py-2 text-left text-sm"
            @click="onPick(m)"
          >
            <UIcon
              name="i-lucide-user"
              class="size-4 text-[var(--ws-text-muted)]"
            />
            <span class="min-w-0 flex-1 truncate font-medium">{{ m.title }}</span>
            <span class="truncate text-xs text-[var(--ws-text-muted)]">{{ m.subtitle }}</span>
          </button>
        </div>

        <div>
          <div class="mb-2 text-xs font-semibold text-[var(--ws-text-muted)]">
            已分享给
          </div>
          <p
            v-if="!props.shares.length"
            class="text-sm text-[var(--ws-text-muted)]"
          >
            还没有协作者。
          </p>
          <ul
            v-else
            class="space-y-2"
          >
            <li
              v-for="s in props.shares"
              :key="s.shareId"
              class="flex items-center gap-2 rounded-md border border-[var(--ws-border-subtle)] px-3 py-2"
            >
              <div class="min-w-0 flex-1">
                <div class="truncate text-sm font-medium">
                  {{ s.granteeDisplayName }}
                </div>
                <div class="text-xs text-[var(--ws-text-muted)]">
                  {{ s.permission === 'EDIT' ? '可编辑' : '可阅读' }}
                </div>
              </div>
              <USelectMenu
                :model-value="s.permission"
                :items="permItems"
                value-key="value"
                class="w-24"
                @update:model-value="(v: string) => emit('updatePermission', { shareId: s.shareId, permission: v as DocsSharePermission })"
              />
              <UButton
                color="neutral"
                variant="ghost"
                size="xs"
                icon="i-lucide-x"
                square
                @click="emit('revoke', s.shareId)"
              />
            </li>
          </ul>
        </div>
      </div>
    </template>
  </UModal>
</template>
