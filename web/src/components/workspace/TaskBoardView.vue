<script setup lang="ts">
import { ref } from 'vue'
import type { TaskItem } from '../../api/app/task'
import { isOverdueTask } from '../../stores/tasks'
import type { TaskGroupBucket } from '../../stores/tasks/groupByLocal'

const props = defineProps<{
  buckets: TaskGroupBucket[]
  canDrag: boolean
  selectedId?: string | null
  loading?: boolean
}>()

const emit = defineEmits<{
  open: [taskId: string]
  move: [payload: { taskId: string, bucketKey: string, beforeId: string | null }]
}>()

const draggingId = ref<string | null>(null)
const overBucketKey = ref<string | null>(null)
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
  overBucketKey.value = null
  overBeforeId.value = null
}

function onColumnDragOver(event: DragEvent, bucketKey: string) {
  if (!props.canDrag || !draggingId.value) {
    return
  }
  event.preventDefault()
  overBucketKey.value = bucketKey
  overBeforeId.value = null
}

function onCardDragOver(event: DragEvent, bucketKey: string, beforeId: string) {
  if (!props.canDrag || !draggingId.value || draggingId.value === beforeId) {
    return
  }
  event.preventDefault()
  event.stopPropagation()
  overBucketKey.value = bucketKey
  overBeforeId.value = beforeId
}

function onDropColumn(event: DragEvent, bucketKey: string) {
  event.preventDefault()
  const taskId = event.dataTransfer?.getData('text/plain') || draggingId.value
  if (!taskId || !props.canDrag) {
    onDragEnd()
    return
  }
  emit('move', {
    taskId,
    bucketKey,
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
      v-for="bucket in buckets"
      :key="bucket.key"
      class="flex w-72 shrink-0 flex-col rounded-xl border border-[var(--ws-border-subtle)] bg-[var(--ws-panel-bg)]/60"
      :class="overBucketKey === bucket.key && overBeforeId === null ? 'ring-2 ring-primary/30' : ''"
      @dragover="onColumnDragOver($event, bucket.key)"
      @drop="onDropColumn($event, bucket.key)"
    >
      <header class="flex items-center justify-between border-b border-[var(--ws-border-subtle)] px-3 py-2">
        <h3 class="text-sm font-medium">
          {{ bucket.label }}
        </h3>
        <span class="text-xs text-[var(--ws-text-muted)]">
          {{ bucket.items.length }}
        </span>
      </header>
      <div class="flex min-h-0 flex-1 flex-col gap-2 overflow-y-auto p-2">
        <div
          v-for="task in bucket.items"
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
          @dragover="onCardDragOver($event, bucket.key, task.id)"
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
          v-if="!bucket.items.length"
          class="px-2 py-6 text-center text-xs text-[var(--ws-text-muted)]"
        >
          {{ canDrag ? '拖到此处' : '暂无任务' }}
        </p>
      </div>
    </section>
  </div>
</template>
