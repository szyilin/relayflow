import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type {
  MeetingJoinCredentials,
  MeetingSchedulePayload,
  MeetingSummary
} from '../api/app/meeting'
import {
  createInstantMeeting,
  fetchJoinCredentials,
  fetchMeetingHistory,
  getMeetingByCode,
  scheduleMeeting
} from '../api/app/meeting'
import { ApiError } from '../api/request'

function toError(e: unknown, fallback: string): Error {
  if (e instanceof ApiError) {
    return new Error(e.message || fallback)
  }
  if (e instanceof Error) {
    return e
  }
  return new Error(fallback)
}

export const useMeetingStore = defineStore('meeting', () => {
  const history = ref<MeetingSummary[]>([])
  const loading = ref(false)
  const usingMock = computed(() => false)

  function resetForTenantSwitch() {
    history.value = []
    loading.value = false
  }

  async function loadHistory() {
    loading.value = true
    try {
      history.value = await fetchMeetingHistory()
    } catch (e) {
      throw toError(e, '加载会议历史失败')
    } finally {
      loading.value = false
    }
  }

  async function createInstant(title?: string): Promise<MeetingSummary> {
    try {
      const meeting = await createInstantMeeting({ title })
      history.value = [meeting, ...history.value.filter(m => m.id !== meeting.id)]
      return meeting
    } catch (e) {
      throw toError(e, '发起会议失败')
    }
  }

  async function schedule(payload: MeetingSchedulePayload): Promise<MeetingSummary> {
    try {
      const meeting = await scheduleMeeting(payload)
      history.value = [meeting, ...history.value.filter(m => m.id !== meeting.id)]
      return meeting
    } catch (e) {
      throw toError(e, '预约会议失败')
    }
  }

  async function resolveByCode(code: string): Promise<MeetingSummary> {
    try {
      return await getMeetingByCode(code.trim().toUpperCase())
    } catch (e) {
      throw toError(e, '会议号无效')
    }
  }

  async function getJoinCredentials(meetingId: string): Promise<MeetingJoinCredentials> {
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
