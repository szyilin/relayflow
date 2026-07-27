<script setup lang="ts">
import type { StarredContactItem } from '../../../stores/contacts'

defineProps<{
  starred: StarredContactItem[]
  loading?: boolean
}>()

const emit = defineEmits<{
  contactClick: [person: StarredContactItem, event: MouseEvent]
  unstar: [userId: string]
}>()
</script>

<template>
  <div class="flex h-full flex-col">
    <div class="border-b border-[var(--ws-border-subtle)] px-5 py-4">
      <h2 class="font-semibold">
        星标联系人
      </h2>
      <p class="mt-1 text-sm text-[var(--ws-text-muted)]">
        常用联系人快捷入口
      </p>
    </div>

    <div class="min-h-0 flex-1 overflow-y-auto p-3">
      <div
        v-if="loading"
        class="space-y-2 p-3"
      >
        <USkeleton class="h-12 w-full" />
        <USkeleton class="h-12 w-full" />
      </div>
      <div
        v-else-if="starred.length"
        class="space-y-0.5"
      >
        <div
          v-for="person in starred"
          :key="person.id"
          class="workspace-list-item flex w-full items-center gap-3 px-3 py-2.5"
        >
          <button
            type="button"
            class="flex min-w-0 flex-1 items-center gap-3 text-left"
            @click="emit('contactClick', person, $event)"
          >
            <UAvatar
              :text="person.avatarText"
              size="md"
            />
            <div class="min-w-0 flex-1">
              <p class="truncate font-medium">
                {{ person.displayName }}
              </p>
              <p class="truncate text-sm text-[var(--ws-text-muted)]">
                {{ person.orgLabel }}
                <template v-if="person.username">
                  · @{{ person.username }}
                </template>
              </p>
            </div>
          </button>
          <UButton
            size="sm"
            color="neutral"
            variant="ghost"
            icon="i-lucide-star"
            class="text-amber-500"
            aria-label="取消星标"
            @click="emit('unstar', person.userId)"
          />
        </div>
      </div>

      <UEmpty
        v-else
        icon="i-lucide-star"
        title="暂无星标联系人"
        description="将常用同事加入星标后在此快速查找"
        class="py-12"
      />
    </div>
  </div>
</template>
