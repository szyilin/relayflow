import { get, put } from '../request'

export type ThemeMode = 'light' | 'dark' | 'auto'
export type ChatBubbleLayout = 'left' | 'split'

export interface CalendarPreferenceSettings {
  weekStartsOn: number
  defaultEventDurationMinutes: number
  defaultRemindBeforeMinutes: number
  allDayRemindTime: string
  dimPastEvents: boolean
  showTaskLayer: boolean
}

export interface TaskPreferenceSettings {
  /** -1 = 系统窗口不预填；0 = 不提醒；>0 = 截止前 N 分钟 */
  defaultRemindBeforeMinutes: number
}

/** Canonical full settings shape (store + UI). */
export interface UserPreferenceSettings {
  general: {
    themeMode: ThemeMode
    themeColor: string
  }
  im: {
    chatBubbleLayout: ChatBubbleLayout
  }
  calendar: CalendarPreferenceSettings
  task: TaskPreferenceSettings
}

/** API may return partial nested objects; client merges with defaults. */
export interface UserPreferenceResp {
  schemaVersion: number
  settings: {
    general?: Partial<UserPreferenceSettings['general']>
    im?: Partial<UserPreferenceSettings['im']>
    calendar?: Partial<CalendarPreferenceSettings>
    task?: Partial<TaskPreferenceSettings>
  }
}

export function getUserPreference(): Promise<UserPreferenceResp> {
  return get<UserPreferenceResp>('/app-api/system/user/preference')
}

export function updateUserPreference(body: {
  schemaVersion: number
  settings: UserPreferenceSettings
}): Promise<UserPreferenceResp> {
  return put<UserPreferenceResp>('/app-api/system/user/preference', body)
}
