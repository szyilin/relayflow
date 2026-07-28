<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { addHours, formatISO } from 'date-fns'
import MeetingPreJoinModal from '../../../components/workspace/MeetingPreJoinModal.vue'
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'
import { useMeetingStore } from '../../../stores/meeting'
import { useToast } from '../../../composables/useToast'

const router = useRouter()
const meetingStore = useMeetingStore()
const toast = useToast()

const preJoinOpen = ref(false)
const preJoinTitle = ref('视频会议')
const preJoinLabel = ref('开始会议')
const pendingMeetingId = ref<string | null>(null)
const joinCode = ref('')
const scheduleOpen = ref(false)
const scheduleForm = reactive({
  title: '',
  startLocal: '',
  endLocal: ''
})
const busy = ref(false)

onMounted(() => {
  void meetingStore.loadHistory().catch((e: Error) => {
    toast.add({ title: e.message || '加载失败', color: 'error' })
  })
})

function toInputLocal(d: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function openSchedule() {
  const start = new Date()
  start.setMinutes(0, 0, 0)
  start.setHours(start.getHours() + 1)
  const end = addHours(start, 1)
  scheduleForm.title = ''
  scheduleForm.startLocal = toInputLocal(start)
  scheduleForm.endLocal = toInputLocal(end)
  scheduleOpen.value = true
}

async function onStartInstant() {
  busy.value = true
  try {
    const m = await meetingStore.createInstant()
    pendingMeetingId.value = m.id
    preJoinTitle.value = m.title
    preJoinLabel.value = '开始会议'
    preJoinOpen.value = true
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '发起失败', color: 'error' })
  } finally {
    busy.value = false
  }
}

async function onJoin() {
  const code = joinCode.value.trim()
  if (!code) {
    toast.add({ title: '请输入会议号', color: 'warning' })
    return
  }
  busy.value = true
  try {
    const m = await meetingStore.resolveByCode(code)
    pendingMeetingId.value = m.id
    preJoinTitle.value = m.title
    preJoinLabel.value = '加入会议'
    preJoinOpen.value = true
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '加入失败', color: 'error' })
  } finally {
    busy.value = false
  }
}

async function onScheduleSubmit() {
  if (!scheduleForm.title.trim()) {
    toast.add({ title: '请填写会议主题', color: 'warning' })
    return
  }
  busy.value = true
  try {
    const start = new Date(scheduleForm.startLocal)
    const end = new Date(scheduleForm.endLocal)
    await meetingStore.schedule({
      title: scheduleForm.title.trim(),
      scheduledStart: formatISO(start),
      scheduledEnd: formatISO(end)
    })
    scheduleOpen.value = false
    toast.add({
      title: meetingStore.usingMock ? '已预约（Mock，未写入真实日历）' : '已预约并写入日历',
      color: 'success'
    })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '预约失败', color: 'error' })
  } finally {
    busy.value = false
  }
}

async function onPreJoinConfirm(opts: { audio: boolean, video: boolean }) {
  const id = pendingMeetingId.value
  if (!id) {
    return
  }
  preJoinOpen.value = false
  await router.push({
    path: `/app/meeting/${id}/room`,
    query: {
      audio: opts.audio ? '1' : '0',
      video: opts.video ? '1' : '0'
    }
  })
}

function statusLabel(s: string) {
  if (s === 'LIVE') return '进行中'
  if (s === 'SCHEDULED') return '已预约'
  return '已结束'
}

function reopen(meetingId: string, title: string) {
  pendingMeetingId.value = meetingId
  preJoinTitle.value = title
  preJoinLabel.value = '进入会议'
  preJoinOpen.value = true
}
</script>

