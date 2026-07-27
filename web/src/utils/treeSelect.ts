import type { DocsLibraryNode } from '../api/app/docs'
import { listDriveFolders } from '../api/app/docs-drive'

export interface AppTreeSelectItem {
  id: string
  label: string
  /** Bound model value; may be null (e.g. drive root). */
  value: string | null
  icon?: string
  disabled?: boolean
  defaultExpanded?: boolean
  children?: AppTreeSelectItem[]
}

/** Library page tree for「移动到… / 移回文档库」parent pickers. */
export function libraryNodesToTreeSelectItems(
  roots: DocsLibraryNode[],
  options?: { excludeNodeId?: string | null }
): AppTreeSelectItem[] {
  const excludeId = options?.excludeNodeId ?? null

  function convert(nodes: DocsLibraryNode[]): AppTreeSelectItem[] {
    const out: AppTreeSelectItem[] = []
    for (const n of nodes) {
      if (excludeId && n.nodeId === excludeId) {
        continue
      }
      const children = convert(n.children ?? [])
      out.push({
        id: n.nodeId,
        label: n.title,
        value: n.nodeId,
        icon: 'i-lucide-file-text',
        ...(children.length ? { children, defaultExpanded: true } : {})
      })
    }
    return out
  }

  return [
    {
      id: '__library_root__',
      label: '根目录',
      value: null,
      icon: 'i-lucide-folder'
    },
    ...convert(roots)
  ]
}

/** Recursively load personal Drive folders for「移动到云盘」. */
export async function loadDriveFolderTreeSelectItems(): Promise<AppTreeSelectItem[]> {
  async function load(parentId: string | null): Promise<AppTreeSelectItem[]> {
    const resp = await listDriveFolders(parentId)
    const folders = resp.folders ?? []
    const items: AppTreeSelectItem[] = []
    for (const f of folders) {
      const children = await load(f.folderId)
      items.push({
        id: f.folderId,
        label: f.name,
        value: f.folderId,
        icon: 'i-lucide-folder',
        ...(children.length ? { children, defaultExpanded: true } : {})
      })
    }
    return items
  }

  const children = await load(null)
  return [
    {
      id: '__drive_root__',
      label: '我的文件夹（根）',
      value: null,
      icon: 'i-lucide-hard-drive'
    },
    ...children
  ]
}
