import { get, put } from '../request'

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
  }
}

export function getUserPreference(): Promise<UserPreferenceResp> {
  return get<UserPreferenceResp>('/app-api/system/user/preference')
}

export function updateUserPreference(body: UserPreferenceResp): Promise<UserPreferenceResp> {
  return put<UserPreferenceResp>('/app-api/system/user/preference', body)
}
