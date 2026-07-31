<script setup lang="ts">
import { computed, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useImStore } from '../../stores/im'
import { useUserPreferenceStore } from '../../stores/userPreference'
import { appConfirm } from '../../composables/appConfirm'
import {
  IM_SIDEBAR_GROUP_ICONS,
  IM_SIDEBAR_GROUP_LABELS,
  type ImSidebarGroupId,
  type ImSidebarSelection
} from '../../composables/imConversationGroups'
import ImEditGroupsModal from './ImEditGroupsModal.vue'

const im = useImStore()
const preference = useUserPreferenceStore()
const { sidebarSelection, userTags, flaggedCount } = storeToRefs(im)

const tagsExpanded = ref(true)
const editOpen = ref(false)
const createTagOpen = ref(false)
const newTagName = ref('')

const visibleGroupIds = computed(() =>
  (preference.settings.im.sidebarGroupOrder ?? []) as ImSidebarGroupId[])

function isSelectedGroup(groupId: ImSidebarGroupId) {
  return sidebarSelection.value.kind === 'group' && sidebarSelection.value.groupId === groupId
}

function isSelectedTag(tagId: string) {
  return sidebarSelection.value.kind === 'tag' && sidebarSelection.value.tagId === tagId
}

function selectGroup(groupId: ImSidebarGroupId) {
  if (groupId === 'tags') {
    tagsExpanded.value = !tagsExpanded.value
    return
  }
  im.setSidebarSelection({ kind: 'group', groupId } as ImSidebarSelection)
}

function selectTag(tagId: string) {
  im.setSidebarSelection({ kind: 'tag', tagId })
}

async function submitNewTag() {
  const tag = await im.createUserTag(newTagName.value)
  if (!tag) {
    return
  }
  newTagName.value = ''
  createTagOpen.value = false
  tagsExpanded.value = true
  selectTag(tag.id)
}

async function confirmDeleteTag(tag: { id: string, name: string, conversationCount?: number }) {
  const count = tag.conversationCount ?? 0
  const description = count > 0
    ? `标签「${tag.name}」已应用于 ${count} 个会话。删除后，这些会话上的该标签也会一并移除。`
    : `确定删除标签「${tag.name}」吗？`

  const ok = await appConfirm({
    title: '删除标签',
    description,
    confirmLabel: '删除',
    cancelLabel: '取消',
    confirmColor: 'error'
  })
  if (!ok) {
    return
  }
  await im.deleteUserTag(tag.id)
}

function groupBadge(groupId: ImSidebarGroupId): string | undefined {
  if (groupId === 'flagged' && flaggedCount.value > 0) {
    return String(flaggedCount.value)
  }
  return undefined
}
</script>

<template>
  <div class="flex h-full w-[108px] shrink-0 flex-col border-r border-[var(--ws-border-subtle)] bg-[var(--ws-rail-bg)] select-none">
    <div class="flex items-center justify-between gap-0.5 px-2 py-2.5">
      <h3 class="text-xs font-semibold">
        分组
      </h3>
      <UButton
        icon="i-lucide-settings"
        color="neutral"
        variant="ghost"
        size="xs"
        aria-label="编辑分组"
        @click="editOpen = true"
      />
    </div>

    <nav class="flex-1 space-y-0.5 overflow-y-auto px-1 pb-3">
      <template v-for="groupId in visibleGroupIds" :key="groupId">
        <button
          type="button"
          class="workspace-list-item flex w-full items-center gap-1.5 px-1.5 py-1.5 text-left text-xs"
          :data-active="isSelectedGroup(groupId) ? 'true' : undefined"
          @click="selectGroup(groupId)"
        >
          <UIcon
            v-if="groupId === 'tags'"
            :name="tagsExpanded ? 'i-lucide-chevron-down' : 'i-lucide-chevron-right'"
            class="size-3.5 shrink-0"
          />
          <UIcon
            v-else
            :name="IM_SIDEBAR_GROUP_ICONS[groupId]"
            class="size-3.5 shrink-0"
            :class="isSelectedGroup(groupId) ? 'text-[var(--ui-primary)]' : 'text-[var(--ws-text-muted)]'"
          />
          <span class="min-w-0 flex-1 truncate">{{ IM_SIDEBAR_GROUP_LABELS[groupId] }}</span>
          <span
            v-if="groupBadge(groupId)"
            class="text-xs tabular-nums text-[var(--ws-text-muted)]"
          >{{ groupBadge(groupId) }}</span>
          <UButton
            v-if="groupId === 'tags'"
            icon="i-lucide-plus"
            color="neutral"
            variant="ghost"
            size="xs"
            class="!size-6"
            aria-label="新建标签"
            @click.stop="createTagOpen = true"
          />
        </button>

        <div
          v-if="groupId === 'tags' && tagsExpanded"
          class="ml-1.5 space-y-0.5 border-l border-[var(--ws-border-subtle)] pl-1"
        >
          <div
            v-for="tag in userTags"
            :key="tag.id"
            class="group/tag workspace-list-item flex w-full items-center gap-0.5 px-0.5 py-0.5"
            :data-active="isSelectedTag(tag.id) ? 'true' : undefined"
          >
            <button
              type="button"
              class="flex min-w-0 flex-1 items-center gap-1.5 px-1 py-1 text-left text-xs"
              @click="selectTag(tag.id)"
            >
              <UIcon
                name="i-lucide-tag"
                class="size-3 shrink-0"
                :class="isSelectedTag(tag.id) ? 'text-[var(--ui-primary)]' : 'text-[var(--ws-text-muted)]'"
              />
              <span class="min-w-0 truncate">{{ tag.name }}</span>
            </button>
            <UButton
              icon="i-lucide-trash-2"
              color="neutral"
              variant="ghost"
              size="xs"
              class="!size-5 shrink-0 opacity-0 group-hover/tag:opacity-100"
              :aria-label="`删除标签 ${tag.name}`"
              @click.stop="confirmDeleteTag(tag)"
            />
          </div>
          <p v-if="userTags.length === 0" class="px-1.5 py-1 text-[11px] text-[var(--ws-text-muted)]">
            暂无标签
          </p>
        </div>
      </template>
    </nav>

    <ImEditGroupsModal v-model:open="editOpen" />

    <UModal v-model:open="createTagOpen" title="新建标签">
      <template #body>
        <UInput
          v-model="newTagName"
          placeholder="标签名称"
          autofocus
          @keyup.enter="submitNewTag"
        />
      </template>
      <template #footer>
        <div class="flex justify-end gap-2">
          <UButton color="neutral" variant="ghost" @click="createTagOpen = false">
            取消
          </UButton>
          <UButton color="primary" @click="submitNewTag">
            创建
          </UButton>
        </div>
      </template>
    </UModal>
  </div>
</template>
