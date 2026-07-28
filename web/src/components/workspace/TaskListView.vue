<script setup lang="ts">
import { computed } from 'vue'
import type { TaskItem } from '../../api/app/task'
import type { TaskGroupBucket } from '../../stores/tasks/groupByLocal'
import { resolveAssigneeIds } from '../../stores/tasks/assigneeLocal'
import { isOverdueTask } from '../../stores/tasks'
import { useTaskViewConfigStore } from '../../stores/tasks/viewConfigStore'
import WorkspaceOwnerChip from './WorkspaceOwnerChip.vue'

const props = withDefaults(defineProps<{
  buckets: TaskGroupBucket[]
  selectedId?: string | null
  showGroupHeaders?: boolean
  showToggle?: boolean
  showDelete?: boolean
  deletableBucketKeys?: string[]
  assigneeLabel?: (userId: string) => string
}>(), {
  selectedId: null,
  showGroupHeaders: false,
  showToggle: true,
  showDelete: true,
  deletableBucketKeys: () => [],
  assigneeLabel: (userId: string) => userId
})

const emit = defineEmits<{
  open: [taskId: string]
  toggle: [payload: { taskId: string, done: boolean }]
  delete: [taskId: string]
  deleteBucket: [bucketKey: string]
}>()

const viewConfig = useTaskViewConfigStore()

const ASSIGNEE_CHIP_LIMIT = 3

const showDue = computed(() => viewConfig.isFieldVisible('dueTime'))
const showStatus = computed(() => viewConfig.isFieldVisible('status'))
const showAssignee = computed(() => viewConfig.isFieldVisible('assignee'))

/** Shared grid for header + every row — grouping must not change column tracks. */
const gridTemplateColumns = computed(() => {
  const cols = ['minmax(12rem, 1fr)']
  if (showDue.value) {
    cols.push('9.5rem')
  }
  if (showStatus.value) {
    cols.push('5.5rem')
  }
  if (showAssignee.value) {
    cols.push('minmax(7rem, 10rem)')
  }
  if (props.showDelete) {
    cols.push('2.5rem')
  }
  return cols.join(' ')
})

