<script setup lang="ts">
import { computed, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useImStore } from '../../stores/im'
import { useUserPreferenceStore } from '../../stores/userPreference'
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

function groupBadge(groupId: ImSidebarGroupId): string | undefined {
  if (groupId === 'flagged' && flaggedCount.value > 0) {
    return String(flaggedCount.value)
  }
  return undefined
}
</script>

<template>
  <div class="flex h-full w-[148px] shrink-0 flex-col border-r border-[var(--ws-border-subtle)] bg-[var(--ws-rail-bg)]">
    <div class="flex items-center justify-between px-3 py-3">
      <h3 class="text-sm font-semibold">
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

    <nav class="flex-1 space-y-0.5 overflow-y-auto px-1.5 pb-3">
      <template v-for="groupId in visibleGroupIds" :key="groupId">
        <button
          type="button"
          class="workspace-list-item flex w-full items-center gap-2 px-2 py-1.5 text-left text-sm"
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
          class="ml-2 space-y-0.5 border-l border-[var(--ws-border-subtle)] pl-1"
        >
          <button
            v-for="tag in userTags"
            :key="tag.id"
            type="button"
            class="workspace-list-item flex w-full items-center gap-2 px-2 py-1.5 text-left text-sm"
            :data-active="isSelectedTag(tag.id) ? 'true' : undefined"
            @click="selectTag(tag.id)"
          >
            <UIcon
              name="i-lucide-tag"
              class="size-3.5 shrink-0"
              :class="isSelectedTag(tag.id) ? 'text-[var(--ui-primary)]' : 'text-[var(--ws-text-muted)]'"
            />
            <span class="min-w-0 truncate">{{ tag.name }}</span>
          </button>
          <p v-if="userTags.length === 0" class="px-2 py-1 text-xs text-[var(--ws-text-muted)]">
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
