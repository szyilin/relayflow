<script setup lang="ts">
import type { ExternalContactItem } from '../../../stores/contacts'
import type { LetterGroup } from '../../../utils/contactSort'

defineProps<{
  groups: LetterGroup<ExternalContactItem>[]
  loading?: boolean
}>()

const emit = defineEmits<{
  contactClick: [person: ExternalContactItem, event: MouseEvent]
  addExternal: []
}>()
</script>

<template>
  <div class="flex h-full flex-col">
    <div class="flex items-start justify-between gap-3 border-b border-[var(--ws-border-subtle)] px-5 py-4">
      <div class="min-w-0">
        <h2 class="font-semibold">
          外部联系人
        </h2>
        <p class="mt-1 text-sm text-[var(--ws-text-muted)]">
          按名称首字母排序
        </p>
      </div>
      <UButton
        size="sm"
        color="primary"
        variant="soft"
        icon="i-lucide-user-plus"
        @click="emit('addExternal')"
      >
        添加
      </UButton>
    </div>

    <div class="min-h-0 flex-1 overflow-y-auto p-3">
      <div
        v-if="loading"
        class="space-y-2 p-3"
      >
        <USkeleton class="h-12 w-full" />
        <USkeleton class="h-12 w-full" />
        <USkeleton class="h-12 w-full" />
      </div>
      <div
        v-else-if="groups.length"
        class="space-y-4"
      >
        <section
          v-for="group in groups"
          :key="group.letter"
        >
          <div class="px-3 py-1 text-xs font-semibold text-[var(--ws-text-muted)]">
            {{ group.letter }}
          </div>
          <div class="space-y-0.5">
            <button
              v-for="person in group.items"
              :key="person.id"
              type="button"
              class="workspace-list-item flex w-full items-center gap-3 px-3 py-2.5 text-left"
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
                </p>
              </div>
            </button>
          </div>
        </section>
      </div>

      <UEmpty
        v-else
        icon="i-lucide-globe"
        title="暂无外部联系人"
        description="点击右上角「添加」，或通过好友申请同意后出现在这里"
        class="py-12"
      >
        <template #actions>
          <UButton
            color="primary"
            variant="soft"
            icon="i-lucide-user-plus"
            @click="emit('addExternal')"
          >
            添加外部联系人
          </UButton>
        </template>
      </UEmpty>
    </div>
  </div>
</template>
