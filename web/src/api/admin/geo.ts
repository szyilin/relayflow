/** Admin geo region — lazy children for China cascader. */

import { get } from '../request'

export interface GeoRegionNode {
  id: string
  code: string
  name: string
  level: string
  parentId: string | null
  hasChildren: boolean
}

function normalizeNode(raw: Record<string, unknown>): GeoRegionNode {
  const parentId = raw.parentId
  return {
    id: String(raw.id ?? ''),
    code: String(raw.code ?? ''),
    name: String(raw.name ?? ''),
    level: String(raw.level ?? ''),
    parentId: parentId == null || parentId === 0 ? null : String(parentId),
    hasChildren: Boolean(raw.hasChildren)
  }
}

export function getGeoChildren(parentId?: string | null): Promise<GeoRegionNode[]> {
  const params: Record<string, string> = {}
  if (parentId && parentId !== '0') {
    params.parentId = parentId
  }
  return get<Record<string, unknown>[]>('/admin-api/infra/geo/children', { params })
    .then(rows => (rows ?? []).map(row => normalizeNode(row)))
}
