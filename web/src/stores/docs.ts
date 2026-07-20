import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  createLibraryNode,
  deleteLibraryNode,
  exportDocumentMarkdown,
  getDocument,
  listLibraryTree,
  listRecentDocuments,
  saveDocumentBody,
  updateLibraryNode,
  type DocsDocument,
  type DocsLibraryNode,
  type DocsRecentItem,
  type TipTapDocJson
} from '../api/app/docs'
import { ApiError } from '../api/request'

/** Business error codes from relayflow-module-docs ErrorCodeConstants */
const DOC_VERSION_CONFLICT = 1_006_001_003

function cloneDoc(body: TipTapDocJson): TipTapDocJson {
  return JSON.parse(JSON.stringify(body)) as TipTapDocJson
}

function toStoreError(error: unknown, fallback: string): Error {
  if (error instanceof ApiError) {
    return new Error(error.message || fallback)
  }
  if (error instanceof Error) {
    return error
  }
  return new Error(fallback)
}

function findNodeInTree(
  nodes: DocsLibraryNode[],
  predicate: (node: DocsLibraryNode) => boolean
): DocsLibraryNode | undefined {
  for (const node of nodes) {
    if (predicate(node)) {
      return node
    }
    const child = findNodeInTree(node.children, predicate)
    if (child) {
      return child
    }
  }
  return undefined
}

function patchTreeTitle(nodes: DocsLibraryNode[], objectId: string, title: string): DocsLibraryNode[] {
  return nodes.map((node) => {
    const children = patchTreeTitle(node.children, objectId, title)
    if (node.objectId === objectId) {
      return { ...node, title, children }
    }
    if (children !== node.children) {
      return { ...node, children }
    }
    return node
  })
}

export const useDocsStore = defineStore('docs', () => {
  const panel = ref<'library' | 'recent'>('library')
  const loading = ref(false)
  const saving = ref(false)
  const error = ref<string | null>(null)
  const hydrated = ref(false)

  const treeRoots = ref<DocsLibraryNode[]>([])
  const recentItems = ref<DocsRecentItem[]>([])

  const activeObjectId = ref<string | null>(null)
  const activeDocument = ref<DocsDocument | null>(null)

  function findNodeByObjectId(objectId: string): DocsLibraryNode | undefined {
    return findNodeInTree(treeRoots.value, n => n.objectId === objectId)
  }

  async function loadTree() {
    const resp = await listLibraryTree()
    treeRoots.value = resp.nodes ?? []
  }

  async function loadRecent(limit = 20) {
    recentItems.value = await listRecentDocuments(limit)
  }

  async function ensureHydrated() {
    if (hydrated.value) {
      return
    }
    loading.value = true
    error.value = null
    try {
      await Promise.all([loadTree(), loadRecent()])
      hydrated.value = true
    } catch (e) {
      error.value = toStoreError(e, '加载文档库失败').message
      throw e
    } finally {
      loading.value = false
    }
  }

  async function createPage(parentId: string | null = null, title = '未命名文档') {
    await ensureHydrated()
    const created = await createLibraryNode({ parentId, title })
    await loadTree()
    await openDocument(created.objectId)
    return { nodeId: created.nodeId, objectId: created.objectId }
  }

  async function renameNode(nodeId: string, title: string) {
    const next = title.trim() || '未命名文档'
    const updated = await updateLibraryNode(nodeId, { title: next })
    treeRoots.value = patchTreeTitle(treeRoots.value, updated.objectId, updated.title)
    if (activeDocument.value?.objectId === updated.objectId) {
      activeDocument.value = { ...activeDocument.value, title: updated.title }
    }
    recentItems.value = recentItems.value.map(item =>
      item.objectId === updated.objectId ? { ...item, title: updated.title } : item
    )
  }

  async function moveNode(nodeId: string, parentId: string | null) {
    await updateLibraryNode(nodeId, { parentId })
    await loadTree()
  }

  async function removeNode(nodeId: string) {
    const node = findNodeInTree(treeRoots.value, n => n.nodeId === nodeId)
    const removedObjectId = node?.objectId
    await deleteLibraryNode(nodeId)
    await loadTree()
    await loadRecent()
    if (removedObjectId && activeObjectId.value === removedObjectId) {
      clearActive()
    }
  }

  async function openDocument(objectId: string) {
    const doc = await getDocument(objectId)
    activeObjectId.value = objectId
    activeDocument.value = {
      ...doc,
      body: cloneDoc(doc.body)
    }
    await loadRecent()
  }

  async function saveTitle(objectId: string, title: string) {
    const node = findNodeByObjectId(objectId)
    if (!node) {
      throw new Error('文档不存在')
    }
    await renameNode(node.nodeId, title)
  }

  async function saveBody(objectId: string, body: TipTapDocJson, contentVersion: number) {
    saving.value = true
    try {
      const resp = await saveDocumentBody(objectId, { body, contentVersion })
      if (activeDocument.value?.objectId === objectId) {
        activeDocument.value = {
          ...activeDocument.value,
          body: cloneDoc(body),
          contentVersion: resp.contentVersion
        }
      }
      return resp.contentVersion
    } catch (e) {
      if (e instanceof ApiError && e.code === DOC_VERSION_CONFLICT) {
        throw toStoreError(e, '文档已被更新，请刷新后重试')
      }
      throw toStoreError(e, '保存失败')
    } finally {
      saving.value = false
    }
  }

  async function exportMarkdown(objectId: string): Promise<string> {
    const resp = await exportDocumentMarkdown(objectId)
    return resp.markdown
  }

  function clearActive() {
    activeObjectId.value = null
    activeDocument.value = null
  }

  function resetLocal() {
    hydrated.value = false
    treeRoots.value = []
    recentItems.value = []
    clearActive()
    error.value = null
    loading.value = false
    saving.value = false
  }

  return {
    panel,
    loading,
    saving,
    error,
    treeRoots,
    recentItems,
    activeObjectId,
    activeDocument,
    ensureHydrated,
    loadTree,
    loadRecent,
    createPage,
    renameNode,
    moveNode,
    removeNode,
    openDocument,
    saveTitle,
    saveBody,
    exportMarkdown,
    clearActive,
    resetLocal,
    findNodeByObjectId
  }
})
