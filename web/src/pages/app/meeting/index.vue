<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MeetingPreJoinModal from '../../../components/workspace/MeetingPreJoinModal.vue'
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'
import { getMeeting } from '../../../api/app/meeting'
import { useMeetingStore } from '../../../stores/meeting'
import { useToast } from '../../../composables/useToast'

const route = useRoute()
const router = useRouter()
const meetingStore = useMeetingStore()
const toast = useToast()

const preJoinOpen = ref(false)
const preJoinTitle = ref('视频会议')
const preJoinLabel = ref('开始会议')
const preJoinCodeEditable = ref(false)
const preJoinMeetingCode = ref('')
const pendingMeetingId = ref<string | null>(null)
const busy = ref(false)

onMounted(() => {
  void meetingStore.loadHistory().catch((e: Error) => {
    toast.add({ title: e.message || '加载失败', color: 'error' })
  })
  const fromCalendar = typeof route.query.meetingId === 'string' ? route.query.meetingId : ''
  if (fromCalendar) {
    void (async () => {
      try {
        const m = await getMeeting(fromCalendar)
        pendingMeetingId.value = m.id
        preJoinTitle.value = m.title
        preJoinLabel.value = '加入会议'
        preJoinCodeEditable.value = false
        preJoinMeetingCode.value = m.meetingCode
        preJoinOpen.value = true
        await router.replace({ path: '/app/meeting' })
      } catch (e) {
        toast.add({
          title: e instanceof Error ? e.message : '无法打开会议',
          color: 'error'
        })
      }
    })()
  }
})

async function onStartInstant() {
  busy.value = true
  try {
    const m = await meetingStore.createInstant()
    pendingMeetingId.value = m.id
    preJoinTitle.value = m.title
    preJoinLabel.value = '开始会议'
    preJoinCodeEditable.value = false
    preJoinMeetingCode.value = m.meetingCode
    preJoinOpen.value = true
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '发起失败', color: 'error' })
  } finally {
    busy.value = false
  }
}

function openJoin() {
  pendingMeetingId.value = null
  preJoinTitle.value = '加入会议'
  preJoinLabel.value = '加入会议'
  preJoinCodeEditable.value = true
  preJoinMeetingCode.value = ''
  preJoinOpen.value = true
}

function openSchedule() {
  void router.push({
    path: '/app/calendar',
    query: { create: '1', videoMeeting: 'RELAYFLOW' }
  })
}

async function onPreJoinConfirm(opts: { audio: boolean, video: boolean, meetingCode: string }) {
  busy.value = true
  try {
    let id = pendingMeetingId.value
    if (preJoinCodeEditable.value || !id) {
      const code = opts.meetingCode.trim()
      if (!code) {
        toast.add({ title: '请输入会议号', color: 'warning' })
        return
      }
      const m = await meetingStore.resolveByCode(code)
      id = m.id
      pendingMeetingId.value = m.id
      preJoinTitle.value = m.title
    }
    preJoinOpen.value = false
    await router.push({
      path: `/app/meeting/${id}/room`,
      query: {
        audio: opts.audio ? '1' : '0',
        video: opts.video ? '1' : '0'
      }
    })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '加入失败', color: 'error' })
  } finally {
    busy.value = false
  }
}

function statusLabel(s: string) {
  if (s === 'LIVE') return '进行中'
  if (s === 'SCHEDULED') return '已预约'
  return '已结束'
}

function reopen(meetingId: string, title: string, code: string) {
  pendingMeetingId.value = meetingId
  preJoinTitle.value = title
  preJoinLabel.value = '进入会议'
  preJoinCodeEditable.value = false
  preJoinMeetingCode.value = code
  preJoinOpen.value = true
}

async function copyMeetingCode(code: string, event: Event) {
  event.stopPropagation()
  event.preventDefault()
  try {
    await navigator.clipboard.writeText(code)
    toast.add({ title: '会议号已复制', color: 'success' })
  } catch {
    toast.add({ title: '复制失败', color: 'error' })
  }
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
          <div
            v-for="item in meetingStore.history"
            :key="item.id"
            class="group flex w-full items-start gap-1 rounded-lg px-2 py-2 transition hover:bg-elevated"
          >
            <button
              type="button"
              class="min-w-0 flex-1 flex-col gap-0.5 rounded-md px-1 text-left"
              @click="item.status !== 'ENDED' ? reopen(item.id, item.title, item.meetingCode) : undefined"
            >
              <span class="block truncate text-sm font-medium text-highlighted">{{ item.title }}</span>
              <span class="block text-xs text-muted">
                {{ statusLabel(item.status) }} · {{ item.meetingCode }}
              </span>
            </button>
            <UButton
              color="neutral"
              variant="ghost"
              size="xs"
              icon="i-lucide-copy"
              class="mt-0.5 opacity-60 group-hover:opacity-100"
              title="复制会议号"
              @click="copyMeetingCode(item.meetingCode, $event)"
            />
          </div>
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

        <button
          type="button"
          class="flex aspect-[4/3] flex-col items-start justify-between rounded-2xl bg-indigo-600 p-5 text-left text-white shadow-sm transition hover:bg-indigo-700 disabled:opacity-60"
          :disabled="busy"
          @click="openJoin"
        >
          <UIcon name="i-lucide-plus" class="size-8" />
          <span class="text-lg font-semibold">加入会议</span>
        </button>

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
    v-model:meeting-code="preJoinMeetingCode"
    :title="preJoinTitle"
    :primary-label="preJoinLabel"
    :code-editable="preJoinCodeEditable"
    @confirm="onPreJoinConfirm"
  />
</template>

<route lang="yaml">
meta:
  layout: workspace
</route>
