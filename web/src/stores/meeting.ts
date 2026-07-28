import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type {
  MeetingJoinCredentials,
  MeetingSchedulePayload,
  MeetingSummary
} from '../api/app/meeting'
import { ApiError } from '../api/request'
import { useAuthStore } from './auth'

/** -web Mock flag; integrate MUST set false and delete seed helpers. */
const USE_MEETING_MOCK = true

function toError(e: unknown, fallback: string): Error {
  if (e instanceof ApiError) {
    return new Error(e.message || fallback)
  }
  if (e instanceof Error) {
    return e
  }
  return new Error(fallback)
}

function randomCode(): string {
  const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  let out = ''
  for (let i = 0; i < 8; i++) {
    out += alphabet[Math.floor(Math.random() * alphabet.length)]
  }
  return out
}

function mockCredentials(meeting: MeetingSummary): MeetingJoinCredentials {
  const auth = useAuthStore()
  return {
    meetingId: meeting.id,
    meetingCode: meeting.meetingCode,
    title: meeting.title,
    livekitUrl: '',
    token: '',
    roomName: `mtg_${meeting.id}`,
    identity: auth.userId ? String(auth.userId) : 'local'
  }
}

export const useMeetingStore = defineStore('meeting', () => {
  const history = ref<MeetingSummary[]>([])
  const loading = ref(false)
  const usingMock = computed(() => USE_MEETING_MOCK)

  function resetForTenantSwitch() {
    history.value = []
    loading.value = false
  }

  function seedHistoryIfEmpty() {
    if (!USE_MEETING_MOCK || history.value.length > 0) {
      return
    }
    const auth = useAuthStore()
    const uid = auth.userId ? String(auth.userId) : '1'
    history.value = [
      {
        id: '90001',
        meetingCode: 'DEMO0001',
        title: '产品同步会',
        status: 'ENDED',
        hostUserId: uid,
        hostNickname: '我',
        scheduledStart: new Date(Date.now() - 86400000).toISOString(),
        createTime: new Date(Date.now() - 86400000).toISOString()
      }
    ]
  }

  async function loadHistory() {
    loading.value = true
    try {
      if (USE_MEETING_MOCK) {
        seedHistoryIfEmpty()
        return
      }
      const { fetchMeetingHistory } = await import('../api/app/meeting')
      history.value = await fetchMeetingHistory()
    } catch (e) {
      throw toError(e, '加载会议历史失败')
    } finally {
      loading.value = false
    }
  }

  async function createInstant(title?: string): Promise<MeetingSummary> {
    if (USE_MEETING_MOCK) {
      const auth = useAuthStore()
      const meeting: MeetingSummary = {
        id: String(Date.now()),
        meetingCode: randomCode(),
        title: title?.trim() || '即时会议',
        status: 'LIVE',
        hostUserId: auth.userId ? String(auth.userId) : '1',
        createTime: new Date().toISOString()
      }
      history.value = [meeting, ...history.value]
      return meeting
    }
    const { createInstantMeeting } = await import('../api/app/meeting')
    try {
      return await createInstantMeeting({ title })
    } catch (e) {
      throw toError(e, '发起会议失败')
    }
  }

  async function schedule(payload: MeetingSchedulePayload): Promise<MeetingSummary> {
    if (USE_MEETING_MOCK) {
      const auth = useAuthStore()
      const meeting: MeetingSummary = {
        id: String(Date.now()),
        meetingCode: randomCode(),
        title: payload.title.trim() || '预约会议',
        status: 'SCHEDULED',
        hostUserId: auth.userId ? String(auth.userId) : '1',
        scheduledStart: payload.scheduledStart,
        scheduledEnd: payload.scheduledEnd,
        calendarEventId: `cal-mock-${Date.now()}`,
        createTime: new Date().toISOString()
      }
      history.value = [meeting, ...history.value]
      return meeting
    }
    const { scheduleMeeting } = await import('../api/app/meeting')
    try {
      return await scheduleMeeting(payload)
    } catch (e) {
      throw toError(e, '预约会议失败')
    }
  }

  async function resolveByCode(code: string): Promise<MeetingSummary> {
    const normalized = code.trim().toUpperCase()
    if (USE_MEETING_MOCK) {
      const hit = history.value.find(m => m.meetingCode === normalized)
      if (hit) {
        return hit
      }
      const auth = useAuthStore()
      const meeting: MeetingSummary = {
        id: String(Date.now()),
        meetingCode: normalized || randomCode(),
        title: '加入的会议',
        status: 'LIVE',
        hostUserId: auth.userId ? String(auth.userId) : '1'
      }
      history.value = [meeting, ...history.value]
      return meeting
    }
    const { getMeetingByCode } = await import('../api/app/meeting')
    try {
      return await getMeetingByCode(normalized)
    } catch (e) {
      throw toError(e, '会议号无效')
    }
  }

  async function getJoinCredentials(meetingId: string): Promise<MeetingJoinCredentials> {
    if (USE_MEETING_MOCK) {
      const meeting = history.value.find(m => m.id === meetingId)
      if (!meeting) {
        throw new Error('会议不存在')
      }
      return mockCredentials(meeting)
    }
    const { fetchJoinCredentials } = await import('../api/app/meeting')
    try {
      return await fetchJoinCredentials({ meetingId })
    } catch (e) {
      throw toError(e, '获取入会凭证失败')
    }
  }

  return {
    history,
    loading,
    usingMock,
    resetForTenantSwitch,
    loadHistory,
    createInstant,
    schedule,
    resolveByCode,
    getJoinCredentials
  }
})
