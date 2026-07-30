<script setup lang="ts">
import type { ContactItem } from '../../../api/app/contacts'
import type { DeptTreeItem } from '../../../api/app/contacts'
import WorkspaceAvatar from '../WorkspaceAvatar.vue'

defineProps<{
  breadcrumb: DeptTreeItem[]
  childDepts: DeptTreeItem[]
  members: ContactItem[]
  loading?: boolean
  activeContactId?: string
  isOnline?: (userId: string) => boolean
}>()

const emit = defineEmits<{
  enterDept: [deptId: string]
  breadcrumbClick: [deptId: string]
  contactClick: [person: ContactItem, event: MouseEvent]
}>()
</script>

<template>
  <div class="flex h-full flex-col">
    <div class="border-b border-[var(--ws-border-subtle)] px-5 py-4">
      <h2 class="font-semibold">
        组织内联系人
      </h2>
      <div
        v-if="breadcrumb.length"
        class="mt-1 flex flex-wrap items-center gap-1 text-sm text-[var(--ws-text-muted)]"
      >
        <template
          v-for="(node, index) in breadcrumb"
          :key="node.id"
        >
          <button
            v-if="index < breadcrumb.length - 1"
            type="button"
            class="truncate hover:text-[var(--ui-primary)]"
            @click="emit('breadcrumbClick', node.id)"
          >
            {{ node.name }}
          </button>
          <span
            v-else
            class="truncate font-medium text-[var(--ws-text)]"
          >{{ node.name }}</span>
          <UIcon
            v-if="index < breadcrumb.length - 1"
            name="i-lucide-chevron-right"
            class="size-3.5 shrink-0 opacity-60"
          />
        </template>
      </div>
    </div>

    <div class="min-h-0 flex-1 overflow-y-auto p-3">
      <div
        v-if="loading"
        class="space-y-2"
      >
        <USkeleton
          v-for="i in 6"
          :key="i"
          class="h-14 w-full"
        />
      </div>

      <template v-else>
        <div
          v-if="childDepts.length"
          class="mb-2 space-y-0.5"
        >
          <button
            v-for="dept in childDepts"
            :key="dept.id"
            type="button"
            class="workspace-list-item flex w-full items-center gap-3 px-3 py-2.5 text-left"
            @click="emit('enterDept', dept.id)"
          >
            <div class="flex size-9 shrink-0 items-center justify-center rounded-lg bg-[var(--ui-primary)]/10">
              <UIcon
                name="i-lucide-folder"
                class="size-4 text-[var(--ui-primary)]"
              />
            </div>
            <div class="min-w-0 flex-1">
              <p class="truncate font-medium">
                {{ dept.name }}
              </p>
            </div>
            <span class="shrink-0 text-sm text-[var(--ui-primary)]">下级</span>
          </button>
        </div>

        <div
          v-if="members.length"
          class="space-y-0.5"
        >
          <button
            v-for="person in members"
            :key="person.id"
            type="button"
            class="workspace-list-item flex w-full items-center gap-3 px-3 py-2.5 text-left"
            :data-active="activeContactId === person.id"
            @click="emit('contactClick', person, $event)"
          >
            <WorkspaceAvatar
              :text="person.avatarText"
              :seed="person.id"
              size="md"
            />
            <div class="min-w-0 flex-1">
              <p class="truncate font-medium">
                {{ person.nickname }}
              </p>
              <p class="truncate text-sm text-[var(--ws-text-muted)]">
                {{ person.deptName }} · @{{ person.username }}
              </p>
            </div>
            <span
              class="size-2 shrink-0 rounded-full"
              :class="isOnline?.(person.id) ? 'bg-success' : 'bg-[var(--ws-text-muted)]'"
            />
          </button>
        </div>

        <UEmpty
          v-if="!childDepts.length && !members.length"
          icon="i-lucide-users"
          title="暂无成员"
          description="该部门下暂无同事或子部门"
          class="py-12"
        />
      </template>
    </div>
  </div>
</template>
