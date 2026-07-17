import { get, put } from '../request'

export interface CalendarPreferenceSettings {
  weekStartsOn?: number
  defaultEventDurationMinutes?: number
  defaultRemindBeforeMinutes?: number
  allDayRemindTime?: string
  dimPastEvents?: boolean
  showTaskLayer?: boolean
}

export interface TaskPreferenceSettings {
  /** -1 = 系统窗口不预填；0 = 不提醒；>0 = 截止前 N 分钟 */
  defaultRemindBeforeMinutes?: number
}

export interface UserPreferenceResp {
  schemaVersion: number
  settings: {
    general?: {
      themeMode?: 'light' | 'dark' | 'auto'
      themeColor?: string
    }
    im?: {
      chatBubbleLayout?: 'left' | 'split'
    }
    calendar?: CalendarPreferenceSettings
    task?: TaskPreferenceSettings
  }
}

export function getUserPreference(): Promise<UserPreferenceResp> {
  return get<UserPreferenceResp>('/app-api/system/user/preference')
}

export function updateUserPreference(body: UserPreferenceResp): Promise<UserPreferenceResp> {
  return put<UserPreferenceResp>('/app-api/system/user/preference', body)
}
