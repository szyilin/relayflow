/**
 * Meeting app-api types + thin HTTP wrappers.
 * Contract: openspec/lanes/meeting/contract.md
 */

import { get, post } from '../request'
import { getAccessToken } from '../../utils/session-storage'
import { normalizeId } from '../../utils/id'

export type MeetingStatus = 'SCHEDULED' | 'LIVE' | 'ENDED'

export interface MeetingSummary {
  id: string
  meetingCode: string
  title: string
  status: MeetingStatus
  hostUserId: string
  hostNickname?: string
  scheduledStart?: string | null
  scheduledEnd?: string | null
  calendarEventId?: string | null
  createTime?: string
}

export interface MeetingJoinCredentials {
  meetingId: string
  meetingCode: string
  title: string
  hostUserId: string
  livekitUrl: string
  token: string
  roomName: string
  identity: string
  /** Unix epoch seconds when the LiveKit JWT expires */
  expiresAtEpochSeconds?: number | null
}

export interface MeetingInstantPayload {
  title?: string
}

export interface MeetingSchedulePayload {
  title: string
  scheduledStart: string
  scheduledEnd: string
}

function asId(value: unknown): string {
  return normalizeId(value) ?? ''
}

function asIso(value: unknown): string | null | undefined {
  if (value == null) {
    return value as null | undefined
  }
  if (typeof value === 'string') {
    return value
  }
  if (value instanceof Date) {
    return value.toISOString()
  }
  return String(value)
}

function normalizeSummary(raw: MeetingSummary & Record<string, unknown>): MeetingSummary {
  return {
    id: asId(raw.id),
    meetingCode: String(raw.meetingCode ?? ''),
    title: String(raw.title ?? ''),
    status: raw.status as MeetingStatus,
    hostUserId: asId(raw.hostUserId),
    hostNickname: raw.hostNickname as string | undefined,
    scheduledStart: asIso(raw.scheduledStart) ?? null,
    scheduledEnd: asIso(raw.scheduledEnd) ?? null,
    calendarEventId: raw.calendarEventId != null ? asId(raw.calendarEventId) : null,
    createTime: asIso(raw.createTime) ?? undefined
  }
}

function normalizeCredentials(raw: MeetingJoinCredentials & Record<string, unknown>): MeetingJoinCredentials {
  const expiresRaw = raw.expiresAtEpochSeconds
  let expiresAtEpochSeconds: number | null = null
  if (typeof expiresRaw === 'number' && Number.isFinite(expiresRaw)) {
    expiresAtEpochSeconds = expiresRaw
  } else if (expiresRaw != null && String(expiresRaw).trim() !== '') {
    const n = Number(expiresRaw)
    expiresAtEpochSeconds = Number.isFinite(n) ? n : null
  }
  return {
    meetingId: asId(raw.meetingId),
    meetingCode: String(raw.meetingCode ?? ''),
    title: String(raw.title ?? ''),
    hostUserId: asId(raw.hostUserId),
    livekitUrl: String(raw.livekitUrl ?? ''),
    token: String(raw.token ?? ''),
    roomName: String(raw.roomName ?? ''),
    identity: String(raw.identity ?? ''),
    expiresAtEpochSeconds
  }
}

export async function createInstantMeeting(payload: MeetingInstantPayload = {}) {
  const data = await post<MeetingSummary>('/app-api/meeting/instant', payload)
  return normalizeSummary(data as MeetingSummary & Record<string, unknown>)
}

export async function scheduleMeeting(payload: MeetingSchedulePayload) {
  const data = await post<MeetingSummary>('/app-api/meeting/schedule', payload)
  return normalizeSummary(data as MeetingSummary & Record<string, unknown>)
}

export async function getMeeting(id: string) {
  const data = await get<MeetingSummary>('/app-api/meeting/get', { params: { id } })
  return normalizeSummary(data as MeetingSummary & Record<string, unknown>)
}

export async function getMeetingByCode(code: string) {
  const data = await get<MeetingSummary>('/app-api/meeting/get-by-code', { params: { code } })
  return normalizeSummary(data as MeetingSummary & Record<string, unknown>)
}

export async function fetchJoinCredentials(body: { meetingId?: string, meetingCode?: string }) {
  const data = await post<MeetingJoinCredentials>('/app-api/meeting/join-credentials', body)
  return normalizeCredentials(data as MeetingJoinCredentials & Record<string, unknown>)
}

export function leaveMeeting(meetingId: string) {
  return post<boolean>('/app-api/meeting/leave', { meetingId })
}

/**
 * Best-effort leave for page teardown / SPA unmount (keepalive fetch).
 * Does not throw; ignores network failures.
 */
export function leaveMeetingKeepalive(meetingId: string): void {
  if (!meetingId) {
    return
  }
  const base = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')
  const token = getAccessToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json'
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  try {
    void fetch(`${base}/app-api/meeting/leave`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ meetingId }),
      keepalive: true,
      credentials: 'same-origin'
    })
  } catch {
    // ignore
  }
}

export function endMeeting(meetingId: string) {
  return post<boolean>('/app-api/meeting/end', { meetingId })
}

export async function fetchMeetingHistory() {
  const data = await get<MeetingSummary[]>('/app-api/meeting/history')
  return (data ?? []).map(item => normalizeSummary(item as MeetingSummary & Record<string, unknown>))
}
