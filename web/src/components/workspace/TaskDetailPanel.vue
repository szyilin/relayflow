<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { TaskItem } from '../../api/app/task'
import { ApiError } from '../../api/request'
import { useAuthStore } from '../../stores/auth'
import { useContactsStore } from '../../stores/contacts'
import { useTasksStore } from '../../stores/tasks'
import { resolveAssigneeIds } from '../../stores/tasks/assigneeLocal'

const props = defineProps<{
  task: TaskItem | null
  subtasks: TaskItem[]
  loading?: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const tasksStore = useTasksStore()
const auth = useAuthStore()
const contacts = useContactsStore()
const toast = useToast()

/** Reka Select 禁止 SelectItem value 为空串；用哨兵表示 null（沿用系统窗口） */
const REMIND_DEFAULT = 'default'

const form = reactive({
  title: '',
  startLocal: '',
  dueLocal: '',
  remindBeforeMinutes: REMIND_DEFAULT as string,
  description: ''
})

const remindOptions = [
  { label: '默认（系统窗口）', value: REMIND_DEFAULT },
  { label: '不提醒', value: '0' },
  { label: '截止前 5 分钟', value: '5' },
  { label: '截止前 15 分钟', value: '15' },
  { label: '截止前 30 分钟', value: '30' },
  { label: '截止前 1 小时', value: '60' },
  { label: '截止前 1 天', value: '1440' }
]

const newSubtaskTitle = ref('')
const newComment = ref('')
const assignOpen = ref(false)
const memberKeyword = ref('')
const membersLoaded = ref(false)
const draftAssigneeIds = ref<string[]>([])

const progressLabel = computed(() => {
  const total = props.subtasks.length
  if (!total) {
    return '0/0'
  }
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

const currentAssigneeIds = computed(() =>
  props.task ? resolveAssigneeIds(props.task) : []
)

function nicknameFor(userId: string): string {
  if (String(auth.userId) === userId) {
    return auth.user?.nickname || auth.user?.username || '我'
  }
  const member = contacts.members.find(m => m.id === userId)
  return member?.nickname || `用户 ${userId}`
}

const assigneeLabels = computed(() =>
  currentAssigneeIds.value.map(id => nicknameFor(id))
)

const assignerLabel = computed(() => {
  const id = props.task?.assignerId
  if (!id) {
    return null
  }
  return nicknameFor(String(id))
})

const candidateMembers = computed(() => {
  const q = memberKeyword.value.trim().toLowerCase()
  return contacts.members.filter((member) => {
    if (!q) {
      return true
    }
    return member.nickname.toLowerCase().includes(q)
      || member.username.toLowerCase().includes(q)
  })
})

function isDraftSelected(id: string) {
  return draftAssigneeIds.value.includes(id)
}

function toggleDraftAssignee(id: string) {
  if (isDraftSelected(id)) {
    draftAssigneeIds.value = draftAssigneeIds.value.filter(x => x !== id)
  } else {
    draftAssigneeIds.value = [...draftAssigneeIds.value, id]
  }
}

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
    form.remindBeforeMinutes = REMIND_DEFAULT
    form.description = ''
    return
  }
  form.title = task.title
  form.startLocal = toLocalInput(task.startTime)
  form.dueLocal = toLocalInput(task.dueTime)
  form.remindBeforeMinutes = task.remindBeforeMinutes == null
    ? REMIND_DEFAULT
    : String(task.remindBeforeMinutes)
  form.description = task.description ?? ''
}

watch(() => props.task, (t) => {
  syncForm(t)
}, { immediate: true, deep: true })

function formatTime(iso?: string | null) {
  if (!iso) {
    return ''
  }
  return new Date(iso).toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function ensureMembers() {
  if (membersLoaded.value && contacts.members.length) {
    return
  }
  try {
    await contacts.fetchDepts()
    const rootId = contacts.rootDeptId()
    if (rootId) {
      await contacts.fetchMembers({ deptId: rootId })
    }
    membersLoaded.value = true
  } catch {
    // member list optional for assign UI
  }
}

async function openAssign() {
  await ensureMembers()
  memberKeyword.value = ''
  draftAssigneeIds.value = [...currentAssigneeIds.value]
  // Ensure current user appears as a toggle option even if not in contacts yet
  const selfId = auth.userId != null ? String(auth.userId) : ''
  if (selfId && !contacts.members.some(m => m.id === selfId)) {
    // draft can still include self; chips use nicknameFor
  }
  assignOpen.value = true
}

async function handleSaveAssignees() {
  if (!props.task) {
    return
  }
  try {
    await tasksStore.setAssignees(props.task.id, [...draftAssigneeIds.value])
    assignOpen.value = false
    const n = draftAssigneeIds.value.length
    toast.add({
      title: n ? `已更新负责人（${n}）` : '已清除负责人',
      color: 'success'
    })
  } catch (error) {
    const message = error instanceof ApiError ? error.message : '更新负责人失败'
    toast.add({ title: message, color: 'error' })
  }
}

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
  const remindBeforeMinutes = remindRaw === REMIND_DEFAULT ? null : Number(remindRaw)
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
  } catch (error) {
    const message = error instanceof ApiError ? error.message : '保存失败'
    toast.add({ title: message, color: 'error' })
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

async function handleToggleFollow() {
  if (!props.task) {
    return
  }
  const next = !tasksStore.iAmFollowing
  try {
    await tasksStore.toggleFollow(props.task.id, next)
    toast.add({ title: next ? '已关注' : '已取消关注', color: 'success' })
  } catch {
    toast.add({ title: '操作失败', color: 'error' })
  }
}

async function handleAddComment() {
  if (!props.task) {
    return
  }
  const content = newComment.value.trim()
  if (!content) {
    toast.add({ title: '请输入评论', color: 'error' })
    return
  }
  try {
    await tasksStore.addComment(props.task.id, content)
    newComment.value = ''
  } catch {
    toast.add({ title: '发表评论失败', color: 'error' })
  }
}
</script>

<template>
  <div class="flex h-full min-h-0 flex-col bg-[var(--ws-panel-bg)]">
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
      <UButton
        v-if="task"
        color="neutral"
        size="sm"
        variant="soft"
        :icon="tasksStore.iAmFollowing ? 'i-lucide-eye-off' : 'i-lucide-eye'"
        @click="handleToggleFollow"
      >
        {{ tasksStore.iAmFollowing ? '取消关注' : '关注' }}
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
      加载中…
    </div>

    <div v-else class="min-h-0 flex-1 space-y-5 overflow-y-auto p-4">
      <UInput
        v-model="form.title"
        size="lg"
        class="w-full font-semibold"
        placeholder="任务标题"
        @blur="handleSave"
      />

      <div class="flex items-start gap-3 text-sm">
        <UIcon name="i-lucide-users" class="mt-0.5 size-4 shrink-0 text-[var(--ws-text-muted)]" />
        <div class="min-w-0 flex-1">
          <p class="text-xs text-[var(--ws-text-muted)]">
            负责人
          </p>
          <div v-if="assigneeLabels.length" class="mt-1 flex flex-wrap gap-1.5">
            <UBadge
              v-for="(label, idx) in assigneeLabels"
              :key="currentAssigneeIds[idx]"
              color="neutral"
              variant="subtle"
            >
              {{ label }}
            </UBadge>
          </div>
          <p v-else class="font-medium">
            未指定
          </p>
        </div>
        <UButton color="neutral" variant="soft" size="xs" @click="openAssign">
          编辑
        </UButton>
      </div>

      <div
        v-if="assignerLabel"
        class="flex items-center gap-3 text-sm"
      >
        <UIcon name="i-lucide-user-cog" class="size-4 shrink-0 text-[var(--ws-text-muted)]" />
        <div class="min-w-0 flex-1">
          <p class="text-xs text-[var(--ws-text-muted)]">
            分配人
          </p>
          <p class="font-medium">
            {{ assignerLabel }}
          </p>
        </div>
      </div>

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
        <h3 class="text-sm font-semibold">
          关注人
        </h3>
        <div v-if="tasksStore.selectedFollowers.length" class="flex flex-wrap gap-2">
          <UBadge
            v-for="f in tasksStore.selectedFollowers"
            :key="f.userId"
            color="neutral"
            variant="subtle"
          >
            {{ f.nickname }}
          </UBadge>
        </div>
        <p v-else class="text-xs text-[var(--ws-text-muted)]">
          暂无关注人
        </p>
      </section>

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

      <section class="space-y-2">
        <h3 class="text-sm font-semibold">
          评论
        </h3>
        <ul class="space-y-3">
          <li
            v-for="c in tasksStore.selectedComments"
            :key="c.id"
            class="rounded-lg bg-[var(--ws-rail-hover)]/40 px-3 py-2"
          >
            <div class="flex items-baseline justify-between gap-2 text-xs text-[var(--ws-text-muted)]">
              <span class="font-medium text-[var(--ws-text)]">{{ c.authorNickname }}</span>
              <span>{{ formatTime(c.createTime) }}</span>
            </div>
            <p class="mt-1 whitespace-pre-wrap text-sm">
              {{ c.content }}
            </p>
          </li>
        </ul>
        <div class="flex gap-2">
          <UTextarea
            v-model="newComment"
            :rows="2"
            placeholder="写评论…"
            class="flex-1"
          />
          <UButton
            color="primary"
            variant="soft"
            class="self-end"
            :loading="tasksStore.saving"
            @click="handleAddComment"
          >
            发送
          </UButton>
        </div>
      </section>

      <section class="space-y-2 pb-4">
        <h3 class="text-sm font-semibold">
          活动
        </h3>
        <ul class="space-y-2">
          <li
            v-for="a in tasksStore.selectedActivities"
            :key="a.id"
            class="flex gap-2 text-xs text-[var(--ws-text-muted)]"
          >
            <span class="shrink-0 font-medium text-[var(--ws-text)]">{{ a.actorNickname }}</span>
            <span class="min-w-0 flex-1">{{ a.summary }}</span>
            <span class="shrink-0">{{ formatTime(a.createTime) }}</span>
          </li>
        </ul>
        <p v-if="!tasksStore.selectedActivities.length" class="text-xs text-[var(--ws-text-muted)]">
          暂无活动
        </p>
      </section>
    </div>

    <UModal v-model:open="assignOpen" title="编辑负责人">
      <template #body>
        <div class="space-y-3">
          <UInput
            v-model="memberKeyword"
            placeholder="搜索成员"
            icon="i-lucide-search"
          />
          <p class="text-xs text-[var(--ws-text-muted)]">
            若保存后自己不在负责人中，你将成为分配人，任务会出现在「我分配的」。
          </p>
          <ul class="max-h-64 space-y-1 overflow-y-auto">
            <li v-if="auth.userId">
              <button
                type="button"
                class="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm hover:bg-[var(--ws-rail-hover)]"
                @click="toggleDraftAssignee(String(auth.userId))"
              >
                <UCheckbox
                  :model-value="isDraftSelected(String(auth.userId))"
                  class="pointer-events-none"
                  tabindex="-1"
                />
                <UAvatar :alt="nicknameFor(String(auth.userId))" size="xs" />
                <span class="font-medium">{{ nicknameFor(String(auth.userId)) }}</span>
                <span class="text-xs text-[var(--ws-text-muted)]">我</span>
              </button>
            </li>
            <li
              v-for="m in candidateMembers.filter(m => m.id !== String(auth.userId ?? ''))"
              :key="m.id"
            >
              <button
                type="button"
                class="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm hover:bg-[var(--ws-rail-hover)]"
                @click="toggleDraftAssignee(m.id)"
              >
                <UCheckbox
                  :model-value="isDraftSelected(m.id)"
                  class="pointer-events-none"
                  tabindex="-1"
                />
                <UAvatar :alt="m.nickname" size="xs" />
                <span class="font-medium">{{ m.nickname }}</span>
                <span class="text-xs text-[var(--ws-text-muted)]">{{ m.username }}</span>
              </button>
            </li>
          </ul>
          <UEmpty
            v-if="!candidateMembers.length && !auth.userId"
            icon="i-lucide-users"
            title="无匹配成员"
            description="请调整关键词或确认通讯录已加载"
          />
          <div class="flex justify-end gap-2 pt-1">
            <UButton color="neutral" variant="soft" @click="assignOpen = false">
              取消
            </UButton>
            <UButton color="primary" :loading="tasksStore.saving" @click="handleSaveAssignees">
              保存
            </UButton>
          </div>
        </div>
      </template>
    </UModal>
  </div>
</template>
