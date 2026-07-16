import { get, put } from '../request'

export interface ContactRemark {
  targetUserId: string
  remarkName: string
  description: string
}

export interface ContactRemarkUpdateReq {
  remarkName?: string
  description?: string
}

type RawContactRemark = Omit<ContactRemark, 'targetUserId'> & {
  targetUserId: number | string
}

function normalize(raw: RawContactRemark): ContactRemark {
  return {
    targetUserId: String(raw.targetUserId),
    remarkName: raw.remarkName ?? '',
    description: raw.description ?? ''
  }
}

export function getContactRemark(targetUserId: string): Promise<ContactRemark> {
  return get<RawContactRemark>(`/app-api/system/contact-remark/${targetUserId}`).then(normalize)
}

export function updateContactRemark(
  targetUserId: string,
  data: ContactRemarkUpdateReq
): Promise<ContactRemark> {
  return put<RawContactRemark>(`/app-api/system/contact-remark/${targetUserId}`, data).then(normalize)
}
