import { get, put } from '../request'

export interface UserProfile {
  userId: number
  username: string
  nickname: string
  avatar?: string
  tenantId: number
  tenantName: string
  tenantVerified: boolean
  isAdmin: boolean
}

export interface UserProfileUpdateReq {
  nickname?: string
  avatar?: string
}

export function getMyProfile(): Promise<UserProfile> {
  return get<UserProfile>('/app-api/system/user/profile')
}

export function updateMyProfile(data: UserProfileUpdateReq): Promise<UserProfile> {
  return put<UserProfile>('/app-api/system/user/profile', data)
}
