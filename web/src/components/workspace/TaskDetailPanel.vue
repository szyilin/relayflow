<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { TaskItem } from '../../api/app/task'
import { useTasksStore } from '../../stores/tasks'

const props = defineProps<{
  task: TaskItem | null
  subtasks: TaskItem[]
  loading?: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const tasksStore = useTasksStore()
const toast = useToast()

const form = reactive({
  title: '',
  startLocal: '',
  dueLocal: '',
  remindBeforeMinutes: '' as string,
  description: ''
})

const remindOptions = [
  { label: '默认（系统窗口）', value: '' },
  { label: '不提醒', value: '0' },
  { label: '截止前 5 分钟', value: '5' },
  { label: '截止前 15 分钟', value: '15' },
  { label: '截止前 30 分钟', value: '30' },
  { label: '截止前 1 小时', value: '60' },
  { label: '截止前 1 天', value: '1440' }
]

const newSubtaskTitle = ref('')

function toLocalInput(iso?: string | null): string {
  if (!iso) {
    return ''
  }
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) {
    return ''
  }
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function fromLocalInput(local: string): string | null {
  if (!local.trim()) {
    return null
  }
  const d = new Date(local)
  if (Number.isNaN(d.getTime())) {
    return null
  }
  return d.toISOString()
}

function syncForm(task: TaskItem | null) {
  if (!task) {
    form.title = ''
    form.startLocal = ''
    form.dueLocal = ''
    form.remindBeforeMinutes = ''
    form.description = ''
    return
  }
  form.title = task.title
  form.startLocal = toLocalInput(task.startTime)
  form.dueLocal = toLocalInput(task.dueTime)
  form.remindBeforeMinutes = task.remindBeforeMinutes == null ? '' : String(task.remindBeforeMinutes)
  form.description = task.description ?? ''
}

watch(() => props.task, (t) => {
  syncForm(t)
}, { immediate: true, deep: true })

const progressLabel = computed(() => {
  const total = props.subtasks.length
  const done = props.subtasks.filter(s => s.status === 'DONE').length
  return `${done}/${total}`
})

const progressPct = computed(() => {
  const total = props.subtasks.length
  if (!total) {
    return 0
  }
  return Math.round((props.subtasks.filter(s => s.status === 'DONE').length / total) * 100)
})

async function handleSave() {
  if (!props.task) {
    return
  }
  const title = form.title.trim()
  if (!title) {
    toast.add({ title: '请输入标题', color: 'error' })
    return
  }
  const startTime = fromLocalInput(form.startLocal)
  const dueTime = fromLocalInput(form.dueLocal)
  if (startTime && dueTime && new Date(startTime) > new Date(dueTime)) {
    toast.add({ title: '开始时间不能晚于截止时间', color: 'error' })
    return
  }
  const remindRaw = form.remindBeforeMinutes
  const remindBeforeMinutes = remindRaw === '' ? null : Number(remindRaw)
  try {
    await tasksStore.saveDetail({
      id: props.task.id,
      title,
      startTime,
      dueTime,
      remindBeforeMinutes: Number.isFinite(remindBeforeMinutes as number)
        ? (remindBeforeMinutes as number)
        : null,
      description: form.description.trim() || null
    })
    toast.add({ title: '已保存', color: 'success' })
  } catch {
    toast.add({ title: '保存失败', color: 'error' })
  }
}

async function handleComplete() {
  if (!props.task) {
    return
  }
  const done = props.task.status !== 'DONE'
  try {
    await tasksStore.setTaskDone(props.task.id, done)
  } catch {
    toast.add({ title: '更新失败', color: 'error' })
  }
}

async function handleAddSubtask() {
  if (!props.task) {
    return
  }
  const title = newSubtaskTitle.value.trim()
  if (!title) {
    toast.add({ title: '请输入子任务标题', color: 'error' })
    return
  }
  try {
    await tasksStore.addSubtask(props.task.id, title)
    newSubtaskTitle.value = ''
  } catch {
    toast.add({ title: '添加子任务失败', color: 'error' })
  }
}

async function handleToggleSubtask(id: string, done: boolean) {
  try {
    await tasksStore.setTaskDone(id, done)
  } catch {
    toast.add({ title: '更新失败', color: 'error' })
  }
}

async function handleDeleteSubtask(id: string) {
  try {
    await tasksStore.removeTask(id)
  } catch {
    toast.add({ title: '删除失败', color: 'error' })
  }
}
</script>

<template>
  <div class="flex h-full flex-col border-l border-[var(--ws-border-subtle)] bg-[var(--ws-panel-bg)]">
    <header class="flex shrink-0 items-center gap-2 border-b border-[var(--ws-border-subtle)] px-4 py-3">
      <UButton
        color="primary"
        size="sm"
        :variant="task?.status === 'DONE' ? 'soft' : 'solid'"
        :disabled="!task"
        @click="handleComplete"
      >
        {{ task?.status === 'DONE' ? '重新打开' : '完成任务' }}
      </UButton>
      <div class="flex-1" />
      <UButton
        color="neutral"
        variant="ghost"
        icon="i-lucide-x"
        size="sm"
        aria-label="关闭"
        @click="emit('close')"
      />
    </header>

    <div v-if="loading" class="flex flex-1 items-center justify-center">
      <UIcon name="i-lucide-loader-circle" class="size-6 animate-spin text-[var(--ws-text-muted)]" />
    </div>

    <div v-else-if="!task" class="flex flex-1 items-center justify-center p-6 text-sm text-[var(--ws-text-muted)]">
      选择左侧任务查看详情
    </div>

    <div v-else class="min-h-0 flex-1 space-y-5 overflow-y-auto p-4">
      <UInput
        v-model="form.title"
        size="lg"
        class="w-full font-semibold"
        placeholder="任务标题"
        @blur="handleSave"
      />

      <div class="space-y-3 text-sm">
        <div class="flex items-start gap-3">
          <UIcon name="i-lucide-calendar-clock" class="mt-2 size-4 shrink-0 text-[var(--ws-text-muted)]" />
          <div class="min-w-0 flex-1 space-y-2">
            <UFormField label="开始">
              <UInput
                v-model="form.startLocal"
                type="datetime-local"
                class="w-full"
                @change="handleSave"
              />
            </UFormField>
            <UFormField label="截止">
              <UInput
                v-model="form.dueLocal"
                type="datetime-local"
                class="w-full"
                @change="handleSave"
              />
            </UFormField>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <UIcon name="i-lucide-bell" class="size-4 shrink-0 text-[var(--ws-text-muted)]" />
          <USelect
            v-model="form.remindBeforeMinutes"
            :items="remindOptions"
            class="w-full"
            @update:model-value="handleSave"
          />
        </div>
      </div>

      <UFormField label="描述">
        <UTextarea
          v-model="form.description"
          :rows="4"
          placeholder="添加描述"
          @blur="handleSave"
        />
      </UFormField>

      <section class="space-y-2">
        <div class="flex items-center justify-between gap-2">
          <h3 class="text-sm font-semibold">
            子任务
          </h3>
          <span class="text-xs text-[var(--ws-text-muted)]">{{ progressLabel }}</span>
        </div>
        <div class="h-1.5 overflow-hidden rounded-full bg-[var(--ws-border-subtle)]">
          <div
            class="h-full rounded-full bg-primary transition-all"
            :style="{ width: `${progressPct}%` }"
          />
        </div>
        <ul class="space-y-1">
          <li
            v-for="sub in subtasks"
            :key="sub.id"
            class="flex items-center gap-2 rounded-lg px-1 py-1.5 hover:bg-[var(--ws-rail-hover)]"
          >
            <UCheckbox
              :model-value="sub.status === 'DONE'"
              @update:model-value="(v: boolean | 'indeterminate') => handleToggleSubtask(sub.id, v === true)"
            />
            <span
              class="min-w-0 flex-1 truncate text-sm"
              :class="sub.status === 'DONE' ? 'line-through text-[var(--ws-text-muted)]' : ''"
            >
              {{ sub.title }}
            </span>
            <UButton
              color="neutral"
              variant="ghost"
              icon="i-lucide-trash-2"
              size="xs"
              @click="handleDeleteSubtask(sub.id)"
            />
          </li>
        </ul>
        <div class="flex gap-2">
          <UInput
            v-model="newSubtaskTitle"
            placeholder="添加子任务"
            class="flex-1"
            @keydown.enter.prevent="handleAddSubtask"
          />
          <UButton
            color="neutral"
            variant="soft"
            icon="i-lucide-plus"
            :loading="tasksStore.saving"
            @click="handleAddSubtask"
          />
        </div>
      </section>
    </div>
  </div>
</template>
