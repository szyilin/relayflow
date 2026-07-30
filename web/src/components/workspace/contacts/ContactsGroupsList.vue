<script setup lang="ts">
import WorkspaceAvatar from '../WorkspaceAvatar.vue'

export interface ContactsGroupRow {
  id: string
  title: string
  avatarText: string
  memberCount?: number
  subtitle?: string
  isPlaceholder?: boolean
}

defineProps<{
  groups: ContactsGroupRow[]
  loading?: boolean
}>()

const emit = defineEmits<{
  open: [group: ContactsGroupRow]
}>()
</script>

<template>
  <div class="flex h-full flex-col">
    <div class="border-b border-[var(--ws-border-subtle)] px-5 py-4">
      <h2 class="font-semibold">
        我的群组
      </h2>
      <p class="mt-1 text-sm text-[var(--ws-text-muted)]">
        你参与的群聊
      </p>
    </div>

    <div class="min-h-0 flex-1 overflow-y-auto p-3">
      <div
        v-if="loading"
        class="space-y-2"
      >
        <USkeleton
          v-for="i in 4"
          :key="i"
          class="h-14 w-full"
        />
      </div>

      <div
        v-else-if="groups.length"
        class="space-y-0.5"
      >
        <button
          v-for="group in groups"
          :key="group.id"
          type="button"
          class="workspace-list-item flex w-full items-center gap-3 px-3 py-2.5 text-left"
          @click="emit('open', group)"
        >
          <WorkspaceAvatar
            :text="group.avatarText"
            :seed="group.id"
            size="md"
          />
          <div class="min-w-0 flex-1">
            <p class="truncate font-medium">
              {{ group.title }}
            </p>
            <p class="truncate text-sm text-[var(--ws-text-muted)]">
              <template v-if="group.memberCount != null">
                {{ group.memberCount }} 人
              </template>
              <template v-else-if="group.subtitle">
                {{ group.subtitle }}
              </template>
              <template v-else>
                群聊
              </template>
            </p>
          </div>
          <UIcon
            name="i-lucide-chevron-right"
            class="size-4 shrink-0 text-[var(--ws-text-muted)]"
          />
        </button>
      </div>

      <UEmpty
        v-else
        icon="i-lucide-users-round"
        title="暂无群组"
        description="加入或创建群聊后会显示在这里"
        class="py-12"
      />
    </div>
  </div>
</template>
