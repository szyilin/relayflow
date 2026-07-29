import { get, post, put } from '../request'

export interface DeptTreeItem {
  id: string
  parentId: string
  name: string
  sort: number
}

export interface ContactItem {
  id: string
  nickname: string
  username: string
  deptId: string
  deptName: string
  avatarText: string
}

export interface ExternalContactItem {
  id: string
  userId: string
  displayName: string
  orgLabel: string
  avatarText: string
  sortLetter?: string
}

export type ContactRequestStatus = 'pending' | 'accepted' | 'rejected'

export interface ContactRequestItem {
  id: string
  fromUserId: string
  displayName: string
  subtitle: string
  avatarText: string
  status: ContactRequestStatus
  createTime?: string
}

export interface StarredContactItem {
  id: string
  userId: string
  displayName: string
  orgLabel: string
  avatarText: string
  username?: string
}

type RawDeptTreeItem = Omit<DeptTreeItem, 'id' | 'parentId'> & {
  id: number | string
  parentId: number | string
}

type RawContactItem = Omit<ContactItem, 'id' | 'deptId'> & {
  id: number | string
  deptId: number | string
}

type RawExternalContactItem = Omit<ExternalContactItem, 'id' | 'userId'> & {
  id: number | string
  userId: number | string
}

type RawContactRequestItem = Omit<ContactRequestItem, 'id' | 'fromUserId'> & {
  id: number | string
  fromUserId: number | string
}

type RawStarredContactItem = Omit<StarredContactItem, 'id' | 'userId'> & {
  id: number | string
  userId: number | string
}

function normalizeDept(item: RawDeptTreeItem): DeptTreeItem {
  return {
    ...item,
    id: String(item.id),
    parentId: String(item.parentId ?? '0')
  }
}

function normalizeContact(item: RawContactItem): ContactItem {
  return {
    ...item,
    id: String(item.id),
    deptId: String(item.deptId)
  }
}

function normalizeExternal(item: RawExternalContactItem): ExternalContactItem {
  return {
    id: String(item.id),
    userId: String(item.userId),
    displayName: item.displayName ?? '',
    orgLabel: item.orgLabel ?? '',
    avatarText: item.avatarText || (item.displayName?.slice(0, 1) || '?'),
    sortLetter: item.sortLetter
  }
}

function normalizeRequest(item: RawContactRequestItem): ContactRequestItem {
  return {
    id: String(item.id),
    fromUserId: String(item.fromUserId),
    displayName: item.displayName ?? '',
    subtitle: item.subtitle ?? '',
    avatarText: item.avatarText || (item.displayName?.slice(0, 1) || '?'),
    status: item.status,
    createTime: item.createTime
  }
}

function normalizeStarred(item: RawStarredContactItem): StarredContactItem {
  return {
    id: String(item.id),
    userId: String(item.userId),
    displayName: item.displayName ?? '',
    orgLabel: item.orgLabel ?? '',
    avatarText: item.avatarText || (item.displayName?.slice(0, 1) || '?'),
    username: item.username
  }
}

export function getDeptTree(): Promise<DeptTreeItem[]> {
  return get<RawDeptTreeItem[]>('/app-api/system/dept/tree').then(list => list.map(normalizeDept))
}

export function getContactsByDept(deptId: string, keyword?: string): Promise<ContactItem[]> {
  return get<RawContactItem[]>('/app-api/system/user/list-by-dept', {
    params: {
      deptId,
      keyword: keyword?.trim() || undefined
    }
  }).then(list => list.map(normalizeContact))
}

export function listExternalContacts(): Promise<ExternalContactItem[]> {
  return get<RawExternalContactItem[]>('/app-api/system/contact/external/list')
    .then(list => (list ?? []).map(normalizeExternal))
}

export function listContactRequests(): Promise<ContactRequestItem[]> {
  return get<RawContactRequestItem[]>('/app-api/system/contact/request/list')
    .then(list => (list ?? []).map(normalizeRequest))
}

export function acceptContactRequest(requestId: string): Promise<ContactRequestItem> {
  return post<RawContactRequestItem>(`/app-api/system/contact/request/${requestId}/accept`)
    .then(normalizeRequest)
}

export function listStarredContacts(): Promise<StarredContactItem[]> {
  return get<RawStarredContactItem[]>('/app-api/system/contact/starred/list')
    .then(list => (list ?? []).map(normalizeStarred))
}

export function updateContactStarred(userId: string, starred: boolean): Promise<boolean> {
  return put<boolean>(`/app-api/system/contact/starred/${userId}`, { starred })
}

/** Account hit from exact mobile/email search. */
export interface ContactAccountSearchHit {
  userId: string
  displayName: string
  orgLabel: string
  avatarText: string
  alreadyAdded: boolean
}

export interface CreateContactRequestPayload {
  targetUserId: string
  message?: string
}

type RawContactAccountSearchHit = Omit<ContactAccountSearchHit, 'userId' | 'alreadyAdded'> & {
  userId: number | string
  alreadyAdded?: boolean
}

function normalizeSearchHit(item: RawContactAccountSearchHit): ContactAccountSearchHit {
  return {
    userId: String(item.userId),
    displayName: item.displayName ?? '',
    orgLabel: item.orgLabel ?? '',
    avatarText: item.avatarText || (item.displayName?.slice(0, 1) || '?'),
    alreadyAdded: item.alreadyAdded === true
  }
}

/** Exact match: pass either `mobile` or `email`. */
export function searchContactAccounts(query: {
  mobile?: string
  email?: string
}): Promise<ContactAccountSearchHit[]> {
  return get<RawContactAccountSearchHit[]>('/app-api/system/contact/search-account', {
    params: {
      mobile: query.mobile || undefined,
      email: query.email || undefined
    }
  }).then(list => (list ?? []).map(normalizeSearchHit))
}

export function createContactRequest(
  payload: CreateContactRequestPayload
): Promise<ContactRequestItem> {
  return post<RawContactRequestItem>('/app-api/system/contact/request', {
    targetUserId: payload.targetUserId,
    message: payload.message?.trim() || undefined
  }).then(normalizeRequest)
}

export function rejectContactRequest(requestId: string): Promise<ContactRequestItem> {
  return post<RawContactRequestItem>(`/app-api/system/contact/request/${requestId}/reject`)
    .then(normalizeRequest)
}
