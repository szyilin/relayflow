import { get } from '../request'

export interface UserPageItem {
  id: number
  username: string
  nickname: string
  dept?: string
  status?: number
  createTime?: string
}

export interface UserPageResult {
  list: UserPageItem[]
  total: number
}

export interface UserPageQuery {
  pageNo: number
  pageSize: number
  keyword?: string
}

export function getUserPage(query: UserPageQuery): Promise<UserPageResult> {
  return get<UserPageResult>('/admin-api/system/user/page', {
    params: {
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      keyword: query.keyword?.trim() || undefined
    }
  })
}