function formatDue(iso?: string | null) {
  if (!iso) {
    return null
  }
  return new Date(iso).toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function subtaskHint(task: TaskItem) {
  const total = task.subtaskTotal ?? 0
  if (!total) {
    return null
  }
  return `${task.subtaskDoneCount ?? 0}/${total}`
}

function assigneeChipIds(task: TaskItem) {
  return resolveAssigneeIds(task).slice(0, ASSIGNEE_CHIP_LIMIT)
}

function assigneeOverflowCount(task: TaskItem) {
  const total = resolveAssigneeIds(task).length
  return total > ASSIGNEE_CHIP_LIMIT ? total - ASSIGNEE_CHIP_LIMIT : 0
}
</script>

<template>
  <div class="task-list-view min-w-0">
    <!-- Sticky column header (one composition for all groups) -->
    <div
      class="task-list-view__header sticky top-0 z-10 grid items-center gap-x-3 border-b border-[var(--ws-border-subtle)] bg-[var(--ws-panel-bg)] px-3 py-2 text-xs text-[var(--ws-text-muted)]"
      :style="{ gridTemplateColumns }"
      role="row"
    >
      <div class="flex min-w-0 items-center gap-1.5" role="columnheader">
        <UIcon name="i-lucide-text" class="size-3.5 shrink-0 opacity-70" />
        <span>任务标题</span>
      </div>
      <div
        v-if="showDue"
        class="flex min-w-0 items-center gap-1.5"
        role="columnheader"
      >
        <UIcon name="i-lucide-calendar" class="size-3.5 shrink-0 opacity-70" />
        <span>截止时间</span>
      </div>
      <div
        v-if="showStatus"
        class="flex min-w-0 items-center gap-1.5"
        role="columnheader"
      >
        <UIcon name="i-lucide-circle-dot" class="size-3.5 shrink-0 opacity-70" />
        <span>状态</span>
      </div>
      <div
        v-if="showAssignee"
        class="flex min-w-0 items-center gap-1.5"
        role="columnheader"
      >
        <UIcon name="i-lucide-user" class="size-3.5 shrink-0 opacity-70" />
        <span>负责人</span>
      </div>
      <div v-if="showDelete" aria-hidden="true" />
    </div>

    <div class="divide-y divide-[var(--ws-border-subtle)]/60">
      <section
        v-for="bucket in buckets"
        :key="bucket.key"
        class="min-w-0"
      >
        <header
          v-if="showGroupHeaders"
          class="flex items-center justify-between gap-2 px-3 py-2.5"
        >
          <h2 class="truncate text-sm font-medium text-[var(--ws-text)]">
            {{ bucket.label }}
          </h2>
          <div class="flex shrink-0 items-center gap-1">
            <span class="text-xs text-[var(--ws-text-muted)]">
              {{ bucket.items.length }}
            </span>
            <UButton
              v-if="deletableBucketKeys.includes(bucket.key)"
              color="neutral"
              variant="ghost"
              icon="i-lucide-trash-2"
              size="xs"
              aria-label="删除分组"
              @click="emit('deleteBucket', bucket.key)"
            />
          </div>
        </header>

        <p
          v-if="showGroupHeaders && !bucket.items.length"
          class="px-3 py-3 text-xs text-[var(--ws-text-muted)]"
        >
          暂无任务
        </p>

        <div
          v-for="task in bucket.items"
          :key="task.id"
          role="button"
          tabindex="0"
          class="group/row grid cursor-pointer items-center gap-x-3 px-3 py-2.5 transition-colors"
          :class="selectedId === task.id
            ? 'bg-primary/5 ring-1 ring-inset ring-primary/20'
            : 'hover:bg-[var(--ws-rail-hover)]/35'"
          :style="{ gridTemplateColumns }"
          @click="emit('open', task.id)"
          @keydown.enter.prevent="emit('open', task.id)"
        >
          <!-- Title -->
          <div class="flex min-w-0 items-center gap-2.5">
            <UCheckbox
              v-if="showToggle"
              class="shrink-0"
              :model-value="task.status === 'DONE'"
              @click.stop
              @update:model-value="(value: boolean | 'indeterminate') => emit('toggle', { taskId: task.id, done: value === true })"
            />
            <div class="min-w-0 flex-1">
              <div class="flex min-w-0 items-center gap-1.5">
                <p
                  class="truncate text-sm font-medium"
                  :class="task.status === 'DONE' ? 'line-through text-[var(--ws-text-muted)]' : 'text-[var(--ws-text)]'"
                >
                  {{ task.title }}
                </p>
                <span
                  v-if="subtaskHint(task)"
                  class="inline-flex shrink-0 items-center gap-0.5 rounded px-1 py-0.5 text-[10px] text-[var(--ws-text-muted)]"
                  title="子任务"
                >
                  <UIcon name="i-lucide-list-tree" class="size-3" />
                  {{ subtaskHint(task) }}
                </span>
              </div>
            </div>
          </div>

          <!-- Due -->
          <div
            v-if="showDue"
            class="min-w-0 truncate text-xs"
            :class="isOverdueTask(task)
              ? 'font-medium text-red-600 dark:text-red-400'
              : 'text-[var(--ws-text-muted)]'"
          >
            <span v-if="formatDue(task.dueTime)" class="inline-flex items-center gap-1">
              <UIcon
                v-if="isOverdueTask(task)"
                name="i-lucide-bell"
                class="size-3 shrink-0"
              />
              {{ formatDue(task.dueTime) }}
            </span>
            <span v-else class="opacity-40">—</span>
          </div>

          <!-- Status -->
          <div
            v-if="showStatus"
            class="min-w-0 truncate text-xs text-[var(--ws-text-muted)]"
          >
            {{ task.status === 'DONE' ? '已完成' : '未完成' }}
          </div>

          <!-- Assignee -->
          <div
            v-if="showAssignee"
            class="flex min-w-0 items-center gap-1 overflow-hidden"
            @click.stop
          >
            <template v-if="assigneeChipIds(task).length">
              <WorkspaceOwnerChip
                v-for="uid in assigneeChipIds(task)"
                :key="uid"
                :user-id="uid"
                :display-name="assigneeLabel(uid)"
              />
              <span
                v-if="assigneeOverflowCount(task)"
                class="shrink-0 text-xs text-[var(--ws-text-muted)]"
              >+{{ assigneeOverflowCount(task) }}</span>
            </template>
            <span v-else class="text-xs text-[var(--ws-text-muted)] opacity-40">—</span>
          </div>

          <!-- Delete -->
          <div
            v-if="showDelete"
            class="flex justify-end opacity-0 transition-opacity group-hover/row:opacity-100 focus-within:opacity-100"
          >
            <UButton
              color="neutral"
              variant="ghost"
              icon="i-lucide-trash-2"
              size="xs"
              aria-label="删除"
              @click.stop="emit('delete', task.id)"
            />
          </div>
        </div>
      </section>
    </div>
  </div>
</template>