<template>
  <WorkspaceShell>
    <template #panel>
      <div class="flex h-full flex-col">
        <div class="border-b border-default px-4 py-3">
          <h1 class="text-base font-semibold text-highlighted">
            视频会议
          </h1>
          <p class="mt-0.5 text-xs text-muted">
            发起、加入或预约会议
          </p>
        </div>
        <div class="flex-1 space-y-1 overflow-y-auto p-2">
          <p class="px-2 py-1 text-xs font-medium text-muted">
            历史记录
          </p>
          <button
            v-for="item in meetingStore.history"
            :key="item.id"
            type="button"
            class="flex w-full flex-col gap-0.5 rounded-lg px-3 py-2 text-left transition hover:bg-elevated"
            @click="item.status !== 'ENDED' ? reopen(item.id, item.title) : undefined"
          >
            <span class="truncate text-sm font-medium text-highlighted">{{ item.title }}</span>
            <span class="text-xs text-muted">
              {{ statusLabel(item.status) }} · {{ item.meetingCode }}
            </span>
          </button>
          <p
            v-if="!meetingStore.history.length"
            class="px-3 py-6 text-center text-xs text-muted"
          >
            暂无会议记录
          </p>
        </div>
      </div>
    </template>

    <div class="flex h-full flex-col overflow-y-auto p-6">
      <div class="mb-6 flex items-start justify-between gap-3">
        <div>
          <h2 class="text-lg font-semibold text-highlighted">
            开始协作
          </h2>
          <p class="mt-1 text-sm text-muted">
            多人音视频会议 · 媒体由 LiveKit 承载
            <span v-if="meetingStore.usingMock" class="ml-1 text-warning">（当前 Mock）</span>
          </p>
        </div>
      </div>

      <div class="grid max-w-3xl grid-cols-1 gap-4 sm:grid-cols-2">
        <button
          type="button"
          class="flex aspect-[4/3] flex-col items-start justify-between rounded-2xl bg-sky-500 p-5 text-left text-white shadow-sm transition hover:bg-sky-600 disabled:opacity-60"
          :disabled="busy"
          @click="onStartInstant"
        >
          <UIcon name="i-lucide-video" class="size-8" />
          <span class="text-lg font-semibold">发起会议</span>
        </button>

        <div class="flex aspect-[4/3] flex-col justify-between rounded-2xl bg-indigo-600 p-5 text-white shadow-sm">
          <UIcon name="i-lucide-plus" class="size-8" />
          <div class="w-full space-y-2">
            <span class="text-lg font-semibold">加入会议</span>
            <div class="flex gap-2">
              <UInput
                v-model="joinCode"
                placeholder="会议号"
                size="sm"
                class="flex-1"
                :ui="{ base: 'bg-white/95 text-neutral-900' }"
                @keydown.enter="onJoin"
              />
              <UButton size="sm" color="neutral" variant="solid" :loading="busy" @click="onJoin">
                加入
              </UButton>
            </div>
          </div>
        </div>

        <button
          type="button"
          class="flex aspect-[4/3] flex-col items-start justify-between rounded-2xl bg-orange-500 p-5 text-left text-white shadow-sm transition hover:bg-orange-600"
          @click="openSchedule"
        >
          <UIcon name="i-lucide-calendar" class="size-8" />
          <span class="text-lg font-semibold">预约会议</span>
        </button>

        <div class="flex aspect-[4/3] flex-col items-start justify-between rounded-2xl bg-neutral-200 p-5 text-left text-neutral-500 dark:bg-neutral-800">
          <UIcon name="i-lucide-monitor" class="size-8 opacity-50" />
          <div>
            <span class="text-lg font-semibold">会议室投屏</span>
            <p class="mt-1 text-xs">
              后续版本
            </p>
          </div>
        </div>
      </div>
    </div>
  </WorkspaceShell>

  <MeetingPreJoinModal
    v-model:open="preJoinOpen"
    :title="preJoinTitle"
    :primary-label="preJoinLabel"
    @confirm="onPreJoinConfirm"
  />

  <UModal v-model:open="scheduleOpen" title="预约会议">
    <template #body>
      <div class="space-y-3">
        <UFormField label="主题">
          <UInput v-model="scheduleForm.title" class="w-full" placeholder="会议主题" />
        </UFormField>
        <UFormField label="开始">
          <UInput v-model="scheduleForm.startLocal" type="datetime-local" class="w-full" />
        </UFormField>
        <UFormField label="结束">
          <UInput v-model="scheduleForm.endLocal" type="datetime-local" class="w-full" />
        </UFormField>
      </div>
    </template>
    <template #footer>
      <div class="flex justify-end gap-2">
        <UButton color="neutral" variant="ghost" @click="scheduleOpen = false">
          取消
        </UButton>
        <UButton color="primary" :loading="busy" @click="onScheduleSubmit">
          预约
        </UButton>
      </div>
    </template>
  </UModal>
</template>

<route lang="yaml">
meta:
  layout: workspace
</route>
