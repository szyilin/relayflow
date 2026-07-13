import { get } from '../request'

export type WorkspaceSearchGroupType = 'member' | 'conversation' | 'task'

export interface WorkspaceSearchItem {
  id: string
  title: string
  subtitle?: string
  route: string
  entityType: string
  entityId: string
}

export interface WorkspaceSearchGroup {
  type: WorkspaceSearchGroupType
  label: string
  items: WorkspaceSearchItem[]
}

export interface WorkspaceSearchResult {
  keyword: string
  groups: WorkspaceSearchGroup[]
}

export async function searchWorkspace(keyword: string, limitPerGroup = 5): Promise<WorkspaceSearchResult> {
  const trimmed = keyword.trim()
  const data = await get<WorkspaceSearchResult>('/app-api/infra/workspace-search', {
    params: { keyword: trimmed, limitPerGroup }
  })
  return {
    keyword: data.keyword ?? trimmed,
    groups: (data.groups ?? []).map(group => ({
      type: group.type,
      label: group.label,
      items: (group.items ?? []).map(item => ({
        id: String(item.id),
        title: item.title,
        subtitle: item.subtitle,
        route: item.route,
        entityType: item.entityType,
        entityId: String(item.entityId)
      }))
    }))
  }
}
