/**
 * Meeting app-api types + thin HTTP wrappers.
 * -web: store may Mock; integrate wires real endpoints.
 * Contract: openspec/lanes/meeting/contract.md
 */

import { get, post } from '../request'

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
  livekitUrl: string
  token: string
  roomName: string
  identity: string
}

export interface MeetingInstantPayload {
  title?: string
}

export interface MeetingSchedulePayload {
  title: string
  scheduledStart: string
  scheduledEnd: string
}

export function createInstantMeeting(payload: MeetingInstantPayload = {}) {
  return post<MeetingSummary>('/app-api/meeting/instant', payload)
}

export function scheduleMeeting(payload: MeetingSchedulePayload) {
  return post<MeetingSummary>('/app-api/meeting/schedule', payload)
}

export function getMeeting(id: string) {
  return get<MeetingSummary>('/app-api/meeting/get', { params: { id } })
}

export function getMeetingByCode(code: string) {
  return get<MeetingSummary>('/app-api/meeting/get-by-code', { params: { code } })
}

export function fetchJoinCredentials(body: { meetingId?: string, meetingCode?: string }) {
  return post<MeetingJoinCredentials>('/app-api/meeting/join-credentials', body)
}

export function leaveMeeting(meetingId: string) {
  return post<boolean>('/app-api/meeting/leave', { meetingId })
}

export function endMeeting(meetingId: string) {
  return post<boolean>('/app-api/meeting/end', { meetingId })
}

export function fetchMeetingHistory() {
  return get<MeetingSummary[]>('/app-api/meeting/history')
}
