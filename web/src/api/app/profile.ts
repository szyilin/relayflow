import { get, put } from '../request'

export interface UserProfile {
  userId: string
  username: string
  nickname: string
  avatar?: string
  signature?: string
  coverFileId?: string
  tenantId: string
  tenantName: string
  tenantVerified: boolean
  isAdmin: boolean
}

export interface UserProfileUpdateReq {
  nickname?: string
  avatar?: string
  signature?: string
  coverFileId?: string
}

type RawUserProfile = Omit<UserProfile, 'userId' | 'tenantId'> & {
  userId: number | string
  tenantId: number | string
}

function normalizeProfile(raw: RawUserProfile): UserProfile {
  return {
    ...raw,
    userId: String(raw.userId),
    tenantId: String(raw.tenantId),
    signature: raw.signature ?? '',
    coverFileId: raw.coverFileId || undefined
  }
}

export function getMyProfile(): Promise<UserProfile> {
  return get<RawUserProfile>('/app-api/system/user/profile').then(normalizeProfile)
}

export function getMemberProfile(userId: string): Promise<UserProfile> {
  return get<RawUserProfile>(`/app-api/system/user/profile/${userId}`).then(normalizeProfile)
}

export function updateMyProfile(data: UserProfileUpdateReq): Promise<UserProfile> {
  return put<RawUserProfile>('/app-api/system/user/profile', data).then(normalizeProfile)
}
