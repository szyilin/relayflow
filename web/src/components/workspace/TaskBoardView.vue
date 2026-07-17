<script setup lang="ts">
import { ref } from 'vue'
import type { TaskItem } from '../../api/app/task'
import { isOverdueTask } from '../../stores/tasks'
import {
  BOARD_COLUMN_LABELS,
  BOARD_STATUSES,
  type BoardStatus
} from '../../stores/tasks/boardLocal'

const props = defineProps<{
  columns: Record<BoardStatus, TaskItem[]>
  canDrag: boolean
  selectedId?: string | null
  loading?: boolean
}>()

const emit = defineEmits<{
  open: [taskId: string]
  move: [payload: { taskId: string, status: BoardStatus, beforeId: string | null }]
}>()

const draggingId = ref<string | null>(null)
const overColumn = ref<BoardStatus | null>(null)
const overBeforeId = ref<string | null>(null)

function formatDue(iso?: string | null) {
  if (!iso) {
    return undefined
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

function onDragStart(event: DragEvent, taskId: string) {
  if (!props.canDrag) {
    event.preventDefault()
    return
  }
  draggingId.value = taskId
  event.dataTransfer?.setData('text/plain', taskId)
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
  }
}

function onDragEnd() {
  draggingId.value = null
  overColumn.value = null
  overBeforeId.value = null
}

function onColumnDragOver(event: DragEvent, status: BoardStatus) {
  if (!props.canDrag || !draggingId.value) {
    return
  }
  event.preventDefault()
  overColumn.value = status
  overBeforeId.value = null
}

function onCardDragOver(event: DragEvent, status: BoardStatus, beforeId: string) {
  if (!props.canDrag || !draggingId.value || draggingId.value === beforeId) {
    return
  }
  event.preventDefault()
  event.stopPropagation()
  overColumn.value = status
  overBeforeId.value = beforeId
}

function onDropColumn(event: DragEvent, status: BoardStatus) {
  event.preventDefault()
  const taskId = event.dataTransfer?.getData('text/plain') || draggingId.value
  if (!taskId || !props.canDrag) {
    onDragEnd()
    return
  }
  emit('move', {
    taskId,
    status,
    beforeId: overBeforeId.value
  })
  onDragEnd()
}
</script>

<template>
  <div v-if="loading" class="flex justify-center py-12">
    <UIcon name="i-lucide-loader-circle" class="size-6 animate-spin text-[var(--ws-text-muted)]" />
  </div>
  <div
    v-else
    class="flex h-full min-h-[420px] gap-3 overflow-x-auto pb-2"
  >
    <section
      v-for="status in BOARD_STATUSES"
      :key="status"
      class="flex w-72 shrink-0 flex-col rounded-xl border border-[var(--ws-border-subtle)] bg-[var(--ws-panel-bg)]/60"
      :class="overColumn === status && overBeforeId === null ? 'ring-2 ring-primary/30' : ''"
      @dragover="onColumnDragOver($event, status)"
      @drop="onDropColumn($event, status)"
    >
      <header class="flex items-center justify-between border-b border-[var(--ws-border-subtle)] px-3 py-2">
        <h3 class="text-sm font-medium">
          {{ BOARD_COLUMN_LABELS[status] }}
        </h3>
        <span class="text-xs text-[var(--ws-text-muted)]">
          {{ columns[status].length }}
        </span>
      </header>
      <div class="flex min-h-0 flex-1 flex-col gap-2 overflow-y-auto p-2">
        <div
          v-for="task in columns[status]"
          :key="task.id"
          class="rounded-lg border px-3 py-2 transition-colors"
          :class="[
            selectedId === task.id
              ? 'border-primary/50 bg-primary/5 ring-1 ring-primary/20'
              : 'border-[var(--ws-border-subtle)] bg-[var(--ws-panel-bg)]',
            canDrag ? 'cursor-grab active:cursor-grabbing' : 'cursor-pointer',
            draggingId === task.id ? 'opacity-50' : '',
            overBeforeId === task.id ? 'border-t-2 border-t-primary' : ''
          ]"
          :draggable="canDrag"
          role="button"
          tabindex="0"
          @dragstart="onDragStart($event, task.id)"
          @dragend="onDragEnd"
          @dragover="onCardDragOver($event, status, task.id)"
          @click="emit('open', task.id)"
          @keydown.enter.prevent="emit('open', task.id)"
        >
          <p
            class="text-sm font-medium"
            :class="task.status === 'DONE' ? 'line-through text-[var(--ws-text-muted)]' : ''"
          >
            {{ task.title }}
          </p>
          <p class="mt-1 flex flex-wrap gap-x-2 text-xs text-[var(--ws-text-muted)]">
            <span
              v-if="formatDue(task.dueTime)"
              :class="isOverdueTask(task) ? 'font-medium text-red-600 dark:text-red-400' : ''"
            >
              {{ isOverdueTask(task) ? '已逾期' : '截止' }} {{ formatDue(task.dueTime) }}
            </span>
            <span v-if="subtaskHint(task)">子任务 {{ subtaskHint(task) }}</span>
          </p>
        </div>
        <p
          v-if="!columns[status].length"
          class="px-2 py-6 text-center text-xs text-[var(--ws-text-muted)]"
        >
          {{ canDrag ? '拖到此处' : '暂无任务' }}
        </p>
      </div>
    </section>
  </div>
</template>
